package com.example.streambox

data class OnlineMovie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val releaseDate: String,
    val mediaType: String = "movie"
)

data class OnlinePage(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val items: List<OnlineMovie>
)

enum class CatalogFeed(val title: String) {
    HOME("Hindi Movies"), LATEST_HINDI("Latest Hindi"), SERIES("Series"), SEARCH("Search results")
}

/** Keeps pagination retries and late callbacks from mixing different tabs or searches. */
class CatalogPager {
    data class Request(val generation: Int, val page: Int)

    private var generation = 0
    private var inFlight: Request? = null
    private val seen = mutableSetOf<String>()
    val items = mutableListOf<OnlineMovie>()
    var nextPage = 1
        private set
    var totalPages = MAX_PAGES
        private set
    var totalResults = 0
        private set
    var error: String? = null
        private set
    val loading get() = inFlight != null
    val hasMore get() = nextPage <= totalPages

    fun reset() {
        generation++
        inFlight = null
        nextPage = 1
        totalPages = MAX_PAGES
        totalResults = 0
        error = null
        items.clear()
        seen.clear()
    }

    fun beginLoad(): Request? {
        if (loading || !hasMore) return null
        return Request(generation, nextPage).also { inFlight = it; error = null }
    }

    fun accept(request: Request, result: OnlinePage): Boolean {
        if (inFlight != request || request.generation != generation) return false
        if (result.page != request.page) return fail(request, "Unexpected page received. Please retry.")
        result.items.forEach { if (seen.add("${it.mediaType}:${it.id}")) items += it }
        totalPages = result.totalPages.coerceIn(0, MAX_PAGES)
        totalResults = result.totalResults.coerceAtLeast(0)
        nextPage = request.page + 1
        inFlight = null
        return true
    }

    fun fail(request: Request, message: String): Boolean {
        if (inFlight != request || request.generation != generation) return false
        inFlight = null
        error = message
        return true
    }

    companion object {
        const val MAX_PAGES = 500 // TMDB's per-query limit; do not request page 501.
        const val LATEST_TARGET = 120
    }
}
