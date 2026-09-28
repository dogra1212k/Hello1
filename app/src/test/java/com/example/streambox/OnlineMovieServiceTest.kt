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

    @Test fun choosesOfficialTrailerAndRejectsMalformedEmbedKeys() {
        val key = OnlineMovieService.trailerFromJson("""{"results":[
            {"site":"YouTube","type":"Trailer","key":"bad\"<key>","official":true},
            {"site":"YouTube","type":"Teaser","key":"abcdefghijk","official":false},
            {"site":"YouTube","type":"Trailer","key":"abcdefghij2","official":true}
        ]}""")
        assertEquals("abcdefghij2", key)
    }
}
