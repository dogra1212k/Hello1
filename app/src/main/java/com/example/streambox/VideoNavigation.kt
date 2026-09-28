package com.example.streambox

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** Converts supported app links to web links; never launches external applications. */
object VideoNavigation {
    fun isYouTubeWebUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        val host = uri.host?.lowercase().orEmpty()
        uri.scheme?.lowercase() in listOf("https", "http") && uri.userInfo == null &&
            (host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ||
                host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com"))
    }.getOrDefault(false)

    fun inAppUrl(value: String): String? = runCatching {
        if (value.startsWith("intent:", true)) return@runCatching intentUrl(value)
        val uri = URI(value)
        when (uri.scheme?.lowercase()) {
            "https" -> value.takeIf { !uri.host.isNullOrBlank() && uri.userInfo == null }
            "http" -> value.replaceFirst(Regex("^http:", RegexOption.IGNORE_CASE), "https:")
                .takeIf { !uri.host.isNullOrBlank() && uri.userInfo == null }
            "vnd.youtube", "youtube" -> {
                val target = uri.rawSchemeSpecificPart.removePrefix("//")
                when {
                    isYouTubeWebUrl("https://$target") -> "https://$target"
                    target.startsWith("watch?") -> "https://m.youtube.com/$target"
                    target.substringBefore('?').matches(Regex("[A-Za-z0-9_-]{11}")) ->
                        "https://m.youtube.com/watch?v=" + target.substringBefore('?')
                    else -> null
                }
            }
            else -> null
        }
    }.getOrNull()

    private fun intentUrl(value: String): String? {
        val marker = value.indexOf("#Intent;")
        if (marker < 0) return null
        val fields = value.substring(marker + 8).split(';')
        val scheme = fields.firstOrNull { it.startsWith("scheme=") }?.substringAfter('=')
        val target = value.substring(0, marker).substringAfter(':')
        if (scheme in listOf("https", "http", "vnd.youtube", "youtube")) {
            val web = inAppUrl("$scheme:$target")
            if (web != null && isYouTubeWebUrl(web)) return web
        }
        val fallback = fields.firstOrNull { it.startsWith("S.browser_fallback_url=") }?.substringAfter('=') ?: return null
        val decoded = URLDecoder.decode(fallback, "UTF-8")
        // Do not recursively interpret an untrusted fallback intent.
        if (!decoded.startsWith("https://", true) && !decoded.startsWith("http://", true)) return null
        return inAppUrl(decoded)
    }

    fun searchUrl(query: String) = "https://m.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
}
