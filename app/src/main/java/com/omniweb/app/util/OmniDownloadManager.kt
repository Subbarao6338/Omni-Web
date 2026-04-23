package com.omniweb.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.DownloadTask
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class OmniDownloadManager(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDownload(url: String, fileName: String) {
        val isVideoUrl = url.contains("youtube.com") || url.contains("youtu.be") || url.contains("instagram.com") || url.contains("x.com") || url.contains("facebook.com")

        if (isVideoUrl) {
            scope.launch {
                startYtDlDownload(url, fileName)
            }
        } else {
            enqueueStandardDownload(url, fileName)
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

                if (settings?.downloadPath != null) {
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
                    filePath = null,
                    status = DownloadManager.STATUS_PENDING,
                    totalSize = 0,
                    downloadedSize = 0
                )
                db.downloadDao().insertDownload(task)
                pollDownloadStatus(id)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to start download: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun startYtDlDownload(url: String, fileName: String) {
        val downloadId = System.currentTimeMillis() // Generate a temporary ID
        val settings = db.settingsDao().getSettings().firstOrNull()
        val downloadFolder = if (settings?.downloadPath != null) {
            File(settings.downloadPath).apply { if (!exists()) mkdirs() }
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        }
        val file = File(downloadFolder, fileName)

        val task = DownloadTask(
            id = downloadId,
            title = fileName,
            url = url,
            filePath = file.absolutePath,
            status = DownloadManager.STATUS_RUNNING,
            totalSize = 0,
            downloadedSize = 0
        )
        db.downloadDao().insertDownload(task)

        try {
            val request = YoutubeDLRequest(url)
            request.addOption("-o", file.absolutePath)

            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, line ->
                scope.launch {
                   db.downloadDao().getDownloadByIdSync(downloadId)?.let { currentTask ->
                       db.downloadDao().updateDownload(currentTask.copy(
                           downloadedSize = progress.toLong(),
                           totalSize = 100 // Progress is 0-100
                       ))
                   }
                }
            }

            db.downloadDao().getDownloadByIdSync(downloadId)?.let { finalTask ->
                db.downloadDao().updateDownload(finalTask.copy(status = DownloadManager.STATUS_SUCCESSFUL, downloadedSize = 100))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            db.downloadDao().getDownloadByIdSync(downloadId)?.let { errorTask ->
                db.downloadDao().updateDownload(errorTask.copy(status = DownloadManager.STATUS_FAILED))
            }
        }
    }

    private fun pollDownloadStatus(downloadId: Long) {
        scope.launch {
            var isDownloading = true
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    db.downloadDao().getDownloadByIdSync(downloadId)?.let { task ->
                        db.downloadDao().updateDownload(task.copy(
                            status = status,
                            downloadedSize = downloaded,
                            totalSize = total
                        ))
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        isDownloading = false
                    }
                } else {
                    isDownloading = false
                }
                cursor.close()
                delay(1000)
            }
        }
    }
}
