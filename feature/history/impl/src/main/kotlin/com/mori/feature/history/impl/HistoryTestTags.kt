package com.mori.feature.history.impl

/** Test tags for the history screen. */
object HistoryTestTags {
    const val Loading = "historyLoading"
    const val List = "historyList"
    const val SearchToggle = "historySearchToggle"
    const val SearchField = "historySearchField"
    const val EmptyState = "historyEmptyState"
    const val EmptyClearSearch = "historyEmptyClearSearch"

    fun rowFor(comicId: String) = "historyRow_$comicId"
}
