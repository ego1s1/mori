package com.mori.feature.history.impl

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.MoriContentWell
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriProgressBar
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.model.HistoryDay
import com.mori.core.model.dayStartMillis
import com.mori.core.model.previousDayStartMillis
import java.text.DateFormat
import java.util.Date

/**
 * Public tab content for the main viewport. Route and tab share one
 * internal content; the ViewModel type never appears in a public signature.
 */
@Composable
fun HistoryTabContent(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryRouteContent(
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        modifier = modifier,
        viewModel = hiltViewModel(),
    )
}

@Composable
internal fun HistoryRouteContent(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = onReadClick,
        onComicLongClick = onComicLongClick,
        modifier = modifier,
    )
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        HistoryUiState.Loading -> MoriLoading(
            modifier = modifier.testTag(HistoryTestTags.Loading),
        )
        is HistoryUiState.Success -> HistoryContent(
            uiState = uiState,
            onAction = onAction,
            onReadClick = onReadClick,
            onComicLongClick = onComicLongClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState.Success,
    onAction: (HistoryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Centered well on expanded windows; phones stay full-bleed.
    MoriContentWell(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.history_title),
            // Same bar-title role as Library so sibling tabs match; this
            // header stands in for an app bar on this screen.
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HistorySearchField(
            text = uiState.queryText,
            onTextChange = { onAction(HistoryAction.SearchTextChanged(it)) },
        )
        if (uiState.isEmpty) {
            MoriEmptyState(
                icon = MoriIcons.History,
                title = if (uiState.isNoResults) {
                    stringResource(R.string.history_no_results_title)
                } else {
                    stringResource(R.string.history_empty_title)
                },
                body = if (uiState.isNoResults) {
                    stringResource(R.string.history_no_results_body, uiState.queryText.trim())
                } else {
                    stringResource(R.string.history_empty_body)
                },
                actionLabel = null,
                onAction = null,
                modifier = Modifier.testTag(HistoryTestTags.EmptyState),
                bottomPadding = FloatingChromeBottomReserve,
            )
        } else {
            HistoryDays(
                days = uiState.days,
                onReadClick = onReadClick,
                onComicLongClick = onComicLongClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
    }
}

@Composable
private fun HistorySearchField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text(stringResource(R.string.history_search_label)) },
        leadingIcon = {
            Icon(imageVector = MoriIcons.Search, contentDescription = null)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .focusRequester(searchFocus)
            .focusable()
            .testTag(HistoryTestTags.SearchField),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryDays(
    days: List<HistoryDay>,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = FloatingChromeBottomReserve),
        modifier = modifier
            .fillMaxSize()
            .testTag(HistoryTestTags.List),
    ) {
            days.forEach { day ->
                stickyHeader(key = "day-${day.dayStartMillis}", contentType = "dayHeader") {
                    HistoryDayHeader(
                        dayStartMillis = day.dayStartMillis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(
                    day.comics,
                    key = { it.id },
                    // Same bitmask bucket as the library grid: variants never
                    // cross-recycle when history re-emits.
                    contentType = { comic ->
                        (if (comic.error != null) 4 else 0) +
                            (if (comic.isInProgress) 2 else 0) +
                            (if (comic.isFinished) 1 else 0)
                    },
                ) { comic ->
                    HistoryRow(
                        comic = comic,
                        onRead = {
                            // Errored rows can't open the reader; tap goes to
                            // details where retry/remove live.
                            if (it.error != null) {
                                onComicLongClick(it.id)
                            } else {
                                onReadClick(it.id, it.resumeIndex)
                            }
                        },
                        onDetails = { onComicLongClick(it.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }
}

@Composable
private fun HistoryDayHeader(dayStartMillis: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val label = remember(dayStartMillis) {
        val today = dayStartMillis(System.currentTimeMillis())
        when (dayStartMillis) {
            today -> context.getString(R.string.history_day_today)
            previousDayStartMillis(today) -> context.getString(R.string.history_day_yesterday)
            else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(dayStartMillis))
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: ((Comic) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.testTag(HistoryTestTags.rowFor(comic.id)).combinedClickable(
            onClick = { onRead(comic) },
            onClickLabel = stringResource(R.string.history_row_read, comic.title),
            onLongClick = onDetails?.let { action -> { action(comic) } },
            onLongClickLabel = onDetails?.let { stringResource(R.string.history_row_details) },
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = null,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(comic.series, comic.number).joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.history_row_position,
                        comic.lastPageIndex + 1,
                        comic.pageCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MoriProgressBar(
                    progress = { comic.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(3.dp),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun HistoryScreenPreview() {
    MoriTheme {
        HistoryScreen(
            uiState = HistoryUiState.Success(
                days = listOf(
                    HistoryDay(
                        dayStartMillis = dayStartMillis(System.currentTimeMillis()),
                        comics = listOf(
                            previewComic("1", "Batman: Court of Owls", 4),
                            previewComic("2", "Saga", 1),
                        ),
                    ),
                ),
                queryText = "",
            ),
            onAction = {},
            onReadClick = { _, _ -> },
            onComicLongClick = {},
        )
    }
}

private fun previewComic(id: String, title: String, lastPage: Int) = Comic(
    id = id,
    title = title,
    series = null,
    number = null,
    format = ComicFormat.CBZ,
    pageCount = 10,
    sourcePath = "/lib/$id.cbz",
    coverPath = null,
    lastPageIndex = lastPage,
    sourceDisplayName = "$id.cbz",
    createdAt = 1L,
    updatedAt = 1L,
)
