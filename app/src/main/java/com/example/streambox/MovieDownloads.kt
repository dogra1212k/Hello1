package com.example.streambox

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object MovieDownloads {
    fun start(context: Context, title: String, url: String) {
        if (!MediaSourcePolicy.isDownloadable(url)) {
            Toast.makeText(context, "Download is available for video files only.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val name = title.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").take(90).ifBlank { "Video" }
            val filename = "$name-${System.currentTimeMillis()}.${MediaSourcePolicy.extension(url)}"
            val request = DownloadManager.Request(Uri.parse(url)).setTitle(title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, filename)
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Download could not start. Please retry.", Toast.LENGTH_LONG).show()
        }
    }
}
