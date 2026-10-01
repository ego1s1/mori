<div align="center">

# Mori

### A reader that gets out of the way
Fast, beautiful comics on your phone or tablet. No account, no ads, no tracking.

[![CI](https://github.com/ego1s1/mori/actions/workflows/ci.yml/badge.svg)](https://github.com/ego1s1/mori/actions)
[![Latest release](https://img.shields.io/github/v/release/ego1s1/mori)](https://github.com/ego1s1/mori/releases)
[![Downloads](https://img.shields.io/github/downloads/ego1s1/mori/total)](https://github.com/ego1s1/mori/releases)
[![License: Apache-2.0](https://img.shields.io/github/license/ego1s1/mori)](LICENSE)

</div>

> **Status:** active development — 0.1.x is out, expect the occasional rough edge.

## Features

**A library that runs itself.** Point Mori at a folder and your shelf fills in with covers, smart search, sorts, filters, and favorites. Shelves (collections) group books your way. Your files stay where they are; nothing is ever moved or duplicated.

**An effortless reader.** Forgiving tap zones, volume-key paging, double-tap zoom that anchors where you tap, pinch and pan with hard-stop edges, dual-page spreads for wide art, reading direction and display filters (grayscale, invert, margin crop), and a slider pill for fast scrubbing.

**Never lose your place.** Per-book progress, bookmarks, and a continue-reading shelf pick up exactly where you left off — across every book.

**Know your habits.** The Stats tab totals your reading time, pages, finishes, and sessions, with a 7-day / 30-day / yearly activity chart, day streaks, and most-read books.

**Private by design.** Optional app lock and incognito reading, thumbnail-cache controls with storage usage, and everything on-device — Mori works fully offline.

**Truly native UI.** Jetpack Compose with Material 3 Expressive: dynamic wallpaper color, AMOLED black, calm-motion support, predictive-back transitions, haptic feedback, and layouts that adapt from phones to tablets and foldables.

## Supported formats

| Format | Extensions |
| ------ | ---------- |
| CBZ | `.cbz`, `.zip` |
| CBR | `.cbr`, `.rar` |
| CB7 | `.cb7`, `.7z` |
| CBT | `.cbt`, `.tar` |

## Download

Grab the latest APK or App Bundle from [GitHub Releases](https://github.com/ego1s1/mori/releases) for your phone or tablet (Android 7.0+).

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](obtainium://app/%7B%22id%22%3A%20%22com.mori.reader%22%2C%20%22url%22%3A%20%22https%3A%2F%2Fgithub.com%2Fego1s1%2Fmori%22%2C%20%22author%22%3A%20%22ego1s1%22%2C%20%22name%22%3A%20%22Mori%22%7D)

## Getting started

1. Install the app and grant folder access when asked.
2. Point Mori at the folder holding your comics — the library indexes it in the background with a determinate progress bar.
3. Tap a cover to read; long-press for quick actions. Organize with shelves from the book details or Settings.
4. Track your reading on the Stats tab; tune theme, reader, and privacy in Settings.

## Build it yourself

Requires JDK 17 and the Android SDK (compile/target SDK 35, min SDK 24):

```bash
./gradlew :app:assembleDebug        # debug APK
./scripts/build-release.sh          # signed local release APK
./scripts/new-release.sh 0.1.24     # tag + trigger the store release flow
./gradlew test lint detekt apiCheck assembleDebug   # full gate
```

## Project structure

```
app/                    # NavHost, main tabs, launcher icons, release wiring
core/
  model/                # Comic, LibraryQuery, ReadingSession, settings models
  data/                 # Offline-first repository over Room + DataStore
  database/             # Room entities and DAOs
  datastore/            # Preferences (theme, reader, library source)
  designsystem/         # Material 3 Expressive theme, motion, shared components
  common/ testing/ test-fakes/     # Shared utilities and test doubles
feature/
  library/              # Shelf: search, sort, filter, collections
  reader/               # Pager: zones, zoom, spreads, scrubber, overview
  detail/               # Book details, bookmarks, shelf membership
  stats/                # Totals, activity chart, streaks, top books
  settings/             # Appearance, reader, shelves, privacy, storage, about
  onboarding/           # First-run folder pick and import
design/                 # Canonical launcher artwork (mori-icon.svg)
scripts/ fastlane/      # Build, release, and store-metadata helpers
```

Offline-first: the library index lives in Room, preferences in DataStore, and every screen renders from observable flows with fakes in `core/test-fakes` for hermetic unit tests.

## Testing

```bash
./gradlew test          # all unit tests
./gradlew detekt        # static analysis
./gradlew lint          # Android lint
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Disclaimer

Mori hosts zero content — it only reads comic files you already own.

## Credits

Thanks to the junrar, Coil, and Jetpack open-source projects. The launcher mark sets U+68EE (森, "forest") in [Yuji Syuku](https://github.com/google/fonts/tree/main/ofl/yujisyuku) (SIL Open Font License 1.1). The full dependency list ships in the app under Settings → About → Open-source licenses.

## License

Apache 2.0 — see [LICENSE](LICENSE).
