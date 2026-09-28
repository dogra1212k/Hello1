package com.example.streambox

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    private val session by lazy { SessionManager(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!session.isLoggedIn() || !session.isAdmin()){ finish(); return }
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(20)); setBackgroundColor(Color.BLACK) }
        root.addView(TextView(this).apply { text="ADMIN DASHBOARD"; textSize=28f; setTextColor(Color.rgb(229,9,20)); setTypeface(typeface, Typeface.BOLD) })
        root.addView(TextView(this).apply { text="Manage StreamBox Free"; setTextColor(Color.LTGRAY); textSize=15f; setPadding(0,0,0,dp(20)) })
        root.addView(info("Users", "Local account system enabled"))
        root.addView(info("Catalog", "Demo catalog active • Firebase-ready next"))
        root.addView(info("App control", "Admin-only area"))
        root.addView(Button(this).apply { text="Open app"; setOnClickListener { startActivity(Intent(this@AdminActivity, MainActivity::class.java)) } })
        root.addView(Button(this).apply { text="Logout admin"; setOnClickListener { session.logout(); startActivity(Intent(this@AdminActivity, AuthActivity::class.java)); finishAffinity() } })
        setContentView(root)
    }
    private fun info(a:String,b:String)=TextView(this).apply { text="$a\n$b"; setTextColor(Color.WHITE); textSize=18f; setBackgroundColor(Color.rgb(28,28,28)); setPadding(dp(16),dp(16),dp(16),dp(16)); val p=LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(12)); layoutParams=p }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}