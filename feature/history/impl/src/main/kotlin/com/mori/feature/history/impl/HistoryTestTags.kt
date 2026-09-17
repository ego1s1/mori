package com.mori.feature.history.impl

/** Test tags for the history screen. */
object HistoryTestTags {
    const val Loading = "historyLoading"
    const val List = "historyList"
    const val SearchField = "historySearchField"
    const val EmptyState = "historyEmptyState"

    fun rowFor(comicId: String) = "historyRow_$comicId"
}
