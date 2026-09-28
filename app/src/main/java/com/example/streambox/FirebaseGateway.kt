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

    fun currentEmail(): String? =
        if (isAvailable()) FirebaseAuth.getInstance().currentUser?.email else null

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (!isAvailable()) {
            onResult(false, "Firebase not configured")
            return
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener onResult(false, "User id missing")
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(mapOf("email" to email, "role" to "user"))
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { onResult(false, it.message) }
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

    fun isCurrentUserAdmin(onResult: (Boolean) -> Unit) {
        if (!isAvailable()) return onResult(false)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(false)
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { onResult(it.getString("role") == "admin") }
            .addOnFailureListener { onResult(false) }
    }

    fun loadMovies(onResult: (List<Movie>?, String?) -> Unit) {
        if (!isAvailable()) return onResult(null, "Firebase not configured")
        FirebaseFirestore.getInstance().collection("movies").get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    Movie(
                        title = title,
                        category = d.getString("category") ?: "Other",
                        description = d.getString("description") ?: "",
                        videoUrl = d.getString("videoUrl") ?: "",
                        posterUrl = d.getString("posterUrl") ?: "",
                        id = d.id
                    )
                }
                onResult(list, null)
            }
            .addOnFailureListener { onResult(null, it.message) }
    }

    fun addMovie(movie: Movie, onResult: (Boolean, String?) -> Unit) {
        if (!isAvailable()) return onResult(false, "Firebase not configured")
        val data = mapOf(
            "title" to movie.title,
            "category" to movie.category,
            "description" to movie.description,
            "videoUrl" to movie.videoUrl,
            "posterUrl" to movie.posterUrl
        )
        FirebaseFirestore.getInstance().collection("movies").add(data)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun deleteMovie(movie: Movie, onResult: (Boolean, String?) -> Unit) {
        if (!isAvailable() || movie.id.isBlank()) return onResult(false, "Cloud movie id missing")
        FirebaseFirestore.getInstance().collection("movies").document(movie.id).delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun listUsers(onResult: (List<String>?, String?) -> Unit) {
        if (!isAvailable()) return onResult(null, "Firebase not configured")
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { snap ->
                onResult(snap.documents.mapNotNull { it.getString("email") }.sorted(), null)
            }
            .addOnFailureListener { onResult(null, it.message) }
    }

    fun signOut() {
        if (isAvailable()) FirebaseAuth.getInstance().signOut()
    }
}
