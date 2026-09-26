package com.mori.feature.history.impl

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.FloatingChromeBottomReserve
import com.mori.core.designsystem.MoriContentWell
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            if (uiState is HistoryUiState.Success) {
                HistoryTopBar(
                    searchOpen = uiState.searchOpen,
                    onSearchClick = { onAction(HistoryAction.ToggleSearch) },
                )
            }
        },
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                HistoryUiState.Loading -> AnimatedVisibility(
                    visible = true,
                    enter = MoriMotion.enter(MoriEnterKind.FADE),
                    exit = MoriMotion.exit(MoriEnterKind.FADE),
                ) {
                    MoriLoading(
                        modifier = Modifier.testTag(HistoryTestTags.Loading),
                    )
                }
                is HistoryUiState.Success -> HistoryContent(
                    uiState = uiState,
                    onAction = onAction,
                    onReadClick = onReadClick,
                    onComicLongClick = onComicLongClick,
                )
            }
        }
    }
}

/**
 * Static compact app bar matching the library: emphasized title, search as
 * a direct icon action. The search field below collapses with the same
 * expressive motion as the library's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    searchOpen: Boolean,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.history_title),
                // Same emphasized screen-title role as Settings and
                // Library so sibling tabs match.
                style = MoriEmphasized.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag(HistoryTestTags.SearchToggle),
            ) {
                Icon(
                    imageVector = if (searchOpen) {
                        MoriIcons.Close
                    } else {
                        MoriIcons.Search
                    },
                    contentDescription = stringResource(R.string.history_action_search),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState.Success,
    onAction: (HistoryAction) -> Unit,
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Focus + keyboard follow the toggle both ways, like the library:
    // opening focuses and lifts the keyboard, closing releases both.
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Centered well on expanded windows; phones stay full-bleed.
    MoriContentWell(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Inside the well's subcomposition so the field is attached before
        // the effect below requests focus on it.
        LaunchedEffect(uiState.searchOpen) {
            if (uiState.searchOpen) {
                searchFocus.requestFocus()
                keyboard?.show()
            } else {
                keyboard?.hide()
                focusManager.clearFocus()
            }
        }
        AnimatedVisibility(
            visible = uiState.searchOpen,
            enter = MoriMotion.enter(MoriEnterKind.SEARCH),
            exit = MoriMotion.exit(MoriEnterKind.SEARCH),
        ) {
            HistorySearchField(
                text = uiState.queryText,
                focusRequester = searchFocus,
                onTextChange = { onAction(HistoryAction.SearchTextChanged(it)) },
                onSearch = { onAction(HistoryAction.ToggleSearch) },
            )
        }
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
                // No-results is recoverable in place; true empty has no
                // action (history fills itself as books are opened).
                actionLabel = if (uiState.isNoResults) {
                    stringResource(R.string.history_clear_search)
                } else {
                    null
                },
                onAction = if (uiState.isNoResults) {
                    { onAction(HistoryAction.ClearSearch) }
                } else {
                    null
                },
                modifier = Modifier.testTag(HistoryTestTags.EmptyState),
                bottomPadding = FloatingChromeBottomReserve,
                actionTestTag = if (uiState.isNoResults) {
                    HistoryTestTags.EmptyClearSearch
                } else {
                    null
                },
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
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text(stringResource(R.string.history_search_label)) },
        leadingIcon = {
            Icon(imageVector = MoriIcons.Search, contentDescription = null)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 8.dp)
            .focusRequester(focusRequester)
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
    // Hoisted so rows don't recreate lambdas on every re-emit; mirrors the
    // error→details else resume logic used inline before.
    val onRowRead = remember(onReadClick, onComicLongClick) {
        { comic: Comic ->
            // Errored rows can't open the reader; tap goes to
            // details where retry/remove live.
            if (comic.error != null) {
                onComicLongClick(comic.id)
            } else {
                onReadClick(comic.id, comic.resumeIndex)
            }
        }
    }
    val onRowDetails = remember(onComicLongClick) {
        { comic: Comic -> onComicLongClick(comic.id) }
    }
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
                    key = { "${day.dayStartMillis}-${it.id}" },
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
                        onRead = onRowRead,
                        onDetails = onRowDetails,
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
        // Container (not page surface) so the stuck header stays visible
        // while rows scroll beneath it.
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { heading() },
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
        modifier = modifier
            .testTag(HistoryTestTags.rowFor(comic.id))
            .sizeIn(minHeight = 48.dp)
            .combinedClickable(
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
                    modifier = Modifier.semantics { heading() },
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
                if (comic.error == null) {
                    MoriProgressBar(
                        progress = { comic.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(4.dp),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector = MoriIcons.BrokenImage,
                            contentDescription = stringResource(R.string.history_row_details),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.history_row_details),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
