package com.mori.feature.detail.impl

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriTheme
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
    if (uiState is DetailUiState.Missing) {
        LaunchedEffect(Unit) { onBackClick() }
    }
    DetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onReadClick = onReadClick,
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
) {
    val snackbarHost = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = MoriIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is DetailUiState.Ready) {
                        IconButton(
                            onClick = { onAction(DetailAction.Refresh) },
                            modifier = Modifier.testTag(DetailTestTags.RefreshButton),
                        ) {
                            Icon(imageVector = MoriIcons.Refresh, contentDescription = "Re-index")
                        }
                        IconButton(
                            onClick = { onAction(DetailAction.AskRemove) },
                            modifier = Modifier.testTag(DetailTestTags.RemoveButton),
                        ) {
                            Icon(imageVector = MoriIcons.Delete, contentDescription = "Remove comic")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        modifier = modifier,
    ) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (uiState) {
                DetailUiState.Loading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }

                DetailUiState.Missing -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                ) {
                    Text(
                        text = "This comic is no longer in your library.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is DetailUiState.Ready -> {
                    LaunchedEffect(uiState.snackbar) {
                        val message = uiState.snackbar ?: return@LaunchedEffect
                        snackbarHost.showSnackbar(message)
                        onAction(DetailAction.DismissSnackbar)
                    }
                    DetailContent(
                        comic = uiState.comic,
                        refreshing = uiState.refreshing,
                        onAction = onAction,
                        onReadClick = onReadClick,
                    )
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
                    .aspectRatio(2f / 3f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = MoriIcons.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp),
                    )
                    if (comic.coverPath != null) {
                        AsyncImage(
                            model = File(comic.coverPath),
                            contentDescription = comic.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.headlineSmall,
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
                    text = "${comic.pageCount} pages • ${comic.format.name}",
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
                        text = "Page ${comic.lastPageIndex + 1} of ${comic.pageCount}",
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
            val label = if (comic.lastPageIndex > 0) "Resume" else "Start reading"
            Button(
                onClick = { onReadClick(comic.id, startPage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DetailTestTags.ReadButton),
            ) {
                Text("$label • Page ${startPage + 1}")
            }
        }

        MetadataRows(comic = comic)

        if (comic.error == null && comic.pageCount > 0) {
            Text(text = "Pages", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(DetailTestTags.PageStrip),
            ) {
                items(comic.pageCount, key = { it }) { index ->
                    FilterChip(
                        selected = index == comic.lastPageIndex,
                        onClick = { onReadClick(comic.id, index) },
                        label = { Text("${index + 1}") },
                        modifier = Modifier.testTag(DetailTestTags.pageChip(index)),
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
        comic.number?.let { MetadataRow(label = "Number", value = it) }
        MetadataRow(label = "Format", value = comic.format.name)
        MetadataRow(label = "File", value = comic.sourceDisplayName)
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
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailTestTags.ErrorCard),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = when (error) {
                    ComicError.CORRUPT -> "This file could not be read. It may be damaged."
                    ComicError.PASSWORD_REQUIRED -> "This archive is password protected."
                    ComicError.EMPTY -> "This archive contains no readable pages."
                    ComicError.UNSUPPORTED -> "This format is not supported."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    OutlinedButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
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
        title = { Text("Remove comic?") },
        text = { Text("\"$title\" and its downloaded pages will be deleted from your library.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(DetailTestTags.ConfirmRemove),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                snackbar = null,
            ),
            onAction = {},
            onBackClick = {},
            onReadClick = { _, _ -> },
        )
    }
}
