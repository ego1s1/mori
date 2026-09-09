package com.mori.feature.library.impl

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic

@Composable
internal fun LibraryRoute(
    onComicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onComicClick = onComicClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onComicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHost = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            LibraryTopBar(
                comicCount = (uiState as? LibraryUiState.Success)?.comics?.size,
                queryText = (uiState as? LibraryUiState.Success)?.query?.text.orEmpty(),
                onAction = onAction,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.testTag(LibraryTestTags.Snackbar),
            )
        },
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                LibraryUiState.Loading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }

                is LibraryUiState.Success -> {
                    LaunchedEffect(uiState.snackbar) {
                        val message = uiState.snackbar ?: return@LaunchedEffect
                        snackbarHost.showSnackbar(message)
                        onAction(LibraryAction.DismissSnackbar)
                    }
                    LibraryContent(
                        state = uiState,
                        onAction = onAction,
                        onComicClick = onComicClick,
                    )
                    if (uiState.filterOpen) {
                        LibrarySortFilterSheet(
                            query = uiState.query,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    comicCount: Int?,
    queryText: String,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchOpen by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                RowTitle(text = "Library", count = comicCount)
            },
            actions = {
                IconButton(
                    onClick = { searchOpen = !searchOpen },
                    modifier = Modifier.testTag(LibraryTestTags.SearchToggle),
                ) {
                    Icon(
                        imageVector = MoriIcons.Search,
                        contentDescription = "Search library",
                    )
                }
                IconButton(
                    onClick = { onAction(LibraryAction.OpenFilter) },
                    modifier = Modifier.testTag(LibraryTestTags.FilterButton),
                ) {
                    Icon(
                        imageVector = MoriIcons.Tune,
                        contentDescription = "Sort and filter",
                    )
                }
                IconButton(
                    onClick = { onAction(LibraryAction.Refresh) },
                    modifier = Modifier.testTag(LibraryTestTags.RefreshButton),
                ) {
                    Icon(
                        imageVector = MoriIcons.Refresh,
                        contentDescription = "Rescan library",
                    )
                }
            },
        )
        if (searchOpen) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { onAction(LibraryAction.SearchTextChanged(it)) },
                label = { Text("Search title, series, number") },
                leadingIcon = {
                    Icon(imageVector = MoriIcons.Search, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchOpen = false }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .testTag(LibraryTestTags.SearchField),
            )
        }
    }
}

@Composable
private fun RowTitle(text: String, count: Int?, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineSmall)
        if (count != null) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LibraryContent(
    state: LibraryUiState.Success,
    onAction: (LibraryAction) -> Unit,
    onComicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { onAction(LibraryAction.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.isEmpty) {
            LibraryEmptyState(
                searching = state.query.text.isNotBlank(),
                onRefresh = { onAction(LibraryAction.Refresh) },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(GRID_CELL_MIN),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(LibraryTestTags.Grid),
            ) {
                items(state.comics, key = { it.id }) { comic ->
                    ComicCard(
                        comic = comic,
                        onClick = { onComicClick(comic.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    searching: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(LibraryTestTags.EmptyState),
    ) {
        Icon(
            imageVector = MoriIcons.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = if (searching) "No comics match your search" else "Your library is empty",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = if (searching) {
                "Try a different title, series, or number."
            } else {
                "Import comics to see them here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Rescan library")
        }
    }
}

@ThemePreviews
@Composable
private fun LibraryScreenPreview() {
    MoriTheme {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                comics = listOf(
                    previewComic("1", "Batman: Court of Owls", 2, 10),
                    previewComic("2", "Saga", 0, 0),
                ),
                query = com.mori.core.model.LibraryQuery(),
                refreshing = false,
                filterOpen = false,
                snackbar = null,
            ),
            onAction = {},
            onComicClick = {},
        )
    }
}

private fun previewComic(id: String, title: String, lastPage: Int, pages: Int) = Comic(
    id = id,
    title = title,
    series = null,
    number = null,
    format = com.mori.core.model.ComicFormat.CBZ,
    pageCount = pages,
    sourcePath = "/lib/$id.cbz",
    coverPath = null,
    lastPageIndex = lastPage,
    sourceDisplayName = "$id.cbz",
    createdAt = 1L,
    updatedAt = 1L,
)

private val GRID_CELL_MIN = 128.dp
