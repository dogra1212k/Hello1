package com.example.streambox

import android.os.Bundle

class WebVideoActivity : InAppVideoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            loadVideoUrl(intent.getStringExtra("url") ?: "https://m.youtube.com")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }
}
