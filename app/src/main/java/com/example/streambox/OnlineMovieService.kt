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
    val releaseDate: String,
    val mediaType: String = "movie"
)

class OnlineMovieService {
    private val apiKey = BuildConfig.TMDB_API_KEY.trim()

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun hindiHome(callback: (List<OnlineMovie>?, String?) -> Unit) {
        requestMany(
            listOf(
                "https://api.themoviedb.org/3/discover/movie?api_key=${enc(apiKey)}&language=hi-IN&with_original_language=hi&sort_by=popularity.desc&include_adult=false&page=1&region=IN",
                "https://api.themoviedb.org/3/discover/movie?api_key=${enc(apiKey)}&language=hi-IN&with_original_language=hi&sort_by=popularity.desc&include_adult=false&page=2&region=IN"
            ),
            "movie",
            callback
        )
    }

    fun latest(callback: (List<OnlineMovie>?, String?) -> Unit) {
        requestMovies(
            "https://api.themoviedb.org/3/movie/now_playing" +
                "?api_key=${enc(apiKey)}&language=en-US&page=1&region=IN",
            callback
        )
    }

    fun search(query: String, callback: (List<OnlineMovie>?, String?) -> Unit) {
        requestMovies(
            "https://api.themoviedb.org/3/search/movie" +
                "?api_key=${enc(apiKey)}&language=hi-IN&include_adult=false&page=1&query=${enc(query)}",
            callback
        )
    }

    fun searchTv(query: String, callback: (List<OnlineMovie>?, String?) -> Unit) {
        requestMovies(
            "https://api.themoviedb.org/3/search/tv" +
                "?api_key=${enc(apiKey)}&language=hi-IN&include_adult=false&page=1&query=${enc(query)}",
            "tv",
            callback
        )
    }

    fun latestTv(callback: (List<OnlineMovie>?, String?) -> Unit) {
        requestMovies(
            "https://api.themoviedb.org/3/tv/on_the_air" +
                "?api_key=${enc(apiKey)}&language=hi-IN&page=1",
            "tv",
            callback
        )
    }

    fun trailerKey(mediaType: String, movieId: Int, callback: (String?, String?) -> Unit) {
        if (!isConfigured()) {
            callback(null, "TMDB API key is not configured")
            return
        }

        thread {
            try {
                val safeType = if (mediaType == "tv") "tv" else "movie"
                val url = "https://api.themoviedb.org/3/$safeType/$movieId/videos" +
                    "?api_key=${enc(apiKey)}&language=en-US"
                val body = get(url)
                val arr = JSONObject(body).optJSONArray("results")
                var fallback: String? = null
                var officialTrailer: String? = null

                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        if (o.optString("site") != "YouTube") continue
                        val key = o.optString("key")
                        if (key.isBlank()) continue
                        if (fallback == null) fallback = key
                        val type = o.optString("type")
                        val official = o.optBoolean("official", false)
                        if (type.equals("Trailer", true) && official) {
                            officialTrailer = key
                            break
                        }
                        if (officialTrailer == null && type.equals("Trailer", true)) {
                            officialTrailer = key
                        }
                    }
                }
                callback(officialTrailer ?: fallback, null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Could not load trailer")
            }
        }
    }

    private fun requestMany(urls: List<String>, mediaType: String, callback: (List<OnlineMovie>?, String?) -> Unit) {
        if (!isConfigured()) {
            callback(null, "TMDB API key is not configured")
            return
        }
        thread {
            try {
                val all = mutableListOf<OnlineMovie>()
                urls.forEach { url ->
                    val body = get(url)
                    val arr = JSONObject(body).optJSONArray("results")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val posterPath = o.optString("poster_path")
                            all += OnlineMovie(
                                id = o.optInt("id"),
                                title = o.optString("title").ifBlank {
                                    o.optString("name").ifBlank { o.optString("original_title").ifBlank { o.optString("original_name") } }
                                },
                                overview = o.optString("overview"),
                                posterUrl = if (posterPath.isBlank() || posterPath == "null") "" else "https://image.tmdb.org/t/p/w500$posterPath",
                                releaseDate = o.optString("release_date").ifBlank { o.optString("first_air_date") },
                                mediaType = mediaType
                            )
                        }
                    }
                }
                callback(all.distinctBy { it.id }.take(40), null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Network error")
            }
        }
    }

    private fun requestMovies(url: String, mediaType: String = "movie", callback: (List<OnlineMovie>?, String?) -> Unit) {
        if (!isConfigured()) {
            callback(null, "TMDB API key is not configured")
            return
        }

        thread {
            try {
                val body = get(url)
                val arr = JSONObject(body).optJSONArray("results")
                val items = mutableListOf<OnlineMovie>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val posterPath = o.optString("poster_path")
                        items += OnlineMovie(
                            id = o.optInt("id"),
                            title = o.optString("title").ifBlank {
                                o.optString("name").ifBlank { o.optString("original_title").ifBlank { o.optString("original_name") } }
                            },
                            overview = o.optString("overview"),
                            posterUrl = if (posterPath.isBlank() || posterPath == "null") ""
                                else "https://image.tmdb.org/t/p/w500$posterPath",
                            releaseDate = o.optString("release_date").ifBlank { o.optString("first_air_date") },
                            mediaType = mediaType
                        )
                    }
                }
                callback(items, null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Network error")
            }
        }
    }

    private fun get(url: String): String {
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
        if (code !in 200..299) error("Online movie service error ($code)")
        return body
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
