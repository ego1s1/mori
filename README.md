# Mori — Comic Reader

Mori is an Android app for reading your comic book archives (CBZ, CBR, and image
folders) in a calm, immersive Material 3 Expressive reader. Import your files once —
everything stays on your device and works fully offline.

> **Status:** active development. Things look good and read well, but expect the
> occasional rough edge while we polish toward 1.0.

## Get the app

Grab the latest APK from
[GitHub Releases](https://github.com/anomalyco/mori/releases) and install it on
your phone or tablet (Android 7.0+). No account, no ads, no tracking.

## Use it

- **Import:** on first launch, pick the folder holding your comics. Mori copies
  them into its own space and builds your library with covers — your originals
  are never touched.
- **Browse:** search, sort, and filter your library; bookmarks and reading
  progress are saved automatically, so you always pick up where you left off.
- **Read:** tap a comic to open it. The controls show themselves for a quick
  tour the very first time, then politely get out of the way.

### Reader gestures

| Gesture | What happens |
| ------- | ------------ |
| Tap left / right edge | Previous / next page (mirrored for right-to-left comics) |
| Tap center | Show or hide the top bar and page controls |
| Double-tap | Zoom into exactly the spot you tapped; double-tap again to fit |
| Pinch / drag | Zoom and pan freely |
| Swipe pages | Fling through pages with a springy carousel feel |
| Edge swipe back | Android's predictive back shrinks the page as you preview leaving |
| Volume keys | Optional page turns (enable in reader settings) |

The slider pill at the bottom scrubs through pages, and the settings sheet offers
reading direction, page fit, margin crop, keep-screen-on, and tap-zone guides.

## Formats & limitations

- **CBZ** fully supported, including `ComicInfo.xml` metadata and natural page order.
- **CBR** supported for RAR4 archives; RAR5 is not yet decodable with open-source
  libraries.
- Corrupt, password-protected, or empty files show a clear message with
  retry/remove actions instead of crashing.

## Credits & inspiration

Reader and library conventions (tap zones, volume-key turns, slider navigator)
are inspired by [Mihon](https://github.com/mihonapp/mihon) — used purely as a
design reference; every line here is original. Thanks also to the junrar,
Coil, and Jetpack open-source projects.

## License

Apache 2.0 — see [LICENSE](LICENSE).
