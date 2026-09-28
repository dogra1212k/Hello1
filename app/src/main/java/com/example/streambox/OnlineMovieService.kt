package com.example.streambox

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.concurrent.thread

data class OnlineMovie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val releaseDate: String
)

class OnlineMovieService {
    private val apiKey = BuildConfig.TMDB_API_KEY.trim()

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun latest(callback: (List<OnlineMovie>?, String?) -> Unit) {
        request(
            "https://api.themoviedb.org/3/movie/now_playing" +
                "?api_key=${enc(apiKey)}&language=en-US&page=1&region=IN",
            callback
        )
    }

    fun search(query: String, callback: (List<OnlineMovie>?, String?) -> Unit) {
        request(
            "https://api.themoviedb.org/3/search/movie" +
                "?api_key=${enc(apiKey)}&language=en-US&include_adult=false&page=1&query=${enc(query)}",
            callback
        )
    }

    fun watchUrl(movie: OnlineMovie): String =
        "https://www.themoviedb.org/movie/${movie.id}/watch"

    private fun request(url: String, callback: (List<OnlineMovie>?, String?) -> Unit) {
        if (!isConfigured()) {
            callback(null, "TMDB API key is not configured")
            return
        }

        thread {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader()
                    .use { it.readText() }

                if (code !in 200..299) {
                    callback(null, "Online movie service error ($code)")
                    return@thread
                }

                val arr = JSONObject(body).optJSONArray("results")
                val items = mutableListOf<OnlineMovie>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val posterPath = o.optString("poster_path")
                        items += OnlineMovie(
                            id = o.optInt("id"),
                            title = o.optString("title").ifBlank { o.optString("original_title") },
                            overview = o.optString("overview"),
                            posterUrl = if (posterPath.isBlank() || posterPath == "null") ""
                                else "https://image.tmdb.org/t/p/w500$posterPath",
                            releaseDate = o.optString("release_date")
                        )
                    }
                }
                callback(items, null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Network error")
            }
        }
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
