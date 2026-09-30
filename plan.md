# Settings overhaul — predictive back, expressive subsections, haptics

Decisions locked: Tomato slide+parallax transitions, full gesture preview, all six
sections overhauled, small detail bar stays, cookies on heroes only, Flex display
values in Storage + About + slider readouts + Stats + library long-press menu.

## Stage 1 — Motion: Tomato slide + predictive preview (settings only)
- `AnimatedContent(category)`: push = full-width slide-in + 1/4-parallax
  slide-out with fade-out; pop = 1/4 slide-in with fade-in + full-width
  slide-out. RTL-aware, token-driven (`tabEnterSpec`/`tabExitSpec`),
  fade-only under calm/reduced-motion.
- `PredictiveBackHandler(enabled = category != null)`: scrub the same slide
  (detail drifts, hub revealed underneath), release commits, cancel springs
  back. `BackHandler` stays for 3-button nav. No back haptic (Tomato parity).

## Stage 2 — Shared row component + Flex roles (designsystem)
- New `MoriSettingRow`: 72dp min, `surfaceContainerLow`, `extraLarge` shape,
  leading icon container, title/subtitle, trailing control slot.
- Flex roles from existing `AppFonts`/`Type.kt`: selected-segment labels +
  setting values → Flex emphasized; hero numerals → Flex display w900.
- Switches gain the missing off-state thumb icon (check/clear both states).

## Stage 3 — Per-section overhaul + satellites
- Appearance: icon-bearing segments, filter-pill swatches, Flex section labels.
- Reader: sliders with Flex value readouts + scrub ticks; segments with icons.
- Shelves: segmented icon rows (cookie icon, Flex count); dialogs go
  tonal-dismiss + filled-confirm.
- Privacy: Feeding-times rows (status value + control).
- Storage: finance-card hero (giant Flex number + filled-tonal clear button).
- About: Cardfolio hero (cookie app-mark + Flex headline + version).
- Satellites: Stats heroes/range selector; library long-press menu rows.

## Stage 4 — Haptics coverage (additive, Pulsar mappings unchanged)
Segments/swatches `Select`; slider scrub `FrequentTick`; shelf + dialog
commits `Confirm`; cache result `Confirm`/`Reject`; licenses `Select`; stats
range `Select`; long-press open `Select`. No back/typing/expand haptics.

## Stage 5 — Verify (emulator only, no phone)
`assembleDebug` + unit tests + `detekt`; emulator round trip incl.
mid-gesture predictive-back screenshots; dark + dynamic-color pass; logcat
haptic smoke with no fatals.

## Non-goals
No `LargeFlexible` headers, no hub redesign, no tab-bar/FAB changes, no Pulsar
API or permission changes, phone-Compact scope, calm-motion fallbacks everywhere.
