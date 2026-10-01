package com.example.streambox

import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test

class WatchSourcesTest {
    @Test fun indiaOffersKeepCostTypesAndDeduplicateRepeatedEntries() {
        val sources = WatchSources.decode("""{"results":{
            "US":{"flatrate":[{"provider_name":"US-only"}]},
            "IN":{"link":"https://www.themoviedb.org/movie/7-film/watch?locale=IN",
              "flatrate":[{"provider_name":"Service A"},{"provider_name":"Service A"}],
              "rent":[{"provider_name":"Service A"},{"provider_name":null}],
              "free":[{"provider_name":"Service B"}],"ads":[{"provider_name":"Service C"}],
              "buy":[{"provider_name":"Service D"}]}
        }}""")
        assertEquals(5, sources.offers.size)
        assertTrue(sources.offers.contains(WatchOffer("Service A", "Rent")))
        assertTrue(sources.offers.contains(WatchOffer("Service A", "Subscription")))
        assertFalse(sources.offers.any { it.provider == "US-only" || it.provider == "null" })
        assertEquals("https://www.themoviedb.org/movie/7-film/watch?locale=IN", sources.link)
    }

    @Test fun missingCountryOrEmptyOffersDoNotImplyFreeOrWorldwideAvailability() {
        assertEquals(WatchSources(emptyList(), null), WatchSources.decode("""{"results":{"US":{}}}"""))
        assertEquals(WatchSources(emptyList(), null), WatchSources.decode("""{"results":{"IN":{}}}"""))
    }

    @Test fun onlyOfficialTmdbWatchPagesCanBeOpenedFromProviderResponses() {
        assertNotNull(WatchLinks.providerLink("https://www.themoviedb.org/movie/7/watch?locale=IN"))
        assertNotNull(WatchLinks.providerLink("https://www.themoviedb.org/tv/7/watch?locale=IN"))
        listOf("http://www.themoviedb.org/movie/7/watch", "https://www.themoviedb.org.evil.example/movie/7/watch",
            "https://user@www.themoviedb.org/movie/7/watch", "https://www.themoviedb.org:8443/movie/7/watch",
            "https://www.themoviedb.org/movie/7/watch#external", "https://www.themoviedb.org/login",
            "javascript:alert(1)", "https://youtube.com/watch?v=abcdefghijk").forEach {
            assertNull(it, WatchLinks.providerLink(it))
        }
    }

    @Test fun searchSafelyEncodesHindiAndPunctuationAndRemainsSeparateFromPlayback() {
        val title = "हिंदी & film? q=other + love"
        val uri = URI(WatchLinks.search(title))
        assertEquals("www.justwatch.com", uri.host)
        assertEquals("/in/search", uri.path)
        assertEquals(title, URLDecoder.decode(uri.rawQuery.substringAfter("q="), "UTF-8"))
        assertEquals(1, uri.rawQuery.split('&').size)
        assertFalse(MediaSourcePolicy.isPlayable(uri.toString()))
    }
}
