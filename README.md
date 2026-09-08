# mori

A highly optimized, Kotlin-first Android library for decoding and serving comic book
archives (CBZ, CBR, image folders — more formats to come) plus pluggable on-device OCR.

The library is intentionally split into two artifacts so consumers that do not need OCR
do not pull in the Tesseract native libraries:

| Module              | Purpose                                                     |
| ------------------- | ----------------------------------------------------------- |
| `comic-core`        | Archive decoding, page model, image decoding (no UI, no OCR) |
| `comic-ocr`         | Pluggable `OcrEngine` abstraction + Tesseract backend        |

## Status

This project is built in phases. See the plan in the repository history and the issue
tracker for the full roadmap.

- [x] Phase 0 — Scaffold, CI, tooling, natural-order page sorting
- [ ] Phase 1 — Core decode (CBZ / CBR / folders, ComicInfo.xml, typed errors)
- [ ] Phase 2 — Image decode hardening (subsample, regions, EXIF, OOM safety)
- [ ] Phase 3 — OCR abstraction + Tesseract backend
- [ ] Phase 4 — Production hardening & 1.0
- [ ] Phase 5 — Reader UI, CB7/CBT/PDF, ML Kit/PaddleOCR backends, KMP

## Requirements

- JDK 17
- Android SDK (compileSdk 35, minSdk 24)

## Build & test

```bash
./gradlew test          # unit tests
./gradlew lint          # Android lint
./gradlew detekt        # static analysis
./gradlew assembleDebug # build AARs
```

## Publishing

`comic-core` and `comic-ocr` are published to Maven coordinates
`com.mori:comic-core` and `com.mori:comic-ocr`. Publish locally with:

```bash
./gradlew publishToMavenLocal
```

## License

Apache 2.0 — see [LICENSE](LICENSE).
