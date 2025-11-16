package com.pTech.trustTheBox.sdk

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object TrustTheBox {

    private const val PASSWORD_COUNT = 10
    private const val PASSWORD_LENGTH_CHARS = 256

    private const val CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_+=[]{}|;:'\",.<>?/`~"

    private fun generatePasswordsFromPassphrase(passphrase: String): List<String> {
        val passwords = mutableListOf<String>()
        val digest = MessageDigest.getInstance("SHA-256")
        var seed = digest.digest(passphrase.toByteArray(Charsets.UTF_8))

        repeat(PASSWORD_COUNT) { index ->
            val indexedSeed = seed.copyOf(seed.size + 4).apply {
                this[seed.size] = (index shr 24).toByte()
                this[seed.size + 1] = (index shr 16).toByte()
                this[seed.size + 2] = (index shr 8).toByte()
                this[seed.size + 3] = index.toByte()
            }
            var nextHash = digest.digest(indexedSeed)

            val byteBuffer = ByteArrayOutputStream()
            while (byteBuffer.size() < PASSWORD_LENGTH_CHARS) {
                byteBuffer.write(nextHash)
                nextHash = digest.digest(nextHash)
            }
            val passwordBytes = byteBuffer.toByteArray().copyOf(PASSWORD_LENGTH_CHARS)

            val password = StringBuilder()
            for (byte in passwordBytes) {
                val charIndex = (byte.toInt() and 0xFF) % CHARSET.length
                password.append(CHARSET[charIndex])
            }
            passwords.add(password.toString())

            seed = digest.digest(nextHash)
        }
        return passwords
    }

    fun encryptFiles(
        context: Context,
        inputUris: List<Uri>,
        passphrase: String
    ) {
        val passwords = generatePasswordsFromPassphrase(passphrase)
        val chosenPassword = passwords.random()
        Log.d("EncryptManager", "Chosen key: $passphrase")
        Log.d("EncryptManager", "Chosen password: $chosenPassword")

        val contentResolver = context.contentResolver
        val tempFiles = mutableListOf<File>()

        inputUris.forEach { uri ->
            val originalName = getFileNameFromUri(contentResolver, uri) ?: "unknown_file"
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val tempFile = File(context.cacheDir, "${System.currentTimeMillis()}_$originalName")
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFiles.add(tempFile)
        }

        val parameters = ZipParameters().apply {
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.ZIP_STANDARD
        }

        val tempZip = File(context.cacheDir, "encrypted_temp.zip")
        ZipFile(tempZip.absolutePath, chosenPassword.toCharArray()).use { zipFile ->
            zipFile.addFiles(tempFiles, parameters)
        }

        saveZipToDownloads(context, tempZip, "encrypted_files.zip")

        tempFiles.forEach { it.delete() }
        tempZip.delete()
    }

    fun decryptFile(
        context: Context,
        inputUri: Uri,
        passphrase: String
    ): List<File> {
        val passwords = generatePasswordsFromPassphrase(passphrase)
        val contentResolver = context.contentResolver
        val tempZip = File(context.cacheDir, "temp_decrypt.zip")

        contentResolver.openInputStream(inputUri)?.use { input ->
            tempZip.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Не вдалося відкрити файл")

        val extractedFiles = mutableListOf<File>()

        for (pwd in passwords) {
            try {
                ZipFile(tempZip.absolutePath, pwd.toCharArray()).use { zipFile ->
                    if (zipFile.fileHeaders.isEmpty()) continue
                    val extractDir =
                        File(context.cacheDir, "extracted_${System.currentTimeMillis()}")
                    extractDir.mkdirs()
                    zipFile.extractAll(extractDir.absolutePath)
                    extractDir.listFiles()?.forEach { file ->
                        if (file.isFile) extractedFiles.add(file)
                    }
                    tempZip.delete()
                    return extractedFiles
                }
            } catch (e: Exception) {
                val failedDir = File(context.cacheDir, "extracted_${System.currentTimeMillis()}")
                if (failedDir.exists()) failedDir.deleteRecursively()
            }
        }

        tempZip.delete()
        throw IllegalArgumentException("Не вдалося розшифрувати файл. Перевірте секретне слово.")
    }

    private fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) cursor.getString(nameIndex) else null
            } else null
        } ?: uri.lastPathSegment
    }

    private fun saveZipToDownloads(context: Context, zipFile: File, displayName: String) {
        val contentResolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Не вдалося створити файл у Downloads")

            contentResolver.openOutputStream(uri)?.use { output ->
                zipFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            val updateValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            contentResolver.update(uri, updateValues, null, null)
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val destFile = File(downloadsDir, displayName)

            FileInputStream(zipFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("application/zip"),
                null
            )
        }
    }
}