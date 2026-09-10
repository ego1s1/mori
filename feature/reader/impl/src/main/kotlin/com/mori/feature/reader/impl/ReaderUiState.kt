package com.mori.feature.reader.impl

import com.mori.core.model.ComicError
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Ready(
        val comicId: String,
        val title: String,
        val subtitle: String,
        val bookmarked: Boolean,
        val pageIndex: Int,
        val pageCount: Int,
        val chromeVisible: Boolean,
        val direction: ReadingDirection,
        val pageFit: PageFit,
        val cropMargins: Boolean,
        val settingsOpen: Boolean,
        val volumeKeys: Boolean,
        val keepScreenOn: Boolean,
        val showTapZones: Boolean,
        val showPageCounter: Boolean,
        val swipeToTurn: Boolean,
        /** False for slider seeks (direct manipulation jumps); true for turns. */
        val turnAnimated: Boolean = true,
    ) : ReaderUiState {
        /** 1-based page number shown in the UI. */
        val currentPage: Int get() = pageIndex + 1
    }

    data class Error(val cause: ReaderErrorCause) : ReaderUiState
}

/** Why the reader shows a fullscreen error instead of pages. */
sealed interface ReaderErrorCause {
    data object Removed : ReaderErrorCause

    data class Failed(val error: ComicError) : ReaderErrorCause
}
