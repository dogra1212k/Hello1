package com.example.streambox

import android.os.Bundle
import java.util.concurrent.Future

class TrailerActivity : InAppVideoActivity() {
    private val online = OnlineMovieService()
    private var request: Future<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title") ?: "Movie"
        showAlternative("$title official trailer")
        loadOfficialTrailer()
    }

    private fun loadOfficialTrailer() {
        val movieId = intent.getIntExtra("movieId", 0)
        if (movieId <= 0) { showProblem("Trailer unavailable."); return }
        showLoading()
        request?.cancel(true)
        request = online.trailerKey(intent.getStringExtra("mediaType") ?: "movie", movieId) { key, error ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (key.isNullOrBlank()) showProblem(error ?: "No trailer is available. Try finding another video.") { loadOfficialTrailer() }
                else loadTrailer(key)
            }
        }
    }

    override fun onDestroy() {
        request?.cancel(true)
        online.close()
        super.onDestroy()
    }
}
