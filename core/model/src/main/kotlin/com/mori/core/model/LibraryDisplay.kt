package com.mori.core.model

/**
 * Persisted library display options: sort, filter, and error visibility.
 * Unlike the ephemeral search text, these survive full app restarts.
 */
data class LibraryDisplay(
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hideErrors: Boolean = false,
    /** Shelves collapsed in the sectioned grid, by collection id. */
    val collapsedShelfIds: Set<Long> = emptySet(),
) {
    fun toQuery(text: String): LibraryQuery = LibraryQuery(
        text = text,
        sortOrder = sortOrder,
        filter = filter,
        hideErrors = hideErrors,
    )
}
