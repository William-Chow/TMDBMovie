# TMDB Movie

An Android movie browser built on [The Movie Database](https://www.themoviedb.org/) API,
written in Kotlin with Jetpack Compose.

## Features

- **Discover** — swipeable carousel of the latest releases, paged as you go
- **Gallery** — two-column grid with server-side genre filtering
- **Search** — debounced, paged title search
- **Detail** — poster, rating, runtime, tagline, genres, overview, cast, and a
  YouTube trailer link
- **Favorites** — save movies from the detail screen; kept on device
- Pull-to-refresh, offline read-through cache, and retry on every failure

## Setup

The TMDB API key is not tracked in this repository. Add yours to
`local.properties` (which git ignores):

```properties
tmdb.apiKey=your_tmdb_api_key
```

Then build as usual:

```bash
./gradlew assembleDebug
```

Without a key the app still builds, but every request comes back `401`.

### Release builds

Release signing reads `key.properties` in the project root:

```properties
storeFile=app/keystore/your_keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile` is resolved relative to the project root and must point at a file
that exists, or `:app:validateSigningRelease` fails.

## Requirements

- JDK 17
- Android SDK 37 (`compileSdk`), min SDK 28
