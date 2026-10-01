package com.mori.feature.stats.impl

import app.cash.turbine.test
import com.mori.core.model.dayStartMillis
import com.mori.core.model.previousDayStartMillis
import com.mori.core.testing.FakeComicsRepository
import com.mori.core.testing.TestDispatcherRule
import com.mori.core.testing.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun viewModel(repository: FakeComicsRepository = FakeComicsRepository()) =
        StatsViewModel(repository)

    @Test
    fun emptyLibraryEmitsZeroedSuccess() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { it is StatsUiState.Success } as StatsUiState.Success

            assertEquals(0, state.totals.totalSessions)
            assertEquals(0L, state.totals.totalDurationMs)
            assertEquals(StatsRange.WEEK, state.range)
            assertEquals(7, state.buckets.size)
            assertEquals(StreakInfo(0, 0), state.streak)
            assertEquals(true, state.topBooks.isEmpty())
        }
    }

    @Test
    fun sessionsAggregateIntoBucketsStreakAndTopBooks() = runTest {
        val repository = FakeComicsRepository(
            listOf(
                FakeComicsRepository.comic("a", title = "Apple"),
                FakeComicsRepository.comic("b", title = "Banana"),
            ),
        )
        val today = dayStartMillis(System.currentTimeMillis())
        // Calendar predecessor, not minus-24h: the pair must be consecutive
        // days even across a DST transition.
        val yesterday = previousDayStartMillis(today)
        // b gets more time today; a read yesterday and briefly today.
        repository.recordSession("a", yesterday + 1_000L, yesterday + 61_000L, pagesTurned = 3)
        repository.recordSession("b", today + 1_000L, today + 301_000L, pagesTurned = 20)
        repository.recordSession("a", today + 400_000L, today + 460_000L, pagesTurned = 4)

        viewModel(repository).uiState.test {
            val state = awaitWhere { it is StatsUiState.Success } as StatsUiState.Success

            assertEquals(3, state.totals.totalSessions)
            assertEquals(27, state.totals.totalPagesTurned)
            // Chart buckets carry the sessions (regression: exact-timestamp keys
            // left every bucket at zero).
            assertEquals(3, state.buckets.sumOf { it.sessions })
            assertEquals(420_000L, state.buckets.sumOf { it.durationMs })
            // Two consecutive days.
            assertEquals(StreakInfo(current = 2, longest = 2), state.streak)
            // Ranked by duration: b first.
            assertEquals(listOf("b", "a"), state.topBooks.map { it.comic.id })
            assertEquals(300_000L, state.topBooks[0].durationMs)
        }
    }

    @Test
    fun selectRangeResizesBuckets() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val week = awaitWhere { it is StatsUiState.Success } as StatsUiState.Success
            assertEquals(7, week.buckets.size)

            vm.onAction(StatsAction.SelectRange(StatsRange.MONTH))
            val month = awaitWhere {
                it is StatsUiState.Success && it.range == StatsRange.MONTH
            } as StatsUiState.Success
            assertEquals(30, month.buckets.size)

            vm.onAction(StatsAction.SelectRange(StatsRange.YEAR))
            val year = awaitWhere {
                it is StatsUiState.Success && it.range == StatsRange.YEAR
            } as StatsUiState.Success
            assertEquals(365, year.buckets.size)
        }
    }

    @Test
    fun streakKeepsLongestAcrossGap() {        val today = dayStartMillis(System.currentTimeMillis())
        val day = 86_400_000L
        val sessions = listOf(
            com.mori.core.model.ReadingSession("a", today - 5 * day, today - 5 * day + 60_000L, 1),
            com.mori.core.model.ReadingSession("a", today - 4 * day, today - 4 * day + 60_000L, 1),
            com.mori.core.model.ReadingSession("a", today - 3 * day, today - 3 * day + 60_000L, 1),
            com.mori.core.model.ReadingSession("a", today, today + 60_000L, 1),
        )
        val streak = readingStreak(sessions, System.currentTimeMillis())

        assertEquals(1, streak.current)
        assertEquals(3, streak.longest)
    }

    @Test
    fun topBooksSkipsRemovedComics() {
        val comics = listOf(FakeComicsRepository.comic("a", title = "Apple"))
        val sessions = listOf(
            com.mori.core.model.ReadingSession("ghost", 1_000L, 61_000L, 2),
            com.mori.core.model.ReadingSession("a", 2_000L, 32_000L, 5),
        )
        val top = topBooks(sessions, comics)

        assertEquals(listOf("a"), top.map { it.comic.id })
        assertEquals(30_000L, top[0].durationMs)
    }

    /**
     * Spring forward (23h Sunday): consecutive calendar days must streak.
     * Pacific/Auckland skipped 2026-09-27 02:00→03:00, so fixed 24h
     * arithmetic reads the pair as broken. Regression for DAY_MS streaks.
     */
    @Test
    fun streakSurvivesSpringForward() {
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        try {
            val saturday = dayStartMillis(noonMillis(2026, Calendar.SEPTEMBER, 26))
            val sunday = dayStartMillis(noonMillis(2026, Calendar.SEPTEMBER, 27))
            val sessions = listOf(
                com.mori.core.model.ReadingSession("a", saturday + 3_600_000L, saturday + 3_660_000L, 4),
                com.mori.core.model.ReadingSession("a", sunday + 3_600_000L, sunday + 3_660_000L, 4),
            )

            assertEquals(StreakInfo(2, 2), readingStreak(sessions, sunday + 7_200_000L))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    /**
     * Fall back (25h Sunday): Auckland ends DST 2026-04-05, repeating the
     * 02:00 hour. Same consecutive-day streak must hold in the other
     * direction.
     */
    @Test
    fun streakSurvivesFallBack() {
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        try {
            val saturday = dayStartMillis(noonMillis(2026, Calendar.APRIL, 4))
            val sunday = dayStartMillis(noonMillis(2026, Calendar.APRIL, 5))
            val sessions = listOf(
                com.mori.core.model.ReadingSession("a", saturday + 3_600_000L, saturday + 3_660_000L, 4),
                com.mori.core.model.ReadingSession("a", sunday + 3_600_000L, sunday + 3_660_000L, 4),
            )

            assertEquals(StreakInfo(2, 2), readingStreak(sessions, sunday + 7_200_000L))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    private fun noonMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
