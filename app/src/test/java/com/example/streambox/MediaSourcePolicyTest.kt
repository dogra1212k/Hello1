package com.example.streambox

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class MediaSourcePolicyTest {
    private fun movie(url: String = "https://media.example/film.mp4", id: Int = 7, type: String = "movie") =
        Movie("Film", "Drama", "", url, "", tmdbId = id, mediaType = type)
    private fun online(id: Int = 7, type: String = "movie") = OnlineMovie(id, "Film", "", "", "2026-01-01", type)

    @Test fun acceptsHttpsFilesAndAdaptiveStreamsWithSignedQueries() {
        listOf("film.mp4", "film.MP4?token=abc%2B123", "movie.webm", "episode.mkv", "clip.m4v",
            "master.m3u8?signature=abc", "manifest.mpd").forEach {
            assertTrue(it, MediaSourcePolicy.isPlayable("https://media.example/$it"))
        }
    }

    @Test fun rejectsWebPagesAppLinksAndUnsupportedSchemes() {
        listOf("", "https://media.example/watch?id=7", "https://media.example/", "https://media.example/watch?file=film.mp4",
            "http://media.example/film.mp4", "file:///film.mp4", "content://video/film.mp4", "intent://film.mp4",
            "javascript:alert(1)", "https://user:secret@media.example/film.mp4", "https://media.example/film.mp4#fragment",
            "https:///film.mp4").forEach { assertFalse(it, MediaSourcePolicy.isPlayable(it)) }
    }

    @Test fun rejectsAllYoutubeEntrypointsEvenWhenDisguisedAsMedia() {
        listOf("youtube.com", "m.youtube.com", "www.youtube-nocookie.com", "youtu.be", "r1.googlevideo.com", "i.ytimg.com",
            "YOUTUBE.COM.").forEach {
            assertFalse(it, MediaSourcePolicy.isPlayable("https://$it/movie.mp4"))
        }
        assertFalse(MediaSourcePolicy.isPlayable("vnd.youtube:abcdefghijk"))
    }

    @Test fun onlyVideoFilesCanBeDownloadedNotStreamManifests() {
        assertTrue(MediaSourcePolicy.isDownloadable("https://media.example/film.webm?token=1"))
        assertFalse(MediaSourcePolicy.isDownloadable("https://media.example/master.m3u8"))
        assertFalse(MediaSourcePolicy.isDownloadable("https://media.example/stream.mpd"))
        assertFalse(MediaSourcePolicy.isDownloadable("https://media.example/watch"))
    }

    @Test fun titleAloneNeverMapsAMovieToTheWrongFullFilm() {
        assertNull(MediaSourcePolicy.findMovie(online(), listOf(movie(id = 0), movie(id = 99))))
    }

    @Test fun movieAndSeriesWithSameIdRemainDistinct() {
        val film = movie()
        val episode = movie(type = "tv")
        assertEquals(film, MediaSourcePolicy.findMovie(online(), listOf(episode, film)))
        assertEquals(episode, MediaSourcePolicy.findMovie(online(type = "tv"), listOf(film, episode)))
    }

    @Test fun linkedButUnplayableSourcesAreUnavailable() {
        assertNull(MediaSourcePolicy.findMovie(online(), listOf(movie("https://youtube.com/watch?v=abcdefghijk"))))
    }

    @Test fun musicAndHindiSearchUseTheSavedCatalog() {
        val music = Movie("नया गाना", "संगीत", "Hindi song", "https://media.example/song.mp4", "", mediaType = "music")
        assertTrue(MediaSourcePolicy.isMusic(music))
        assertTrue(MediaSourcePolicy.matches(music, "  गाना  "))
        assertTrue(MediaSourcePolicy.matches(music, "HINDI"))
        assertFalse(MediaSourcePolicy.matches(music, "unrelated"))
        assertFalse(MediaSourcePolicy.isMusic(movie()))
    }

    @Test fun oldCatalogJsonStillLoadsWithSafeDefaults() {
        val movie = MovieJson.decode("""[{"title":"Old film","videoUrl":"https://media.example/film.mp4","posterUrl":null}]""").single()
        assertEquals("movie", movie.mediaType)
        assertEquals(0, movie.tmdbId)
        assertEquals("", movie.posterUrl)
        assertTrue(MediaSourcePolicy.isPlayable(movie.videoUrl))
    }

    @Test fun catalogRoundTripRetainsExplicitSourceMappingAndUnicode() {
        val expected = movie().copy(title = "हिंदी फिल्म", description = "Credits\nLine 2", id = "cloud-id")
        assertEquals(listOf(expected), MovieJson.decode(MovieJson.encode(listOf(expected))))
        assertTrue(MovieJson.decode(MovieJson.encode(emptyList())).isEmpty())
    }

    @Test fun builtinCatalogContainsCreditedFullFilmsAndNoOldShortAds() {
        val entries = MovieJson.decode(File("src/main/assets/open_movies.json").readText())
        assertEquals(4, entries.size)
        assertEquals(4, entries.map { it.id }.distinct().size)
        entries.forEach {
            assertTrue(it.title, MediaSourcePolicy.isPlayable(it.videoUrl))
            assertTrue(it.description.contains("creativecommons.org/licenses/"))
            assertTrue(it.category.contains("Full short film"))
            assertFalse(it.videoUrl.contains("ForBigger"))
        }
    }
}
