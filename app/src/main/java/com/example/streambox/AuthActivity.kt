package com.example.streambox

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {
    private val users by lazy { getSharedPreferences("users", MODE_PRIVATE) }
    private val session by lazy { SessionManager(this) }
    private val firebase by lazy { FirebaseGateway(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (session.isLoggedIn()) return openMain()
        showLogin()
    }

    private fun base(title: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))
            setBackgroundColor(Color.rgb(25,25,25))
        }
        val brand = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        }
        brand.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_streambox_logo)
            contentDescription = "StreamBox A logo"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(64), dp(64)))
        brand.addView(TextView(this).apply {
            text="STREAMBOX"
            textSize=30f
            setTextColor(Color.rgb(229,9,20))
            setTypeface(typeface, Typeface.BOLD)
            gravity=Gravity.CENTER
            setPadding(dp(6),0,0,0)
        })
        root.addView(brand, LinearLayout.LayoutParams(-1,-2))
        card.addView(TextView(this).apply {
            text=title
            textSize=24f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text=if(firebase.isAvailable()) "Cloud mode" else "Local fallback mode"
            setTextColor(Color.LTGRAY)
        })
        root.addView(card, LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
        return card
    }

    private fun field(hint: String, password: Boolean=false) = EditText(this).apply {
        this.hint=hint
        setHintTextColor(Color.GRAY)
        setTextColor(Color.WHITE)
        setSingleLine(true)
        setPadding(dp(12),0,dp(12),0)
        setBackgroundColor(Color.rgb(40,40,40))
        if(password) inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun showLogin() {
        val card=base("Login")
        val email=field("Email")
        val pass=field("Password", true)
        card.addView(email, LinearLayout.LayoutParams(-1,dp(52)))
        card.addView(space())
        card.addView(pass, LinearLayout.LayoutParams(-1,dp(52)))

        card.addView(Button(this).apply {
            text="Login"
            setOnClickListener {
                val e=email.text.toString().trim().lowercase()
                val p=pass.text.toString()
                if(firebase.isAvailable()) {
                    firebase.signIn(e,p) { ok,msg ->
                        if(ok) {
                            firebase.isCurrentUserAdmin { admin ->
                                session.login(e, admin)
                                openMain()
                            }
                        } else toast(msg ?: "Firebase login failed")
                    }
                } else {
                    if(e.isNotEmpty() && users.getString(e,null)==p) {
                        session.login(e)
                        openMain()
                    } else toast("Invalid email or password")
                }
            }
        })

        card.addView(Button(this).apply {
            text="Create account"
            setOnClickListener { showSignup() }
        })

        if(!firebase.isAvailable()) {
            card.addView(Button(this).apply {
                text="Admin login"
                setOnClickListener { showAdmin() }
            })
        }
    }

    private fun showSignup() {
        val card=base("Create account")
        val email=field("Email")
        val pass=field("Password", true)
        val confirm=field("Confirm password", true)

        card.addView(email, LinearLayout.LayoutParams(-1,dp(52)))
        card.addView(space())
        card.addView(pass, LinearLayout.LayoutParams(-1,dp(52)))
        card.addView(space())
        card.addView(confirm, LinearLayout.LayoutParams(-1,dp(52)))

        card.addView(Button(this).apply {
            text="Sign up"
            setOnClickListener {
                val e=email.text.toString().trim().lowercase()
                val p=pass.text.toString()
                when {
                    e.isBlank() || !e.contains("@") -> toast("Enter a valid email")
                    p.length < 6 -> toast("Password must be at least 6 characters")
                    p != confirm.text.toString() -> toast("Passwords do not match")
                    firebase.isAvailable() -> firebase.signUp(e,p) { ok,msg ->
                        if(ok) {
                            session.login(e)
                            openMain()
                        } else toast(msg ?: "Firebase signup failed")
                    }
                    users.contains(e) -> toast("Account already exists")
                    else -> {
                        users.edit().putString(e,p).apply()
                        session.login(e)
                        openMain()
                    }
                }
            }
        })

        card.addView(Button(this).apply {
            text="Back to login"
            setOnClickListener { showLogin() }
        })
    }

    private fun showAdmin() {
        val card=base("Admin access")
        val code=field("Admin ID code", true)
        card.addView(code, LinearLayout.LayoutParams(-1,dp(52)))
        card.addView(Button(this).apply {
            text="Open admin"
            setOnClickListener {
                if(code.text.toString()=="98789") {
                    session.login("admin@streambox.local", true)
                    startActivity(Intent(this@AuthActivity, AdminActivity::class.java))
                    finish()
                } else toast("Invalid admin code")
            }
        })
        card.addView(Button(this).apply {
            text="Back"
            setOnClickListener { showLogin() }
        })
    }

    private fun openMain(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun space()=Space(this).apply { layoutParams=LinearLayout.LayoutParams(1,dp(10)) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
