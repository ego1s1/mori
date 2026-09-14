# Mori — Comic Reader

Mori is an Android comic reader built around a simple deal: point it at the folder where your comics already live, and it builds a fast, beautiful shelf without copying a single file. Everything stays on your device and works fully offline.

> **Status:** active development toward 1.0. It reads well; expect the occasional rough edge.

## Get the app

Grab the latest APK from [GitHub Releases](https://github.com/ego1s1/mori/releases) and install it on your phone or tablet (Android 7.0+). No account, no ads, no tracking.

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](obtainium://app/%7B%22id%22%3A%20%22com.mori.reader%22%2C%20%22url%22%3A%20%22https%3A%2F%2Fgithub.com%2Fego1s1%2Fmori%22%2C%20%22author%22%3A%20%22ego1s1%22%2C%20%22name%22%3A%20%22Mori%22%7D)

## Library

- **Link, don't copy.** Pick a folder once during setup — Mori reads your CBZ and CBR files where they are and rescans the same folder later. Your originals stay put. Never moved, never duplicated. Removing a book from Mori unlinks it; your files survive.
- **A shelf that fills itself.** Covers load in the background while the grid fills in. Pull-to-refresh re-scans the linked folder, pruning books you deleted.
- **Find anything fast.** Text search across titles, series, and numbers. Four sort orders. Filters for in-progress, unread, and finished books, plus an option to hide unreadable files.
- **Pick up where you left off.** Per-book progress saves automatically, with a resume button that jumps back to your exact page, a pages-left badge, and a progress bar on every started comic.

## Reader

- **Tap zones that forgive.** Edge taps turn pages, center taps toggle the chrome. Every tap waits out a short double-tap window first, so a double-tap never misfires into a page turn.
- **Zoom that behaves.** Double-tap zooms into exactly the spot you touched; double-tap again to fit. Pinch zooms with panning clamped to the page, and pans hand off to page swipes at the edges.
- **Swipe, taps, or keys — your call.** Fling through pages with a springy carousel feel, switch to taps-only reading, or use the volume buttons. The app handles those before the system sees them, so your ringer volume never moves.
- Right-to-left reading with mirrored zones. Width, height, and original page fits. Margin cropping for scanned gutters, keep-screen-on, a page counter, and tap-zone guides while learning the layout.
- **Chrome that gets out of the way.** Controls fade in on tap and hide themselves after a few idle seconds. A slider pill scrubs through pages, and the settings sheet holds direction, fit, crop, and display toggles.

## Make it yours

- **Theme:** system, light, or dark. Wallpaper dynamic color or four built-in schemes, including AMOLED black. Calm or expressive motion physics.
- **Launcher icon:** a forest-mark adaptive icon with a monochrome themed-icon layer for Android 13+.

## Formats & limitations

- **CBZ** fully supported, including `ComicInfo.xml` metadata and natural page order.
- **CBR** works for RAR4 archives; RAR5 doesn't work with open-source libraries yet.
- A corrupt, locked, or empty file gives you a retry/remove prompt instead of a crash. Password entry is not supported yet.

## Build it yourself

Requires JDK 17 and the Android SDK:

```bash
./gradlew :app:assembleDebug        # debug APK
./scripts/build-release.sh          # signed local release APK
./scripts/new-release.sh 1.0.1      # tag + trigger the store release flow
./gradlew test lint apiCheck assembleDebug   # full gate
```

## Credits & inspiration

Reader and library conventions (tap zones, volume-key turns, slider navigator) are inspired by [Mihon](https://github.com/mihonapp/mihon) — used purely as a design reference; every line here is original. Thanks also to the junrar, Coil, and Jetpack open-source projects. The full list ships in the app under Settings → About → Open-source licenses.

## License

Apache 2.0 — see [LICENSE](LICENSE).
