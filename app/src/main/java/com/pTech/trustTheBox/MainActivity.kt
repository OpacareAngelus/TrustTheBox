package com.pTech.trustTheBox

import TrustTheBoxTheme
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.pTech.trustTheBox.model.MItem
import com.pTech.trustTheBox.sdk.KeepScreenOn
import com.pTech.trustTheBox.sdk.TrustTheBox.decryptFile
import com.pTech.trustTheBox.sdk.TrustTheBox.encryptFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("trustthebox", MODE_PRIVATE)
        currentPassphrase = prefs.getString("passphrase", "") ?: ""

        val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                            encryptSelectedFiles(uris)
                        }
                    }
                }
            }
        }

        selectZipLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (currentPassphrase.isBlank()) {
                    showPassphraseDialog = true
                    pendingDecryptUri = uri
                } else {
                    decryptArchive(uri)
                }
            }
        }

        setContent {
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
                                MainScreen(selectFileLauncher, selectZipLauncher)
                            }
                            composable("media") { MediaScreen() }
                            composable("viewer/{index}") { entry ->
                                val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
                                val item = mediaFiles.value.getOrNull(index)
                                if (item != null) ViewerScreen(item)
                            }
                        }

                        if (showPassphraseDialog) {
                            PassphraseDialog(
                                currentValue = currentPassphrase,
                                onSave = { passphrase ->
                                    currentPassphrase = passphrase.trim()
                                    getSharedPreferences("trustthebox", MODE_PRIVATE)
                                        .edit()
                                        .putString("passphrase", currentPassphrase)
                                        .apply()

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
                    Toast.makeText(this@MainActivity, "Зашифровано → Downloads/encrypted_files.zip", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
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
                    runOnUiThread { Toast.makeText(this@MainActivity, "Медіа не знайдено", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Неправильне слово або пошкоджений архів", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun registerScreenshotObserver() {
        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, screenshotObserver)
    }

    private fun unregisterScreenshotObserver() {
        try { contentResolver.unregisterContentObserver(screenshotObserver) } catch (_: Exception) {}
    }

    private fun checkForScreenshot() {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
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
            val name = file.name.lowercase()
            val ext = name.substringAfterLast('.', "")
            if (ext in listOf("jpg", "jpeg", "png", "gif", "webp")) {
                val isLand = BitmapFactory.decodeFile(file.path)?.let { it.width > it.height } ?: false
                list.add(MItem("image", Uri.fromFile(file), null, isLand))
            } else if (ext in listOf("mp4", "mov", "avi", "mkv", "webm")) {
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
                    list.add(MItem("video", Uri.fromFile(file), thumbUri, isLand))
                } catch (_: Exception) {} finally { retriever.release() }
            }
        }
        return list
    }

    @Composable
    fun MainScreen(
        selectFileLauncher: ActivityResultLauncher<Intent>,
        selectZipLauncher: ActivityResultLauncher<Intent>
    ) {
        val alpha = remember { mutableFloatStateOf(0.75f) }

        LaunchedEffect(Unit) {
            var dir = -0.003f
            while (true) {
                alpha.floatValue += dir
                if (alpha.floatValue <= 0.1f) { alpha.floatValue = 0.1f; dir = 0.003f }
                else if (alpha.floatValue >= 0.75f) { alpha.floatValue = 0.75f; dir = -0.003f }
                delay(24)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Image(
                painter = painterResource(R.drawable.ic),
                contentDescription = null,
                modifier = Modifier.size(200.dp).alpha(alpha.floatValue),
                contentScale = ContentScale.FillBounds
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        selectFileLauncher.launch(intent)
                    },
                    modifier = Modifier.width(300.dp).height(64.dp)
                ) {
                    Text(stringResource(R.string.select_files_for_password), textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/zip"
                        }
                        selectZipLauncher.launch(intent)
                    },
                    modifier = Modifier.width(300.dp).height(64.dp)
                ) {
                    Text(stringResource(R.string.select_archive_for_viewing), textAlign = TextAlign.Center)
                }
            }
        }
    }

    @Composable
    fun MediaScreen() {
        val sorted = mediaFiles.value.sortedByDescending { it.isLandscape }
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Немає медіафайлів")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sorted.size) { i ->
                    val item = sorted[i]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clickable { navController.navigate("viewer/$i") }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(item.thumbnailUri ?: item.uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    @Composable
    fun ViewerScreen(item: MItem) {
        val context = LocalContext.current

        if (item.type == "image") {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            val player = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(item.uri))
                    repeatMode = Player.REPEAT_MODE_ALL
                    prepare()
                }
            }
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            DisposableEffect(Unit) {
                player.playWhenReady = true
                onDispose { player.release() }
            }
        }
    }
}

@Composable
fun PassphraseBottomBar(hasPassphrase: Boolean, onClick: () -> Unit) {
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (hasPassphrase) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (hasPassphrase) "Секретне слово встановлено" else "Натисніть, щоб ввести секретне слово",
                    color = if (hasPassphrase) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassphraseDialog(currentValue: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Секретне слово") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Наприклад: Мій кіт Барсик 2025") },
                supportingText = { Text("Обидва користувачі мають знати це слово") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.trim().isNotBlank()
            ) { Text("Зберегти") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}