package com.mori.feature.reader.impl

import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection

/** User intentions in the reader. State changes flow through [ReaderViewModel.onAction]. */
sealed interface ReaderAction {
    data object ToggleChrome : ReaderAction

    /** Explicit hide for timers (auto-hide, overview): never toggles back on. */
    data object HideChrome : ReaderAction

    data object NextPage : ReaderAction

    data object PrevPage : ReaderAction

    /** Jump to a 0-based page (slider scrub, page overview). */
    data class SeekPage(val index: Int) : ReaderAction

    /** The pager settled on a 0-based page (swipe). */
    data class PageChanged(val index: Int) : ReaderAction

    data object ToggleBookmark : ReaderAction

    data object OpenSettings : ReaderAction

    data object CloseSettings : ReaderAction

    data object OpenOverview : ReaderAction

    data object CloseOverview : ReaderAction

    data class SetDirection(val direction: ReadingDirection) : ReaderAction

    data class SetPageFit(val fit: PageFit) : ReaderAction

    data object ToggleCrop : ReaderAction

    data object ToggleVolumeKeys : ReaderAction

    data object ToggleVolumeKeysInverted : ReaderAction

    data object ToggleKeepScreenOn : ReaderAction

    data object ToggleTapZones : ReaderAction

    /** Pause progress, history, and stats recording while on. */
    data object ToggleIncognito : ReaderAction

    data object TogglePageCounter : ReaderAction

    data object ToggleSwipeToTurn : ReaderAction

    data object ToggleDualSplit : ReaderAction

    data object ToggleDualInvert : ReaderAction

    /** Display-filter edits apply to this book as a per-comic override. */
    data class SetFilterBrightness(val brightness: Float) : ReaderAction

    data class SetFilterNightTint(val nightTint: Float) : ReaderAction

    data object ToggleFilterGrayscale : ReaderAction

    data object ToggleFilterInvert : ReaderAction

    /** Deletes the override, restoring the global default. */
    data object ResetDisplayFilter : ReaderAction
}
