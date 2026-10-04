# Mori UI Hierarchy & Expressive Design System Specification

## 1. Design System Philosophy & Architecture

Mori is a modern, high-craft offline-first comic and manga reader designed for Android API 24+ following **Material 3 Expressive (M3 Expressive)** principles introduced at Google I/O.

### Core Tenets
1. **Emotion-Driven Aesthetics & Contrast:** Bolder dynamic color palettes, tactile surfaces, and variable-font typography rather than uniform pastels.
2. **Physics-Based Motion:** Spring and momentum physics (`MoriMotion`) for spatial and effect transitions; avoiding mechanical cubic beziers.
3. **Tactile Haptic Grammar:** Full tactile feedback integration (`MoriHaptic` via Pulsar haptics) aligned with navigation, scrubbing, toggling, and dialog confirmations.
4. **Content-First Immersion:** Controls float in elevated, detached islands and docks; reading surfaces remain edge-to-edge and unencumbered.

---

## 2. Global Design Tokens

### 2.1 Typographic Hierarchy (Unified Scale)

| Level | Role | Variable Font & Style | Size / Line Height | Semantic Locations |
|:---|:---|:---|:---|:---|
| **L1** | Screen Title | `topBarTitle` (wght 900, wdth 100) | 28sp / 34sp | `MoriCollapsingTopBar`, `DetailScreen`, `ReaderTopBar` |
| **L1b** | Hero / Step Title | `HeadingFlex` (wght 700–800) | 28sp / 36sp | Onboarding wizard steps, Empty states |
| **L2** | Major Section Header | `MoriEmphasized.titleLarge` (wght 700) | 22sp / 28sp | `MoriSectionCard`, Shelf headers, Stats sections |
| **L3** | Sheet & Group Header | `MoriEmphasized.titleMedium` (wght 700) | 16sp / 24sp | Sheet subheads, Category groups, Filter headings |
| **L4** | Item / Row Title | `HeadingFlex` via `MoriEmphasized.bodyLarge` | 16sp / 24sp | Setting rows, Book card labels, List items |
| **Hero Data** | Metric Numeral | `displayFlex` (wght 900, wdth 125, slant -10) | 28–64sp | Stats `HeroNumber`, Bento counters |
| **Hero Label** | Metric Caption | `MoriEmphasized.labelLarge` (wght 600) | 14sp / 20sp | "Time reading", "Streak days", Scrubber counters |
| **Caption** | Subtitle / Explainer | `MaterialTheme.typography.bodySmall` | 12sp / 16sp | Setting subtitles, Secondary metadata, Timestamps |

### 2.2 Color & Surface Elevation Mapping (M3 Expressive)

| Token | Role in Mori | Visual Appearance |
|:---|:---|:---|
| `surface` | Base app background | Neutral dark/light canvas |
| `surfaceContainerLow` | Embedded Bento cards, large grouped blocks | Subtle tonal contrast against background |
| `surfaceContainerHigh` | Floating action docks, scrubber islands, navigation bars | Elevated floating islands (4.dp tonal, 6.dp shadow) |
| `surfaceContainerHighest` | Inactive chip/segment containers, progress tracks, neutral zones | Muted interactive targets, menu tap zones |
| `primaryContainer` | Active toggles, selected filter chips, NEXT tap zones | High-contrast accent fill (`onPrimaryContainer` content) |
| `tertiaryContainer` | PREV tap zones, special milestone badges | Expressive secondary accent fill |
| `outlineVariant` | Subtle 1.dp structural dividers and borders | Low-alpha separation (`alpha = 0.2f` to `0.25f`) |

### 2.3 Motion System (`MoriMotion`)

- **Spatial Physics:** Spring-based damping (`stiffness = Spring.StiffnessMediumLow`, `dampingRatio = Spring.DampingRatioLowBouncy`).
- **Effect Physics:** Quick fade/color transition (`stiffness = Spring.StiffnessMedium`, `dampingRatio = Spring.DampingRatioNoBouncy`).
- **Enter/Exit Transitions:**
  - `CHROME_TOP`: Slide down + fade in.
  - `CHROME_BOTTOM`: Slide up + fade in.
  - `RISE`: Staggered upward emergence for cards and lists.
  - `SHEET`: Modal spring expansion with backdrop dimming.

### 2.4 Haptic Feedback Signatures (`MoriHaptic`)

- `Tap` / `Light`: Standard chip selection, category tab click.
- `Select` / `Toggle`: Setting toggle switch, mode card selection, bookmark flip.
- `FrequentTick`: Reader scrubber page drag, slider notch tick.
- `Confirm` / `Success`: Library sync complete, action dialog confirmation.
- `Warning` / `Impact`: Collection delete, cache clearing.

---

## 3. Screen & Feature UI Hierarchy

```
Mori App
├── 1. Library (Root Navigation Tab)
│   ├── Top Bar (Collapsing, 28sp Title, Search & Filter Split Actions)
│   ├── Dynamic Search Island (Pill Input with clear / filter badges)
│   ├── Segmented Quick-Filters (All, In Progress, Unread, Favorites)
│   ├── Comic Grid (Asymmetric cover cards with progress pills & bookmarks)
│   ├── Shelves Carousel (Collection chip carousels)
│   └── Floating Action Island (Persistent Bottom Dock)
│
├── 2. Comic Detail Screen
│   ├── Hero Cover Art Header (Dynamic backdrop with parallax)
│   ├── Metadata Card (Format badges, page count, file size, reading state)
│   ├── Action Island (Resume Reading primary split button + Bookmark toggle)
│   └── Chapter / Issue Navigator (List with reading completion indicators)
│
├── 3. Reader Experience (Full Immersion)
│   ├── Immersive Canvas (Hardware-accelerated bitmap/PDF/CBZ renderer)
│   ├── Overlay Tap Zones (Interactive architectural box partitions)
│   ├── Floating Scrubber Island (Dynamic pill with scrub tooltip bubble)
│   ├── Floating Action Dock (Elevated Segmented Dock: Direction, Fit, Crop, Overview, Settings)
│   └── Reader Settings Modal Sheet
│       ├── Reading Direction (LTR, RTL, Vertical)
│       ├── Page Fit (Width, Height, Original)
│       ├── Margin Cropping Toggle
│       ├── Tap Zones Minimal Box Choice Cards (Live phone-ratio geometric partitions)
│       └── Invert & Advanced Navigation Controls
│
├── 4. Reading Statistics (Insights & Journey)
│   ├── Screen Header (28sp Title)
│   ├── Range Selector (Week, Month, Year Segmented Group)
│   ├── Hero Bento Grid:
│   │   ├── Primary Hero Card: Total Reading Time (Rolling ticker + pace badge)
│   │   ├── Pages Turned Card (Ticker + Bookmarks count)
│   │   └── Finished Volumes Card (Completion rate)
│   ├── Expressive Activity Chart (Rounded-bar distribution with peak day accent)
│   ├── Streak Journey Card (Flame badge + Current & Longest records)
│   └── Top Books List (Ranked cover row with total time spent)
│
└── 5. Settings & About
    ├── Settings Categories Hub (Library, Reader, Appearance, Storage, Advanced, About)
    └── About Section:
        ├── Unified Brand & Creator Hero (App mark, gradient Flex title, tagline, combined Mori version + dev avatar @ego1s1 attribution pill bar)
        └── Setting Link Rows (Discrete staggered reveal rows: GitHub, Issue tracker, Changelog, Privacy policy, Licenses)
```

---

## 4. Component Implementation Registry

| Component | File Path | Expressive Tokens Applied |
|:---|:---|:---|
| `ReaderBottomChrome` | `feature/reader/impl/ReaderScreen.kt` | Segmented dock, `surfaceContainerHigh`, `CircleShape`, animated toggle tint |
| `NavModeChoiceCards` | `feature/reader/impl/ReaderSettingsSheet.kt` | Minimal box geometric partitions, `tertiaryContainer`/`primaryContainer`, live preview |
| `ReadingTimeHeroCard` / `StaggeredDurationHero` | `feature/stats/impl/StatsScreen.kt` | Staggered Google Sans Flex variable typography (`displayFlex`, `displayFlexMedium`, `displayUnit`), baseline offsets |
| `SessionsInsightCard` / `SessionRhythmSparkGraph` | `feature/stats/impl/StatsScreen.kt` | Asymmetric Bento card, animated cubic spline area graph, daily rhythm capsules |
| `MoriMorphingShape` | `core/designsystem/MoriMorphingShape.kt` | `androidx.graphics.shapes.Morph`, rotating polygonal blossom, M3 Expressive motion |
| `ReadingBarChart` | `feature/stats/impl/StatsScreen.kt` | Animated spring growth, peak gradient highlight, interactive tooltips |
| `AboutSection` | `feature/settings/impl/SettingsScreen.kt` | Unified Brand & Creator Hero card, dual attribution pill bar, staggered reveal rows |
