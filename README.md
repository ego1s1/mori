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

## Download

Grab the latest APK or App Bundle from [GitHub Releases](https://github.com/ego1s1/mori/releases) for your phone or tablet (Android 7.0+).

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](obtainium://app/%7B%22id%22%3A%20%22com.mori.reader%22%2C%20%22url%22%3A%20%22https%3A%2F%2Fgithub.com%2Fego1s1%2Fmori%22%2C%20%22author%22%3A%20%22ego1s1%22%2C%20%22name%22%3A%20%22Mori%22%7D)

## Why Mori

* **Reads like Mihon.** Forgiving tap zones, volume-key paging (invertible), anchored double-tap zoom, hard-stop zoom pan with second-swipe turns, dual-page spreads, and a slider pill that stays out of the art.
* **A shelf that keeps up.** Covers fill in behind you, continue-reading tracks recency, search/sorts/favorites find anything fast.
* **Yours, offline.** System/light/dark themes, wallpaper color, AMOLED black, calm or expressive motion — everything on-device, nothing uploaded.

## Build it yourself

Requires JDK 17 and the Android SDK:

```bash
./gradlew :app:assembleDebug        # debug APK
./scripts/build-release.sh          # signed local release APK
./scripts/new-release.sh 1.1.1      # tag + trigger the store release flow
./gradlew test lint detekt apiCheck assembleDebug   # full gate
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Disclaimer

Mori hosts zero content — it only reads comic files you already own.

## Credits

Reader and library conventions (tap zones, volume-key turns, slider navigator) are inspired by [Mihon](https://github.com/mihonapp/mihon) — used purely as a design reference; every line here is original. Thanks also to the junrar, Coil, and Jetpack open-source projects. The full list ships in the app under Settings → About → Open-source licenses.

## License

Apache 2.0 — see [LICENSE](LICENSE).
