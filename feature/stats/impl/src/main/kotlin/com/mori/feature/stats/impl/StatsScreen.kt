package com.mori.feature.stats.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.MoriChoiceGroup
import com.mori.core.designsystem.MoriChoiceOption
import com.mori.core.designsystem.MoriCollapsingTopBar
import com.mori.core.designsystem.MoriContentWell
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriSectionCard
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.DailyReadingStat
import com.mori.core.model.ReadingStats
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.model.dayStartMillis

/**
 * Public tab content for the main viewport. Route and tab share one internal
 * content; the ViewModel type never appears in a public signature.
 */
@Composable
fun StatsTabContent(modifier: Modifier = Modifier) {
    StatsRouteContent(modifier = modifier, viewModel = hiltViewModel())
}

@Composable
internal fun StatsRouteContent(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(
    uiState: StatsUiState,
    onAction: (StatsAction) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            MoriCollapsingTopBar(
                title = stringResource(R.string.stats_title),
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                StatsUiState.Loading -> MoriLoading(
                    modifier = Modifier.testTag(StatsTestTags.Loading),
                )
                is StatsUiState.Success -> StatsContent(
                    state = uiState,
                    onAction = onAction,
                    listState = listState,
                )
            }
        }
    }
}

@Composable
private fun StatsContent(
    state: StatsUiState.Success,
    onAction: (StatsAction) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    MoriContentWell(modifier = modifier) {
        if (state.totals.totalSessions == 0) {
            MoriEmptyState(
                icon = MoriIcons.History,
                title = stringResource(R.string.stats_empty_title),
                body = stringResource(R.string.stats_empty_body),
                actionLabel = null,
                onAction = null,
                bottomPadding = FloatingChromeBottomReserve,
                modifier = Modifier.testTag(StatsTestTags.EmptyState),
            )
            return@MoriContentWell
        }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = FloatingChromeBottomReserve,
            ),
            modifier = Modifier
                .fillMaxSize()
                .testTag(StatsTestTags.Content),
        ) {
            item("totals") {
                TotalsGrid(totals = state.totals)
            }
            item("chart") {
                MoriSectionCard(title = stringResource(R.string.stats_chart_title)) {
                    RangeSelector(range = state.range, onSelect = { onAction(StatsAction.SelectRange(it)) })
                    ReadingBarChart(
                        buckets = state.buckets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(top = 12.dp)
                            .testTag(StatsTestTags.Chart),
                    )
                }
            }
            item("streak") {
                StreakCard(streak = state.streak)
            }
            if (state.topBooks.isNotEmpty()) {
                item("topHeader") {
                    SectionHeading(stringResource(R.string.stats_top_books))
                }
                items(state.topBooks, key = { it.comic.id }) { book ->
                    TopBookRow(book = book)
                }
            }
        }
    }
}

/** 2x2 hero numbers; the value rides the heavy display face. */
@Composable
private fun TotalsGrid(
    totals: ReadingStats,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.stats_total_time),
                value = formatDuration(totals.totalDurationMs),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stats_total_pages),
                value = totals.totalPagesTurned.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.stats_total_finished),
                value = totals.booksFinished.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stats_total_sessions),
                value = totals.totalSessions.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    MoriSectionCard(modifier = modifier.testTag(StatsTestTags.totalFor(label))) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = LocalAppFonts.current.displayFlex,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RangeSelector(
    range: StatsRange,
    onSelect: (StatsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = StatsRange.entries
    val tagForLabel = options.associate { rangeLabel(it) to StatsTestTags.rangeFor(it) }
    MoriChoiceGroup(
        options = options.map { MoriChoiceOption(label = rangeLabel(it)) },
        selectedIndex = options.indexOf(range),
        onSelect = { onSelect(options[it]) },
        modifier = modifier,
        testTagFor = { tagForLabel.getValue(it) },
    )
}

@Composable
private fun rangeLabel(range: StatsRange): String = stringResource(
    when (range) {
        StatsRange.WEEK -> R.string.stats_range_week
        StatsRange.MONTH -> R.string.stats_range_month
        StatsRange.YEAR -> R.string.stats_range_year
    },
)

/** Rounded-bar chart of reading time per bucket, normalised to the peak. */
@Composable
private fun ReadingBarChart(
    buckets: List<DailyReadingStat>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    if (buckets.isEmpty()) return
    val maxDuration = buckets.maxOf { it.durationMs }.coerceAtLeast(1L)
    Canvas(modifier = modifier) {
        val count = buckets.size
        val slot = size.width / count
        val barWidth = (slot * 0.6f).coerceAtLeast(2f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        buckets.forEachIndexed { index, bucket ->
            val ratio = bucket.durationMs.toFloat() / maxDuration.toFloat()
            val barHeight = (size.height * ratio).coerceAtLeast(if (bucket.durationMs > 0L) 4f else 2f)
            val left = index * slot + (slot - barWidth) / 2f
            // Track keeps an empty day visible as a faint stub.
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, size.height - 2f),
                size = Size(barWidth, 2f),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = if (bucket.durationMs > 0L) barColor else trackColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun StreakCard(
    streak: StreakInfo,
    modifier: Modifier = Modifier,
) {
    MoriSectionCard(
        title = stringResource(R.string.stats_streak_title),
        modifier = modifier.testTag(StatsTestTags.Streak),
    ) {
        Text(
            text = stringResource(R.string.stats_streak_current, streak.current),
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = LocalAppFonts.current.displayFlex,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.stats_streak_longest, streak.longest),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TopBookRow(
    book: TopBook,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp),
    ) {
        MoriCoverArt(
            coverPath = book.comic.coverPath,
            contentDescription = null,
            modifier = Modifier
                .width(44.dp)
                .height(64.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.comic.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Text(
                text = stringResource(
                    R.string.stats_top_book_detail,
                    formatDuration(book.durationMs),
                    book.pagesTurned,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/** Compact duration: 45s, 12m, 3h 20m. */
internal fun formatDuration(totalMs: Long): String {
    val totalSeconds = totalMs.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m"
        else -> "${seconds}s"
    }
}

@ThemePreviews
@Composable
private fun StatsScreenPreview() {
    MoriTheme {
        StatsScreen(
            uiState = StatsUiState.Success(
                totals = ReadingStats(
                    totalSessions = 12,
                    totalDurationMs = 5_400_000L,
                    totalPagesTurned = 240,
                    booksFinished = 3,
                ),
                buckets = (0 until 7).map { index ->
                    DailyReadingStat(
                        dayStartMillis = dayStartMillis(System.currentTimeMillis()) - index * 86_400_000L,
                        durationMs = (index % 4) * 600_000L,
                        pagesTurned = index * 3,
                        sessions = index,
                    )
                },
                range = StatsRange.WEEK,
                streak = StreakInfo(current = 4, longest = 9),
                topBooks = listOf(
                    TopBook(
                        comic = Comic(
                            id = "1",
                            title = "Batman: Court of Owls",
                            series = null,
                            number = null,
                            format = ComicFormat.CBZ,
                            pageCount = 173,
                            sourcePath = "/lib/1.cbz",
                            coverPath = null,
                            lastPageIndex = 12,
                            sourceDisplayName = "1.cbz",
                            createdAt = 1L,
                            updatedAt = 1L,
                        ),
                        durationMs = 3_600_000L,
                        pagesTurned = 120,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
