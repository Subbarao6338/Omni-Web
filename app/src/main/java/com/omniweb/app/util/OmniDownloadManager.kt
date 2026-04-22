package com.omniweb.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.omniweb.app.data.AppDatabase
import com.omniweb.app.data.DownloadTask
import kotlinx.coroutines.*

class OmniDownloadManager(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDownload(url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading file...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val id = downloadManager.enqueue(request)

        scope.launch {
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
