package com.example.streambox

import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URLEncoder
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {
    private val session by lazy { SessionManager(this) }
    private val catalog by lazy { CatalogStore(this) }
    private val firebase by lazy { FirebaseGateway(this) }
    private val prefs by lazy { getSharedPreferences("favorites", MODE_PRIVATE) }
    private val online = OnlineMovieService()
    private val pager = CatalogPager()
    private val cards = MovieGridAdapter()
    private lateinit var searchBox: EditText
    private lateinit var grid: RecyclerView
    private lateinit var heading: TextView
    private lateinit var status: TextView
    private lateinit var more: Button
    private lateinit var videoActions: LinearLayout
    private var request: Future<*>? = null
    private var feed: CatalogFeed? = null
    private var screen = "HOME"
    private var query = ""
    private var screenGeneration = 0
    private var fallbackVisible = false
    private var localMovieCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        buildUi()
        query = savedInstanceState?.getString("query").orEmpty()
        searchBox.setText(savedInstanceState?.getString("search") ?: query)
        when (savedInstanceState?.getString("screen")) {
            "MUSIC" -> showMusic()
            "LOCAL" -> showLocal()
            else -> openFeed(runCatching { CatalogFeed.valueOf(savedInstanceState?.getString("screen") ?: "HOME") }
                .getOrDefault(CatalogFeed.HOME), query)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(12), dp(8), dp(12), dp(6))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(12) + bars.left, dp(8) + bars.top, dp(12) + bars.right, dp(6) + bars.bottom)
            insets
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("STREAMBOX", 25f).apply {
            setTextColor(Color.rgb(229, 9, 20)); setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(button(if (session.isAdmin()) "Admin" else "Logout") {
            if (session.isAdmin()) startActivity(Intent(this, AdminActivity::class.java))
            else {
                firebase.signOut(); session.logout()
                startActivity(Intent(this, AuthActivity::class.java)); finish()
            }
        }, LinearLayout.LayoutParams(-2, dp(44)))
        root.addView(top)
        searchBox = EditText(this).apply {
            hint = "Search movies, series, music & videos"
            setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setBackgroundColor(Color.rgb(35, 35, 35))
            setPadding(dp(10), 0, dp(10), 0)
            setOnEditorActionListener { _, _, _ -> runSearch(); true }
        }
        root.addView(searchBox, LinearLayout.LayoutParams(-1, dp(48)))
        val searchTypes = LinearLayout(this)
        searchTypes.addView(button("All") { runSearch() }, LinearLayout.LayoutParams(0, dp(44), 1f))
        searchTypes.addView(button("Music") { videoSearch(true) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        searchTypes.addView(button("Videos") { videoSearch(false) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(searchTypes)
        val tabs = LinearLayout(this)
        listOf("Home" to { openFeed(CatalogFeed.HOME) }, "Latest Hindi" to { openFeed(CatalogFeed.LATEST_HINDI) },
            "Music" to { showMusic() }, "Series" to { openFeed(CatalogFeed.SERIES) }, "My Videos" to { showLocal() })
            .forEach { (name, action) -> tabs.addView(button(name, action), LinearLayout.LayoutParams(-2, dp(44))) }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(tabs) })
        heading = label("", 20f).apply { setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(8), 0, dp(4)) }
        root.addView(heading)
        videoActions = LinearLayout(this).apply {
            addView(button("♫ Matching music") { openVideoSearch("$query music official video", "Music") }, LinearLayout.LayoutParams(0, dp(40), 1f))
            addView(button("▶ Matching videos") { openVideoSearch(query, "Videos") }, LinearLayout.LayoutParams(0, dp(40), 1f))
            visibility = View.GONE
        }
        root.addView(videoActions)
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = cards
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && nearEnd() && pager.error == null) loadMore()
                }
            })
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        status = label("", 12f).apply { setTextColor(Color.LTGRAY); setPadding(0, dp(4), 0, dp(4)) }
        root.addView(status)
        more = button("Load more") { loadMore() }
        root.addView(more, LinearLayout.LayoutParams(-1, dp(44)))
        setContentView(root)
    }

    private fun resetScreen(name: String) {
        screenGeneration++
        screen = name
        request?.cancel(true)
        pager.reset()
        feed = null
        fallbackVisible = false
        localMovieCount = 0
        cards.replace(emptyList())
        grid.scrollToPosition(0)
        videoActions.visibility = View.GONE
        more.visibility = View.GONE
        status.text = ""
    }

    private fun openFeed(selected: CatalogFeed, search: String = "") {
        resetScreen(selected.name)
        feed = selected
        query = search
        heading.text = selected.title
        if (selected == CatalogFeed.SEARCH) {
            videoActions.visibility = View.VISIBLE
            val local = catalog.getMovies().filter { it.title.contains(query, true) || it.category.contains(query, true) }
            localMovieCount = local.size
            cards.append(local.map(::localTile))
        }
        loadMore()
    }

    private fun loadMore() {
        val currentFeed = feed ?: return
        val ticket = pager.beginLoad() ?: return
        val generation = screenGeneration
        renderStatus()
        request = online.loadPage(currentFeed, ticket.page, query) { page, error ->
            runOnUiThread {
                if (isDestroyed || isFinishing || generation != screenGeneration) return@runOnUiThread
                val previousSize = pager.items.size
                if (page == null) {
                    if (!pager.fail(ticket, error ?: "Could not load movies. Tap Retry.")) return@runOnUiThread
                    if (currentFeed == CatalogFeed.HOME && previousSize == 0 && !fallbackVisible) {
                        fallbackVisible = true
                        cards.replace(catalog.getMovies().map(::localTile))
                    }
                    renderStatus()
                    return@runOnUiThread
                }
                if (!pager.accept(ticket, page)) return@runOnUiThread
                if (fallbackVisible) {
                    cards.replace(emptyList())
                    fallbackVisible = false
                }
                cards.append(pager.items.drop(previousSize).map(::onlineTile))
                renderStatus()
                // Latest fills 120 unique titles; scroll loading continues beyond the initial batch.
                grid.post {
                    if (generation == screenGeneration && !isDestroyed && pager.error == null && pager.hasMore &&
                        ((currentFeed == CatalogFeed.LATEST_HINDI && pager.items.size < CatalogPager.LATEST_TARGET) ||
                            !grid.canScrollVertically(1))) loadMore()
                }
            }
        }
    }

    private fun renderStatus() {
        val currentFeed = feed ?: return
        val count = pager.items.size
        heading.text = "${currentFeed.title}${if (count > 0) " · $count" else ""}"
        status.text = when {
            pager.error != null -> (if (fallbackVisible) "Showing saved videos. " else "") + pager.error
            pager.loading && count == 0 -> "Loading…"
            pager.loading -> "$count titles loaded • Loading more…"
            !pager.hasMore -> if (count + localMovieCount == 0) "No titles found. Try another search." else "All available titles loaded."
            else -> "$count titles loaded • Scroll for more"
        }
        more.visibility = if (pager.hasMore || pager.error != null) View.VISIBLE else View.GONE
        more.isEnabled = !pager.loading
        more.text = when { pager.loading -> "Loading…"; pager.error != null -> "Retry"; else -> "Load more" }
    }

    private fun nearEnd(): Boolean {
        val layout = grid.layoutManager as GridLayoutManager
        return layout.findLastVisibleItemPosition() >= cards.itemCount - 9
    }

    private fun runSearch() {
        val text = searchBox.text.toString().trim()
        if (text.isBlank()) openFeed(CatalogFeed.HOME) else openFeed(CatalogFeed.SEARCH, text)
    }

    private fun videoSearch(music: Boolean) {
        val text = searchBox.text.toString().trim()
        openVideoSearch(if (text.isBlank()) {
            if (music) "latest hindi songs official video" else "latest hindi official videos"
        } else text + if (music) " music official video" else "", if (music) "Music" else "Videos")
    }

    private fun openVideoSearch(text: String, title: String) {
        startActivity(Intent(this, WebVideoActivity::class.java).apply {
            putExtra("title", title)
            putExtra("url", "https://m.youtube.com/results?search_query=" + URLEncoder.encode(text, "UTF-8"))
        })
    }

    private fun showMusic() {
        resetScreen("MUSIC")
        heading.text = "Music Videos"
        val topics = listOf("Latest Hindi", "New Bollywood", "Latest Punjabi", "Romantic", "Party Hits", "Movie Songs", "Sad Songs", "Trending", "New Indian")
        cards.replace(topics.map { title -> MovieGridAdapter.Tile(title, "Official music videos", action = "♫ Browse",
            onClick = { openVideoSearch("$title songs official video", title) }) })
        status.text = "Browse and watch inside StreamBox."
    }

    private fun showLocal() {
        resetScreen("LOCAL")
        heading.text = "My Videos"
        showSavedMovies(catalog.getMovies())
        val generation = screenGeneration
        if (firebase.isAvailable() && firebase.currentEmail() != null) firebase.loadMovies { movies, _ ->
            runOnUiThread {
                if (isDestroyed || isFinishing || generation != screenGeneration) return@runOnUiThread
                if (movies != null && movies.isNotEmpty()) {
                    catalog.saveMovies(movies)
                    showSavedMovies(movies)
                }
            }
        }
    }

    private fun showSavedMovies(movies: List<Movie>) {
        cards.replace(movies.map(::localTile))
        status.text = "${movies.size} videos • Long press for download and favorites"
    }

    private fun onlineTile(movie: OnlineMovie) = MovieGridAdapter.Tile(movie.title,
        listOf(movie.releaseDate, if (movie.mediaType == "tv") "Series" else "Movie").filter { it.isNotBlank() }.joinToString(" · "),
        movie.posterUrl, "▶ Trailer", onClick = {
            startActivity(Intent(this, TrailerActivity::class.java).apply {
                putExtra("movieId", movie.id); putExtra("mediaType", movie.mediaType); putExtra("title", movie.title)
            })
        })

    private fun localTile(movie: Movie) = MovieGridAdapter.Tile(movie.title, movie.category, movie.posterUrl,
        "▶ Watch", onClick = { play(movie) }, onLongClick = { movieOptions(movie) })

    private fun play(movie: Movie) {
        val web = VideoNavigation.isYouTubeWebUrl(movie.videoUrl)
        startActivity(Intent(this, if (web) WebVideoActivity::class.java else PlayerActivity::class.java).apply {
            putExtra("title", movie.title); putExtra("url", movie.videoUrl)
        })
    }

    private fun movieOptions(movie: Movie) {
        val favorite = prefs.getBoolean(movie.title, false)
        android.app.AlertDialog.Builder(this).setTitle(movie.title)
            .setItems(arrayOf("Watch", "Download", if (favorite) "Remove favorite" else "Add favorite")) { _, which ->
                when (which) {
                    0 -> play(movie)
                    1 -> download(movie)
                    2 -> { prefs.edit().putBoolean(movie.title, !favorite).apply()
                        Toast.makeText(this, if (favorite) "Favorite removed" else "Favorite saved", Toast.LENGTH_SHORT).show() }
                }
            }.show()
    }

    private fun download(movie: Movie) {
        if (VideoNavigation.isYouTubeWebUrl(movie.videoUrl)) {
            Toast.makeText(this, "Download is available for direct video files only.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val name = movie.title.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".mp4"
            val download = DownloadManager.Request(Uri.parse(movie.videoUrl)).setTitle(movie.title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_MOVIES, name)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(download)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { Toast.makeText(this, "Download unavailable for this video", Toast.LENGTH_LONG).show() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen)
        outState.putString("query", query)
        if (::searchBox.isInitialized) outState.putString("search", searchBox.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        screenGeneration++
        request?.cancel(true)
        online.close()
        if (::grid.isInitialized) grid.adapter = null
        super.onDestroy()
    }

    private fun label(value: String, size: Float) = TextView(this).apply { text = value; textSize = size; setTextColor(Color.WHITE) }
    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(45, 45, 45)); setOnClickListener { action() }
        textSize = 13f
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
