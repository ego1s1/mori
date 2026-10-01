package com.mori.core.model

/** How the library grid is ordered. */
enum class LibrarySortOrder {
    RECENTLY_ADDED,
    RECENTLY_READ,
    TITLE,
    UNFINISHED_FIRST,
}

/** Which subset of the library is shown. */
enum class LibraryFilter {
    ALL,
    IN_PROGRESS,
    UNREAD,
    FINISHED,
    FAVORITES,
}

/**
 * A text query plus sort/filter preferences for the library grid.
 *
 * The non-text fields are persisted display options (survive full restarts);
 * only [text] is ephemeral (restored across process death, cleared on full
 * restart).
 */
data class LibraryQuery(
    val text: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hideErrors: Boolean = false,
    /** Shelves collapsed in the sectioned grid, by collection id. */
    val collapsedShelfIds: Set<Long> = emptySet(),
) {
    /**
     * True when any control beyond the text field diverges from defaults.
     * Collapsed shelves are view state, not a filter, and never count.
     */
    fun hasActiveFilters(): Boolean = this != copy(text = "", collapsedShelfIds = emptySet())
}
