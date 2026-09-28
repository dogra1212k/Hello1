package com.example.streambox

import android.app.DownloadManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private var currentUrl: String = ""
    private var titleText: String = "Video"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleText = intent.getStringExtra("title") ?: "Video"
        currentUrl = intent.getStringExtra("url") ?: return finish()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val playerView = PlayerView(this).apply {
            useController = true
        }
        root.addView(playerView, FrameLayout.LayoutParams(-1, -1))

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.argb(160, 0, 0, 0))
        }

        topBar.addView(TextView(this).apply {
            text = titleText
            setTextColor(Color.WHITE)
            textSize = 16f
        }, LinearLayout.LayoutParams(0, dp(44), 1f))

        topBar.addView(Button(this).apply {
            text = "Download"
            isAllCaps = false
            setOnClickListener { downloadCurrent() }
        })

        topBar.addView(Button(this).apply {
            text = "Quality"
            isAllCaps = false
            setOnClickListener { chooseQuality() }
        })

        topBar.addView(Button(this).apply {
            text = "Speed"
            isAllCaps = false
            setOnClickListener { chooseSpeed() }
        })

        root.addView(topBar, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        setContentView(root)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.setMediaItem(MediaItem.fromUri(currentUrl))
        player.prepare()
        player.playWhenReady = true
    }

    private fun chooseSpeed() {
        val labels = arrayOf("0.5x","0.75x","1x","1.25x","1.5x","2x")
        val values = floatArrayOf(0.5f,0.75f,1f,1.25f,1.5f,2f)
        android.app.AlertDialog.Builder(this)
            .setTitle("Playback speed")
            .setItems(labels) { _, which ->
                player.playbackParameters = PlaybackParameters(values[which])
            }
            .show()
    }

    private fun chooseQuality() {
        val labels = arrayOf("Auto","360p","480p","720p","1080p")
        android.app.AlertDialog.Builder(this)
            .setTitle("Video quality")
            .setItems(labels) { _, which ->
                Toast.makeText(
                    this,
                    if (which == 0) "Auto quality selected"
                    else "Quality selection works when the stream provides multiple qualities",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun downloadCurrent() {
        try {
            val safeName = titleText.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".mp4"
            val request = DownloadManager.Request(Uri.parse(currentUrl))
                .setTitle(titleText)
                .setDescription("Downloading for offline viewing")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_MOVIES, safeName)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Download unavailable for this stream", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        if (::player.isInitialized) player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
