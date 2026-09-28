# Hello1 / StreamBox Free

Android movie catalog app.

## Online latest movies and search

The app is already wired for TMDB-powered movie discovery/search. A TMDB API key is intentionally **not committed to this public repository**.

Add this line to your local `gradle.properties`:

```properties
TMDB_API_KEY=YOUR_TMDB_API_KEY
```

Then rebuild the app.

The key is read by `app/build.gradle.kts` and exposed to the app through `BuildConfig.TMDB_API_KEY`.

## Current features

- New A-style app logo
- Local movie catalog
- Latest releases tab (TMDB)
- Online movie search (TMDB)
- Watch-options link
- Download button for direct video URLs that you own or are licensed to distribute
- Login / signup / admin flow
