# Changelog

## 0.4.0

### Highlights

**New Stats tab.** History is now Stats: reading totals, a 7-day / 30-day /
yearly activity chart, day streaks, and most-read books — headlined by big
italic expressive numerals that follow your wallpaper colors.

**Floating library search.** Search is now a floating panel: entries glide
behind it as you scroll, with full-text filter and sort pills
(All / In progress / Unread / Finished / Favorites, Recently added /
Recently opened / Title / Unfinished first) and a long-press quick-actions
menu on every cover.

**Expressive Settings.** A Tomato-style hub with sub-screens (Appearance,
Reader, Shelves, Privacy, Storage, About), slide + predictive-back
transitions, haptic feedback throughout, per-section heroes, and new
color schemes: Catppuccin, Nord, Gruvbox, Dracula, Tokyo Night, Everforest,
and Monochrome — plus dynamic wallpaper color and AMOLED black.

**A reader that stays out of the way.** Visible chrome with auto-hide,
a scrub slider, crossfade overview seeks, volume-key paging, dual-page
spreads, display filters, and true incognito reading that never records
progress, history, or stats.

**New app icon.** A centered brush-calligraphy 森 (forest) on a dark
Rosé Pine × Everforest night scene with a gold moon.

**Privacy and storage.** Optional app lock with biometric / device
credential, thumbnail-cache controls with storage usage, and an About
page with open-source licenses.

### Reliability

- Safer archive indexing (cancellation, symlinks, truncated files) with a
  distinct "file gone" state for moved or revoked files.
- Streaks survive daylight-saving transitions; progress saves respect
  incognito toggles in either order.
- Bounded splash start, lockout messaging, and DST-safe daily buckets.

Full history: https://github.com/ego1s1/mori/compare/v0.3.2...v0.4.0
