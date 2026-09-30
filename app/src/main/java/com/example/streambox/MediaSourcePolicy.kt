package com.example.streambox

import java.net.URI
import java.util.Locale

/** Only direct media enters the native player. Discovery pages use the separate Watch options screen. */
object MediaSourcePolicy {
    private val files = setOf("mp4", "m4v", "webm", "mkv")
    private val manifests = setOf("m3u8", "mpd")
    private val blocked = setOf("youtube.com", "youtu.be", "youtube-nocookie.com", "googlevideo.com", "ytimg.com")

    fun extension(url: String): String? = runCatching {
        val uri = URI(url)
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return null
        if (!uri.scheme.equals("https", true) || uri.userInfo != null || uri.fragment != null) return null
        if (blocked.any { host == it || host.endsWith(".$it") }) return null
        uri.path.orEmpty().substringAfterLast('/', "").substringAfterLast('.', "").lowercase(Locale.ROOT)
            .takeIf { it in files || it in manifests }
    }.getOrNull()

    fun isPlayable(url: String) = extension(url) != null
    fun isDownloadable(url: String) = extension(url) in files

    /** Explicit IDs avoid playing a different film/remake with the same title. */
    fun findMovie(movie: OnlineMovie, catalog: List<Movie>): Movie? = catalog.firstOrNull {
        it.tmdbId > 0 && it.tmdbId == movie.id && it.mediaType == movie.mediaType && isPlayable(it.videoUrl)
    }

    fun matches(movie: Movie, query: String) = query.isBlank() ||
        listOf(movie.title, movie.category, movie.description).any { it.contains(query.trim(), ignoreCase = true) }

    fun isMusic(movie: Movie) = movie.mediaType == "music" ||
        listOf("music", "song", "संगीत", "गाना").any { movie.category.contains(it, ignoreCase = true) }
}
