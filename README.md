# TMDB Movie

An Android movie browser built on [The Movie Database](https://www.themoviedb.org/) API,
written in Kotlin with Jetpack Compose.

> [!CAUTION]
> **Security note for the owner: rotate the leaked credentials.**
> Earlier commits of this public repository contained the release upload
> keystore (`app/keystore/keystore`), its passwords (`key.properties`, and a
> comment in `app/build.gradle`), and a release bundle
> (`app/release/app-release.aab`) with the TMDB API key compiled in. They are
> no longer tracked, but **they are still in the git history**, so anyone can
> retrieve them. Removing them from the current tree does not undo that.
>
> 1. **Replace the signing key.** Treat the old keystore and its password as
>    public. If the app uses Play App Signing, request an upload key reset in
>    Play Console (*App integrity › App signing*), create a new keystore with a
>    strong password, and keep it outside the repository. If that keystore
>    held the app signing key itself, ask Play Console support about
>    upgrading the app signing key.
> 2. **Regenerate the TMDB API key** in your
>    [TMDB API settings](https://www.themoviedb.org/settings/api), put the new
>    one in `local.properties`, and ship an update soon: the version on Play
>    still uses the old key and stops working once it is revoked.
> 3. **Optionally purge the history** with
>    [`git filter-repo`](https://github.com/newren/git-filter-repo): for
>    example `--invert-paths --path key.properties --path app/keystore
>    --path app/release`, plus `--replace-text` for the passwords in old
>    versions of `app/build.gradle`, then force-push. That rewrites every
>    commit and cannot reach existing clones or forks, which is why rotating
>    comes first. This has **not** been done here.

## Features

- **Discover**: swipeable carousel of the latest releases (nothing dated in
  the future), paged as you go
- **Gallery**: two-column grid with server-side genre filtering
- **Search**: debounced, paged title search
- **Detail**: poster, rating, runtime, tagline, genres, overview, cast, and a
  YouTube trailer link (also for films that are not in English)
- **Favorites**: save movies from the detail screen; kept on device
- Pull-to-refresh (plus a refresh button on the home screen) that always
  fetches fresh data, offline fallback to the last cached copy, and retry on
  every failure
- Every screen keeps its data across rotation, including the movie on the
  detail screen; after process death the search term, the genre and the
  open movie come back and are loaded again
- **About**: version, TMDB attribution, and ad privacy settings for users in
  regions where ad consent applies

## Setup

The TMDB API key is not tracked in this repository. Add yours to
`local.properties` in the project root (git ignores it):

```properties
tmdb.apiKey=your_tmdb_api_key
```

`TMDB_API_KEY=...` in `local.properties`, or a `TMDB_API_KEY` environment
variable, work as well. Without any key the app still builds; every screen
then says that the build has no TMDB API key instead of loading.

Then build as usual:

```bash
./gradlew assembleDebug
```

### Tests and lint

```bash
./gradlew testDebugUnitTest lintDebug
```

Unit tests cover the pager, the screens' ViewModels (the detail screen's
too), the HTTP cache/offline interceptors (against MockWebServer), the TMDB
API requests, ad pacing and the URL helpers. `app/src/androidTest` has an
on-device test for favourites (`./gradlew connectedDebugAndroidTest`), which
CI runs on an emulator.

### Ads

Debug builds use Google's sample AdMob app ID and test ad units, so
development never serves or clicks live ads. Release builds use the real IDs;
both sets are `resValue`s in `app/build.gradle`. Ads are only requested after
the UMP consent flow allows them.

### Release builds

Release signing is optional and reads `key.properties` in the project root,
which git ignores. Copy the template and fill it in:

```bash
cp key.properties.example key.properties
```

```properties
storeFile=app/keystore/upload-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile` is resolved relative to the project root. Keystores (`*.jks`,
`*.keystore`, `app/keystore/`) are git-ignored, but keeping yours outside the
repository is safer. Without `key.properties`, or if `storeFile` does not
exist, release builds are produced **unsigned** instead of failing.

## Continuous integration

`.github/workflows/ci.yml` runs on pushes to `main` and `claude/**` and on
demand, as two independent jobs:

- **build** sets up JetBrains Runtime 17 and the Android SDK, runs
  `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`, then
  an unsigned `assembleRelease` so R8 and the keep rules get exercised, and
  uploads the test and lint reports (with R8's `missing_rules.txt` when it
  fails) and the debug APK.
- **instrumented** boots an API 34 emulator (google_apis, x86_64) and runs
  `connectedDebugAndroidTest`, uploading its reports whether it passes or not.

CI needs no secrets: it builds without a TMDB key and without signing. Don't
add the real TMDB key to CI, since anyone who can read this public repository
can download the APK artifact.

## Requirements

- JDK 17 (Gradle provisions the JetBrains Runtime 17 named in
  `gradle/gradle-daemon-jvm.properties` if needed)
- Android SDK 36 (`compileSdk`), min SDK 28, target SDK 36

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB.
Movie data and images come from [The Movie Database](https://www.themoviedb.org/).
