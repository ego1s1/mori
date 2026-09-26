package com.mori.feature.reader.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.mori.core.designsystem.MoriLoadingIndicator
import com.mori.core.designsystem.MoriScrimPill
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.mori.core.data.ComicPageKey
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriSheet

/**
 * Modal sheet hosting [ReaderOverviewSheetContent].
 *
 * Sheet-body content is split out so unit tests can render it directly (the modal
 * presentation does not settle under Robolectric legacy graphics).
 */
@Composable
internal fun ReaderOverviewSheet(
    comicId: String,
    currentPage: Int,
    expandedCount: Int,
    currentArchiveIndex: Int,
    archivePageCount: Int,
    expandedForArchive: List<Int>,
    cropMargins: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MoriSheet(
        onDismissRequest = { onAction(ReaderAction.CloseOverview) },
        modifier = modifier.testTag(ReaderTestTags.OverviewSheet),
    ) {
        ReaderOverviewSheetContent(
            comicId = comicId,
            currentPage = currentPage,
            expandedCount = expandedCount,
            currentArchiveIndex = currentArchiveIndex,
            archivePageCount = archivePageCount,
            expandedForArchive = expandedForArchive,
            cropMargins = cropMargins,
            onAction = onAction,
        )
    }
}

/** Sheet body content, exposed for testing. */
@Composable
internal fun ReaderOverviewSheetContent(
    comicId: String,
    currentPage: Int,
    expandedCount: Int,
    currentArchiveIndex: Int,
    archivePageCount: Int,
    expandedForArchive: List<Int>,
    cropMargins: Boolean,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.reader_overview_title, currentPage, expandedCount),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(OVERVIEW_CELL_MIN),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ReaderTestTags.OverviewGrid),
    ) {
        // The grid walks archive pages (stable identities with full-page
        // thumbs); tapping one seeks to its first expanded position, so a
        // split wide page opens on its first half.
        items(
            archivePageCount,
            key = { archive -> "$comicId:$archive:$cropMargins" },
            contentType = { "thumb" },
        ) { archive ->
            OverviewThumb(
                comicId = comicId,
                pageIndex = archive,
                pageNumber = archive + 1,
                cropMargins = cropMargins,
                selected = archive == currentArchiveIndex,
                onClick = {
                    onAction(ReaderAction.SeekPage(expandedForArchive.getOrElse(archive) { archive }))
                    onAction(ReaderAction.CloseOverview)
                },
            )
        }
    }
}

@Composable
private fun OverviewThumb(
    comicId: String,
    pageIndex: Int,
    pageNumber: Int,
    cropMargins: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(comicId, pageIndex, cropMargins) { mutableIntStateOf(0) }
    key(attempt) {
        val painter = rememberAsyncImagePainter(
            model = ComicPageKey(comicId, pageIndex, OVERVIEW_MAX_DIMENSION, cropMargins),
            contentScale = ContentScale.Crop,
        )
        val painterState by painter.state.collectAsStateWithLifecycle()
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (selected) {
                BorderStroke(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                )
            } else {
                null
            },
            modifier = modifier
                .testTag(ReaderTestTags.thumbFor(pageIndex))
                .clip(MaterialTheme.shapes.large)
                .clickable(
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.reader_overview_open, pageNumber),
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.aspectRatio(PAGE_ASPECT),
            ) {
                // Crop fills the fixed cell: Fit would pillarbox cropped or
                // wide thumbs inside identical boxes.
                Image(
                    painter = painter,
                    contentDescription = stringResource(R.string.reader_page_art, pageNumber),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                when (painterState) {
                    is AsyncImagePainter.State.Loading -> MoriLoadingIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                    // Docked to the bottom so center taps still seek: the
                    // whole thumb (including failed ones, which retry on tap)
                    // stays a single seek target with an explicit retry.
                    is AsyncImagePainter.State.Error -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = MoriIcons.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        TextButton(onClick = { attempt++ }) {
                            Text(stringResource(R.string.reader_page_retry))
                        }
                    }
                    else -> Unit
                }
                // Page number survives cropping: scrim pill anchored top-start,
                // clear of the bottom-docked retry.
                MoriScrimPill(
                    text = pageNumber.toString(),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
        }
    }
}

/** Longest-side bound for overview thumbnails (small, many on screen). */
private const val OVERVIEW_MAX_DIMENSION = 256

private val OVERVIEW_CELL_MIN = 96.dp

private const val PAGE_ASPECT = 2f / 3f
