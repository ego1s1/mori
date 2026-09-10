package com.mori.feature.library.impl

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.sharedCoverModifier
import com.mori.core.model.Comic
import java.io.File

/**
 * Compact overlay-title grid cell in the Mihon tradition: full-bleed 2:3 cover, bottom
 * gradient scrim with the title, progress bar for started comics, error pill for failed
 * rows, and a continue affordance while in progress.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun ComicCard(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = false,
) {
    // Wrappers remembered on the full comic: grid items skip recomposition
    // when handlers and content are unchanged, and progress updates refresh
    // the captured comic (keyed by equality, not id).
    val click = remember(comic, onRead) { { onRead(comic) } }
    val longClick = remember(comic, onDetails) { { onDetails(comic) } }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(LibraryTestTags.cardFor(comic.id))
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        // Cover morphs into the detail hero on launch (shared element), but
        // only the launching card registers — tracking every card costs a
        // shared-transition overlay per scroll frame.
        Box(
            modifier = Modifier
                .aspectRatio(COVER_ASPECT)
                .then(if (sharedCover) sharedCoverModifier(comic.id) else Modifier),
        ) {
            MoriCoverArt(
                coverPath = comic.coverPath,
                contentDescription = comic.title,
            )

            if (comic.error != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.library_card_unreadable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Pages-left corner badge while in progress (reference-library style):
            // glanceable remaining count next to the progress bar.
            if (comic.isInProgress) {
                val left = comic.pageCount - comic.lastPageIndex - 1
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.library_card_pages_left, left),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                // Hoisted out of the scroll frame: one brush per card composition,
                // not one allocation per redraw.
                val scrim = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f),
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scrim)
                        .padding(horizontal = 8.dp)
                        .padding(top = 18.dp, bottom = 6.dp),
                ) {
                    Text(
                        text = comic.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        minLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (comic.isInProgress || comic.isFinished) {
                    LinearProgressIndicator(
                        progress = { comic.progress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }

            if (comic.isInProgress) {
                FilledIconButton(
                    onClick = click,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 28.dp)
                        .size(36.dp),
                ) {
                    Icon(
                        imageVector = MoriIcons.PlayArrow,
                        contentDescription = stringResource(R.string.library_card_continue, comic.title),
                    )
                }
            }
        }
    }
}

private const val COVER_ASPECT = 2f / 3f
