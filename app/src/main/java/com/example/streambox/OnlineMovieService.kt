package com.example.streambox

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.Future

class OnlineMovieService(
    private val apiKey: String = BuildConfig.TMDB_API_KEY.trim(),
    private val transport: (String) -> String = ::fetchJson
) {
    private val executor = Executors.newFixedThreadPool(2)

    fun isConfigured() = apiKey.isNotBlank()

    fun loadPage(feed: CatalogFeed, page: Int, query: String = "", callback: (OnlinePage?, String?) -> Unit): Future<*> {
        return executor.submit {
            try {
                check(isConfigured()) { "Online catalog is unavailable in this build. Add the TMDB key and rebuild." }
                val body = transport(pageUrl(feed, page, query, todayInIndia()))
                if (!Thread.currentThread().isInterrupted) callback(parsePage(body, feed), null)
            } catch (e: Exception) {
                if (!Thread.currentThread().isInterrupted) callback(null, friendlyError(e))
            }
        }
    }

    fun loadWatchSources(id: Int, type: String, title: String, year: Int,
                         callback: (Int, WatchSources?, String?) -> Unit): Future<*> = executor.submit {
        var resolvedId = id.coerceAtLeast(0)
        try {
            check(isConfigured()) { "Movie service access is unavailable." }
            require(type == "movie" || type == "tv") { "Unsupported media type." }
            if (resolvedId == 0 && type == "movie" && year > 0) {
                val url = "https://api.themoviedb.org/3/search/movie?api_key=${enc(apiKey)}" +
                    "&language=en-IN&include_adult=false&query=${enc(title)}&year=$year"
                resolvedId = exactHindiMovieId(transport(url), title, year)
            }
            if (Thread.currentThread().isInterrupted) return@submit
            val sources = if (resolvedId > 0) WatchSources.decode(transport(watchSourcesUrl(resolvedId, type)))
                else WatchSources(emptyList(), null)
            if (!Thread.currentThread().isInterrupted) callback(resolvedId, sources, null)
        } catch (e: Exception) {
            if (!Thread.currentThread().isInterrupted) callback(resolvedId, null, friendlyError(e))
        }
    }

    internal fun watchSourcesUrl(id: Int, type: String): String {
        require(id > 0 && type in setOf("movie", "tv"))
        return "https://api.themoviedb.org/3/$type/$id/watch/providers?api_key=${enc(apiKey)}"
    }

    internal fun pageUrl(feed: CatalogFeed, page: Int, query: String, today: String): String {
        require(page in 1..CatalogPager.MAX_PAGES) { "End of available catalog." }
        val path = when (feed) {
            CatalogFeed.HOME, CatalogFeed.LATEST_HINDI -> "discover/movie"
            CatalogFeed.SERIES -> "tv/on_the_air"
            CatalogFeed.SEARCH -> "search/multi"
        }
        val params = linkedMapOf(
            "api_key" to apiKey, "language" to "hi-IN", "include_adult" to "false", "page" to page.toString()
        )
        when (feed) {
            CatalogFeed.HOME, CatalogFeed.LATEST_HINDI -> {
                params["with_original_language"] = "hi"
                params["include_video"] = "false"
                params["sort_by"] = if (feed == CatalogFeed.HOME) "popularity.desc" else "primary_release_date.desc"
                params["primary_release_date.lte"] = today
            }
            CatalogFeed.SEARCH -> params["query"] = query
            CatalogFeed.SERIES -> Unit
        }
        return "https://api.themoviedb.org/3/$path?" + params.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
    }

    fun close() { executor.shutdownNow() }

    companion object {
        /** Resolve catalog metadata only. Native playback still requires an explicit TMDB ID mapping. */
        internal fun exactHindiMovieId(body: String, title: String, year: Int): Int {
            fun normalized(value: String) = value.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
            val expected = normalized(title)
            if (expected.isBlank() || year !in 1900..2100) return 0
            val entries = JSONObject(body).optJSONArray("results") ?: return 0
            val ids = (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                if (entry.optBoolean("adult") || entry.optString("original_language") != "hi" ||
                    entry.optString("release_date").take(4) != year.toString()) return@mapNotNull null
                if (listOf("title", "original_title").none { normalized(entry.optString(it)) == expected })
                    return@mapNotNull null
                entry.optInt("id").takeIf { it > 0 }
            }.distinct()
            return ids.singleOrNull() ?: 0
        }

        internal fun parsePage(body: String, feed: CatalogFeed): OnlinePage {
            val root = JSONObject(body)
            val arr = root.optJSONArray("results") ?: error("Invalid catalog response. Please retry.")
            val items = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    if (o.optBoolean("adult", false)) continue
                    val type = if (feed == CatalogFeed.SEARCH) o.optString("media_type")
                        else if (feed == CatalogFeed.SERIES) "tv" else "movie"
                    if (type != "movie" && type != "tv") continue
                    val id = o.optInt("id")
                    val title = listOf("title", "name", "original_title", "original_name")
                        .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() && it != "null" } } ?: continue
                    if (id <= 0) continue
                    val poster = o.optString("poster_path")
                    val dateKey = if (type == "tv") "first_air_date" else "release_date"
                    add(OnlineMovie(id, title, o.optString("overview").takeUnless { it == "null" }.orEmpty(),
                        if (poster.startsWith("/")) "https://image.tmdb.org/t/p/w342$poster" else "",
                        o.optString(dateKey).takeUnless { it == "null" }.orEmpty(), type))
                }
            }
            return OnlinePage(root.optInt("page", 1), root.optInt("total_pages", 0), root.optInt("total_results", 0), items)
        }

        private fun todayInIndia() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date())

        private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

        private fun friendlyError(e: Exception) = when (e) {
            is UnknownHostException -> "Cannot reach the movie service. Check your internet or DNS, then retry."
            is SocketTimeoutException -> "The movie service timed out. Tap Retry."
            else -> e.message ?: "Could not load movies. Tap Retry."
        }

        private fun fetchJson(url: String): String {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
            }
            try {
                when (val code = connection.responseCode) {
                    in 200..299 -> return connection.inputStream.bufferedReader().use { it.readText() }
                    401, 403 -> error("Movie service access is unavailable. Check the configured TMDB key.")
                    429 -> error("Movie service is busy. Wait a moment, then tap Retry.")
                    else -> error("Movie service unavailable ($code). Tap Retry.")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
