package com.mori.core.model

/** Where the library lives. */
enum class StorageLocation {
    /** Copies imports into app-private storage (originals untouched). */
    APP,

    /** Reads from a user-chosen folder; stays linked for rescans. */
    CUSTOM,
}

/** Preset color schemes offered alongside dynamic (wallpaper) color. */
enum class ColorSchemeChoice {
    MORI,
    OCEAN,
    FOREST,
    SUNSET,
}
