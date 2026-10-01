package com.example.streambox

import java.net.URI
import java.net.URLEncoder
import org.json.JSONObject

data class WatchOffer(val provider: String, val kind: String)
data class WatchSources(val offers: List<WatchOffer>, val link: String?) {
    companion object {
        /** Availability is regional; never substitute another country's offers. */
        fun decode(raw: String, region: String = "IN"): WatchSources {
            val country = JSONObject(raw).optJSONObject("results")?.optJSONObject(region)
                ?: return WatchSources(emptyList(), null)
            val offers = buildList {
                listOf("free" to "Free", "ads" to "With ads", "flatrate" to "Subscription",
                    "rent" to "Rent", "buy" to "Buy").forEach { (key, label) ->
                    val entries = country.optJSONArray(key) ?: return@forEach
                    for (i in 0 until entries.length()) {
                        val name = entries.optJSONObject(i)?.optString("provider_name")?.trim().orEmpty()
                        if (name.isNotBlank() && name != "null") add(WatchOffer(name, label))
                    }
                }
            }.distinct()
            return WatchSources(offers, WatchLinks.providerLink(country.optString("link")))
        }
    }
}

/** Discovery pages are kept separate from direct files accepted by the native player. */
object WatchLinks {
    fun search(title: String) = "https://www.justwatch.com/in/search?q=" +
        URLEncoder.encode(title.trim(), "UTF-8")

    fun providerLink(value: String): String? = runCatching {
        val uri = URI(value)
        value.takeIf {
            uri.scheme == "https" && uri.host in setOf("www.themoviedb.org", "themoviedb.org") &&
                uri.userInfo == null && uri.port == -1 && uri.fragment == null &&
                uri.path.matches(Regex("/(movie|tv)/[1-9][0-9]*(?:-[^/]+)?/watch"))
        }
    }.getOrNull()
}
