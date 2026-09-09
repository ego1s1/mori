# Mori — Comic Reader Backend & App 🚧 Work in Progress

> **Status: active development, not production-ready.** APIs, screens, and storage
> layouts may change without notice. See [Roadmap](#roadmap) for what works today.

Mori is a Kotlin-first Android project for reading comic book archives, split in two halves:

| Part | What it is |
| ---- | ---------- |
| **Backend** (`comic-core`, `comic-ocr`) | Decoding library for CBZ / CBR / image folders: archive handling, natural page ordering, `ComicInfo.xml` metadata, subsampled + region image decoding with EXIF support, and a pluggable on-device OCR engine (Tesseract default). Published as `com.mori:comic-core` and `com.mori:comic-ocr`. |
| **App** (`app` + `feature/*` + `core/*`) | Jetpack Compose reader app built on that backend: first-launch folder import, indexed offline-first library grid, comic detail, and an immersive Material 3 Expressive reader. |

## Credits & inspiration

The reader and library UX conventions (tap-zone navigation, volume-key page turns,
slider-pill chapter navigator, badge/display-mode/sort-filter organization) are
**inspired by [Mihon](https://github.com/mihonapp/mihon)**, an excellent open-source
manga reader. Mihon served purely as a design reference — **no Mihon code is included
in this repository; every implementation here is original**, restyled on top of
Material 3 Expressive components.

Supporting open-source acknowledgements: Tesseract OCR, junrar, Coil, Jetpack libraries.

## Requirements

- JDK 17
- Android SDK (compileSdk 35, minSdk 24)
- An arm64 device for on-device OCR (the bundled Tesseract native library is arm64-only)

## Build, test, install

```bash
./gradlew test          # unit tests (JVM + Robolectric)
./gradlew lint          # Android lint
./gradlew detekt        # static analysis (backend modules)
./gradlew apiCheck      # public API compatibility (backend modules)
./gradlew assembleDebug # build backend AARs + app APK

./scripts/install-app.sh  # install the debug APK on a connected device/emulator
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

Every commit builds as `0.1.N` (N = commit count) so installs stay
ordered; explicit `-PappVersionName=` / `-PappVersionCode=` override it.

## App releases

Pushing a `v*` tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) triggers the
`Release` workflow, which builds a signed release APK and attaches it to a GitHub
Release with auto-generated notes. It can also be started manually from the Actions
tab. Version name comes from the tag; version code from the run number.

One-time setup — create a single permanent keystore (rotating it breaks updates)
and store these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| ------ | ----- |
| `KEYSTORE_BASE64` | base64 of the `.jks` file |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |
| `STORE_PASSWORD` | keystore password |

```bash
keytool -genkeypair -v -keystore mori-release.jks \
  -alias mori -keyalg RSA -keysize 2048 -validity 10000
base64 -i mori-release.jks | pbcopy
```

The workflow fails fast with a clear message when secrets are missing.

## How it fits together

```text
app/                          # Hilt app, MainActivity, top-level navigation
feature/onboarding|library|detail|reader
  api/                        # type-safe navigation routes (public)
  impl/                       # screens, ViewModels, UI state (internal)
core/
  model/                      # pure-Kotlin domain (Comic, queries, prefs)
  data/                       # repository, backend bridge, covers, Coil fetchers
  database/                   # Room index of the library
  datastore/                  # onboarding + preference flags
  designsystem/               # M3 Expressive theme, motion, icons
  common/ testing/            # dispatchers, test rules
comic-core/ comic-ocr/        # the decoding + OCR backend (see above)
build-logic/                  # Gradle convention plugins
```

Key behaviors:

- **Import, don't link**: onboarding copies CBZ/CBR files via SAF into app-private
  storage, then indexes them with generated cover thumbnails. Originals are untouched.
- **Offline-first**: Room is the source of truth; the library grid, detail, and
  reader all observe it reactively.
- **Bounded decoding**: covers and pages decode through resolution-capped
  subsampling and region reads, served to Compose through a Coil fetcher.
- **Errors are data**: corrupt, password-protected, and empty archives become
  visible error rows with retry/remove actions instead of crashes.

## Known limitations

- CBR support covers **RAR4** (junrar, the available open-source decoder, does not
  yet implement RAR5 extraction).
- OCR runs on-device and is arm64-only; x86_64 emulators can't load it.
- Reader pages currently render placeholders until real-data wiring lands (see roadmap).

## Roadmap

- [x] Backend 1.0: decode CBZ/CBR/folders, subsample/region/EXIF, Tesseract OCR, hardening
- [x] App foundation: modular Compose shell, M3 Expressive theme, navigation
- [x] Onboarding with SAF import flow
- [x] Reader chrome matching the target mockup (slider pill, toolbar, settings)
- [x] Data layer: Room index, repository, covers, Coil pipeline
- [x] Library grid with search/sort/filter
- [x] Detail screen with page strip and error handling
- [x] Reader wired to real pages + saved progress
- [x] Polish & release: accessibility, adaptive layouts, storage manager

## License

Apache 2.0 — see [LICENSE](LICENSE).
