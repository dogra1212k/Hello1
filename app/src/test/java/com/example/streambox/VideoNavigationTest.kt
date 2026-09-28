package com.example.streambox

import org.junit.Assert.*
import org.junit.Test

class VideoNavigationTest {
    @Test fun normalSearchAndWatchRemainWebUrls() {
        listOf("https://m.youtube.com/results?search_query=hindi", "https://www.youtube.com/watch?v=abcdefghijk",
            "https://youtu.be/abcdefghijk", "https://www.youtube-nocookie.com/embed/abcdefghijk").forEach {
            assertEquals(it, VideoNavigation.inAppUrl(it))
            assertTrue(VideoNavigation.isYouTubeWebUrl(it))
        }
    }

    @Test fun youtubeDeepLinksConvertToWebUrls() {
        assertEquals("https://m.youtube.com/watch?v=abcdefghijk", VideoNavigation.inAppUrl("vnd.youtube:abcdefghijk"))
        assertEquals("https://m.youtube.com/watch?v=abcdefghijk", VideoNavigation.inAppUrl("youtube://watch?v=abcdefghijk"))
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", VideoNavigation.inAppUrl(
            "intent://www.youtube.com/watch?v=abcdefghijk#Intent;scheme=https;package=com.google.android.youtube;end"))
    }

    @Test fun intentFallbackStaysInWebView() {
        assertEquals("https://m.youtube.com/watch?v=abcdefghijk", VideoNavigation.inAppUrl(
            "intent://unsupported#Intent;scheme=market;S.browser_fallback_url=https%3A%2F%2Fm.youtube.com%2Fwatch%3Fv%3Dabcdefghijk;end"))
    }

    @Test fun unsupportedSchemesAndUnsafeFallbacksAreBlocked() {
        listOf("market://details?id=x", "file:///etc/file", "javascript:alert(1)", "data:text/html,hello", "tel:1234",
            "intent://x#Intent;scheme=file;S.browser_fallback_url=javascript%3Aalert%281%29;end",
            "https://user:password@youtube.com/watch", "vnd.youtube:bad-id", "intent:broken").forEach {
            assertNull("Unexpectedly accepted $it", VideoNavigation.inAppUrl(it))
        }
    }

    @Test fun fakeYoutubeDomainsDoNotMatch() {
        listOf("https://youtube.com.evil.example/watch", "https://notyoutube.com/watch", "file://youtube.com/watch").forEach {
            assertFalse(VideoNavigation.isYouTubeWebUrl(it))
        }
    }
}
