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
    private val session by lazy { SessionManager(this) }
    private val catalog by lazy { CatalogStore(this) }
    private val firebase by lazy { FirebaseGateway(this) }
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

    override fun onResume() {
        super.onResume()
        if (::contentHolder.isInitialized) refreshCatalog()
    }

    private fun refreshCatalog() {
        if(firebase.isAvailable() && firebase.currentEmail()!=null) {
            firebase.loadMovies { cloud,error ->
                if(cloud != null && cloud.isNotEmpty()) {
                    movies=cloud
                    catalog.saveMovies(cloud)
                    renderMovies(filtered())
                } else {
                    movies=catalog.getMovies()
                    renderMovies(filtered())
                    if(error!=null) Toast.makeText(this,"Using offline catalog",Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            movies=catalog.getMovies()
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
            hint="Search movies or categories"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setBackgroundColor(Color.rgb(35,35,35))
            setPadding(dp(12),0,dp(12),0)
        }
        root.addView(searchBox,LinearLayout.LayoutParams(-1,dp(48)))

        val tabs=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setPadding(0,dp(8),0,dp(8))
        }
        tabs.addView(button("Home"){renderMovies(filtered())},LinearLayout.LayoutParams(0,dp(44),1f))
        tabs.addView(button("My List"){renderMovies(movies.filter{isFavorite(it)})},LinearLayout.LayoutParams(0,dp(44),1f))
        root.addView(tabs)

        val scroll=ScrollView(this)
        contentHolder=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(contentHolder)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))

        searchBox.setOnEditorActionListener { _,_,_-> renderMovies(filtered()); false }
        setContentView(root)
        refreshCatalog()
    }

    private fun filtered():List<Movie>{
        val q=searchBox.text.toString().trim().lowercase()
        return if(q.isEmpty()) movies else movies.filter{
            it.title.lowercase().contains(q)||it.category.lowercase().contains(q)
        }
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
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            val hsv=HorizontalScrollView(this).apply{
                isHorizontalScrollBarEnabled=false
                addView(row)
            }
            contentHolder.addView(hsv,LinearLayout.LayoutParams(-1,dp(285)))
            items.forEach{row.addView(movieCard(it))}
        }
    }

    private fun movieCard(m:Movie):View{
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24,24,24))
            setPadding(dp(8),dp(8),dp(8),dp(8))
            layoutParams=LinearLayout.LayoutParams(dp(190),dp(270)).apply{setMargins(0,0,dp(12),0)}
        }
        val poster=ImageView(this).apply{
            setBackgroundColor(Color.DKGRAY)
            scaleType=ImageView.ScaleType.CENTER_CROP
        }
        card.addView(poster,LinearLayout.LayoutParams(-1,dp(155)))
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
        a.addView(button("▶ Play"){play(m)},LinearLayout.LayoutParams(0,dp(42),1f))
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

    private fun toggleFavorite(m:Movie){prefs.edit().putBoolean(m.title,!isFavorite(m)).apply()}
    private fun isFavorite(m:Movie)=prefs.getBoolean(m.title,false)

    private fun loadImage(url:String,target:ImageView){
        thread{
            try{
                val b=URL(url).openStream().use{BitmapFactory.decodeStream(it)}
                runOnUiThread{target.setImageBitmap(b)}
            }catch(_:Exception){}
        }
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
