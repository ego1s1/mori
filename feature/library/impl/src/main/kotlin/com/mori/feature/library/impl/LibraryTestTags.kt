package com.mori.feature.library.impl

/** Test tags for the library screen. */
object LibraryTestTags {
    const val Grid = "libraryGrid"
    const val Loading = "libraryLoading"
    const val SearchToggle = "librarySearchToggle"
    const val SearchField = "librarySearchField"
    const val FilterButton = "libraryFilterButton"
    const val SortFilterSheet = "librarySortFilterSheet"
    const val EmptyState = "libraryEmpty"
    const val EmptyRescan = "libraryEmptyRescan"
    const val EmptyChooseFolder = "libraryEmptyChooseFolder"
    const val Shelf = "libraryContinueShelf"
    const val Snackbar = "librarySnackbar"

    fun cardFor(id: String): String = "libraryCard:$id"

    fun shelfCardFor(id: String): String = "libraryShelfCard:$id"

    fun bookmarkBadgeFor(id: String): String = "libraryBookmark:$id"

    const val CollectionRow = "libraryCollectionRow"
    const val CollectionNew = "libraryCollectionNew"
    const val CollectionCreateDialog = "libraryCollectionCreateDialog"
    const val CollectionCreateField = "libraryCollectionCreateField"
    const val CollectionCreateConfirm = "libraryCollectionCreateConfirm"
    const val CollectionDeleteDialog = "libraryCollectionDeleteDialog"
    const val CollectionConfirmDelete = "libraryCollectionConfirmDelete"

    fun collectionChip(id: Long): String = "libraryCollection:$id"
}
