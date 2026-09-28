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
    private lateinit var body: LinearLayout

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
            session.logout()
            startActivity(Intent(this@AdminActivity, AuthActivity::class.java))
            finishAffinity()
        },LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(bottom)

        setContentView(root)
    }

    private fun showDashboard() {
        body.removeAllViews()
        body.addView(info("Registered users", users.all.size.toString()))
        body.addView(info("Catalog titles", catalog.getMovies().size.toString()))
        body.addView(info("Admin access", "Active"))
        body.addView(TextView(this).apply {
            text="Changes made here are stored on this device and appear in the Home catalog immediately."
            setTextColor(Color.LTGRAY)
            textSize=14f
            setPadding(0,dp(12),0,0)
        })
    }

    private fun showUsers() {
        body.removeAllViews()
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
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("Delete user?")
                        .setMessage(email)
                        .setPositiveButton("Delete"){_,_->
                            users.edit().remove(email).apply()
                            showUsers()
                        }
                        .setNegativeButton("Cancel",null)
                        .show()
                }
            },LinearLayout.LayoutParams(dp(100),dp(48)))

            val lp=LinearLayout.LayoutParams(-1,-2)
            lp.setMargins(0,0,0,dp(10))
            body.addView(row,lp)
        }
    }

    private fun showCatalog() {
        body.removeAllViews()

        body.addView(Button(this).apply {
            text="+ Add movie"
            isAllCaps=false
            setOnClickListener { showAddMovieDialog() }
        })

        body.addView(Button(this).apply {
            text="Reset demo catalog"
            isAllCaps=false
            setOnClickListener {
                catalog.resetDefaults()
                showCatalog()
            }
        })

        val movies=catalog.getMovies()
        movies.forEachIndexed { index, movie ->
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                setBackgroundColor(Color.rgb(28,28,28))
                setPadding(dp(12),dp(8),dp(8),dp(8))
            }

            row.addView(TextView(this).apply {
                text="${movie.title}\n${movie.category}"
                setTextColor(Color.WHITE)
                textSize=16f
            },LinearLayout.LayoutParams(0,dp(66),1f))

            row.addView(Button(this).apply {
                text="Remove"
                isAllCaps=false
                setOnClickListener {
                    catalog.removeAt(index)
                    showCatalog()
                }
            },LinearLayout.LayoutParams(dp(110),dp(56)))

            val lp=LinearLayout.LayoutParams(-1,-2)
            lp.setMargins(0,0,0,dp(10))
            body.addView(row,lp)
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
                val t=title.text.toString().trim()
                val c=category.text.toString().trim()
                val d=description.text.toString().trim()
                val v=video.text.toString().trim()
                val p=poster.text.toString().trim()

                if(t.isNotBlank() && c.isNotBlank() && v.startsWith("http")) {
                    catalog.add(Movie(t,c,d,v,p))
                    showCatalog()
                } else {
                    Toast.makeText(this,"Title, category and valid video URL are required",Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel",null)
            .show()
    }

    private fun input(h:String)=EditText(this).apply {
        hint=h
        setSingleLine(true)
    }

    private fun navButton(label:String, action:()->Unit)=Button(this).apply {
        text=label
        isAllCaps=false
        setOnClickListener { action() }
    }

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
