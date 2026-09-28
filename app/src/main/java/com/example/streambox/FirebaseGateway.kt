package com.example.streambox

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseGateway(private val context: Context) {

    fun isAvailable(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
        } catch (_: Exception) {
            false
        }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (!isAvailable()) {
            onResult(false, "Firebase not configured")
            return
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("users")
                    .document(it.user?.uid ?: email)
                    .set(mapOf("email" to email, "role" to "user"))
                onResult(true, null)
            }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (!isAvailable()) {
            onResult(false, "Firebase not configured")
            return
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun signOut() {
        if (isAvailable()) FirebaseAuth.getInstance().signOut()
    }
}
