package com.mori.feature.reader.impl

import com.mori.core.model.ComicError
import com.mori.core.model.PageFit
import com.mori.core.model.PageHalf
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
        val overviewOpen: Boolean,
        val volumeKeys: Boolean,
        val keepScreenOn: Boolean,
        val showTapZones: Boolean,
        val showPageCounter: Boolean,
        val swipeToTurn: Boolean,
        /** False for slider seeks (direct manipulation jumps); true for turns. */
        val turnAnimated: Boolean = true,
        /**
         * Pager positions. Without the dual-page split this is the identity
         * mapping (one FULL entry per archive page); with the split on, wide
         * pages expand into halves. [pageCount] always equals this size, so
         * the pager, slider and counter treat positions uniformly.
         */
        val viewerPages: List<ReaderViewerPage> =
            List(pageCount) { ReaderViewerPage(it, PageHalf.FULL) },
        /** Archive page count (overview grid size, progress domain). */
        val archivePageCount: Int = pageCount,
        /**
         * First pager position per archive page. Defaults to identity; the
         * ViewModel always passes the mapped value.
         */
        val expandedForArchive: List<Int> = List(pageCount) { it },
        val dualPageSplit: Boolean = false,
        val dualPageInvert: Boolean = false,
    ) : ReaderUiState {
        /** 1-based page number shown in the UI. */
        val currentPage: Int get() = pageIndex + 1

        /** Archive page behind the current pager position (overview selection). */
        val currentArchiveIndex: Int get() = viewerPages.getOrNull(pageIndex)?.archiveIndex
            ?: pageIndex.coerceIn(0, (archivePageCount - 1).coerceAtLeast(0))
    }

    data class Error(val cause: ReaderErrorCause) : ReaderUiState
}

/** Why the reader shows a fullscreen error instead of pages. */
sealed interface ReaderErrorCause {
    data object Removed : ReaderErrorCause

    data class Failed(val error: ComicError) : ReaderErrorCause
}
