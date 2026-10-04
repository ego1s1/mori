# UI unification / polish — carried over from the Antigravity session

Local scratch file (gitignored). Not a commitment to ship; working notes only.

## Problem

Two things felt unfinished:

1. **Stats typography is soft.** `HeroNumber` uses
   `LocalAppFonts.current.displaySoft`, which sets `FontVariation.Setting("ROND", 100f)`.
   That extreme roundness softens straight stems on uppercase text
   (`"3H 20M"`, `"4 DAYS"`) and costs the numerals their punch.
   Hero captions use `MaterialTheme.typography.bodyMedium` → `BodyFlex`
   (weight 400), so they read thin next to a 64sp numeral.
   `MoriSectionCard` renders its title with `MaterialTheme.typography.titleLarge`,
   and in `Type.kt` that maps to `baseline.titleLarge.body()` (weight 400) —
   so "Reading time" and "Streak" headers are thin too.

2. **Header hierarchy is disjointed across features.** Same semantic level,
   different styles depending on the module:

| Level | Previous usage | Unified Target |
|---|---|---|
| Screen top bar | `MoriCollapsingTopBar` → `ScreenTitleSize` (28sp) + `topBarTitle` (Black 900). `DetailScreen` was `headlineSmall` (24sp). `OnboardingScreen` uses `headlineMedium` + `displaySoft` + `FontWeight.Black`. | Standardized `ScreenTitleSize` (28sp) / `ScreenTitleLineHeight` (34sp) with `topBarTitle` (wght 900, wdth 112.5). |
| Section header | `MoriSectionCard` → `titleLarge` (400). `ShelfSectionHeader` → `titleLarge` (400). `SectionHeading` (Stats) → `MoriEmphasized.titleSmall` (14sp bold). | Unified to `MoriEmphasized.titleLarge` (Bold 700, 22sp/28sp) + `semantics { heading() }`. |
| Sheet sub-header | `LibrarySortFilterSheet` → `titleMedium`. `ReaderSettingsSheet` was `titleMedium` for sheet title, `titleSmall` for groups. | Unified: Sheet title → `MoriEmphasized.titleLarge` (22sp), Group headings → `MoriEmphasized.titleMedium` (16sp) + `heading()`. |
| Row item title | `MoriSettingSwitch` was hardcoded to `MaterialTheme.typography.bodyLarge`. | Parameterized `titleStyle: TextStyle = MoriEmphasized.bodyLarge` matching `MoriSettingRow`. |
| Dialog headlines | `MoriConfirmDialog` and `ShelvesDialog` were using `displaySoft` + `FontWeight.Black`. | Unified to `MaterialTheme.typography.headlineSmall` (`HeadingFlex`, wght 600, opsz 24). |

## Target hierarchy (Audited & Implemented)

| Tier | Role | Font | Size / line height | Where | Status |
|---|---|---|---|---|---|
| L1 | Screen title | `topBarTitle` (Black 900, 112.5% width) | 28sp / 34sp | `MoriCollapsingTopBar`, `DetailScreen`, `ReaderTopBar` | ✅ Complete |
| L1b | Screen hero / step title | `HeadingFlex` SemiBold/Bold | 28sp / 36sp | Onboarding wizard steps, `MoriEmptyState` | ✅ Complete |
| L2 | Major section header | `MoriEmphasized.titleLarge` (Bold 700) | 22sp / 28sp | `MoriSectionCard`, `ShelfSectionHeader`, `SectionHeading` | ✅ Complete |
| L3 | Sheet / group sub-header | `MoriEmphasized.titleMedium` (Bold 700) | 16sp / 24sp | Sheet groups, sheet titles (`LibrarySortFilterSheet`, `ReaderSettingsSheet`) | ✅ Complete |
| L4 | Item / row title | `HeadingFlex` SemiBold via `MoriEmphasized.bodyLarge` | 16sp / 24sp | `MoriSettingRow`, `MoriSettingSwitch`, book card labels, list items | ✅ Complete |
| Hero data | Metric numeral | `displayFlex` (Black 900, 125% width) | 28–64sp | Stats `HeroNumber` | ✅ Complete |
| Hero label | Metric caption | `MoriEmphasized.labelLarge` (SemiBold 600) | 14sp / 20sp | "Time reading", "Longest: 4 days" | ✅ Complete |

## Implementation steps

### Step 1 — design system foundations (`core:designsystem`) — [COMPLETED]
- `Type.kt`: `titleLarge = baseline.titleLarge.heading()` so the token carries `HeadingFlex` (600) instead of thin `BodyFlex` (400).
- `MoriSettingSwitch.kt`: Added `titleStyle: TextStyle = MoriEmphasized.bodyLarge` parameter and bound to row title for L4 typographic unification.
- `MoriDialog.kt`: Standardized `MoriConfirmDialog` title from `displaySoft` + `FontWeight.Black` to `MaterialTheme.typography.headlineSmall`.
- `MoriSectionCard.kt`: title → `MoriEmphasized.titleLarge`, `onSurface` colour. Every section card picks it up app-wide.
- Verified: `:core:designsystem:testDebugUnitTest` PASSED (0 errors).

### Step 2 — Stats (`feature:stats:impl`) — [COMPLETED]
- `HeroNumber`: `displaySoft` → `displayFlex` (Black 900, 125% width, ROND 0, slant -10f).
- `HeroNumber` label: `bodyMedium` → `MoriEmphasized.labelLarge`, `onSurfaceVariant`.
- `SectionHeading` ("Most Read"): → `MoriEmphasized.titleLarge` + `semantics { heading() }`.
- `TopBookRow` readout: → `MoriEmphasized.bodyMedium`.
- Verified: `:feature:stats:impl:testDebugUnitTest` PASSED (0 errors).

### Step 3 — Chrome & Dialogs (`feature:detail:impl`, `feature:library:impl`, `feature:reader:impl`) — [COMPLETED]
- `DetailScreen.kt`:
  - Standardized `LargeFlexibleTopAppBar` titles with `ScreenTitleSize` (28.sp) and `ScreenTitleLineHeight` (34.sp) using `titleFont`.
  - Replaced `comic.title` hero heading from `displaySoft` + `FontWeight.Black` to `MoriEmphasized.headlineSmall`.
  - Replaced `ShelvesDialog` title from `displaySoft` + `FontWeight.Black` to `MaterialTheme.typography.headlineSmall`.
- `LibraryScreen.kt` & `LibrarySortFilterSheet.kt`:
  - `ShelfSectionHeader` title → `MoriEmphasized.titleLarge` (L2) with count badge and spring chevron.
  - `LibrarySortFilterSheet` group headings → `MoriEmphasized.titleMedium` + `heading()`.
- `ReaderSettingsSheet.kt` & `ReaderScreen.kt`:
  - Sheet title → `MoriEmphasized.titleLarge`, groups → `MoriEmphasized.titleMedium`.
  - `ReaderTopBar` → Promoted to L1 (`MoriEmphasized.headlineSmall` with `topBarTitle`).
- Verified: `:feature:detail:impl:testDebugUnitTest`, `:feature:library:impl:testDebugUnitTest`, `:feature:reader:impl:testDebugUnitTest` PASSED (0 errors).

### Step 4 — verify & full test suite — [COMPLETED]
1. Ran all module-level unit tests:
   - `:core:designsystem:testDebugUnitTest`
   - `:feature:stats:impl:testDebugUnitTest`
   - `:feature:library:impl:testDebugUnitTest`
   - `:feature:reader:impl:testDebugUnitTest`
   - `:feature:detail:impl:testDebugUnitTest`
   - `:feature:settings:impl:testDebugUnitTest`
   - `:feature:onboarding:impl:testDebugUnitTest`
2. Full suite run: `./gradlew testDebugUnitTest` → 558 tasks, 0 failures, BUILD SUCCESSFUL.

## Notes / risks

- Bumping the `titleLarge` token to `heading()` is app-wide. Checked settings shelves, stats cards, and reader groups — all render cleanly with robust contrast.
- `HeroNumber` renders mixed text+numerals (`"3H 20M"`), now crisp and punchy with straight stems (`ROND 0f`).
- Environment: AGP 8.7.3 needs JDK 17 — `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`.

## Related context
The reader chrome was reworked with floating expressive pill bar, live scrub tooltip, persistent scrim page pill, `MoriHaptic.FrequentTick` on page increments, chrome auto-hide 5000ms, and top bar promoted to L1.
