package com.mori.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import com.mori.feature.reader.api.ReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Holds reader UI state.
 *
 * F2 will back this with `ComicsRepository` (metadata, page count, saved progress);
 * until then it serves clearly-marked placeholder content so the reader chrome,
 * gestures, and settings are fully interactive and testable.
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: ReaderRoute = savedStateHandle.toRoute<ReaderRoute>()

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        // TODO(F2): load Comic from repository (title, page count, saved page, bookmark).
        _uiState.value = ReaderUiState.Ready(
            title = args.comicId,
            subtitle = "",
            bookmarked = false,
            pageIndex = args.pageIndex.coerceIn(0, PLACEHOLDER_PAGE_COUNT - 1),
            pageCount = PLACEHOLDER_PAGE_COUNT,
            chromeVisible = true,
            direction = ReadingDirection.LEFT_TO_RIGHT,
            pageFit = PageFit.WIDTH,
            cropMargins = false,
            settingsOpen = false,
        )
    }

    fun onAction(action: ReaderAction) {
        val current = _uiState.value
        if (current !is ReaderUiState.Ready) return
        _uiState.update {
            when (action) {
                ReaderAction.ToggleChrome -> current.copy(chromeVisible = !current.chromeVisible)
                ReaderAction.NextPage -> current.copy(
                    pageIndex = (current.pageIndex + 1).coerceAtMost(current.pageCount - 1),
                    chromeVisible = true,
                )
                ReaderAction.PrevPage -> current.copy(
                    pageIndex = (current.pageIndex - 1).coerceAtLeast(0),
                    chromeVisible = true,
                )
                is ReaderAction.SeekPage -> current.copy(
                    pageIndex = action.index.coerceIn(0, current.pageCount - 1),
                    chromeVisible = true,
                )
                is ReaderAction.PageChanged -> current.copy(
                    pageIndex = action.index.coerceIn(0, current.pageCount - 1),
                )
                ReaderAction.ToggleBookmark -> current.copy(bookmarked = !current.bookmarked)
                ReaderAction.OpenSettings -> current.copy(settingsOpen = true, chromeVisible = true)
                ReaderAction.CloseSettings -> current.copy(settingsOpen = false)
                is ReaderAction.SetDirection -> current.copy(direction = action.direction)
                is ReaderAction.SetPageFit -> current.copy(pageFit = action.fit)
                ReaderAction.ToggleCrop -> current.copy(cropMargins = !current.cropMargins)
            }
        }
    }

    private companion object {
        // Matches the mockup's page count so the slider/pill layout can be verified.
        const val PLACEHOLDER_PAGE_COUNT = 173
    }
}
