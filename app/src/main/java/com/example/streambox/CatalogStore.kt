package com.example.streambox

import android.content.Context
import android.content.SharedPreferences

class CatalogStore private constructor(
    private val prefs: SharedPreferences,
    loadDefaults: () -> List<Movie>
) {
    constructor(context: Context) : this(context.getSharedPreferences("catalog", Context.MODE_PRIVATE), {
        context.assets.open("open_movies.json").bufferedReader().use { MovieJson.decode(it.readText()) }
    })

    internal constructor(prefs: SharedPreferences, defaults: List<Movie>) : this(prefs, { defaults })

    private val defaults by lazy(loadDefaults)

    internal fun getLocalMovies(): List<Movie> {
        val raw = prefs.getString("movies", null) ?: return defaults
        val movies = runCatching { MovieJson.decode(raw) }.getOrElse { return defaults }
        // Migrate only the old built-in demos; retain every user-created entry.
        return movies.mapNotNull { movie ->
            if (movie.id.isBlank() && movie.videoUrl in oldDemos) null
            else {
                val builtin = defaults.firstOrNull {
                    if (movie.id.isNotBlank()) it.id == movie.id
                    else it.videoUrl == movie.videoUrl || legacySources[it.id] == movie.videoUrl
                }
                builtin?.copy(tmdbId = movie.tmdbId, mediaType = movie.mediaType) ?: movie
            }
        }
    }

    fun getMovies(): MutableList<Movie> {
        val cloud = runCatching { MovieJson.decode(prefs.getString("cloudMovies", "[]") ?: "[]") }.getOrDefault(emptyList())
        return (cloud + getLocalMovies()).distinctBy { it.videoUrl.ifBlank { it.id + it.title } }.toMutableList()
    }

    fun saveMovies(movies: List<Movie>) = prefs.edit().putString("movies", MovieJson.encode(movies)).apply()
    fun saveCloudMovies(movies: List<Movie>) = prefs.edit().putString("cloudMovies", MovieJson.encode(movies)).apply()
    fun add(movie: Movie) = saveMovies(getLocalMovies() + movie)
    fun removeAt(index: Int) {
        val movies = getLocalMovies().toMutableList()
        if (index !in movies.indices) return
        movies.removeAt(index)
        saveMovies(movies)
    }
    fun resetDefaults() = prefs.edit().remove("movies").apply()

    companion object {
        private val legacySources = mapOf(
            "open-big-buck-bunny" to "BigBuckBunny",
            "open-elephants-dream" to "ElephantsDream",
            "open-sintel" to "Sintel",
            "open-tears-of-steel" to "TearsOfSteel"
        ).mapValues { (_, filename) -> "https://storage.googleapis.com/gtv-videos-bucket/sample/$filename.mp4" }
        private val oldDemos = listOf("ForBiggerBlazes", "ForBiggerEscapes", "ForBiggerJoyrides").map {
            "https://storage.googleapis.com/gtv-videos-bucket/sample/$it.mp4"
        }.toSet()
    }
}
