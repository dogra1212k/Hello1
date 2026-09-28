package com.example.streambox

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TrailerActivity : AppCompatActivity() {
    private val online by lazy { OnlineMovieService() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val movieId = intent.getIntExtra("movieId", 0)
        val mediaType = intent.getStringExtra("mediaType") ?: "movie"
        val title = intent.getStringExtra("title") ?: "Movie"
        if (movieId == 0) {
            finish()
            return
        }

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val web = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
        }
        root.addView(web, FrameLayout.LayoutParams(-1, -1))

        val loading = ProgressBar(this)
        root.addView(loading, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))

        val label = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(Color.argb(150, 0, 0, 0))
        }
        root.addView(label, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        setContentView(root)

        online.trailerKey(mediaType, movieId) { key, error ->
            runOnUiThread {
                loading.visibility = ProgressBar.GONE
                if (key.isNullOrBlank()) {
                    Toast.makeText(this, error ?: "Official trailer is not available.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val html = """
                        <html><body style="margin:0;background:#000;">
                        <iframe width="100%" height="100%"
                          src="https://www.youtube-nocookie.com/embed/$key?autoplay=1&playsinline=1"
                          frameborder="0"
                          allow="autoplay; encrypted-media; picture-in-picture"
                          allowfullscreen></iframe>
                        </body></html>
                    """.trimIndent()
                    web.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                }
            }
        }
    }

    override fun onDestroy() {
        (findViewById<FrameLayout>(android.R.id.content).getChildAt(0) as? WebView)?.destroy()
        super.onDestroy()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
