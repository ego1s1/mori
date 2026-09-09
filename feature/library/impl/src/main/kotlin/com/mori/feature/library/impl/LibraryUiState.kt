package com.mori.feature.library.impl

import com.mori.core.model.Comic
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import com.mori.core.model.ThemePreferences

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val comics: List<Comic>,
        val query: LibraryQuery,
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val settingsOpen: Boolean,
        val theme: ThemePreferences,
        val snackbar: String?,
    ) : LibraryUiState {
        val isEmpty: Boolean get() = comics.isEmpty()

        /** Most recently touched comic; the resume button opens it at its saved page. */
        val resumeTarget: Comic? get() = comics.maxByOrNull { it.updatedAt }
    }
}

sealed interface LibraryAction {
    data class SearchTextChanged(val text: String) : LibraryAction

    data class SortSelected(val sort: LibrarySortOrder) : LibraryAction

    data class FilterSelected(val filter: LibraryFilter) : LibraryAction

    data class ToggleHideErrors(val hide: Boolean) : LibraryAction

    data object OpenFilter : LibraryAction

    data object CloseFilter : LibraryAction

    data object OpenSettings : LibraryAction

    data object CloseSettings : LibraryAction

    data class SetThemeMode(val mode: com.mori.core.model.ThemeMode) : LibraryAction

    data class SetDynamicColor(val enabled: Boolean) : LibraryAction

    data class SetAmoled(val enabled: Boolean) : LibraryAction

    data object Refresh : LibraryAction

    data object DismissSnackbar : LibraryAction
}
