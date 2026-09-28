package com.example.streambox

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WebVideoActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title") ?: "Videos"
        val url = intent.getStringExtra("url") ?: "https://m.youtube.com"

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl(url)
        }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))

        root.addView(TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(Color.argb(150, 0, 0, 0))
        }, FrameLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
