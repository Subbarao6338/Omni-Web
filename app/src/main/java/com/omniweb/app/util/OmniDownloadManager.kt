package com.omniweb.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.DownloadTask
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileInputStream

class OmniDownloadManager(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDownload(url: String, fileName: String) {
        val sanitizedName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val isVideoUrl = url.contains("youtube.com") || url.contains("youtu.be") || url.contains("instagram.com") || url.contains("x.com") || url.contains("facebook.com")

        if (isVideoUrl) {
            scope.launch {
                startYtDlDownload(url, sanitizedName)
            }
        } else {
            enqueueStandardDownload(url, sanitizedName)
        }
    }

    private fun enqueueStandardDownload(url: String, fileName: String) {
        scope.launch {
            try {
                val settings = db.settingsDao().getSettings().firstOrNull()
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(fileName)
                    .setDescription("Downloading file...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                var customUri: Uri? = null
                if (settings?.downloadPath != null && settings.downloadPath.startsWith("content://")) {
                    val treeUri = Uri.parse(settings.downloadPath)
                    val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                    if (pickedDir != null && pickedDir.exists() && pickedDir.canWrite()) {
                        val newFile = pickedDir.createFile(getMimeType(fileName), fileName)
                        if (newFile != null) {
                            customUri = newFile.uri
                            request.setDestinationUri(customUri)
                        } else {
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                        }
                    } else {
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
                } else if (settings?.downloadPath != null) {
                    val file = File(settings.downloadPath, fileName)
                    request.setDestinationUri(Uri.fromFile(file))
                } else {
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }

                val id = downloadManager.enqueue(request)

                val task = DownloadTask(
                    id = id,
                    title = fileName,
                    url = url,
                    filePath = customUri?.toString(),
                    status = DownloadManager.STATUS_PENDING,
                    totalSize = 0,
                    downloadedSize = 0
                )
                db.downloadDao().insertDownload(task)
                pollDownloadStatus(id)
            } catch (e: Exception) {
                LogUtils.e("Failed to start standard download", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private suspend fun startYtDlDownload(url: String, fileName: String) {
        val downloadId = System.currentTimeMillis() // Generate a temporary ID
        val settings = db.settingsDao().getSettings().firstOrNull()

        val tempFile = File(context.cacheDir, "yt_dl_temp_${System.currentTimeMillis()}")

        val task = DownloadTask(
            id = downloadId,
            title = fileName,
            url = url,
            filePath = null, // Will update after moving
            status = DownloadManager.STATUS_RUNNING,
            totalSize = 0,
            downloadedSize = 0
        )
        db.downloadDao().insertDownload(task)

        try {
            val request = YoutubeDLRequest(url)
            request.addOption("-o", tempFile.absolutePath)
            request.addOption("--no-check-certificate")
            request.addOption("--socket-timeout", "10")
            request.addOption("--retries", "3")

            withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                    scope.launch {
                       db.downloadDao().getDownloadByIdSync(downloadId)?.let { currentTask ->
                           db.downloadDao().updateDownload(currentTask.copy(
                               downloadedSize = progress.toLong(),
                               totalSize = 100,
                               status = DownloadManager.STATUS_RUNNING
                           ))
                       }
                    }
                    }
                } catch (e: Exception) {
                    throw e
                }
            }

            // Move to destination
            var finalPath: String? = null
            if (settings?.downloadPath != null && settings.downloadPath.startsWith("content://")) {
                val treeUri = Uri.parse(settings.downloadPath)
                val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                if (pickedDir == null || !pickedDir.exists() || !pickedDir.canWrite()) {
                    throw Exception("Selected download folder is not accessible or writable")
                }
                val newFile = pickedDir.createFile(getMimeType(fileName), fileName)
                if (newFile != null) {
                    context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                        FileInputStream(tempFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    finalPath = newFile.uri.toString()
                } else {
                    throw Exception("Failed to create file in selected folder")
                }
            } else if (settings?.downloadPath != null) {
                val downloadFolder = File(settings.downloadPath).apply { if (!exists()) mkdirs() }
                val destFile = File(downloadFolder, fileName)
                tempFile.renameTo(destFile)
                finalPath = destFile.absolutePath
            } else {
                val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                val destFile = File(downloadFolder, fileName)
                tempFile.renameTo(destFile)
                finalPath = destFile.absolutePath
            }

            if (tempFile.exists()) tempFile.delete()

            db.downloadDao().getDownloadByIdSync(downloadId)?.let { finalTask ->
                db.downloadDao().updateDownload(finalTask.copy(
                    status = DownloadManager.STATUS_SUCCESSFUL,
                    downloadedSize = 100,
                    filePath = finalPath
                ))
            }
        } catch (e: Exception) {
            LogUtils.e("Failed to start YouTube download", e)
            if (tempFile.exists()) tempFile.delete()
            db.downloadDao().getDownloadByIdSync(downloadId)?.let { errorTask ->
                db.downloadDao().updateDownload(errorTask.copy(status = DownloadManager.STATUS_FAILED))
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Video download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pollDownloadStatus(downloadId: Long) {
        scope.launch {
            var isDownloading = true
            var lastDownloaded: Long = 0
            var lastTime: Long = System.currentTimeMillis()
            val speedSamples = mutableListOf<Long>()

            while (isDownloading) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        val currentTime = System.currentTimeMillis()
                        val timeDiff = (currentTime - lastTime) / 1000f
                        val currentSpeed = if (timeDiff > 0 && downloaded > lastDownloaded) ((downloaded - lastDownloaded) / timeDiff).toLong() else 0L

                        if (currentSpeed > 0) {
                            speedSamples.add(currentSpeed)
                            if (speedSamples.size > 5) speedSamples.removeAt(0)
                        }

                        val averageSpeed = if (speedSamples.isNotEmpty()) speedSamples.average().toLong() else currentSpeed
                        val remaining = if (averageSpeed > 0 && total > 0) (total - downloaded) / averageSpeed else 0L

                        lastDownloaded = downloaded
                        lastTime = currentTime

                        db.downloadDao().getDownloadByIdSync(downloadId)?.let { task ->
                            db.downloadDao().updateDownload(task.copy(
                                status = status,
                                downloadedSize = downloaded,
                                totalSize = total,
                                downloadSpeed = averageSpeed,
                                estimatedTimeRemaining = remaining
                            ))
                        }

                        if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                            isDownloading = false
                        }
                    } else {
                        // Check if it exists in DB as YtDl download which might not be in Android DownloadManager
                        val dbTask = db.downloadDao().getDownloadByIdSync(downloadId)
                        if (dbTask == null || dbTask.status == DownloadManager.STATUS_SUCCESSFUL || dbTask.status == DownloadManager.STATUS_FAILED) {
                            isDownloading = false
                        }
                    }
                    cursor?.close()
                } catch (e: Exception) {
                    LogUtils.e("Error polling download status", e)
                    // Don't kill the loop on transient errors, just delay and retry
                    delay(2000)
                }
                if (isDownloading) delay(1000)
            }
        }
    }
}
