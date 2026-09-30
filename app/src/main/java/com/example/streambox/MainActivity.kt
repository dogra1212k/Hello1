package com.example.streambox

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private var request: Future<*>? = null
    private var feed: CatalogFeed? = null
    private var screen = "WATCH"
    private var query = ""
    private var screenGeneration = 0
    private var savedMovies: List<Movie> = emptyList()
    private var cloudLoading = false
    private var cloudError: String? = null
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
        savedMovies = catalog.getMovies()
        val restored = savedInstanceState?.getString("screen") ?: "WATCH"
        val restoredFeed = runCatching { CatalogFeed.valueOf(restored) }.getOrNull()
        if (restoredFeed != null) openFeed(restoredFeed, query) else showSaved(restored, query)
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
            hint = "Search movies, music & videos"
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
        searchTypes.addView(button("Music") { showSaved("MUSIC", searchBox.text.toString().trim()) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        searchTypes.addView(button("Videos") { showSaved("LOCAL", searchBox.text.toString().trim()) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(searchTypes)
        val tabs = LinearLayout(this)
        listOf("Home" to { showSaved("WATCH") }, "Hindi Movies" to { openFeed(CatalogFeed.HOME) },
            "Latest Hindi" to { openFeed(CatalogFeed.LATEST_HINDI) }, "Music" to { showSaved("MUSIC") },
            "Series" to { openFeed(CatalogFeed.SERIES) }, "My Videos" to { showSaved("LOCAL") })
            .forEach { (name, action) -> tabs.addView(button(name, action), LinearLayout.LayoutParams(-2, dp(44))) }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(tabs) })
        heading = label("", 20f).apply { setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(8), 0, dp(4)) }
        root.addView(heading)
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = cards
            itemAnimator = null
            addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                val height = view.height - view.paddingTop - view.paddingBottom
                val metrics = resources.displayMetrics
                // Adapter notifications must run outside RecyclerView's layout pass.
                view.post {
                    if (!isDestroyed) cards.fitThreeRows(height, metrics.density, resources.configuration.fontScale)
                }
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && nearEnd() && pager.error == null) loadMore()
                }
            })
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        status = label("", 12f).apply { setTextColor(Color.LTGRAY); setPadding(0, dp(4), 0, dp(4)) }
        root.addView(status)
        more = button("Load more") { if (feed != null) loadMore() else syncCloud() }
        root.addView(more, LinearLayout.LayoutParams(-1, dp(44)))
        setContentView(root)
    }

    private fun resetScreen(name: String) {
        screenGeneration++
        screen = name
        request?.cancel(true)
        pager.reset()
        feed = null
        localMovieCount = 0
        cards.replace(emptyList())
        grid.scrollToPosition(0)
        more.visibility = View.GONE
        status.text = ""
    }

    private fun openFeed(selected: CatalogFeed, search: String = "") {
        resetScreen(selected.name)
        feed = selected
        query = search
        heading.text = selected.title
        if (selected == CatalogFeed.SEARCH) {
            val local = searchMovies()
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
                    renderStatus()
                    return@runOnUiThread
                }
                if (!pager.accept(ticket, page)) return@runOnUiThread
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
            pager.error != null -> pager.error
            pager.loading && count == 0 -> "Loading…"
            pager.loading -> "$count titles loaded • Loading more…"
            !pager.hasMore -> if (count + localMovieCount == 0) "No titles found. Try another search." else "All available titles loaded."
            else -> "$count movie details • Full video required for Watch"
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
        if (text.isBlank()) showSaved("WATCH") else openFeed(CatalogFeed.SEARCH, text)
    }

    private fun searchMovies() = savedMovies.filter { MediaSourcePolicy.matches(it, query) }

    private fun showSaved(name: String, search: String = "") {
        resetScreen(name)
        query = search
        renderSaved()
    }

    private fun renderSaved() {
        val movies = searchMovies().filter {
            when (screen) {
                "WATCH" -> MediaSourcePolicy.isPlayable(it.videoUrl) && !MediaSourcePolicy.isMusic(it)
                "MUSIC" -> MediaSourcePolicy.isMusic(it)
                else -> true
            }
        }
        heading.text = when (screen) {
            "WATCH" -> "Watch now · ${movies.size}"
            "MUSIC" -> "Music videos · ${movies.size}"
            else -> "My Videos · ${movies.size}"
        }
        cards.replace(movies.map(::localTile))
        status.text = when {
            cloudLoading -> "Updating videos…"
            cloudError != null -> "Showing saved videos. $cloudError"
            movies.isEmpty() -> if (query.isNotBlank()) "No matching videos. Try another search."
                else "No videos added here yet."
            screen == "WATCH" -> "Complete films • Long press for details, download and favorites"
            else -> "${movies.size} videos • Long press for details and options"
        }
        more.visibility = if (cloudError != null || cloudLoading) View.VISIBLE else View.GONE
        more.isEnabled = !cloudLoading
        more.text = if (cloudLoading) "Updating…" else "Retry"
    }

    private fun refreshCards() {
        if (feed == null) renderSaved() else {
            val local = if (feed == CatalogFeed.SEARCH) searchMovies() else emptyList()
            localMovieCount = local.size
            cards.replace(local.map(::localTile) + pager.items.map(::onlineTile))
            renderStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::grid.isInitialized) return
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java)); finish(); return
        }
        savedMovies = catalog.getMovies()
        refreshCards()
        syncCloud()
    }

    private fun syncCloud() {
        if (cloudLoading || !firebase.isAvailable() || firebase.currentEmail() == null) return
        cloudLoading = true
        cloudError = null
        if (feed == null) renderSaved()
        firebase.loadMovies { movies, _ -> runOnUiThread {
            if (isDestroyed || isFinishing) return@runOnUiThread
            cloudLoading = false
            if (movies != null) {
                // Empty cloud responses clear removed titles, while keeping local films.
                catalog.saveCloudMovies(movies)
                savedMovies = catalog.getMovies()
            } else cloudError = "Could not update videos. Check your connection and retry."
            refreshCards()
        } }
    }

    private fun onlineTile(movie: OnlineMovie): MovieGridAdapter.Tile {
        val stream = MediaSourcePolicy.findMovie(movie, savedMovies)
        return MovieGridAdapter.Tile(movie.title,
            listOf(movie.releaseDate, if (movie.mediaType == "tv") "Series" else "Movie")
                .filter { it.isNotBlank() }.joinToString(" · "),
            movie.posterUrl, if (stream != null) "▶ Watch" else "Unavailable", onClick = {
                if (stream != null) play(stream) else android.app.AlertDialog.Builder(this)
                    .setTitle(movie.title)
                    .setMessage(listOf(movie.overview, "Full video abhi StreamBox mein available nahi hai.")
                        .filter { it.isNotBlank() }.joinToString("\n\n"))
                    .setPositiveButton("OK", null).show()
            })
    }

    private fun localTile(movie: Movie) = MovieGridAdapter.Tile(movie.title, movie.category, movie.posterUrl,
        if (MediaSourcePolicy.isPlayable(movie.videoUrl)) "▶ Watch" else "Unavailable",
        onClick = { play(movie) }, onLongClick = { movieOptions(movie) })

    private fun play(movie: Movie) {
        if (!MediaSourcePolicy.isPlayable(movie.videoUrl)) {
            Toast.makeText(this, "Is video ka direct stream available nahi hai.", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("title", movie.title); putExtra("url", movie.videoUrl)
            putExtra("description", movie.description)
        })
    }

    private fun movieOptions(movie: Movie) {
        val favorite = prefs.getBoolean(movie.title, false)
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        if (MediaSourcePolicy.isPlayable(movie.videoUrl)) actions += "Watch" to { play(movie) }
        if (MediaSourcePolicy.isDownloadable(movie.videoUrl)) actions += "Download" to {
            MovieDownloads.start(this, movie.title, movie.videoUrl)
        }
        actions += "Details & credits" to {
            android.app.AlertDialog.Builder(this).setTitle(movie.title)
                .setMessage(movie.description.ifBlank { movie.category }).setPositiveButton("OK", null).show()
            Unit
        }
        actions += (if (favorite) "Remove favorite" else "Add favorite") to {
            prefs.edit().putBoolean(movie.title, !favorite).apply()
            Toast.makeText(this, if (favorite) "Favorite removed" else "Favorite saved", Toast.LENGTH_SHORT).show()
        }
        android.app.AlertDialog.Builder(this).setTitle(movie.title)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }.show()
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
