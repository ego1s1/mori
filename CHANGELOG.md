# Changelog

## 1.0.1

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
