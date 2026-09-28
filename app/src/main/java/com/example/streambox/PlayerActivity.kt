package com.example.streambox

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title") ?: "Video"
        val url = intent.getStringExtra("url") ?: return finish()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        videoView = VideoView(this)
        root.addView(videoView, FrameLayout.LayoutParams(-1, -1))

        val loading = ProgressBar(this)
        root.addView(loading, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))

        val label = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.argb(130, 0, 0, 0))
        }
        root.addView(label, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        setContentView(root)

        val controls = MediaController(this)
        controls.setAnchorView(videoView)
        videoView.setMediaController(controls)
        videoView.setVideoPath(url)
        videoView.setOnPreparedListener {
            loading.visibility = ProgressBar.GONE
            it.isLooping = false
            videoView.start()
        }
        videoView.setOnErrorListener { _, _, _ ->
            loading.visibility = ProgressBar.GONE
            Toast.makeText(this, "Video could not be played.", Toast.LENGTH_LONG).show()
            true
        }
        videoView.requestFocus()
    }

    override fun onPause() {
        if (::videoView.isInitialized && videoView.isPlaying) videoView.pause()
        super.onPause()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
