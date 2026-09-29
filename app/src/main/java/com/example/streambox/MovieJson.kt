package com.example.streambox

import org.json.JSONArray
import org.json.JSONObject

object MovieJson {
    fun decode(raw: String): List<Movie> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            fun value(key: String, default: String = "") = o.optString(key, default).takeUnless { it == "null" } ?: default
            val title = value("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Movie(title, value("category"), value("description"), value("videoUrl"), value("posterUrl"),
                value("id"), o.optInt("tmdbId", 0).coerceAtLeast(0), value("mediaType", "movie"))
        }
    }

    fun encode(movies: List<Movie>): String = JSONArray().apply {
        movies.forEach { m -> put(JSONObject().apply {
            put("title", m.title); put("category", m.category); put("description", m.description)
            put("videoUrl", m.videoUrl); put("posterUrl", m.posterUrl); put("id", m.id)
            put("tmdbId", m.tmdbId); put("mediaType", m.mediaType)
        }) }
    }.toString()
}
