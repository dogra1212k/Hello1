# Hello1 / StreamBox Free

Android movie catalog and native in-app video player, version **1.7.0**.

## What changed

- **No YouTube connection:** YouTube searches, embeds, trailer requests, WebViews and app-link handlers have been removed. Every Watch action opens the native StreamBox player.
- **Home / Watch now:** shows playable saved full films. Includes four complete Blender open short films with credits: Big Buck Bunny, Elephants Dream, Sintel and Tears of Steel. These are short films (about 10–15 minutes), not latest Hindi feature films. Old promotional clips and the placeholder series episode are removed from the default catalog.
- **Hindi Movies / Latest Hindi / Series:** retain poster grids, search and continuous pagination. Latest Hindi loads 120 available metadata titles initially, subject to source availability. A card offers Watch only when an admin has linked a matching full video by TMDB ID and media type. Otherwise it says Unavailable and shows details. TMDB metadata does not include full-film files.
- **Music / Videos search:** searches the saved local and Firebase catalog inside the app. Music is empty until music videos are added; it does not search an external video service.
- **Native player:** HTTPS MP4/M4V/WebM/MKV files, HLS and DASH streams, buffering, error/retry, speed, real quality limits for multi-quality streams, resume position, audio focus and lifecycle cleanup. Codec support depends on the device.
- **Downloads:** direct files only, using their actual extension. HLS/DASH manifests are not offered as fake MP4 downloads. Long press a card for download, favorites and credits.
- **Catalog sync:** cloud additions/removals update all tabs and search. An empty cloud catalog clears cached cloud entries while preserving locally saved films.

## Adding full movies

In **Admin → Catalog → Add full video**, supply the title, category, description/credits and a direct HTTPS media URL ending in `.mp4`, `.m4v`, `.webm`, `.mkv`, `.m3u8` or `.mpd` (query parameters are supported). Use content you own or are authorized to distribute. Web-page links are rejected. Add a poster URL if available.

To enable Watch on a Hindi/series metadata card, set its numeric TMDB ID and select **Full movie** or **Series episode**. Titles are never matched by name because remakes and different media can share a title. An authorized full-movie source must be supplied for each latest Hindi film; this release does not include such a feed.

Firebase `movies` documents use `title`, `category`, `description`, `videoUrl`, `posterUrl`, optional numeric `tmdbId`, and `mediaType` (`movie`, `tv`, `music`, `video`). Existing entries without the new fields remain readable. In local mode, additions stay on that device; configure Firebase to share the catalog across users.

## Build

Set the GitHub repository secret **TMDB_API_KEY** for online metadata. Optionally set **GOOGLE_SERVICES_JSON_B64** for Firebase. Do not commit keys. Saved films work without a TMDB key.

GitHub Actions uses Java 17 and Gradle 8.9:

```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug -PTMDB_API_KEY="$TMDB_API_KEY" --stacktrace
```

The **Android Build** workflow uploads `streambox-debug-apk` and `streambox-check-reports`. `scripts/check_builtin_streams.py` checks the actual built-in video URLs, MP4 duration and poster bytes from the CI runner. This is a source-availability check, not proof of Android-device playback.

## Validation

Unit tests cover pagination/retries, stale responses, metadata filtering, direct-source validation, rejecting YouTube and web links, file-vs-manifest downloads, exact movie/series mapping, Hindi/music search and catalog JSON compatibility.

Device checks still required: play/seek/retry each film; background and reopen the player; test Back, screen rotation and offline failure; add a full-video mapping; test HLS/DASH on the target device; and verify downloads. No physical-device test result is implied by a successful APK build.

## Open-film credits

Credits and license links are included in the in-app **Details & credits** dialogs and in `app/src/main/assets/open_movies.json`.

- [Big Buck Bunny](https://peach.blender.org/about/) — Blender Foundation, CC BY 3.0.
- [Elephants Dream](https://orange.blender.org/blog/creative-commons-license-2/) — Blender Foundation / Netherlands Media Art Institute, CC BY 2.5.
- [Sintel](https://durian.blender.org/sharing/) — Blender Foundation, CC BY 3.0.
- [Tears of Steel](https://mango.blender.org/about/) — Blender Foundation, CC BY 3.0.
