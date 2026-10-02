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

/** Visual layout representation for library books. */
enum class LibraryDisplayMode {
    COMPACT_GRID,
    COMFORTABLE_GRID,
    COVER_ONLY_GRID,
    LIST,
}

/**
 * A text query plus sort/filter/display preferences for the library grid.
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
    val displayMode: LibraryDisplayMode = LibraryDisplayMode.COMPACT_GRID,
    /** 0 = Adaptive column sizing, or 1..6 for fixed column counts. */
    val gridColumns: Int = 0,
) {
    /**
     * True when any control beyond display styling and text field diverges from defaults.
     * Collapsed shelves and display modes are view state / layout preferences and never count.
     */
    fun hasActiveFilters(): Boolean =
        sortOrder != LibrarySortOrder.RECENTLY_ADDED ||
                filter != LibraryFilter.ALL ||
                hideErrors
}
