# Changelog

## 0.1.24

Reader experience pass, verified against Mihon's pager viewer:

- Volume keys are invertible (volume up forward / down back), matching
  Mihon's `readWithVolumeKeysInverted` pref in both reader and system settings
- Zoomed panning is hard-stop 1:1 with no mid-gesture page turns; a fresh
  second swipe pushing past the clamped edge turns the page from anywhere,
  like an unzoomed swipe — tap hop-to-edge kept
- Tap rhythm fixed: fire-time stamping and same-zone repeats, so rapid
  skipping stays consistent and direction flips never dispatch stale taps
- README refocused on the reader, the shelf, and offline-first strengths

## 0.1.23 (hotfix)

Reader-backend rollback for the "all comics Can't read" regression: page
reads and the SAF cache copy return to their pre-sweep implementations
while the real-stack end-to-end tripwire stays green. Kept from the sweep:
typed refresh errors, FileProvider share, bounded XML parsing, backup
scoping, and all history/motion work.

## 0.1.0

Fresh line after the v1.x reset: versioning restarts at 0.1.0 with patch
increments from here (0.1.1, …). Highlights since the reset point:

- History tab (day-grouped reading history between Library and Settings)
- Determinate rescan progress, coalesced refreshes, cancel-safe index locks
- Typed refresh error rows, single-emission wide-page scan, stale-anchor guards
- FileProvider share boundary with ClipData grants, capped ComicInfo parsing
- Least-privilege backup rules, remount-on-rebuild zoom gate release

- Unified onboarding storage choice (custom folders link in place with zero
  copies; app storage copies), transient Done beat into the library
- Reader gesture overhaul to Mihon parity: hold-to-confirm taps, double-tap
  zoom on second contact, instant rhythm skipping, pager-first swipe routing,
  long-press guard
- App-wide expressive motion standards (gated fade-through/fade transitions,
  documented duration table)
- Vector launcher icon with monochrome themed-icon layer; release pipeline
  (`scripts/new-release.sh`) and signed local release builds

## 1.0.0

First production release.

### comic-core
- Archive decoding for CBZ (ZIP random access), CBR (junrar, RAR4), and image folders
- Natural-order page sorting; hidden/non-image entry filtering; subdirectory support
- `ComicInfo.xml` (ComicRack) metadata parsing with XXE hardening
- Typed error hierarchy: unsupported / corrupt / empty / password-required / page-not-found / closed / decode
- Image decoding with bounds-only probing, power-of-two subsampling (`maxDimension`),
  region decode (`BitmapRegionDecoder` fast path + crop fallback), EXIF orientation, RGB_565
- `DecodingComicArchive` suspend helpers (`decode`, `decodeRegion`, `pageDimensions`)
- Closeable lifecycle guarantees; thread-safe concurrent reads; cancellation-safe

### comic-ocr
- Pluggable `OcrEngine` abstraction + thread-safe `OcrEngines` registry
- `TesseractOcrEngine` (Tesseract 5, on-device, lazy init, language packs, word→line grouping)
- `FakeOcrEngine` deterministic reference implementation for tests
- Typed OCR errors: unavailable / recognition / language

### Hardening
- Consumer ProGuard/R8 rules for both artifacts
- Binary-compatibility-validator API freeze (`./gradlew apiCheck`)
- 131 unit tests (JVM + Robolectric) covering archives, decode, OCR, concurrency, lifecycle
- detekt + Android lint + CI (test, lint, detekt, apiCheck, assemble)
