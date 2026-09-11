package com.mori.feature.library.impl

/** Test tags for the library screen. */
object LibraryTestTags {
    const val Grid = "libraryGrid"
    const val Loading = "libraryLoading"
    const val SearchToggle = "librarySearchToggle"
    const val SearchField = "librarySearchField"
    const val FilterButton = "libraryFilterButton"
    const val RefreshButton = "libraryRefreshButton"
    const val SortFilterSheet = "librarySortFilterSheet"
    const val EmptyState = "libraryEmpty"
    const val EmptyRescan = "libraryEmptyRescan"
    const val EmptyChooseFolder = "libraryEmptyChooseFolder"
    const val Snackbar = "librarySnackbar"
    const val MenuButton = "libraryMenuButton"
    const val MenuPopup = "libraryMenuPopup"
    const val SettingsSheet = "librarySettingsSheet"

    fun cardFor(id: String): String = "libraryCard:$id"
}
