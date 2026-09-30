package com.mori.feature.stats.impl

/** Test tags for the stats screen. */
object StatsTestTags {
    const val Content = "statsContent"
    const val Loading = "statsLoading"
    const val EmptyState = "statsEmptyState"
    const val Chart = "statsChart"
    const val TopBooks = "statsTopBooks"
    const val Streak = "statsStreak"

    fun rangeFor(range: StatsRange): String = "statsRange:${range.name}"

    fun totalFor(label: String): String = "statsTotal:$label"
}
