package com.mori.core.data

import com.mori.core.model.Comic
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder

/**
 * Applies a [LibraryQuery] to in-memory rows. Kept out of SQL so sorting/filtering stays
 * unit-testable without a database; library sizes (hundreds of rows) make this cheap.
 * Public so feature tests and future consumers share one semantic.
 */
fun List<Comic>.applyQuery(query: LibraryQuery): List<Comic> {
    var result = this
    if (query.hideErrors) {
        result = result.filter { it.error == null }
    }
    result = when (query.filter) {
        LibraryFilter.ALL -> result
        LibraryFilter.IN_PROGRESS -> result.filter { it.isInProgress }
        LibraryFilter.UNREAD -> result.filter { !it.isInProgress && !it.isFinished }
        LibraryFilter.FINISHED -> result.filter { it.isFinished }
    }
    val text = query.text.trim()
    if (text.isNotEmpty()) {
        result = result.filter { comic ->
            comic.title.contains(text, ignoreCase = true) ||
                (comic.series?.contains(text, ignoreCase = true) == true) ||
                (comic.number?.contains(text, ignoreCase = true) == true)
        }
    }
    return when (query.sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> result.sortedByDescending { it.createdAt }
        LibrarySortOrder.RECENTLY_READ -> result.sortedByDescending { it.updatedAt }
        LibrarySortOrder.TITLE -> result.sortedWith(
            compareBy<Comic>({ it.title.lowercase() }, { it.series?.lowercase().orEmpty() }),
        )
        LibrarySortOrder.UNFINISHED_FIRST -> result.sortedWith(
            compareBy<Comic> { it.isFinished }.thenByDescending { it.updatedAt },
        )
    }
}
