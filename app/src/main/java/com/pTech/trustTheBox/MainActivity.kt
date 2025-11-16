package com.pTech.trustTheBox

import TrustTheBoxTheme
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pTech.trustTheBox.di.data.PassphraseDataStore
import com.pTech.trustTheBox.model.MItem
import com.pTech.trustTheBox.sdk.KeepScreenOn
import com.pTech.trustTheBox.sdk.TrustTheBox.decryptFile
import com.pTech.trustTheBox.sdk.TrustTheBox.encryptFiles
import com.pTech.trustTheBox.ui.theme.component.PassphraseBottomBar
import com.pTech.trustTheBox.ui.theme.component.PassphraseDialog
import com.pTech.trustTheBox.ui.theme.screens.mainScreen.MainScreen
import com.pTech.trustTheBox.ui.theme.screens.mediaScreen.MediaScreen
import com.pTech.trustTheBox.ui.theme.screens.viewerScreen.ViewerScreen
import com.pTech.trustTheBox.util.AdManager
import com.pTech.trustTheBox.util.BillingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

class MainActivity : ComponentActivity() {

    private lateinit var navController: androidx.navigation.NavHostController
    private lateinit var selectZipLauncher: ActivityResultLauncher<Intent>

    private val mediaFiles = mutableStateOf<List<MItem>>(emptyList())
    private var currentPassphrase by mutableStateOf("")

    private var showPassphraseDialog by mutableStateOf(false)
    private var pendingEncryptUris by mutableStateOf<List<Uri>?>(null)
    private var pendingDecryptUri by mutableStateOf<Uri?>(null)

    private val screenshotObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            checkForScreenshot()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillingManager.init(this, this)
        lifecycleScope.launch {
            currentPassphrase = PassphraseDataStore.getPassphrase(this@MainActivity).first()
        }

        val selectFileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.let { data ->
                        val uris = mutableListOf<Uri>()
                        data.clipData?.let {
                            for (i in 0 until it.itemCount) uris.add(it.getItemAt(i).uri)
                        } ?: data.data?.let { uris.add(it) }

                        if (uris.isNotEmpty()) {
                            if (currentPassphrase.isBlank()) {
                                showPassphraseDialog = true
                                pendingEncryptUris = uris
                            } else {
                                AdManager.showInterstitial(this) {
                                    encryptSelectedFiles(uris)
                                }
                            }
                        }
                    }
                }
            }

        selectZipLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val uri = result.data?.data ?: return@registerForActivityResult
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    if (currentPassphrase.isBlank()) {
                        showPassphraseDialog = true
                        pendingDecryptUri = uri
                    } else {
                        AdManager.showInterstitial(this) {
                            decryptArchive(uri)
                        }
                    }
                }
            }

        setContent {
            val passphraseFlow by PassphraseDataStore.getPassphrase(this@MainActivity)
                .collectAsState(initial = currentPassphrase)
            currentPassphrase = passphraseFlow

            navController = rememberNavController()
            KeepScreenOn()

            LaunchedEffect(navController) {
                navController.addOnDestinationChangedListener { _, dest, _ ->
                    when (dest.route) {
                        "media", "viewer/{index}" -> {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            registerScreenshotObserver()
                        }

                        else -> {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            unregisterScreenshotObserver()
                        }
                    }
                }
            }

            TrustTheBoxTheme {
                Scaffold(
                    bottomBar = {
                        PassphraseBottomBar(
                            hasPassphrase = currentPassphrase.isNotBlank(),
                            onClick = { showPassphraseDialog = true }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        NavHost(navController = navController, startDestination = "main") {
                            composable("main") {
                                MainScreen(
                                    selectFileLauncher = selectFileLauncher,
                                    selectZipLauncher = selectZipLauncher,
                                    hasPassphrase = currentPassphrase.isNotBlank()
                                )
                            }
                            composable("media") {
                                MediaScreen(
                                    mediaFiles = mediaFiles.value,
                                    navController = navController
                                )
                            }
                            composable("viewer/{id}") { entry ->
                                val idStr = entry.arguments?.getString("id")
                                val id = idStr?.toLongOrNull() ?: 0L
                                val item = mediaFiles.value.firstOrNull { it.id == id }
                                if (item != null) {
                                    ViewerScreen(item = item)
                                } else {
                                    Text("Файл не знайдено")
                                }
                            }
                        }

                        if (showPassphraseDialog) {
                            PassphraseDialog(
                                currentValue = currentPassphrase,
                                onSave = { passphrase ->
                                    val trimmed = passphrase.trim()
                                    currentPassphrase = trimmed
                                    lifecycleScope.launch {
                                        PassphraseDataStore.savePassphrase(
                                            this@MainActivity,
                                            trimmed
                                        )
                                    }

                                    pendingEncryptUris?.let {
                                        encryptSelectedFiles(it)
                                        pendingEncryptUris = null
                                    }
                                    pendingDecryptUri?.let {
                                        decryptArchive(it)
                                        pendingDecryptUri = null
                                    }

                                    showPassphraseDialog = false
                                },
                                onDismiss = {
                                    showPassphraseDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun encryptSelectedFiles(uris: List<Uri>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                encryptFiles(this@MainActivity, uris, currentPassphrase)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Зашифровано → Downloads/encrypted_files.zip",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Помилка: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun decryptArchive(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val files = decryptFile(this@MainActivity, uri, currentPassphrase)
                val items = buildMediaItems(files)
                mediaFiles.value = items
                if (items.isNotEmpty()) {
                    runOnUiThread { navController.navigate("media") }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Медіа не знайдено",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Неправильне слово або пошкоджений архів",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun registerScreenshotObserver() {
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver
        )
    }

    private fun unregisterScreenshotObserver() {
        try {
            contentResolver.unregisterContentObserver(screenshotObserver)
        } catch (_: Exception) {
        }
    }

    private fun checkForScreenshot() {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val path =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                if (name.contains("screenshot", true) || path.contains("/screenshots/", true)) {
                    runOnUiThread {
                        Toast.makeText(this, "Скріншоти заборонені", Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    private fun buildMediaItems(extractedFiles: List<File>): List<MItem> {
        val list = mutableListOf<MItem>()
        extractedFiles.forEach { file ->
            val id = file.absolutePath.hashCode().toLong()
            val name = file.name.lowercase()
            val ext = name.substringAfterLast('.', "")
            when (ext) {
                in listOf("jpg", "jpeg", "png", "gif", "webp") -> {
                    val bitmap = BitmapFactory.decodeFile(file.path)
                    val isLand = bitmap?.let { it.width > it.height } ?: false
                    list.add(MItem(id = id, type = "image", uri = Uri.fromFile(file), thumbnailUri = null, isLandscape = isLand))
                }

                in listOf("mp4", "mov", "avi", "mkv", "webm") -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.path)
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        val isLand = width > height
                        val thumb = retriever.getFrameAtTime(0)
                        val thumbUri = thumb?.let {
                            val f = File.createTempFile("thumb_", ".jpg", cacheDir)
                            f.outputStream().use { out -> it.compress(Bitmap.CompressFormat.JPEG, 85, out) }
                            Uri.fromFile(f)
                        }
                        list.add(MItem(id = id, type = "video", uri = Uri.fromFile(file), thumbnailUri = thumbUri, isLandscape = isLand))
                    } catch (_: Exception) {
                    } finally {
                        retriever.release()
                    }
                }

                "txt" -> {
                    val text = file.readText(Charsets.UTF_8)
                    val preview = text.take(200) + if (text.length > 200) "..." else ""
                    list.add(MItem(id = id, type = "text", uri = Uri.fromFile(file), thumbnailUri = null, isLandscape = false, previewText = preview))
                }

                "pdf" -> {
                    val thumbUri = generatePdfThumbnail(file)
                    list.add(MItem(id = id, type = "pdf", uri = Uri.fromFile(file), thumbnailUri = thumbUri, isLandscape = false))
                }

                "docx" -> {
                    val preview = extractDocxPreview(file)
                    list.add(MItem(id = id, type = "document", uri = Uri.fromFile(file), thumbnailUri = null, isLandscape = false, previewText = preview))
                }
            }
        }
        return list
    }

    private fun generatePdfThumbnail(file: File): Uri? {
        return try {
            val parcelFileDescriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            val page = pdfRenderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            parcelFileDescriptor.close()

            val thumbFile = File.createTempFile("pdf_thumb_", ".jpg", cacheDir)
            thumbFile.outputStream()
                .use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
            Uri.fromFile(thumbFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDocxPreview(file: File): String {
        return try {
            FileInputStream(file).use { input ->
                val doc = XWPFDocument(input)
                val fullText = doc.paragraphs.joinToString("\n") { it.text }.take(200) + "..."
                doc.close()
                fullText
            }
        } catch (e: Exception) {
            "Помилка завантаження прев'ю: ${e.message}"
        }
    }
}