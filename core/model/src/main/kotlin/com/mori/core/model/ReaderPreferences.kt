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

/** Reader preferences persisted in DataStore. */
data class ReaderPreferences(
    val direction: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val pageFit: PageFit = PageFit.WIDTH,
    val cropMargins: Boolean = false,
    val volumeKeys: Boolean = false,
    val keepScreenOn: Boolean = true,
    val showPageCounter: Boolean = true,
)
