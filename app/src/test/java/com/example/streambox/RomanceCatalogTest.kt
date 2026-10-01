package com.example.streambox

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class RomanceCatalogTest {
    private val movies = (1..23).map { RomanticMovie("Film $it", 2000 + it) }

    @Test fun pagesHaveThreeColumnsAndThreeRowsWithoutSkippingMovies() {
        assertEquals(3, RomanceCatalog.COLUMNS)
        assertEquals(3, RomanceCatalog.ROWS)
        val pages = (0..2).map { RomanceCatalog.page(movies, index = it) }
        assertEquals(listOf(9, 9, 5), pages.map { it.movies.size })
        assertEquals(movies, pages.flatMap { it.movies })
        assertEquals(listOf(3, 3, 3), pages.map { it.pages })
    }

    @Test fun pagesClampAfterSearchOrRotationAndHaveNoPhantomEmptyPage() {
        assertEquals(0, RomanceCatalog.page(movies, index = -1).index)
        assertEquals(2, RomanceCatalog.page(movies, index = 500).index)
        val result = RomanceCatalog.page(movies.take(18))
        assertEquals(2, result.pages)
        val empty = RomanceCatalog.page(movies, "no match", 4)
        assertEquals(0, empty.pages)
        assertEquals(0, empty.index)
        assertTrue(empty.movies.isEmpty())
    }

    @Test fun searchesTitlesYearsAndHindiAliases() {
        val entries = listOf(RomanticMovie("Jab We Met", 2007, "जब वी मेट"), RomanticMovie("Love Aaj Kal", 2020))
        assertEquals(entries.take(1), RomanceCatalog.page(entries, "  JAB  ").movies)
        assertEquals(entries.take(1), RomanceCatalog.page(entries, "मेट").movies)
        assertEquals(entries.takeLast(1), RomanceCatalog.page(entries, "2020").movies)
    }

    @Test fun catalogRetainsRemakesByYearAndRejectsNonHindiNonRomanceAndInvalidData() {
        val entries = RomanceCatalog.decode("""[
            {"title":"Love Aaj Kal","year":2009,"language":"hi","genre":"romance"},
            {"title":"Love Aaj Kal","year":2020,"language":"hi","genre":"romance"},
            {"title":"Love Aaj Kal","year":2020,"language":"hi","genre":"romance"},
            {"title":"Other","year":2020,"language":"en","genre":"romance"},
            {"title":"Other","year":2020,"language":"hi","genre":"action"},
            {"title":null,"year":2020,"language":"hi","genre":"romance"},
            {"title":"Bad year","year":0,"language":"hi","genre":"romance"}
        ]""")
        assertEquals(listOf(2009, 2020), entries.map { it.year })
    }

    @Test fun bundledCatalogOffersMoreThan100RealDistinctTitlesWithoutPretendingToIncludeFullFiles() {
        val raw = File("src/main/assets/hindi_romance.json").readText()
        val entries = RomanceCatalog.decode(raw)
        assertEquals(170, entries.size)
        assertTrue(entries.size > 100)
        assertEquals(entries.size, entries.map { it.title to it.year }.distinct().size)
        assertTrue(entries.any { it.title == "Veer-Zaara" && it.year == 2004 })
        assertTrue(entries.any { it.title == "Jab We Met" && it.year == 2007 })
        assertFalse(raw.contains("videoUrl"))
        val pages = (0 until RomanceCatalog.page(entries).pages).flatMap { RomanceCatalog.page(entries, index = it).movies }
        assertEquals(entries, pages)
    }
}
