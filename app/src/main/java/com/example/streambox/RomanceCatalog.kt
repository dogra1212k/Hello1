package com.example.streambox

import java.util.Locale
import org.json.JSONArray

data class RomanticMovie(val title: String, val year: Int, val hindiTitle: String = "")
data class RomancePage(val movies: List<RomanticMovie>, val index: Int, val pages: Int, val total: Int)

object RomanceCatalog {
    const val COLUMNS = 3
    const val ROWS = 3
    const val PAGE_SIZE = COLUMNS * ROWS

    fun decode(raw: String): List<RomanticMovie> {
        val entries = JSONArray(raw)
        return (0 until entries.length()).mapNotNull { index ->
            val entry = entries.optJSONObject(index) ?: return@mapNotNull null
            val title = entry.optString("title").trim()
            val year = entry.optInt("year")
            if (title.isBlank() || title == "null" || year !in 1900..2100 ||
                entry.optString("language") != "hi" || entry.optString("genre") != "romance") return@mapNotNull null
            RomanticMovie(title, year, entry.optString("hindiTitle").takeUnless { it == "null" }.orEmpty())
        }.distinctBy { it.title.lowercase(Locale.ROOT) to it.year }
    }

    fun page(movies: List<RomanticMovie>, query: String = "", index: Int = 0): RomancePage {
        val text = query.trim()
        val matches = movies.filter { text.isBlank() || it.title.contains(text, true) ||
            it.hindiTitle.contains(text, true) || it.year.toString().contains(text) }
        val pages = (matches.size + PAGE_SIZE - 1) / PAGE_SIZE
        val safeIndex = index.coerceIn(0, (pages - 1).coerceAtLeast(0))
        return RomancePage(matches.drop(safeIndex * PAGE_SIZE).take(PAGE_SIZE), safeIndex, pages, matches.size)
    }
}
