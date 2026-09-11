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

/** A text query plus sort/filter preferences for the library grid. */
data class LibraryQuery(
    val text: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hideErrors: Boolean = false,
)
