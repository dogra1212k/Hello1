package com.example.streambox

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    private val session by lazy { SessionManager(this) }
    private val users by lazy { getSharedPreferences("users", MODE_PRIVATE) }
    private val catalog by lazy { CatalogStore(this) }
    private val firebase by lazy { FirebaseGateway(this) }
    private lateinit var body: LinearLayout
    private var cloudMovies: List<Movie> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!session.isLoggedIn() || !session.isAdmin()){
            finish()
            return
        }
        buildUi()
        showDashboard()
    }

    private fun buildUi() {
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(16),dp(16),dp(16))
            setBackgroundColor(Color.BLACK)
        }

        root.addView(TextView(this).apply {
            text="ADMIN DASHBOARD"
            textSize=27f
            setTextColor(Color.rgb(229,9,20))
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text=if(firebase.isAvailable()) "Firebase admin mode" else "Local fallback admin"
            setTextColor(Color.LTGRAY)
        })

        val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        nav.addView(navButton("Dashboard"){showDashboard()},LinearLayout.LayoutParams(0,dp(46),1f))
        nav.addView(navButton("Users"){showUsers()},LinearLayout.LayoutParams(0,dp(46),1f))
        nav.addView(navButton("Catalog"){showCatalog()},LinearLayout.LayoutParams(0,dp(46),1f))
        root.addView(nav)

        val scroll=ScrollView(this)
        body=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(0,dp(12),0,dp(12))
        }
        scroll.addView(body)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))

        val bottom=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        bottom.addView(navButton("Open app"){
            startActivity(Intent(this@AdminActivity, MainActivity::class.java))
        },LinearLayout.LayoutParams(0,dp(48),1f))
        bottom.addView(navButton("Logout"){
            firebase.signOut()
            session.logout()
            startActivity(Intent(this@AdminActivity, AuthActivity::class.java))
            finishAffinity()
        },LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(bottom)

        setContentView(root)
    }

    private fun showDashboard() {
        body.removeAllViews()
        body.addView(info("Mode", if(firebase.isAvailable()) "Firebase cloud" else "Local"))
        body.addView(info("Admin access", "Active"))
        if(firebase.isAvailable()) {
            firebase.loadMovies { list,_ ->
                body.addView(info("Cloud catalog titles", (list?.size ?: 0).toString()))
            }
        } else {
            body.addView(info("Registered users", users.all.size.toString()))
            body.addView(info("Catalog titles", catalog.getMovies().size.toString()))
        }
    }

    private fun showUsers() {
        body.removeAllViews()
        if(firebase.isAvailable()) {
            body.addView(TextView(this).apply{
                text="Firebase users"
                setTextColor(Color.WHITE)
                textSize=18f
            })
            firebase.listUsers { list,error ->
                if(list==null) {
                    body.addView(info("Users", error ?: "Could not load users"))
                } else if(list.isEmpty()) {
                    body.addView(info("Users","No user profiles found"))
                } else {
                    list.forEach { email -> body.addView(info("User",email)) }
                }
            }
            body.addView(TextView(this).apply{
                text="Deleting Firebase Authentication accounts requires a trusted server/Admin SDK, so the Android client only displays cloud users."
                setTextColor(Color.LTGRAY)
                textSize=13f
                setPadding(0,dp(12),0,0)
            })
            return
        }

        val all=users.all.keys.sorted()
        if(all.isEmpty()) {
            body.addView(info("Users", "No registered users yet"))
            return
        }

        all.forEach { email ->
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                setBackgroundColor(Color.rgb(28,28,28))
                setPadding(dp(12),dp(8),dp(8),dp(8))
            }
            row.addView(TextView(this).apply {
                text=email
                setTextColor(Color.WHITE)
                textSize=16f
            },LinearLayout.LayoutParams(0,dp(48),1f))
            row.addView(Button(this).apply {
                text="Delete"
                isAllCaps=false
                setOnClickListener {
                    users.edit().remove(email).apply()
                    showUsers()
                }
            },LinearLayout.LayoutParams(dp(100),dp(48)))
            body.addView(row)
        }
    }

    private fun showCatalog() {
        body.removeAllViews()
        body.addView(Button(this).apply {
            text="+ Add movie"
            isAllCaps=false
            setOnClickListener { showAddMovieDialog() }
        })

        if(firebase.isAvailable()) {
            firebase.loadMovies { list,error ->
                cloudMovies=list ?: emptyList()
                if(list==null) {
                    body.addView(info("Catalog",error ?: "Could not load"))
                    return@loadMovies
                }
                if(list.isEmpty()) body.addView(info("Catalog","No cloud movies yet"))
                list.forEach { movie -> body.addView(cloudMovieRow(movie)) }
            }
        } else {
            body.addView(Button(this).apply {
                text="Reset demo catalog"
                isAllCaps=false
                setOnClickListener {
                    catalog.resetDefaults()
                    showCatalog()
                }
            })
            catalog.getMovies().forEachIndexed { index,movie ->
                body.addView(localMovieRow(index,movie))
            }
        }
    }

    private fun cloudMovieRow(movie: Movie): LinearLayout {
        return LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(28,28,28))
            setPadding(dp(12),dp(8),dp(8),dp(8))
            addView(TextView(this@AdminActivity).apply {
                text="${movie.title}\n${movie.category}"
                setTextColor(Color.WHITE)
                textSize=16f
            },LinearLayout.LayoutParams(0,dp(66),1f))
            addView(Button(this@AdminActivity).apply {
                text="Remove"
                isAllCaps=false
                setOnClickListener {
                    firebase.deleteMovie(movie) { ok,msg ->
                        if(ok) showCatalog() else Toast.makeText(this@AdminActivity,msg ?: "Delete failed",Toast.LENGTH_LONG).show()
                    }
                }
            },LinearLayout.LayoutParams(dp(110),dp(56)))
        }
    }

    private fun localMovieRow(index:Int, movie:Movie): LinearLayout {
        return LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(28,28,28))
            setPadding(dp(12),dp(8),dp(8),dp(8))
            addView(TextView(this@AdminActivity).apply {
                text="${movie.title}\n${movie.category}"
                setTextColor(Color.WHITE)
                textSize=16f
            },LinearLayout.LayoutParams(0,dp(66),1f))
            addView(Button(this@AdminActivity).apply {
                text="Remove"
                isAllCaps=false
                setOnClickListener {
                    catalog.removeAt(index)
                    showCatalog()
                }
            },LinearLayout.LayoutParams(dp(110),dp(56)))
        }
    }

    private fun showAddMovieDialog() {
        val wrap=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(8),dp(18),0)
        }
        val title=input("Title")
        val category=input("Category")
        val description=input("Description")
        val video=input("Video URL")
        val poster=input("Poster URL")
        listOf(title,category,description,video,poster).forEach { wrap.addView(it) }

        AlertDialog.Builder(this)
            .setTitle("Add movie")
            .setView(wrap)
            .setPositiveButton("Add"){_,_->
                val movie=Movie(
                    title.text.toString().trim(),
                    category.text.toString().trim(),
                    description.text.toString().trim(),
                    video.text.toString().trim(),
                    poster.text.toString().trim()
                )
                if(movie.title.isBlank() || movie.category.isBlank() || !movie.videoUrl.startsWith("http")) {
                    Toast.makeText(this,"Title, category and valid video URL are required",Toast.LENGTH_LONG).show()
                } else if(firebase.isAvailable()) {
                    firebase.addMovie(movie) { ok,msg ->
                        if(ok) showCatalog() else Toast.makeText(this,msg ?: "Add failed",Toast.LENGTH_LONG).show()
                    }
                } else {
                    catalog.add(movie)
                    showCatalog()
                }
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun input(h:String)=EditText(this).apply { hint=h; setSingleLine(true) }
    private fun navButton(label:String, action:()->Unit)=Button(this).apply { text=label; isAllCaps=false; setOnClickListener { action() } }
    private fun info(a:String,b:String)=TextView(this).apply {
        text="$a\n$b"
        setTextColor(Color.WHITE)
        textSize=18f
        setBackgroundColor(Color.rgb(28,28,28))
        setPadding(dp(16),dp(16),dp(16),dp(16))
        val p=LinearLayout.LayoutParams(-1,-2)
        p.setMargins(0,0,0,dp(12))
        layoutParams=p
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
