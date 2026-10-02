package com.mori.feature.library.impl

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mori.core.designsystem.MoriCoverArt
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriProgressBar
import com.mori.core.designsystem.MoriScrimPill
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.designsystem.sharedCoverModifier
import com.mori.core.model.Comic

/**
 * Compact grid cell: full-bleed 2:3 cover with the
 * title set below it in normal flow — no scrim gradient, no text blended
 * over the image — plus a progress bar for started comics and an error pill
 * for failed rows.
 *
 * Tapping the card continues at the saved page, so there is no separate
 * continue button; the pages-left badge marks in-progress books.
 * Long-press opens the quick-actions menu.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun ComicCard(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = false,
    cardTag: String = LibraryTestTags.cardFor(comic.id),
) {
    // Wrappers keyed by click-relevant fields (identity, saved page, error)
    // — not full-comic equality: cover/metadata re-emissions (updatedAt
    // bumps on any write) no longer invalidate the handler, while a real
    // progress save still refreshes the captured resume index.
    val click = remember(comic.id, comic.lastPageIndex, comic.error, onRead) { { onRead(comic) } }
    val haptics = rememberMoriHaptics()
    val longClick = remember(comic.id, onDetails, haptics) {
        {
            haptics(MoriHaptic.LongPress)
            onDetails(comic)
        }
    }
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Column {
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
                    contentDescription = null,
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

                // Pages-left corner badge while in progress (reference-library
                // style): glanceable remaining count next to the progress bar.
                // Otherwise a bookmark badge marks favorites in the same slot.
                if (comic.isInProgress) {
                    MoriScrimPill(
                        text = stringResource(R.string.library_card_pages_left, comic.pagesLeft),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                } else if (comic.bookmarked) {
                    // Same scrim language as the pages-left pill: tonal
                    // containers wash out on bright covers, black does not.
                    MoriScrimPill(
                        text = "",
                        icon = MoriIcons.Bookmark,
                        shape = CircleShape,
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .semantics {
                                stateDescription = bookmarkedLabel
                            }
                            .testTag(LibraryTestTags.bookmarkBadgeFor(comic.id)),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comic.isInProgress || comic.isFinished) {
                    MoriProgressBar(
                        progress = { comic.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

private const val COVER_ASPECT = 2f / 3f


/**
 * Comfortable grid cell: full-bleed cover with additional metadata
 * (series, issue number, or page counts) below the title.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun ComfortableComicCard(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = false,
    cardTag: String = LibraryTestTags.cardFor(comic.id),
) {
    val click = remember(comic.id, comic.lastPageIndex, comic.error, onRead) { { onRead(comic) } }
    val haptics = rememberMoriHaptics()
    val longClick = remember(comic.id, onDetails, haptics) {
        {
            haptics(MoriHaptic.LongPress)
            onDetails(comic)
        }
    }
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)
    val subtitle = listOfNotNull(
        comic.series?.takeIf { it.isNotBlank() },
        comic.number?.takeIf { it.isNotBlank() }?.let { "#$it" },
    ).joinToString(" ").ifBlank {
        if (comic.pageCount > 0) "${comic.pageCount} pages" else null
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .aspectRatio(COVER_ASPECT)
                    .then(if (sharedCover) sharedCoverModifier(comic.id) else Modifier),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = null,
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

                if (comic.isInProgress) {
                    MoriScrimPill(
                        text = stringResource(R.string.library_card_pages_left, comic.pagesLeft),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                } else if (comic.bookmarked) {
                    MoriScrimPill(
                        text = "",
                        icon = MoriIcons.Bookmark,
                        shape = CircleShape,
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .semantics {
                                stateDescription = bookmarkedLabel
                            }
                            .testTag(LibraryTestTags.bookmarkBadgeFor(comic.id)),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (comic.isInProgress || comic.isFinished) {
                    MoriProgressBar(
                        progress = { comic.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * Cover-only grid cell: edge-to-edge artwork with scrim title overlay and badges.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun CoverOnlyComicCard(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = false,
    cardTag: String = LibraryTestTags.cardFor(comic.id),
) {
    val click = remember(comic.id, comic.lastPageIndex, comic.error, onRead) { { onRead(comic) } }
    val haptics = rememberMoriHaptics()
    val longClick = remember(comic.id, onDetails, haptics) {
        {
            haptics(MoriHaptic.LongPress)
            onDetails(comic)
        }
    }
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(COVER_ASPECT)
                .then(if (sharedCover) sharedCoverModifier(comic.id) else Modifier),
        ) {
            MoriCoverArt(
                coverPath = comic.coverPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            // Scrim title gradient at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        ),
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Column {
                    Text(
                        text = comic.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (comic.isInProgress || comic.isFinished) {
                        MoriProgressBar(
                            progress = { comic.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }

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

            if (comic.isInProgress) {
                MoriScrimPill(
                    text = stringResource(R.string.library_card_pages_left, comic.pagesLeft),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            } else if (comic.bookmarked) {
                MoriScrimPill(
                    text = "",
                    icon = MoriIcons.Bookmark,
                    shape = CircleShape,
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .semantics {
                            stateDescription = bookmarkedLabel
                        }
                        .testTag(LibraryTestTags.bookmarkBadgeFor(comic.id)),
                )
            }
        }
    }
}

/**
 * List row cell: horizontal row with compact cover thumbnail, title,
 * metadata subtitle, and badges.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ComicListRow(
    comic: Comic,
    onRead: (Comic) -> Unit,
    onDetails: (Comic) -> Unit,
    modifier: Modifier = Modifier,
    cardTag: String = LibraryTestTags.cardFor(comic.id),
) {
    val click = remember(comic.id, comic.lastPageIndex, comic.error, onRead) { { onRead(comic) } }
    val haptics = rememberMoriHaptics()
    val longClick = remember(comic.id, onDetails, haptics) {
        {
            haptics(MoriHaptic.LongPress)
            onDetails(comic)
        }
    }
    val subtitle = listOfNotNull(
        comic.series?.takeIf { it.isNotBlank() },
        comic.number?.takeIf { it.isNotBlank() }?.let { "#$it" },
    ).joinToString(" ").ifBlank {
        if (comic.pageCount > 0) "${comic.pageCount} pages" else null
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, comic.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(COVER_ASPECT)
                    .clip(MaterialTheme.shapes.small),
            ) {
                MoriCoverArt(
                    coverPath = comic.coverPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (comic.isInProgress || comic.isFinished) {
                    MoriProgressBar(
                        progress = { comic.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (comic.error != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = stringResource(R.string.library_card_unreadable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else if (comic.isInProgress) {
                MoriScrimPill(
                    text = stringResource(R.string.library_card_pages_left, comic.pagesLeft),
                )
            } else if (comic.bookmarked) {
                Icon(
                    imageVector = MoriIcons.Bookmark,
                    contentDescription = stringResource(R.string.library_card_bookmarked),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
