package com.example.streambox

import android.app.DownloadManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
    private val session by lazy { SessionManager(this) }
    private val catalog by lazy { CatalogStore(this) }
    private val firebase by lazy { FirebaseGateway(this) }
    private val online by lazy { OnlineMovieService() }
    private var movies: List<Movie> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!session.isLoggedIn()){
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        buildUi()
    }

    private fun refreshCatalog() {
        movies=catalog.getMovies()
        if (online.isConfigured()) {
            loadHindiHome()
            return
        }
        if(firebase.isAvailable() && firebase.currentEmail()!=null) {
            firebase.loadMovies { cloud,error ->
                if(cloud != null && cloud.isNotEmpty()) {
                    movies=cloud
                    catalog.saveMovies(cloud)
                    renderMovies(filtered())
                } else {
                    renderMovies(filtered())
                    if(error!=null) Toast.makeText(this,"Using offline catalog",Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            renderMovies(filtered())
        }
    }

    private fun buildUi() {
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(16),dp(16),dp(16),dp(12))
        }

        val top=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
        }

        top.addView(TextView(this).apply {
            text="STREAMBOX"
            setTextColor(Color.rgb(229,9,20))
            textSize=28f
            setTypeface(typeface,Typeface.BOLD)
        }, LinearLayout.LayoutParams(0,-2,1f))

        top.addView(Button(this).apply {
            text=if(session.isAdmin()) "Admin" else "Logout"
            isAllCaps=false
            setOnClickListener {
                if(session.isAdmin()) {
                    startActivity(Intent(this@MainActivity,AdminActivity::class.java))
                } else {
                    firebase.signOut()
                    session.logout()
                    startActivity(Intent(this@MainActivity,AuthActivity::class.java))
                    finishAffinity()
                }
            }
        })

        root.addView(top)
        root.addView(TextView(this).apply {
            text="${session.email()} • ${if(firebase.isAvailable()) "Cloud" else "Local"} mode"
            setTextColor(Color.LTGRAY)
            textSize=13f
            setPadding(0,0,0,dp(12))
        })

        searchBox=EditText(this).apply {
            hint="Search movies, series, music & videos"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setBackgroundColor(Color.rgb(35,35,35))
            setPadding(dp(12),0,dp(12),0)
        }
        root.addView(searchBox,LinearLayout.LayoutParams(-1,dp(48)))

        val searchTypes=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setPadding(0,dp(6),0,0)
        }
        searchTypes.addView(button("All"){ runSearchFromBox() },LinearLayout.LayoutParams(0,dp(40),1f))
        searchTypes.addView(button("Music"){ openYouTubeSearch("music") },LinearLayout.LayoutParams(0,dp(40),1f))
        searchTypes.addView(button("Videos"){ openYouTubeSearch("video") },LinearLayout.LayoutParams(0,dp(40),1f))
        root.addView(searchTypes)

        val tabs=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setPadding(0,dp(8),0,dp(8))
        }
        tabs.addView(button("Home"){loadHindiHome()},LinearLayout.LayoutParams(0,dp(44),1f))
        tabs.addView(button("Latest"){loadLatestHindiOnline()},LinearLayout.LayoutParams(0,dp(44),1f))
        tabs.addView(button("Music"){loadMusicVideos()},LinearLayout.LayoutParams(0,dp(44),1f))
        tabs.addView(button("Series"){loadLatestSeries()},LinearLayout.LayoutParams(0,dp(44),1f))
        root.addView(tabs)

        val scroll=ScrollView(this)
        contentHolder=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(contentHolder)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))

        searchBox.setOnEditorActionListener { _,_,_->
            runSearchFromBox()
            true
        }
        setContentView(root)
        refreshCatalog()
    }

    private fun filtered():List<Movie>{
        val q=searchBox.text.toString().trim().lowercase()
        return if(q.isEmpty()) movies else movies.filter{
            it.title.lowercase().contains(q)||it.category.lowercase().contains(q)
        }
    }

    private fun loadHindiHome() {
        contentHolder.removeAllViews()
        contentHolder.addView(TextView(this).apply {
            text="Loading Hindi movies…"
            setTextColor(Color.LTGRAY)
            textSize=17f
        })
        online.hindiHome { list,error ->
            runOnUiThread {
                if(list!=null && list.isNotEmpty()) renderOnline("Hindi Movies • 3 × 3",list.take(9))
                else {
                    renderMovies(filtered())
                    Toast.makeText(this,error ?: "Could not load Hindi movies",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadLatestHindiOnline() {
        contentHolder.removeAllViews()
        contentHolder.addView(TextView(this).apply {
            text="Loading latest Hindi movies…"
            setTextColor(Color.LTGRAY)
            textSize=17f
        })
        online.latestHindi { list,error ->
            runOnUiThread {
                if(list!=null && list.isNotEmpty()) renderOnline("Latest Hindi Movies • 3 × 3",list.take(9))
                else {
                    Toast.makeText(this,error ?: "Could not load latest Hindi movies",Toast.LENGTH_LONG).show()
                    loadLatestOnline()
                }
            }
        }
    }

    private fun loadMusicVideos() {
        contentHolder.removeAllViews()
        contentHolder.addView(TextView(this).apply{
            text="Latest Hindi Music Videos • 3 × 3"
            setTextColor(Color.WHITE)
            textSize=21f
            setTypeface(typeface,Typeface.BOLD)
            setPadding(0,dp(20),0,dp(8))
        })
        val queries=listOf(
            "latest hindi songs official video",
            "new bollywood songs official video",
            "latest punjabi songs official video",
            "new hindi romantic songs official video",
            "latest hindi party songs official video",
            "new bollywood movie songs official video",
            "latest hindi sad songs official video",
            "trending hindi music videos official",
            "new indian music videos official"
        )
        val labels=listOf(
            "Latest Hindi","New Bollywood","Latest Punjabi",
            "Romantic","Party Hits","Movie Songs",
            "Sad Songs","Trending","New Indian"
        )
        val grid=GridLayout(this).apply {
            columnCount=3
            rowCount=3
            alignmentMode=GridLayout.ALIGN_BOUNDS
            useDefaultMargins=false
        }
        val width=(resources.displayMetrics.widthPixels-dp(32)-dp(16))/3
        queries.forEachIndexed { i,q ->
            val card=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                gravity=Gravity.CENTER
                setPadding(dp(6),dp(8),dp(6),dp(8))
                setBackgroundColor(Color.rgb(24,24,24))
                layoutParams=GridLayout.LayoutParams().apply {
                    width=width
                    height=dp(128)
                    setMargins(dp(2),dp(2),dp(2),dp(2))
                }
                setOnClickListener { openVideoSearchInApp(q) }
            }
            card.addView(TextView(this).apply {
                text="♫"
                textSize=30f
                gravity=Gravity.CENTER
                setTextColor(Color.rgb(229,9,20))
            },LinearLayout.LayoutParams(-1,0,1f))
            card.addView(TextView(this).apply {
                text=labels[i]
                gravity=Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize=13f
                setTypeface(typeface,Typeface.BOLD)
                maxLines=2
            },LinearLayout.LayoutParams(-1,dp(38)))
            grid.addView(card)
        }
        contentHolder.addView(grid,LinearLayout.LayoutParams(-1,-2))
        contentHolder.addView(TextView(this).apply {
            text="Tap a tile to browse official music videos inside StreamBox."
            setTextColor(Color.LTGRAY)
            textSize=12f
            setPadding(0,dp(8),0,dp(16))
        })
    }

    private fun openVideoSearchInApp(query:String) {
        val url="https://m.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query,"UTF-8")
        startActivity(Intent(this,WebVideoActivity::class.java).apply {
            putExtra("title","Music Videos")
            putExtra("url",url)
        })
    }

    private fun loadLatestOnline() {
        contentHolder.removeAllViews()
        contentHolder.addView(TextView(this).apply {
            text="Loading latest releases…"
            setTextColor(Color.LTGRAY)
            textSize=17f
        })
        online.latest { list,error ->
            runOnUiThread {
                if(list!=null) renderOnline("Latest releases • 3 × 3",list.take(9))
                else {
                    renderMovies(filtered())
                    Toast.makeText(this,error ?: "Could not load latest movies",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadLatestSeries() {
        contentHolder.removeAllViews()
        contentHolder.addView(TextView(this).apply {
            text="Loading web series…"
            setTextColor(Color.LTGRAY)
            textSize=17f
        })
        online.latestTv { list,error ->
            runOnUiThread {
                if(list!=null) renderOnline("Web series • 3 × 3",list.take(9))
                else {
                    renderMovies(filtered())
                    Toast.makeText(this,error ?: "Could not load web series",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun runSearchFromBox() {
        val q=searchBox.text.toString().trim()
        if(q.isNotEmpty()) searchOnline(q) else loadHindiHome()
    }

    private fun searchOnline(query:String) {
        contentHolder.removeAllViews()
        contentHolder.addView(TextView(this).apply {
            text="Searching movies, series and online video…"
            setTextColor(Color.LTGRAY)
            textSize=17f
        })
        online.search(query) { movieList,movieError ->
            online.searchTv(query) { tvList,tvError ->
                runOnUiThread {
                    contentHolder.removeAllViews()
                    appendOnlineSection("Movies", movieList ?: emptyList())
                    appendOnlineSection("Series & Shows", tvList ?: emptyList())
                    addYouTubeSearchSection(query)
                    if(movieList==null && tvList==null) {
                        Toast.makeText(this,movieError ?: tvError ?: "Online search unavailable",Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun addYouTubeSearchSection(query:String) {
        contentHolder.addView(TextView(this).apply{
            text="Music & Videos"
            setTextColor(Color.WHITE)
            textSize=21f
            setTypeface(typeface,Typeface.BOLD)
            setPadding(0,dp(20),0,dp(8))
        })
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        row.addView(button("♫ Music"){ openYouTubeSearch("music") },LinearLayout.LayoutParams(0,dp(48),1f))
        row.addView(button("▶ Videos"){ openYouTubeSearch("video") },LinearLayout.LayoutParams(0,dp(48),1f))
        contentHolder.addView(row,LinearLayout.LayoutParams(-1,-2))
        contentHolder.addView(TextView(this).apply {
            text="YouTube online search for: $query"
            setTextColor(Color.LTGRAY)
            textSize=12f
            setPadding(0,dp(6),0,dp(12))
        })
    }

    private fun openYouTubeSearch(kind:String) {
        val q=searchBox.text.toString().trim()
        if(q.isEmpty()) {
            Toast.makeText(this,"Type something in Search first",Toast.LENGTH_SHORT).show()
            return
        }
        val suffix=if(kind=="music") " music" else ""
        openVideoSearchInApp(q + suffix)
    }

    private fun renderOnline(title:String,list:List<OnlineMovie>){
        contentHolder.removeAllViews()
        appendOnlineSection(title,list)
    }

    private fun appendOnlineSection(title:String,list:List<OnlineMovie>){
        contentHolder.addView(TextView(this).apply{
            text=title
            setTextColor(Color.WHITE)
            textSize=21f
            setTypeface(typeface,Typeface.BOLD)
            setPadding(0,dp(20),0,dp(8))
        })
        if(list.isEmpty()){
            contentHolder.addView(TextView(this).apply{
                text="No online titles found."
                setTextColor(Color.LTGRAY)
                textSize=17f
            })
            return
        }
        val grid=GridLayout(this).apply {
            columnCount=3
            rowCount=3
            alignmentMode=GridLayout.ALIGN_BOUNDS
            useDefaultMargins=false
        }
        list.take(9).forEach { grid.addView(onlineCard(it)) }
        contentHolder.addView(grid,LinearLayout.LayoutParams(-1,-2))
    }

    private fun onlineCard(m:OnlineMovie):View{
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24,24,24))
            setPadding(dp(8),dp(8),dp(8),dp(8))
            layoutParams=GridLayout.LayoutParams().apply{
                width=(resources.displayMetrics.widthPixels-dp(48))/3
                height=dp(250)
                setMargins(dp(2),dp(2),dp(2),dp(6))
            }
        }
        val poster=ImageView(this).apply{
            setBackgroundColor(Color.DKGRAY)
            scaleType=ImageView.ScaleType.CENTER_CROP
        }
        card.addView(poster,LinearLayout.LayoutParams(-1,dp(135)))
        if(m.posterUrl.isNotBlank()) loadImage(m.posterUrl,poster)
        card.addView(TextView(this).apply{
            text=m.title
            setTextColor(Color.WHITE)
            textSize=13f
            setTypeface(typeface,Typeface.BOLD)
            maxLines=2
        })
        card.addView(TextView(this).apply{
            text=if(m.releaseDate.isBlank()) m.overview else "${m.releaseDate} • ${m.overview}"
            setTextColor(Color.LTGRAY)
            textSize=12f
            maxLines=3
        },LinearLayout.LayoutParams(-1,0,1f))
        card.addView(button("▶ Trailer"){
            openOnlineMovie(m)
        },LinearLayout.LayoutParams(-1,dp(42)))
        card.setOnClickListener { openOnlineMovie(m) }
        return card
    }

    private fun renderMovies(list:List<Movie>){
        contentHolder.removeAllViews()
        if(list.isEmpty()){
            contentHolder.addView(TextView(this).apply{
                text="No titles found."
                setTextColor(Color.LTGRAY)
                textSize=17f
            })
            return
        }
        list.groupBy{it.category}.forEach{(cat,items)->
            contentHolder.addView(TextView(this).apply{
                text=cat
                setTextColor(Color.WHITE)
                textSize=21f
                setTypeface(typeface,Typeface.BOLD)
                setPadding(0,dp(20),0,dp(8))
            })
            val grid=GridLayout(this).apply {
                columnCount=3
                rowCount=3
                alignmentMode=GridLayout.ALIGN_BOUNDS
                useDefaultMargins=false
            }
            items.take(9).forEach { grid.addView(movieCard(it)) }
            contentHolder.addView(grid,LinearLayout.LayoutParams(-1,-2))
        }
    }

    private fun movieCard(m:Movie):View{
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24,24,24))
            setPadding(dp(8),dp(8),dp(8),dp(8))
            layoutParams=GridLayout.LayoutParams().apply{
                width=(resources.displayMetrics.widthPixels-dp(48))/3
                height=dp(250)
                setMargins(dp(2),dp(2),dp(2),dp(6))
            }
        }
        val poster=ImageView(this).apply{
            setBackgroundColor(Color.DKGRAY)
            scaleType=ImageView.ScaleType.CENTER_CROP
        }
        card.addView(poster,LinearLayout.LayoutParams(-1,dp(135)))
        loadImage(m.posterUrl,poster)
        card.addView(TextView(this).apply{
            text=m.title
            setTextColor(Color.WHITE)
            textSize=16f
            setTypeface(typeface,Typeface.BOLD)
        })
        card.addView(TextView(this).apply{
            text=m.description
            setTextColor(Color.LTGRAY)
            textSize=12f
            maxLines=2
        },LinearLayout.LayoutParams(-1,0,1f))
        val a=LinearLayout(this)
        a.addView(button("▶ Watch"){play(m)},LinearLayout.LayoutParams(0,dp(42),1f))
        a.addView(button("↓"){download(m)},LinearLayout.LayoutParams(dp(52),dp(42)))
        a.addView(button(if(isFavorite(m))"★" else "☆"){
            toggleFavorite(m)
            renderMovies(filtered())
        },LinearLayout.LayoutParams(dp(52),dp(42)))
        card.addView(a)
        card.setOnClickListener{play(m)}
        return card
    }

    private fun button(t:String,c:()->Unit)=Button(this).apply{
        text=t
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(50,50,50))
        isAllCaps=false
        setOnClickListener{c()}
    }

    private fun play(m:Movie){
        startActivity(Intent(this,PlayerActivity::class.java).apply{
            putExtra("title",m.title)
            putExtra("url",m.videoUrl)
        })
    }

    private fun openOnlineMovie(m:OnlineMovie){
        startActivity(Intent(this,TrailerActivity::class.java).apply{
            putExtra("movieId",m.id)
            putExtra("mediaType",m.mediaType)
            putExtra("title",m.title)
        })
    }

    private fun download(m:Movie){
        try {
            val safeName=m.title.replace(Regex("[^A-Za-z0-9._-]"),"_") + ".mp4"
            val request=DownloadManager.Request(Uri.parse(m.videoUrl))
                .setTitle(m.title)
                .setDescription("Downloading for offline viewing")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this,Environment.DIRECTORY_MOVIES,safeName)
            val dm=getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this,"Download started",Toast.LENGTH_SHORT).show()
        } catch (_:Exception) {
            Toast.makeText(this,"Download could not start",Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleFavorite(m:Movie){prefs.edit().putBoolean(m.title,!isFavorite(m)).apply()}
    private fun isFavorite(m:Movie)=prefs.getBoolean(m.title,false)

    private fun loadImage(url:String,target:ImageView){
        if(url.isBlank()) return
        thread{
            try{
                val connection=URL(url).openConnection().apply{
                    connectTimeout=8000
                    readTimeout=8000
                }
                val b=connection.getInputStream().use{BitmapFactory.decodeStream(it)}
                if(!isFinishing && !isDestroyed) runOnUiThread{
                    if(b!=null) target.setImageBitmap(b)
                }
            }catch(_:Exception){}
        }
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
