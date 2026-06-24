# KuroMusic — AGENTS.md

## Build

```bash
gradlew assembleRelease              # full release build
gradlew assembleRelease -x lintVitalRelease  # skip lint (faster)
```

**Prerequisites:** JDK 17 (`JAVA_HOME`), Android SDK (`ANDROID_HOME`). On this machine:
- `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`

APK always outputs as `app/build/outputs/apk/release/KuroMusic.apk` (forced by `applicationVariants.configureEach` in `app/build.gradle.kts:76`).

## Signing & Secrets

Signing credentials and API keys are in `local.properties` (gitignored). Required entries:
```
RELEASE_KEYSTORE_FILE
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS=KuroMusic
RELEASE_KEY_PASSWORD
GOOGLE_API_KEY
PO_TOKEN_REQUEST_KEY
INNER_TUBE_API_KEY
YOUTUBE_SESSION_COOKIES
```

The app connects **directly to YouTube/InnerTube** — no backend server. The user is **not logged in** (uses visitor data from DataStore).

## Modules

| Module | Type | Purpose |
|--------|------|---------|
| `:app` | Android app | Main app, UI, playback, DB |
| `:innertube` | JVM lib | YouTube/InnerTube API client (Ktor + OkHttp + NewPipeExtractor) |
| `:kugou` | JVM lib | KuGou lyrics provider (Ktor) |
| `:lrclib` | JVM lib | LRCLib lyrics provider (Ktor) |
| `:kizzy` | JVM lib | Unknown (Ktor + json) |
| `:jossredconnect` | JVM lib | JossRed client (OkHttp) |
| `:material-color-utilities` | Android lib | Color extraction for dynamic theming |
| `:baselineprofile` | Android lib | Baseline profiles for startup perf |

All Kotlin/JVM modules use `jvmToolchain(17)`. Dependencies are versioned in `gradle/libs.versions.toml`.

## Architecture

- **Clean Architecture + MVVM**: UI (Jetpack Compose/Material3) → ViewModels → Use Cases → Repositories
- **DI**: Dagger Hilt (`@HiltAndroidApp` on `App.kt`, `@AndroidEntryPoint` on `MainActivity.kt`)
- **Database**: Room with auto-migrations (historical schemas in `app/schemas/`)
- **Preferences**: DataStore Preferences (keys in `constants/PreferenceKeys.kt`)
- **Image loading**: Coil (config in `App.newImageLoader()`)
- **Playback**: Media3 ExoPlayer with custom `CacheDataSource` chain (download → player → network)
- **YouTube auth**: `YouTubeSessionInterceptor` adds cookies + SAPISIDHASH to stream requests

## Key Gotchas

- **No tests exist** in the project. No test runner configured beyond default `androidx.test.runner.AndroidJUnitRunner`.
- **Configuration cache** is enabled (`org.gradle.unsafe.configuration-cache=true`). When source code changes, run `gradlew clean` first or the cache may serve stale output.
- **Room schemas** in `app/schemas/` are required for auto-migration generation. Deleting them breaks the build.
- **scrub.cmd** is obsolete (passwords were moved from `build.gradle.kts` to `local.properties`). Do not use.
- **MissingTranslation lint** is suppressed globally. Don't worry about untranslated strings.
- **Release keystore** is at `C:\Users\KiritoAPT2\KuroMusic\kuro-music-release.jks`. Installing over an old signature requires uninstall first.
- **In-app updater** reads `api.github.com/repos/KiritoAPT2/KuroMusic/releases/latest` — first `.apk` asset wins.

## Release Flow

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts:32-33`
2. `gradlew assembleRelease`
3. Create GitHub release with tag `vX.Y.Z` targeting `main`
4. Upload `KuroMusic.apk` from `app/build/outputs/apk/release/`
5. In-app UpdateChecker will pick it up automatically
