package com.mori.feature.stats.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.common.formatDuration
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriChoiceGroup
import com.mori.core.designsystem.MoriChoiceOption
import com.mori.core.designsystem.MoriCollapsingTopBar
import com.mori.core.designsystem.MoriContentWell
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.MoriSectionCard
import com.mori.core.designsystem.MoriSectionHeader
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.model.DailyReadingStat
import com.mori.core.model.ReadingStats
import com.mori.core.model.dayStartMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                icon = MoriIcons.BarChart,
                title = stringResource(R.string.stats_empty_title),
                body = stringResource(R.string.stats_empty_body),
                actionLabel = null,
                onAction = null,
                modifier = Modifier
                    .testTag(StatsTestTags.EmptyState)
                    .padding(bottom = FloatingChromeBottomReserve),
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
                TotalsGrid(totals = state.totals, buckets = state.buckets)
            }
            item("chart") {
                MoriSectionCard(
                    title = stringResource(R.string.stats_chart_title),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    RangeSelector(range = state.range, onSelect = { onAction(StatsAction.SelectRange(it)) })
                    ReadingBarChart(
                        buckets = state.buckets,
                        contentDescription = stringResource(R.string.stats_chart_desc),
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
                    MoriSectionHeader(
                        title = stringResource(R.string.stats_top_books),
                        modifier = Modifier.testTag(StatsTestTags.TopBooks),
                    )
                }
                items(state.topBooks, key = { it.comic.id }) { book ->
                    TopBookRow(book = book)
                }
            }
        }
    }
}

/**
 * Expressive Bento Grid adhering strictly to Material 3 Expressive contrast:
 * 1. Spotlight Hero Card for reading time with primaryContainer & onPrimaryContainer
 * 2. Balanced companion cards with contrasting secondaryContainer and tertiaryContainer
 * 3. Centered Sessions & Cadence Card with surfaceContainerHigh & session rhythm spark-graph
 */
@Composable
private fun TotalsGrid(
    totals: ReadingStats,
    buckets: List<DailyReadingStat>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Spotlight Hero Card: Reading Time in primaryContainer with Staggered Multi-size Typography
        ReadingTimeHeroCard(
            durationMs = totals.totalDurationMs,
            label = stringResource(R.string.stats_total_time),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StatsTestTags.totalFor(stringResource(R.string.stats_total_time))),
        )

        // Bento Row 1: Pages Turned (secondaryContainer) & Books Finished (tertiaryContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveStatCard(
                label = stringResource(R.string.stats_total_pages),
                value = totals.totalPagesTurned.toString(),
                icon = MoriIcons.MenuBook,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag(StatsTestTags.totalFor(stringResource(R.string.stats_total_pages))),
            )
            ExpressiveStatCard(
                label = stringResource(R.string.stats_total_finished),
                value = totals.booksFinished.toString(),
                icon = MoriIcons.Trophy,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag(StatsTestTags.totalFor(stringResource(R.string.stats_total_finished))),
            )
        }

        // Bento Row 2: Reading Sessions & Cadence Card with centered layout & sparkline
        SessionsInsightCard(
            totals = totals,
            buckets = buckets,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StatsTestTags.totalFor(stringResource(R.string.stats_total_sessions))),
        )
    }
}

/** Featured Hero Bento card for total reading time with primaryContainer & onPrimaryContainer */
@Composable
private fun ReadingTimeHeroCard(
    durationMs: Long,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
        ) {
            StaggeredDurationHero(
                durationMs = durationMs,
                label = label,
                primaryColor = MaterialTheme.colorScheme.onPrimaryContainer,
                secondaryColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
        }
    }
}

/**
 * Staggered duration display leveraging Google Sans Flex variable font axes:
 * - Huge displayFlex for primary hours (wide 125, black 900, upright)
 * - Compact displayUnit for 'h' and 'm' unit labels (width 95, bold 700, upright)
 * - Staggered intermediate displayFlexMedium for minutes (width 115, extra bold 800, upright)
 * - Expressive baseline alignment and optical sizing
 */
@Composable
private fun StaggeredDurationHero(
    durationMs: Long,
    label: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val fonts = LocalAppFonts.current

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier.fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = Triple(hours, minutes, seconds),
            transitionSpec = {
                (fadeIn(MoriMotion.defaultEffectsSpec()) togetherWith
                        fadeOut(MoriMotion.defaultEffectsSpec()))
            },
            label = "StaggeredDurationTransition",
        ) { (h, m, s) ->
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (horizontalAlignment == Alignment.CenterHorizontally) Arrangement.Center else Arrangement.Start,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (h > 0) {
                    // Hours numeral: Large displayFlex upright
                    Text(
                        text = "$h",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = fonts.displayFlex,
                            fontWeight = FontWeight.Black,
                            fontSize = if (h >= 100) 44.sp else 52.sp,
                            lineHeight = if (h >= 100) 44.sp else 52.sp,
                        ),
                        color = primaryColor,
                    )
                    // Hours unit: Staggered smaller displayUnit upright
                    Text(
                        text = "h",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = fonts.displayUnit,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                        ),
                        color = primaryColor.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp, end = 10.dp),
                    )
                    // Minutes numeral: Staggered intermediate displayFlexMedium upright
                    Text(
                        text = "$m",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = fonts.displayFlexMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (h >= 100) 34.sp else 38.sp,
                            lineHeight = if (h >= 100) 34.sp else 38.sp,
                        ),
                        color = secondaryColor,
                    )
                    // Minutes unit: Staggered compact displayUnit upright
                    Text(
                        text = "m",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = fonts.displayUnit,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 17.sp,
                        ),
                        color = secondaryColor.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = 5.dp, start = 2.dp),
                    )
                } else if (m > 0) {
                    // Minutes only: Large displayFlex upright
                    Text(
                        text = "$m",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = fonts.displayFlex,
                            fontWeight = FontWeight.Black,
                            fontSize = 52.sp,
                            lineHeight = 52.sp,
                        ),
                        color = primaryColor,
                    )
                    Text(
                        text = "m",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = fonts.displayUnit,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                        ),
                        color = primaryColor.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp, end = 10.dp),
                    )
                    if (s > 0) {
                        Text(
                            text = "$s",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = fonts.displayFlexMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 38.sp,
                                lineHeight = 38.sp,
                            ),
                            color = secondaryColor,
                        )
                        Text(
                            text = "s",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = fonts.displayUnit,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 17.sp,
                            ),
                            color = secondaryColor.copy(alpha = 0.72f),
                            modifier = Modifier.padding(bottom = 5.dp, start = 2.dp),
                        )
                    }
                } else {
                    // Seconds / Zero
                    Text(
                        text = "$seconds",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = fonts.displayFlex,
                            fontWeight = FontWeight.Black,
                            fontSize = 52.sp,
                            lineHeight = 52.sp,
                        ),
                        color = primaryColor,
                    )
                    Text(
                        text = "s",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = fonts.displayUnit,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                        ),
                        color = primaryColor.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                    )
                }
            }
        }

        Text(
            text = label,
            style = MoriEmphasized.labelLarge,
            textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            color = labelColor,
            modifier = Modifier
                .padding(top = 6.dp)
                .then(if (horizontalAlignment == Alignment.CenterHorizontally) Modifier.fillMaxWidth() else Modifier),
        )
    }
}

/**
 * Expressive Sessions Bento Card:
 * Fully centered, clean layout adhering strictly to Material 3 Expressive standards:
 * - Centered icon pill, hero numeral, and label
 * - Subtle cadence pill in surfaceContainerHighest
 * - Centered 7-day Session Rhythm Spark Graph
 */
@Composable
private fun SessionsInsightCard(
    totals: ReadingStats,
    buckets: List<DailyReadingStat>,
    modifier: Modifier = Modifier,
) {
    val avgSessionMs = if (totals.totalSessions > 0) totals.totalDurationMs / totals.totalSessions else 0L
    val avgPages = if (totals.totalSessions > 0) totals.totalPagesTurned / totals.totalSessions else 0

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = MoriIcons.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HeroNumber(
                value = totals.totalSessions.toString(),
                label = stringResource(R.string.stats_total_sessions),
                numberColor = MaterialTheme.colorScheme.onSurface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                horizontalAlignment = Alignment.CenterHorizontally,
            )

            if (avgSessionMs > 0L || avgPages > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = buildString {
                            if (avgSessionMs > 0L) append(formatDuration(avgSessionMs) + " / sess")
                            if (avgSessionMs > 0L && avgPages > 0) append("  •  ")
                            if (avgPages > 0) append("$avgPages pgs / sess")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered 7-day Session Rhythm Spark Graph
            SessionRhythmSparkGraph(
                buckets = buckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            )
        }
    }
}

/**
 * Interactive / Animated Session Cadence Mini-Graph.
 * Shows session rhythm distribution across recent days with:
 * - Animated vertical rhythm capsules (proportional to daily sessions)
 * - Flowing spline/bezier curve with translucent gradient underfill
 * - Glowing accent nodes for peak sessions
 * - Day initials underneath in chronological order
 */
@Composable
private fun SessionRhythmSparkGraph(
    buckets: List<DailyReadingStat>,
    modifier: Modifier = Modifier,
) {
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    // Take chronological recent days (up to 7)
    val recent = remember(buckets) {
        buckets.takeLast(7)
    }
    val maxSessions = remember(recent) {
        recent.maxOfOrNull { it.sessions }?.coerceAtLeast(1) ?: 1
    }

    val animProgress = remember { Animatable(if (expressiveMotion) 0f else 1f) }
    LaunchedEffect(buckets) {
        if (expressiveMotion) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(750, easing = FastOutSlowInEasing),
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val dayNames = remember(recent) {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("EEEEE", Locale.getDefault())
        recent.map { stat ->
            calendar.timeInMillis = stat.dayStartMillis
            format.format(calendar.time).uppercase()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val progress = animProgress.value
            if (recent.isEmpty()) return@Canvas

            val count = recent.size
            if (count > 1) {
                val dx = size.width / (count - 1)
                val points = recent.indices.map { i ->
                    val stat = recent[i]
                    val ratio = (stat.sessions.toFloat() / maxSessions.toFloat()).coerceIn(0f, 1f)
                    val y = size.height - (ratio * (size.height - 12.dp.toPx()) * progress) - 6.dp.toPx()
                    Offset(i * dx, y)
                }

                // 1. Draw subtle grid baseline
                drawLine(
                    color = outlineVariant.copy(alpha = 0.25f),
                    start = Offset(0f, size.height - 2.dp.toPx()),
                    end = Offset(size.width, size.height - 2.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )

                // 2. Smooth cubic bezier spline fill
                val fillPath = Path().apply {
                    moveTo(points[0].x, size.height)
                    lineTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(points.last().x, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f * progress),
                            primaryColor.copy(alpha = 0.02f),
                        ),
                        startY = 0f,
                        endY = size.height,
                    ),
                )

                // Spline stroke
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = primaryColor.copy(alpha = 0.70f * progress),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )

                // 3. Draw rhythm capsule bars & glowing node dots
                val barWidth = 6.dp.toPx()
                recent.indices.forEach { i ->
                    val pt = points[i]
                    val stat = recent[i]
                    if (stat.sessions > 0) {
                        val barHeight = (size.height - pt.y).coerceAtLeast(6.dp.toPx())
                        // Draw rounded bar capsule
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.5f)),
                                startY = pt.y,
                                endY = size.height,
                            ),
                            topLeft = Offset(pt.x - barWidth / 2f, pt.y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        )
                        // Glowing node cap
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx() * progress,
                            center = pt,
                        )
                        drawCircle(
                            color = surfaceContainerHigh,
                            radius = 1.5.dp.toPx() * progress,
                            center = pt,
                        )
                    } else {
                        // Inactive day baseline tick dot
                        drawCircle(
                            color = outlineVariant.copy(alpha = 0.4f),
                            radius = 2.dp.toPx(),
                            center = Offset(pt.x, size.height - 2.dp.toPx()),
                        )
                    }
                }
            }
        }

        // Row of day initials underneath
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** Expressive Bento companion stat card with dedicated container colorScheme and centered typography */
@Composable
private fun ExpressiveStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(contentColor.copy(alpha = 0.12f), shape = CircleShape),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HeroNumber(
                value = value,
                label = label,
                numberColor = contentColor,
                labelColor = contentColor.copy(alpha = 0.85f),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
        }
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
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    if (buckets.isEmpty()) return
    val maxDuration = buckets.maxOf { it.durationMs }.coerceAtLeast(1L)
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        },
    ) {
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
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .testTag(StatsTestTags.Streak),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.stats_streak_title),
                    style = MoriEmphasized.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                        ),
                ) {
                    Icon(
                        imageVector = MoriIcons.Fire,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HeroNumber(
                value = stringResource(R.string.stats_streak_current, streak.current),
                label = stringResource(R.string.stats_streak_longest, streak.longest),
                numberColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hero numeral + caption shared by stat and streak cards.
 *
 * Upright bold Google Sans Flex numeral with optical sizing and shrink-to-fit
 * to never clip long counts.
 */
@Composable
private fun HeroNumber(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    numberColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    var heroSize by remember(value) { mutableStateOf(HeroMaxSize) }
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = LocalAppFonts.current.displayFlex,
                fontWeight = FontWeight.ExtraBold,
                fontSize = heroSize,
                lineHeight = heroSize,
            ),
            textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            onTextLayout = { layout ->
                if (layout.hasVisualOverflow && heroSize > HeroMinSize) {
                    heroSize *= 0.9f
                }
            },
            color = numberColor,
            maxLines = 1,
            modifier = if (horizontalAlignment == Alignment.CenterHorizontally) Modifier.fillMaxWidth() else Modifier,
        )
        Text(
            text = label,
            style = MoriEmphasized.labelLarge,
            textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            color = labelColor,
            modifier = Modifier
                .padding(top = 4.dp)
                .then(if (horizontalAlignment == Alignment.CenterHorizontally) Modifier.fillMaxWidth() else Modifier),
        )
    }
}

private val HeroMaxSize = 56.sp
private val HeroMinSize = 28.sp

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
                style = MoriEmphasized.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
