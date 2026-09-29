package com.example.streambox

import android.content.Context

class CatalogStore(context: Context) {
    private val prefs = context.getSharedPreferences("catalog", Context.MODE_PRIVATE)
    private val defaults by lazy {
        context.assets.open("open_movies.json").bufferedReader().use { MovieJson.decode(it.readText()) }
    }

    private fun localMovies(): List<Movie> {
        val raw = prefs.getString("movies", null) ?: return defaults
        val movies = runCatching { MovieJson.decode(raw) }.getOrElse { return defaults }
        // Migrate only the old built-in demos; retain every user-created entry.
        return movies.mapNotNull { movie ->
            if (movie.id.isBlank() && movie.videoUrl in oldDemos) null
            else defaults.firstOrNull { it.videoUrl == movie.videoUrl }?.copy(tmdbId = movie.tmdbId) ?: movie
        }
    }

    fun getMovies(): MutableList<Movie> {
        val cloud = runCatching { MovieJson.decode(prefs.getString("cloudMovies", "[]") ?: "[]") }.getOrDefault(emptyList())
        return (cloud + localMovies()).distinctBy { it.videoUrl.ifBlank { it.id + it.title } }.toMutableList()
    }

    fun saveMovies(movies: List<Movie>) = prefs.edit().putString("movies", MovieJson.encode(movies)).apply()
    fun saveCloudMovies(movies: List<Movie>) = prefs.edit().putString("cloudMovies", MovieJson.encode(movies)).apply()
    fun add(movie: Movie) = saveMovies(localMovies() + movie)
    fun removeAt(index: Int) {
        val selected = getMovies().getOrNull(index) ?: return
        saveMovies(localMovies().filterNot { it == selected })
    }
    fun resetDefaults() = prefs.edit().remove("movies").apply()

    companion object {
        private val oldDemos = listOf("ForBiggerBlazes", "ForBiggerEscapes", "ForBiggerJoyrides").map {
            "https://storage.googleapis.com/gtv-videos-bucket/sample/$it.mp4"
        }.toSet()
    }
}
