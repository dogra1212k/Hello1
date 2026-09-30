package com.example.streambox

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Future

/** Official discovery for films without a saved full-video source; no fake media URLs. */
class WatchOptionsActivity : AppCompatActivity() {
    private val online = OnlineMovieService()
    private var request: Future<*>? = null
    private lateinit var status: TextView
    private lateinit var offers: TextView
    private lateinit var progress: ProgressBar
    private lateinit var providerButton: Button
    private lateinit var nativeButton: Button
    private lateinit var retry: Button
    private var titleText = ""
    private var movieId = 0
    private var mediaType = "movie"
    private var year = 0
    private var providerLink: String? = null
    private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        titleText = intent.getStringExtra("title").orEmpty()
        movieId = savedInstanceState?.getInt("tmdbId") ?: intent.getIntExtra("tmdbId", 0)
        mediaType = intent.getStringExtra("mediaType").takeIf { it == "tv" } ?: "movie"
        year = intent.getIntExtra("year", 0)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.BLACK)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(16) + bars.left, dp(16) + bars.top, dp(16) + bars.right, dp(16) + bars.bottom)
            insets
        }
        root.addView(button("Back") { finish() })
        root.addView(label(titleText, 24f))
        root.addView(label("Watch options · India${if (year > 0) " · $year" else ""}", 16f))
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        details.addView(label(intent.getStringExtra("overview").orEmpty(), 14f))
        progress = ProgressBar(this)
        details.addView(progress)
        status = label("", 16f)
        details.addView(status)
        offers = label("", 16f)
        details.addView(offers)
        nativeButton = button("▶ Watch in StreamBox") { playSavedVideo() }.apply { visibility = View.GONE }
        details.addView(nativeButton)
        providerButton = button("Open provider links") { providerLink?.let(::openWebsite) }
            .apply { visibility = View.GONE }
        details.addView(providerButton)
        details.addView(button("Find on JustWatch") { openWebsite(WatchLinks.search(titleText)) })
        retry = button("Retry availability check") { loadSources() }.apply { visibility = View.GONE }
        details.addView(retry)
        details.addView(label("Streaming availability provided by JustWatch via TMDB.\n" +
            "Opens the service's website or app. A subscription, rental or purchase may be required. " +
            "Check the title, year and Hindi audio on the service.", 13f))
        root.addView(ScrollView(this).apply { addView(details) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadSources()
    }

    private fun loadSources() {
        val currentGeneration = ++generation
        request?.cancel(true)
        providerLink = null
        providerButton.visibility = View.GONE
        retry.visibility = View.GONE
        offers.text = ""
        refreshNativeOption()
        if (!online.isConfigured() || (movieId <= 0 && year <= 0)) {
            progress.visibility = View.GONE
            status.text = "Find streaming and rental options for this title on JustWatch."
            return
        }
        progress.visibility = View.VISIBLE
        status.text = "Checking streaming services in India…"
        request = online.loadWatchSources(movieId, mediaType, titleText, year) { resolvedId, sources, error ->
            runOnUiThread {
                if (isDestroyed || isFinishing || generation != currentGeneration) return@runOnUiThread
                progress.visibility = View.GONE
                if (resolvedId > 0) movieId = resolvedId
                refreshNativeOption()
                providerLink = sources?.link
                providerButton.visibility = if (providerLink != null) View.VISIBLE else View.GONE
                status.text = when {
                    error != null -> "Could not check availability. Retry or search JustWatch below."
                    resolvedId == 0 -> "Check the matching title and year on JustWatch below."
                    sources?.offers.isNullOrEmpty() -> "No streaming offers found for India. Check JustWatch for updates."
                    else -> "Streaming and rental options in India:"
                }
                offers.text = sources?.offers.orEmpty().joinToString("\n") { "${it.provider} · ${it.kind}" }
                retry.visibility = if (error != null) View.VISIBLE else View.GONE
            }
        }
    }

    private fun savedVideo() = if (movieId > 0) MediaSourcePolicy.findMovie(
        OnlineMovie(movieId, titleText, "", "", "", mediaType), CatalogStore(this).getMovies()) else null

    private fun refreshNativeOption() {
        nativeButton.visibility = if (savedVideo() != null) View.VISIBLE else View.GONE
    }

    private fun playSavedVideo() {
        val movie = savedVideo() ?: return
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("title", movie.title); putExtra("url", movie.videoUrl)
            putExtra("description", movie.description); putExtra("tmdbId", movie.tmdbId)
            putExtra("mediaType", movie.mediaType); putExtra("year", year)
        })
    }

    private fun openWebsite(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser is available to open this link.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("tmdbId", movieId)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        generation++
        request?.cancel(true)
        online.close()
        super.onDestroy()
    }

    private fun label(value: String, size: Float) = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.WHITE)
        setPadding(0, dp(8), 0, dp(8))
    }
    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; setOnClickListener { action() }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        fun createIntent(context: Context, title: String, tmdbId: Int = 0, type: String = "movie",
                         year: Int = 0, overview: String = "") = Intent(context, WatchOptionsActivity::class.java).apply {
            putExtra("title", title); putExtra("tmdbId", tmdbId); putExtra("mediaType", type)
            putExtra("year", year); putExtra("overview", overview)
        }
    }
}
