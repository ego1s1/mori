package com.mori.feature.detail.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriComicErrorCard
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEmptyState
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.sharedCoverModifier
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat
import java.io.File

@Composable
internal fun DetailRoute(
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                DetailMessage.RescanFailed ->
                    snackbarHost.showSnackbar(context.getString(R.string.detail_snack_rescan_failed))
                DetailMessage.RemoveFailed ->
                    snackbarHost.showSnackbar(context.getString(R.string.detail_snack_remove_failed))
                is DetailMessage.ShareFile ->
                    launchShare(context, message, context.getString(R.string.detail_share_title))
            }
        }
    }
    DetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onReadClick = onReadClick,
        snackbarHost = snackbarHost,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailScreen(
    uiState: DetailUiState,
    onAction: (DetailAction) -> Unit,
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            val readyComic = (uiState as? DetailUiState.Ready)?.comic
            if (readyComic != null) {
                MediumTopAppBar(
                    title = {
                        Text(
                            text = readyComic.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = MoriIcons.Back, contentDescription = stringResource(R.string.detail_back))
                        }
                    },
                    actions = {
                        DetailTopActions(
                            bookmarked = readyComic.bookmarked,
                            onAction = onAction,
                            modifier = Modifier,
                        )
                    },
                )
            } else {
                MediumTopAppBar(
                    title = { Text(stringResource(R.string.detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = MoriIcons.Back, contentDescription = stringResource(R.string.detail_back))
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                DetailUiState.Loading -> MoriLoading()

                DetailUiState.Missing -> AnimatedVisibility(
                    visible = true,
                    enter = MoriMotion.enter(MoriEnterKind.FADE),
                    exit = MoriMotion.exit(MoriEnterKind.FADE),
                ) {
                    MoriEmptyState(
                        icon = MoriIcons.MenuBook,
                        title = stringResource(R.string.detail_removed_title),
                        body = stringResource(R.string.detail_removed_body),
                        actionLabel = stringResource(R.string.detail_back_to_library),
                        onAction = onBackClick,
                    )
                }

                is DetailUiState.Ready -> {
                    // One-shot arrival fade; static visibility never replays it.
                    AnimatedVisibility(
                        visible = true,
                        enter = MoriMotion.enter(MoriEnterKind.FADE_THROUGH),
                        exit = MoriMotion.exit(MoriEnterKind.FADE_THROUGH),
                    ) {
                    DetailContent(
                        comic = uiState.comic,
                        refreshing = uiState.refreshing,
                        onAction = onAction,
                        onReadClick = onReadClick,
                    )
                    }
                    if (uiState.confirmRemove) {
                        RemoveDialog(
                            title = uiState.comic.title,
                            onConfirm = { onAction(DetailAction.ConfirmRemove) },
                            onDismiss = { onAction(DetailAction.CancelRemove) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopActions(
    bookmarked: Boolean,
    onAction: (DetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        IconButton(
            onClick = { onAction(DetailAction.ToggleBookmark) },
            modifier = Modifier.testTag(DetailTestTags.BookmarkButton),
        ) {
            Icon(
                imageVector = if (bookmarked) MoriIcons.Bookmark else MoriIcons.BookmarkBorder,
                contentDescription = stringResource(
                    if (bookmarked) {
                        R.string.detail_action_unbookmark
                    } else {
                        R.string.detail_action_bookmark
                    },
                ),
            )
        }
        IconButton(
            onClick = { onAction(DetailAction.Share) },
            modifier = Modifier.testTag(DetailTestTags.ShareButton),
        ) {
            Icon(
                imageVector = MoriIcons.Share,
                contentDescription = stringResource(R.string.detail_action_share),
            )
        }
        IconButton(
            onClick = { onAction(DetailAction.Refresh) },
            modifier = Modifier.testTag(DetailTestTags.RefreshButton),
        ) {
            Icon(imageVector = MoriIcons.Refresh, contentDescription = stringResource(R.string.detail_action_rescan))
        }
        IconButton(
            onClick = { onAction(DetailAction.AskRemove) },
            modifier = Modifier.testTag(DetailTestTags.RemoveButton),
        ) {
            Icon(imageVector = MoriIcons.Delete, contentDescription = stringResource(R.string.detail_action_remove_comic))
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun DetailContent(
    comic: Comic,
    refreshing: Boolean,
    onAction: (DetailAction) -> Unit,
    onReadClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Centered readable column on expanded windows; phones stay full-bleed.
    androidx.compose.foundation.layout.BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        val contentWidth = minOf(maxWidth, EXPANDED_CONTENT_MAX_WIDTH)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .width(contentWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DetailTestTags.Hero),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .then(sharedCoverModifier(comic.id)),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = comic.title,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = comic.title,
                    style = MoriEmphasized.headlineSmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comic.series != null) {
                    Text(
                        text = comic.series.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.detail_meta_pages, comic.pageCount, comic.format.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (comic.isInProgress || comic.isFinished) {
                    LinearProgressIndicator(
                        progress = { comic.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.detail_meta_position,
                            comic.lastPageIndex + 1,
                            comic.pageCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val error = comic.error
        if (error != null) {
            ErrorCard(
                error = error,
                refreshing = refreshing,
                onRetry = { onAction(DetailAction.Refresh) },
                onRemove = { onAction(DetailAction.AskRemove) },
            )
        } else {
            val startPage = if (comic.isInProgress) comic.lastPageIndex else 0
            Button(
                onClick = { onReadClick(comic.id, startPage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DetailTestTags.ReadButton),
            ) {
                Text(
                    if (comic.lastPageIndex > 0) {
                        stringResource(R.string.detail_read_resume, startPage + 1)
                    } else {
                        stringResource(R.string.detail_read_start)
                    },
                )
            }
        }

        MetadataRows(comic = comic)

        if (comic.error == null && comic.pageCount > 0) {
            Text(text = stringResource(R.string.detail_section_pages), style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(DetailTestTags.PageStrip),
            ) {
                items(comic.pageCount, key = { it }) { index ->
                    val chipDescription = if (index == comic.lastPageIndex) {
                        stringResource(R.string.detail_page_chip_current, index + 1)
                    } else {
                        stringResource(R.string.detail_page_chip, index + 1)
                    }
                    FilterChip(
                        selected = index == comic.lastPageIndex,
                        onClick = { onReadClick(comic.id, index) },
                        label = { Text("${index + 1}") },
                        modifier = Modifier
                            .testTag(DetailTestTags.pageChip(index))
                            .semantics { contentDescription = chipDescription },
                    )
                }
            }
        }
    }
}
    }

private val EXPANDED_CONTENT_MAX_WIDTH = 840.dp

@Composable
private fun MetadataRows(comic: Comic, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        comic.number?.let { MetadataRow(label = stringResource(R.string.detail_meta_number), value = it) }
        MetadataRow(label = stringResource(R.string.detail_meta_format), value = comic.format.name)
        MetadataRow(label = stringResource(R.string.detail_meta_file), value = comic.sourceDisplayName)
    }
}

@Composable
private fun MetadataRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorCard(
    error: ComicError,
    refreshing: Boolean,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriComicErrorCard(
        error = error,
        primaryLabel = stringResource(R.string.detail_error_retry),
        onPrimary = onRetry,
        secondaryLabel = stringResource(R.string.detail_error_remove),
        onSecondary = onRemove,
        loading = refreshing,
        modifier = modifier.testTag(DetailTestTags.ErrorCard),
    )
}

@Composable
private fun RemoveDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_remove_title)) },
        text = { Text(stringResource(R.string.detail_remove_body, title)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(DetailTestTags.ConfirmRemove),
            ) {
                Text(stringResource(R.string.detail_remove_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_remove_cancel))
            }
        },
        modifier = modifier.testTag(DetailTestTags.RemoveDialog),
    )
}

@ThemePreviews
@Composable
private fun DetailScreenPreview() {
    MoriTheme {
        DetailScreen(
            uiState = DetailUiState.Ready(
                comic = Comic(
                    id = "1",
                    title = "Batman: Court of Owls",
                    series = "Batman",
                    number = "1",
                    format = ComicFormat.CBZ,
                    pageCount = 173,
                    sourcePath = "/lib/1.cbz",
                    coverPath = null,
                    lastPageIndex = 12,
                    sourceDisplayName = "batman.cbz",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
                refreshing = false,
                confirmRemove = false,
                removed = false,
            ),
            onAction = {},
            onBackClick = {},
            onReadClick = { _, _ -> },
        )
    }
}
