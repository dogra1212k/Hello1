package com.example.streambox

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var contentHolder: LinearLayout
    private lateinit var searchBox: EditText
    private val prefs by lazy { getSharedPreferences("favorites", MODE_PRIVATE) }

    private val movies = listOf(
        Movie(
            "Big Buck Bunny",
            "Animation",
            "Open movie demo. Replace this URL with content you own or are licensed to stream.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg"
        ),
        Movie(
            "Elephant Dream",
            "Sci‑Fi",
            "Open movie demo for testing the streaming player.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg"
        ),
        Movie(
            "For Bigger Blazes",
            "Action",
            "Short demo video for your home feed.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg"
        ),
        Movie(
            "For Bigger Escape",
            "Adventure",
            "Short demo video. Add your own catalog later.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        renderMovies(movies)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }

        val header = TextView(this).apply {
            text = "STREAMBOX"
            setTextColor(Color.rgb(229, 9, 20))
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.START
        }
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val sub = TextView(this).apply {
            text = "Free streaming • No premium tier"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(sub)

        searchBox = EditText(this).apply {
            hint = "Search movies or categories"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setBackgroundColor(Color.rgb(35, 35, 35))
            setPadding(dp(12), 0, dp(12), 0)
        }
        root.addView(searchBox, LinearLayout.LayoutParams(-1, dp(48)))

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val home = button("Home") { renderMovies(filtered()) }
        val fav = button("My List") { renderMovies(movies.filter { isFavorite(it) }) }
        tabs.addView(home, LinearLayout.LayoutParams(0, dp(44), 1f))
        tabs.addView(fav, LinearLayout.LayoutParams(0, dp(44), 1f))
        root.addView(tabs)

        val scroll = ScrollView(this)
        contentHolder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        scroll.addView(contentHolder)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        searchBox.setOnEditorActionListener { _, _, _ ->
            renderMovies(filtered())
            false
        }
        searchBox.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) renderMovies(filtered()) }

        setContentView(root)
    }

    private fun filtered(): List<Movie> {
        val q = searchBox.text.toString().trim().lowercase()
        if (q.isEmpty()) return movies
        return movies.filter {
            it.title.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    private fun renderMovies(list: List<Movie>) {
        contentHolder.removeAllViews()
        if (list.isEmpty()) {
            contentHolder.addView(TextView(this).apply {
                text = "No titles found."
                setTextColor(Color.LTGRAY)
                textSize = 17f
                setPadding(0, dp(24), 0, 0)
            })
            return
        }

        list.groupBy { it.category }.forEach { (category, items) ->
            val title = TextView(this).apply {
                text = category
                setTextColor(Color.WHITE)
                textSize = 21f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(20), 0, dp(8))
            }
            contentHolder.addView(title)

            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val hsv = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(row)
            }
            contentHolder.addView(hsv, LinearLayout.LayoutParams(-1, dp(285)))

            items.forEach { movie -> row.addView(movieCard(movie)) }
        }
    }

    private fun movieCard(movie: Movie): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24, 24, 24))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val lp = LinearLayout.LayoutParams(dp(190), dp(270)).apply { setMargins(0, 0, dp(12), 0) }
        card.layoutParams = lp

        val poster = ImageView(this).apply {
            setBackgroundColor(Color.DKGRAY)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        card.addView(poster, LinearLayout.LayoutParams(-1, dp(155)))
        loadImage(movie.posterUrl, poster)

        val t = TextView(this).apply {
            text = movie.title
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(7), 0, 0)
        }
        card.addView(t)

        val d = TextView(this).apply {
            text = movie.description
            setTextColor(Color.LTGRAY)
            textSize = 12f
            maxLines = 2
        }
        card.addView(d, LinearLayout.LayoutParams(-1, 0, 1f))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val play = button("▶ Play") { play(movie) }
        val fav = button(if (isFavorite(movie)) "★" else "☆") {
            toggleFavorite(movie)
            renderMovies(filtered())
        }
        actions.addView(play, LinearLayout.LayoutParams(0, dp(42), 1f))
        actions.addView(fav, LinearLayout.LayoutParams(dp(52), dp(42)))
        card.addView(actions)
        card.setOnClickListener { play(movie) }
        return card
    }

    private fun button(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(50, 50, 50))
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun play(movie: Movie) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("title", movie.title)
            putExtra("url", movie.videoUrl)
        })
    }

    private fun toggleFavorite(movie: Movie) {
        prefs.edit().putBoolean(movie.title, !isFavorite(movie)).apply()
    }

    private fun isFavorite(movie: Movie): Boolean = prefs.getBoolean(movie.title, false)

    private fun loadImage(url: String, target: ImageView) {
        thread {
            try {
                val bitmap = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                runOnUiThread { target.setImageBitmap(bitmap) }
            } catch (_: Exception) { }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
