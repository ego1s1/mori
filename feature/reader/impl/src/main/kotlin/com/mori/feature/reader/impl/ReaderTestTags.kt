package com.mori.feature.reader.impl

/** Test tags for the reader screen. */
object ReaderTestTags {
    const val TopBar = "readerTopBar"
    const val Bookmark = "readerBookmark"
    const val Pager = "readerPager"
    const val Prev = "readerPrev"
    const val Next = "readerNext"
    const val Slider = "readerSlider"
    const val SettingsButton = "readerSettingsButton"
    const val SettingsSheet = "readerSettingsSheet"
    const val OverviewButton = "readerOverviewButton"
    const val OverviewSheet = "readerOverviewSheet"
    const val OverviewGrid = "readerOverviewGrid"
    const val DirectionButton = "readerDirectionButton"
    const val FitButton = "readerFitButton"
    const val CropButton = "readerCropButton"
    const val PageCounter = "readerPageCounter"
    const val IncognitoBadge = "readerIncognitoBadge"

    fun thumbFor(pageIndex: Int): String = "readerThumb:$pageIndex"
}
