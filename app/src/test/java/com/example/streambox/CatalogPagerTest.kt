package com.example.streambox

import org.junit.Assert.*
import org.junit.Test

class CatalogPagerTest {
    private fun movie(id: Int, type: String = "movie") = OnlineMovie(id, "Title $id", "", "", "2026-09-01", type)
    private fun page(number: Int, items: List<OnlineMovie>, total: Int = 20) = OnlinePage(number, total, total * 20, items)

    @Test fun sixPagesLoad120UniqueTitlesAndCanContinue() {
        val pager = CatalogPager()
        for (number in 1..6) {
            val ticket = pager.beginLoad()!!
            assertEquals(number, ticket.page)
            assertNull(pager.beginLoad())
            assertTrue(pager.accept(ticket, page(number, (1..20).map { movie((number - 1) * 20 + it) })))
        }
        assertEquals(CatalogPager.LATEST_TARGET, pager.items.size)
        assertEquals(7, pager.nextPage)
        assertTrue(pager.hasMore)
    }

    @Test fun duplicatesAreRemovedWithoutDiscardingMovieAndTvWithSameId() {
        val pager = CatalogPager()
        pager.accept(pager.beginLoad()!!, page(1, listOf(movie(1), movie(1), movie(1, "tv"))))
        pager.accept(pager.beginLoad()!!, page(2, listOf(movie(1), movie(2))))
        assertEquals(listOf("movie:1", "tv:1", "movie:2"), pager.items.map { "${it.mediaType}:${it.id}" })
    }

    @Test fun failedPageRetriesWithoutSkippingOrDiscardingLoadedMovies() {
        val pager = CatalogPager()
        pager.accept(pager.beginLoad()!!, page(1, listOf(movie(1))))
        val failed = pager.beginLoad()!!
        assertTrue(pager.fail(failed, "offline"))
        assertEquals(1, pager.items.size)
        assertEquals(2, pager.beginLoad()!!.page)
        assertNull(pager.error)
    }

    @Test fun tabSwitchIgnoresOldResponseAndOldFailure() {
        val pager = CatalogPager()
        val old = pager.beginLoad()!!
        pager.reset()
        val current = pager.beginLoad()!!
        assertFalse(pager.accept(old, page(1, listOf(movie(99)))))
        assertFalse(pager.fail(old, "offline"))
        assertTrue(pager.loading)
        assertTrue(pager.accept(current, page(1, listOf(movie(1)))))
        assertEquals(1, pager.items.single().id)
    }

    @Test fun exhaustedCatalogStopsEvenIfFewerThan120Titles() {
        val pager = CatalogPager()
        pager.accept(pager.beginLoad()!!, page(1, listOf(movie(1)), total = 1))
        assertFalse(pager.hasMore)
        assertNull(pager.beginLoad())
    }

    @Test fun emptyResultsStopAtServerEnd() {
        val pager = CatalogPager()
        pager.accept(pager.beginLoad()!!, page(1, emptyList(), total = 0))
        assertFalse(pager.hasMore)
    }

    @Test fun emptyFilteredSearchPageDoesNotSkipLaterPages() {
        val pager = CatalogPager()
        pager.accept(pager.beginLoad()!!, page(1, emptyList(), total = 2))
        assertEquals(2, pager.beginLoad()!!.page)
    }

    @Test fun neverRequestsPage501() {
        val pager = CatalogPager()
        repeat(500) { index -> pager.accept(pager.beginLoad()!!, page(index + 1, emptyList(), total = 1000)) }
        assertFalse(pager.hasMore)
        assertNull(pager.beginLoad())
    }

    @Test fun mismatchedResponseCanBeRetried() {
        val pager = CatalogPager()
        pager.accept(pager.beginLoad()!!, page(2, listOf(movie(2))))
        assertEquals(0, pager.items.size)
        assertNotNull(pager.error)
        assertEquals(1, pager.beginLoad()!!.page)
    }
}
