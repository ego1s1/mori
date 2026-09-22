package com.mori.feature.reader.impl

import com.mori.core.model.PageHalf
import com.mori.core.model.ReadingDirection

/**
 * One pager position in the reader.
 *
 * With the dual-page split off this is the identity mapping (one entry per
 * archive page, [half] = [PageHalf.FULL]). With the split on, every wide
 * (landscape) archive page expands into two consecutive entries, one per
 * half — an insert model, where the split half is inserted after
 * the original page instead of shifting anything before it.
 */
data class ReaderViewerPage(
    /** 0-based index into the archive (what progress, covers and the overview use). */
    val archiveIndex: Int,
    /** Which part of the archive page this position shows. */
    val half: PageHalf,
)

/**
 * Expands archive pages into pager positions.
 *
 * Ordering follows reading direction: left-to-right reads LEFT
 * then RIGHT; right-to-left reads RIGHT then LEFT; [invert]
 * swaps the halves. Out-of-range entries in [wide]
 * are ignored so a stale scan can never crash the pager.
 */
fun buildViewerPages(
    archivePageCount: Int,
    wide: Set<Int>,
    direction: ReadingDirection,
    invert: Boolean,
): List<ReaderViewerPage> {
    if (archivePageCount <= 0) return emptyList()
    val inRange = wide.filter { it in 0 until archivePageCount }.toSet()
    return buildList {
        for (archive in 0 until archivePageCount) {
            if (archive !in inRange) {
                add(ReaderViewerPage(archive, PageHalf.FULL))
                continue
            }
            val first = when (direction) {
                ReadingDirection.LEFT_TO_RIGHT -> PageHalf.LEFT
                ReadingDirection.RIGHT_TO_LEFT -> PageHalf.RIGHT
            }
            val second = when (first) {
                PageHalf.LEFT -> PageHalf.RIGHT
                PageHalf.RIGHT -> PageHalf.LEFT
                PageHalf.FULL -> PageHalf.FULL
            }
            if (invert) {
                add(ReaderViewerPage(archive, second))
                add(ReaderViewerPage(archive, first))
            } else {
                add(ReaderViewerPage(archive, first))
                add(ReaderViewerPage(archive, second))
            }
        }
    }
}

/**
 * First pager position for each archive page (size [archivePageCount]).
 *
 * The overview and resume paths use this to translate archive indices into
 * expanded positions: opening archive page N always lands on its first half.
 */
fun archiveToExpandedPositions(
    viewerPages: List<ReaderViewerPage>,
    archivePageCount: Int,
): List<Int> {
    val positions = MutableList(archivePageCount) { 0 }
    val seen = BooleanArray(archivePageCount)
    viewerPages.forEachIndexed { expanded, page ->
        val archive = page.archiveIndex
        if (archive in 0 until archivePageCount && !seen[archive]) {
            seen[archive] = true
            positions[archive] = expanded
        }
    }
    // Archives missing from the list (shouldn't happen) pin to neighbors.
    var last = 0
    for (archive in 0 until archivePageCount) {
        if (seen[archive]) {
            last = positions[archive]
        } else {
            positions[archive] = last
        }
    }
    return positions
}
