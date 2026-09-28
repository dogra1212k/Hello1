package com.example.streambox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CatalogStore(context: Context) {
    private val prefs = context.getSharedPreferences("catalog", Context.MODE_PRIVATE)

    private val defaults = listOf(
        Movie("Big Buck Bunny","Animation","Open movie demo. Replace this URL with content you own or are licensed to stream.","https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4","https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg"),
        Movie("Elephant Dream","Sci‑Fi","Open movie demo for testing the streaming player.","https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4","https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg"),
        Movie("For Bigger Blazes","Action","Short demo video for your home feed.","https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4","https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg"),
        Movie("For Bigger Escape","Adventure","Short demo video. Add your own catalog later.","https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4","https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg")
    )

    fun getMovies(): MutableList<Movie> {
        val raw = prefs.getString("movies", null) ?: return defaults.toMutableList()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Movie(
                    o.optString("title"),
                    o.optString("category"),
                    o.optString("description"),
                    o.optString("videoUrl"),
                    o.optString("posterUrl")
                )
            }
        } catch (_: Exception) {
            defaults.toMutableList()
        }
    }

    fun saveMovies(movies: List<Movie>) {
        val arr = JSONArray()
        movies.forEach { m ->
            arr.put(JSONObject().apply {
                put("title", m.title)
                put("category", m.category)
                put("description", m.description)
                put("videoUrl", m.videoUrl)
                put("posterUrl", m.posterUrl)
            })
        }
        prefs.edit().putString("movies", arr.toString()).apply()
    }

    fun add(movie: Movie) {
        val list = getMovies()
        list.add(movie)
        saveMovies(list)
    }

    fun removeAt(index: Int) {
        val list = getMovies()
        if (index in list.indices) {
            list.removeAt(index)
            saveMovies(list)
        }
    }

    fun resetDefaults() = prefs.edit().remove("movies").apply()
}
