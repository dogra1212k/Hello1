package com.example.streambox

import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class OnlineMovieServiceTest {
    private fun params(url: String) = URI(url).rawQuery.split('&').associate {
        it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8")
    }

    @Test fun latestUsesRequestedPageHindiAndDescendingPastReleaseDates() {
        val service = OnlineMovieService("test-key")
        val url = service.pageUrl(CatalogFeed.LATEST_HINDI, 6, "", "2026-09-28")
        assertEquals("/3/discover/movie", URI(url).path)
        val values = params(url)
        assertEquals("6", values["page"])
        assertEquals("hi", values["with_original_language"])
        assertEquals("primary_release_date.desc", values["sort_by"])
        assertEquals("2026-09-28", values["primary_release_date.lte"])
        assertEquals("false", values["include_adult"])
        service.close()
    }

    @Test fun homeCanRequestBeyondTheOldFortyTitleLimit() {
        val service = OnlineMovieService("key")
        val values = params(service.pageUrl(CatalogFeed.HOME, 50, "", "2026-09-28"))
        assertEquals("50", values["page"])
        assertEquals("popularity.desc", values["sort_by"])
        service.close()
    }

    @Test fun searchEncodesHindiAndPunctuationWithoutAddingQueryParameters() {
        val service = OnlineMovieService("key")
        val query = "हिंदी songs & page=500 + music"
        val url = service.pageUrl(CatalogFeed.SEARCH, 2, query, "2026-09-28")
        assertEquals("/3/search/multi", URI(url).path)
        assertEquals(query, params(url)["query"])
        assertEquals("2", params(url)["page"])
        service.close()
    }

    @Test fun parserRetainsMovieAndTvButSkipsPeopleAdultAndInvalidEntries() {
        val page = OnlineMovieService.parsePage("""{"page":3,"total_pages":8,"total_results":140,"results":[
            {"id":1,"media_type":"movie","title":"Hindi film","poster_path":null,"release_date":"2026-01-01"},
            {"id":1,"media_type":"tv","name":"Show","poster_path":"/poster.jpg","first_air_date":"2025-01-01"},
            {"id":3,"media_type":"person","name":"Person"},
            {"id":4,"media_type":"movie","title":"Excluded","adult":true},
            {"id":0,"media_type":"movie","title":"Invalid"}
        ]}""", CatalogFeed.SEARCH)
        assertEquals(3, page.page)
        assertEquals(8, page.totalPages)
        assertEquals(2, page.items.size)
        assertEquals("", page.items[0].posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", page.items[1].posterUrl)
        assertEquals("2025-01-01", page.items[1].releaseDate)
    }

    @Test fun serviceReturnsNetworkFailureWithoutDiscardingPagerState() {
        val service = OnlineMovieService("key") { throw java.net.UnknownHostException("hidden hostname") }
        val latch = CountDownLatch(1)
        var resultError: String? = null
        service.loadPage(CatalogFeed.HOME, 1) { page, error ->
            assertNull(page)
            resultError = error
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertTrue(resultError!!.contains("DNS"))
        service.close()
    }

    @Test fun missingKeyDoesNotSendNetworkRequest() {
        val service = OnlineMovieService("") { error("Network should not be called") }
        val latch = CountDownLatch(1)
        var resultError: String? = null
        service.loadPage(CatalogFeed.HOME, 1) { _, error -> resultError = error; latch.countDown() }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertTrue(resultError!!.contains("TMDB"))
        service.close()
    }

    @Test fun watchSourcesKeepMovieAndSeriesIdsSeparate() {
        val service = OnlineMovieService("key with & punctuation")
        assertEquals("/3/movie/7/watch/providers", URI(service.watchSourcesUrl(7, "movie")).path)
        assertEquals("/3/tv/7/watch/providers", URI(service.watchSourcesUrl(7, "tv")).path)
        assertEquals("key with & punctuation", params(service.watchSourcesUrl(7, "movie"))["api_key"])
        service.close()
    }

    @Test fun catalogResolutionRejectsWrongLanguageWrongYearAndAmbiguousRemakes() {
        val raw = """{"results":[
            {"id":7,"title":"Love Aaj Kal","release_date":"2009-07-31","original_language":"hi"},
            {"id":8,"title":"Love Aaj Kal","release_date":"2020-02-14","original_language":"hi"},
            {"id":9,"title":"Love Aaj Kal","release_date":"2020-01-01","original_language":"en"}
        ]}"""
        assertEquals(7, OnlineMovieService.exactHindiMovieId(raw, "Love Aaj Kal", 2009))
        assertEquals(8, OnlineMovieService.exactHindiMovieId(raw, "Love Aaj Kal", 2020))
        assertEquals(0, OnlineMovieService.exactHindiMovieId(raw, "Love Aaj Kal", 2021))
        val ambiguous = """{"results":[
            {"id":7,"title":"Film","release_date":"2020-01-01","original_language":"hi"},
            {"id":8,"title":"Film","release_date":"2020-01-01","original_language":"hi"}
        ]}"""
        assertEquals(0, OnlineMovieService.exactHindiMovieId(ambiguous, "Film", 2020))
    }

    @Test fun knownIdLoadsIndiaProvidersWithoutTitleSearch() {
        val calls = mutableListOf<String>()
        val service = OnlineMovieService("key") { url ->
            calls += url
            """{"results":{"IN":{"flatrate":[{"provider_name":"Service"}]}}}"""
        }
        val latch = CountDownLatch(1)
        var result: WatchSources? = null
        var resultId = 0
        var resultError: String? = null
        service.loadWatchSources(7, "tv", "Film", 2020) { id, sources, error ->
            resultId = id; result = sources; resultError = error; latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals(7, resultId)
        assertNull(resultError)
        assertEquals(listOf(WatchOffer("Service", "Subscription")), result!!.offers)
        assertEquals(1, calls.size)
        assertEquals("/3/tv/7/watch/providers", URI(calls.single()).path)
        service.close()
    }

    @Test fun seededMovieResolvesTitleAndYearBeforeFetchingExactProviders() {
        val calls = mutableListOf<String>()
        val service = OnlineMovieService("key") { url ->
            calls += url
            if (URI(url).path == "/3/search/movie") """{"results":[
                {"id":7,"title":"Film & Love","release_date":"2020-01-01","original_language":"hi"}
            ]}""" else """{"results":{"IN":{}}}"""
        }
        val latch = CountDownLatch(1)
        var resultId = 0
        service.loadWatchSources(0, "movie", "Film & Love", 2020) { id, _, _ -> resultId = id; latch.countDown() }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertEquals(7, resultId)
        assertEquals(2, calls.size)
        assertEquals("Film & Love", params(calls.first())["query"])
        assertEquals("2020", params(calls.first())["year"])
        assertEquals("/3/movie/7/watch/providers", URI(calls.last()).path)
        service.close()
    }

    @Test fun missingWatchKeyDoesNotAttemptNetworkAndAllowsDiscoveryFallback() {
        val service = OnlineMovieService("") { error("Network should not be called") }
        val latch = CountDownLatch(1)
        var resultError: String? = null
        service.loadWatchSources(7, "movie", "Film", 2020) { _, _, error -> resultError = error; latch.countDown() }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertNotNull(resultError)
        service.close()
    }

}
