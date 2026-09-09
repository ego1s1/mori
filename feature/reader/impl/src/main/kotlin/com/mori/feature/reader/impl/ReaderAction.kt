package com.mori.feature.reader.impl

import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection

/** User intentions in the reader. State changes flow through [ReaderViewModel.onAction]. */
sealed interface ReaderAction {
    data object ToggleChrome : ReaderAction

    data object NextPage : ReaderAction

    data object PrevPage : ReaderAction

    /** Jump to a 0-based page (slider scrub, page overview). */
    data class SeekPage(val index: Int) : ReaderAction

    /** The pager settled on a 0-based page (swipe). */
    data class PageChanged(val index: Int) : ReaderAction

    data object ToggleBookmark : ReaderAction

    data object OpenSettings : ReaderAction

    data object CloseSettings : ReaderAction

    data class SetDirection(val direction: ReadingDirection) : ReaderAction

    data class SetPageFit(val fit: PageFit) : ReaderAction

    data object ToggleCrop : ReaderAction

    data object ToggleVolumeKeys : ReaderAction

    data object ToggleKeepScreenOn : ReaderAction

    data object ToggleTapZones : ReaderAction
}
