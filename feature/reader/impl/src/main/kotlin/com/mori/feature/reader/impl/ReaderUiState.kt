package com.mori.feature.reader.impl

import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Ready(
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
    ) : ReaderUiState {
        /** 1-based page number shown in the UI. */
        val currentPage: Int get() = pageIndex + 1
    }

    data class Error(val message: String) : ReaderUiState
}
