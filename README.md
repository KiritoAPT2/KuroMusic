# KuroMusic

<div align="center">
  <img src="https://raw.githubusercontent.com/KuroMusic/KuroMusic/main/assets/BannerKuro.png" alt="KuroMusic" width="100%"/>

  A Material Design 3 YouTube Music client for Android.

  [![GitHub release](https://img.shields.io/github/v/release/KiritoAPT2/KuroMusic?style=flat-square)](https://github.com/KiritoAPT2/KuroMusic/releases)
  [![License](https://img.shields.io/github/license/KiritoAPT2/KuroMusic?style=flat-square)](LICENSE)
  [![Downloads](https://img.shields.io/github/downloads/KiritoAPT2/KuroMusic/total?style=flat-square)](https://github.com/KiritoAPT2/KuroMusic/releases)
  [![Telegram](https://img.shields.io/badge/Telegram-Channel-26A5E4?style=flat-square&logo=telegram)](https://t.me/KiritoAPT2)
</div>

## 📸 Screenshots

<p align="center">
  <img src="screenshots/home.png" width="30%" alt="Home" />
  <img src="screenshots/player.png" width="30%" alt="Player" />
  <img src="screenshots/settings.png" width="30%" alt="Settings" />
</p>

## 📥 Download

[![GitHub](https://img.shields.io/badge/Download-GitHub_Release-181717?style=for-the-badge&logo=github)](https://github.com/KiritoAPT2/KuroMusic/releases/latest)

Download the latest APK from [Releases](https://github.com/KiritoAPT2/KuroMusic/releases/latest). Minimum Android 6.0 (API 23).

## ✨ Features

### 🎵 Playback
- Ad-free streaming from YouTube Music
- Background playback with Media3 notification controls
- High-quality audio streaming
- Skip silence
- Volume normalization (loudness normalization)
- Sleep timer
- Persistent queue across app restarts
- Queue reorder

### 🔍 Browse & Search
- Search songs, albums, artists, and playlists
- Home feed with recommendations
- Explore page with new releases and curated content
- Mood & genres browsing
- Song, album, artist radio / auto-play similar content

### 📚 Library
- Liked songs, playlists, albums, and artists
- Listening history (local and remote)
- Import YouTube Music playlists
- Create and manage local playlists
- Listening statistics
- Bookmark artists and albums

### 🎨 Visual & Customization
- Material 3 Dynamic theming (follows wallpaper colors)
- Pure black OLED theme
- Light / Dark / System / Dynamic theme modes
- Album art-based color extraction
- Animated player UI

### 🎤 Lyrics
- Real-time synchronized lyrics
- Multiple providers: LRCLib, KuGou, Kizzy
- Auto-search and fallback between providers

### 🔗 Integrations
- Discord Rich Presence *(experimental — shows current song in profile)*
- Last.fm scrobbling

### ⚙️ Other
- Download songs for offline playback
- In-app update checker
- Cache management
- Force AAC fallback for devices without Opus support

## 🛠 Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Playback | Media3 ExoPlayer |
| Architecture | MVVM + Clean Architecture |
| DI | Dagger Hilt |
| Database | Room with auto-migrations |
| Image loading | Coil |
| API client | InnerTube (Ktor + OkHttp) |
| YouTube API | InnerTube (ANDROID_MUSIC, WEB_REMIX, and 7+ fallback clients) |
| Lyrics APIs | LRCLib, KuGou, Kizzy |
| Discord RPC | WebSocket Gateway (user token) |
| Preferences | DataStore |

## 🏗️ Architecture

```
:app              → Android app (UI, playback, database, DI)
:innertube        → YouTube Music API client (JVM library)
:kugou            → KuGou lyrics provider
:lrclib           → LRCLib lyrics provider
:kizzy            → Unknown/experimental module
:jossredconnect   → JossRed client
:lastfm           → Last.fm scrobbling
```

## 🔧 Build

Requires JDK 17 and Android SDK.

```bash
git clone https://github.com/KuroMusic/KuroMusic.git
cd KuroMusic
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/KuroMusic.apk`

> [!NOTE]
> A `local.properties` file with API keys and signing credentials is required for release builds. See [AGENTS.md](AGENTS.md) for details.

## 🛡️ Security & Privacy

KuroMusic connects directly to YouTube/InnerTube — no backend server. No user data is sent to third parties beyond the services you explicitly use (Last.fm, Discord). The app does not require a YouTube account; guest playback uses visitor data.

## 💬 Community

- [Telegram Channel](https://t.me/KiritoAPT2)
- [GitHub Issues](https://github.com/KiritoAPT2/KuroMusic/issues) — bug reports and feature requests

## ⚠️ Disclaimer

KuroMusic is not affiliated with Google LLC or YouTube Music. All media content is streamed from YouTube Music's public API via InnerTube. This project is for educational purposes.

## 🙏 Créditos

**TeamAnimax** — Por el apoyo incondicional y los buenos momentos. Esto también es suyo. 🫶

## 📄 License

[GNU General Public License v3.0](LICENSE)

Copyright © 2026 KuroMusic
