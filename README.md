# Hello1 / StreamBox Free

Android movie catalog and in-app video player, version 1.6.1.

Version 1.6.1 fixes the video-screen compilation error, keeps Reload on the current page after navigating, clears old connection errors on retry, and handles system Back through fullscreen and browser history. Trailer requests disable reload while loading and ignore obsolete callbacks.

## Movies and search

- **Home:** three-column, recycled poster grid. Scrolling loads the next page of Hindi movies; the old nine-card and forty-title caps are removed.
- **Latest Hindi:** automatically loads at least 120 unique available Hindi titles, newest release date first, then continues on scroll or **Load more**. Future releases are excluded. If the source has fewer titles, all available titles are shown.
- **Pagination:** duplicate titles are removed by media type and ID. Failed pages can be retried without clearing loaded titles. Switching tabs/searches ignores obsolete responses. Each query respects TMDB's maximum of 500 pages; “continuous scrolling” does not mean an infinite source catalog.
- **All search:** paginated movie and series results, matching saved videos, and buttons for matching music/videos.
- **Music / Videos:** YouTube's mobile search page opens inside StreamBox. Supported YouTube app links are converted to web URLs; unsupported external app schemes are blocked. Fullscreen, Back, retry, and WebView lifecycle cleanup are shared with trailers.
- **My Videos:** local/Firebase catalog with in-app direct-stream playback. Long press a card to download a direct file or save a favorite.
- **Posters:** recycled views with Glide image caching and a fallback logo.

TMDB supplies metadata and trailers, not full movie video files. Catalog cards say **Trailer**; saved direct streams say **Watch**. To publish full films, add video URLs you own or are authorized to distribute through the existing admin catalog. No explicit adult catalog is included. YouTube may restrict individual videos by region, login, age, or embedding settings; unavailable videos are not bypassed and no external app is launched.

## Configuration and APK build

The TMDB key is not committed to this public repository. Set the GitHub Actions repository secret **TMDB_API_KEY**. Optionally configure **GOOGLE_SERVICES_JSON_B64** for Firebase.

For local builds, pass `-PTMDB_API_KEY=YOUR_KEY` or use your private Gradle user properties. Do not commit a real key.

GitHub Actions runs:

```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug -PTMDB_API_KEY="$TMDB_API_KEY" --stacktrace
```

Download **streambox-debug-apk** from the completed **Android Build** run. Unit-test and lint reports are uploaded as **streambox-check-reports**. Without a configured key, saved videos still work, while online catalog requests show a configuration message.

## Validation

Unit tests cover 120-title pagination, deduplication, retries, stale tab/search responses, end-of-catalog handling, page limits, mixed search parsing, Hindi/date filters, and in-app deep-link conversion. Device checks: scroll beyond 120 movies, switch tabs during loading, retry after disconnecting, search Hindi text, play music/trailers, enter/exit fullscreen, and verify Back stays inside StreamBox until leaving the video screen.
