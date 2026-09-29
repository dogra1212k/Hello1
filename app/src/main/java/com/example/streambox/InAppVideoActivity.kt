package com.example.streambox

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Shared browser, navigation, fullscreen and lifecycle handling for videos and trailers. */
open class InAppVideoActivity : AppCompatActivity() {
    protected lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private lateinit var normal: LinearLayout
    private lateinit var loadingBar: ProgressBar
    private lateinit var reloadButton: Button
    private lateinit var message: TextView
    private lateinit var retry: Button
    private lateinit var alternatives: Button
    private var fullscreen: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var reloadAction: (() -> Unit)? = null
    private var trailerKey: String? = null
    private val trailerBaseUrl get() = "https://$packageName/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        normal = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(normal, FrameLayout.LayoutParams(-1, -1))
        ViewCompat.setOnApplyWindowInsetsListener(normal) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val toolbar = LinearLayout(this)
        toolbar.addView(Button(this).apply {
            text = "‹"; contentDescription = "Back"; setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        toolbar.addView(TextView(this).apply {
            text = intent.getStringExtra("title") ?: "Videos"
            setTextColor(Color.WHITE); textSize = 16f
            gravity = android.view.Gravity.CENTER_VERTICAL
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        reloadButton = Button(this).apply {
            text = "↻"; contentDescription = "Reload"; setOnClickListener { reloadAction?.invoke() }
        }
        toolbar.addView(reloadButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        normal.addView(toolbar)
        loadingBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        normal.addView(loadingBar, LinearLayout.LayoutParams(-1, dp(3)))
        message = TextView(this).apply { setTextColor(Color.LTGRAY); setPadding(dp(12), dp(6), dp(12), dp(6)); visibility = View.GONE }
        normal.addView(message)
        val actions = LinearLayout(this)
        retry = Button(this).apply { text = "Retry"; isAllCaps = false; visibility = View.GONE }
        alternatives = Button(this).apply { text = "Find another video"; isAllCaps = false; visibility = View.GONE }
        actions.addView(retry, LinearLayout.LayoutParams(0, dp(42), 1f))
        actions.addView(alternatives, LinearLayout.LayoutParams(0, dp(42), 1f))
        normal.addView(actions)
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // A target=_blank navigation stays in this WebView. Popups cannot launch another app.
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = route(view, request.url.toString())
                @Deprecated("Legacy WebView callback")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = route(view, url)
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    clearProblem()
                    loadingBar.visibility = View.VISIBLE
                    reloadButton.isEnabled = true
                    val key = trailerKey
                    reloadAction = if (url == trailerBaseUrl && key != null) {
                        { loadTrailer(key) }
                    } else {
                        { reloadPage() }
                    }
                }
                override fun onPageFinished(view: WebView, url: String?) { loadingBar.visibility = View.GONE }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) showProblem("Video page could not load. Check your connection and retry.") { reloadPage() }
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) showProblem("Video service unavailable (${response.statusCode}).") { reloadPage() }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, value: Int) { loadingBar.progress = value }
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (fullscreen != null) { callback.onCustomViewHidden(); return }
                    fullscreen = view
                    fullscreenCallback = callback
                    normal.visibility = View.GONE
                    root.addView(view, FrameLayout.LayoutParams(-1, -1))
                    WindowCompat.getInsetsController(window, root).apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
                override fun onHideCustomView() { exitFullscreen() }
            }
        }
        normal.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        reloadAction = { reloadPage() }
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullscreen != null -> exitFullscreen()
                    webView.canGoBack() -> webView.goBack()
                    else -> finish()
                }
            }
        })
    }

    private fun route(view: WebView, raw: String): Boolean {
        val destination = VideoNavigation.inAppUrl(raw)
        if (destination == null) {
            Toast.makeText(this, "This link is unavailable inside StreamBox.", Toast.LENGTH_SHORT).show()
            return true
        }
        if (destination == raw) return false // HTTP(S) follows normal WebView navigation.
        view.loadUrl(destination)
        return true
    }

    protected fun loadVideoUrl(raw: String) {
        val url = VideoNavigation.inAppUrl(raw)
        if (url == null) { showProblem("This video link is not supported."); return }
        clearProblem()
        reloadButton.isEnabled = true
        reloadAction = { loadVideoUrl(url) }
        webView.loadUrl(url)
    }

    protected fun loadTrailer(key: String) {
        if (!key.matches(Regex("[A-Za-z0-9_-]{11}"))) { showProblem("Trailer unavailable."); return }
        clearProblem()
        trailerKey = key
        reloadButton.isEnabled = true
        reloadAction = { loadTrailer(key) }
        // YouTube requires an app-specific HTTP Referer for Android WebView embeds.
        val origin = "https://$packageName"
        val html = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            </head><body style="margin:0;background:black;height:100vh">
            <iframe title="Trailer" style="width:100%;height:100%;border:0"
              src="https://www.youtube-nocookie.com/embed/$key?playsinline=1&origin=$origin"
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
            </body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(trailerBaseUrl, html, "text/html", "UTF-8", null)
    }

    protected fun showAlternative(query: String) {
        alternatives.visibility = View.VISIBLE
        alternatives.isEnabled = true
        alternatives.setOnClickListener { loadVideoUrl(VideoNavigation.searchUrl(query)) }
    }

    protected fun showLoading() {
        clearProblem()
        loadingBar.visibility = View.VISIBLE
        reloadButton.isEnabled = false
        alternatives.isEnabled = false
    }

    protected fun showProblem(text: String, onRetry: (() -> Unit)? = null) {
        loadingBar.visibility = View.GONE
        reloadButton.isEnabled = true
        alternatives.isEnabled = true
        message.text = text
        message.visibility = View.VISIBLE
        retry.visibility = if (onRetry != null) View.VISIBLE else View.GONE
        retry.setOnClickListener { clearProblem(); onRetry?.invoke() }
        if (onRetry != null) reloadAction = onRetry
    }

    private fun clearProblem() { message.visibility = View.GONE; retry.visibility = View.GONE }

    private fun reloadPage() {
        clearProblem()
        webView.reload()
    }

    private fun exitFullscreen() {
        fullscreen?.let { root.removeView(it) }
        fullscreen = null
        normal.visibility = View.VISIBLE
        val callback = fullscreenCallback
        fullscreenCallback = null
        callback?.onCustomViewHidden()
        WindowCompat.getInsetsController(window, root).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            exitFullscreen()
            webView.stopLoading()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
