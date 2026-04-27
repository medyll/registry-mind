package com.registry.mind.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadManager(private val context: Context) {

    companion object {
        const val MODEL_FILENAME = "gemma-2b-it-q4.bin"
    }

    val modelFile: File get() = File(context.filesDir, MODEL_FILENAME)

    val isDownloaded: Boolean get() = modelFile.exists() && modelFile.length() > 100_000_000L

    suspend fun download(
        url: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                requestMethod = "GET"
                connect()
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            val tmpFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

            connection.inputStream.use { input ->
                tmpFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) onProgress(downloaded.toFloat() / totalBytes)
                    }
                }
            }

            tmpFile.renameTo(modelFile)
            Result.success(modelFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun delete() = modelFile.delete()
}
