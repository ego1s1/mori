package com.mori.core.model

/** Reading direction for the pager. */
enum class ReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

/** How a page fills the viewport. */
enum class PageFit {
    WIDTH,
    HEIGHT,
    ORIGINAL,
}

/** Tap navigation layout modes for the reader pager. */
enum class ReaderNavMode {
    DEFAULT,
    L_SHAPE,
    KINDLISH,
    EDGE,
    RIGHT_AND_LEFT,
    DISABLED,
}

/** Inversion mode for reader tap zones. */
enum class TapInvertMode {
    NONE,
    HORIZONTAL,
    VERTICAL,
    BOTH,
}

/** Reader preferences persisted in DataStore. */
data class ReaderPreferences(
    val direction: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val pageFit: PageFit = PageFit.WIDTH,
    val cropMargins: Boolean = false,
    val volumeKeys: Boolean = false,
    /** Volume-down goes to previous page instead of next. */
    val volumeKeysInverted: Boolean = false,
    val keepScreenOn: Boolean = true,
    val showPageCounter: Boolean = true,
    /** Swipe pager gestures turn pages alongside tap zones; on by default. */
    val swipeToTurn: Boolean = true,
    /** Zone overlay preview; persisted like the other reader defaults. */
    val showTapZones: Boolean = false,
    /**
     * Dual-page split: wide pages are divided
     * into halves that read as consecutive pager positions instead of one
     * squeezed page. Off by default — it rescans page dimensions on enable.
     */
    val dualPageSplit: Boolean = false,
    /** Read the second half before the first. */
    val dualPageInvert: Boolean = false,
    val navMode: ReaderNavMode = ReaderNavMode.DEFAULT,
    val invertTaps: TapInvertMode = TapInvertMode.NONE,
    /**
     * Display filters for page art. This is the global default; a per-comic
     * override (when present) replaces it wholesale.
     */
    val displayFilter: DisplayFilter = DisplayFilter(),
    /** Pause progress, history, and stats recording while in the reader. */
    val incognito: Boolean = false,
)
