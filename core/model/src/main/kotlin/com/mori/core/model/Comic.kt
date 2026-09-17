package com.mori.core.model

/**
 * A comic in the user's library.
 *
 * Pure Kotlin with no Android dependencies. All file access goes through
 * [sourcePath], which always points inside app-private storage after import.
 */
data class Comic(
    val id: String,
    val title: String,
    val series: String?,
    val number: String?,
    val format: ComicFormat,
    val pageCount: Int,
    val sourcePath: String,
    val coverPath: String?,
    val lastPageIndex: Int,
    val sourceDisplayName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val error: ComicError? = null,
    val bookmarked: Boolean = false,
) {
    /**
     * True when the user has started but not finished this comic.
     *
     * Single-page books (pageCount == 1) are born finished: index 0 is both
     * the first and the last page, so they never enter progress-driven
     * surfaces (continue shelf, resume target, history) and always open at
     * 0. This is deliberate — there is no "unread" state to resume.
     */
    val isInProgress: Boolean
        get() = lastPageIndex > 0 && lastPageIndex < pageCount - 1

    /** True when the user has reached the last page. */
    val isFinished: Boolean
        get() = pageCount > 0 && lastPageIndex >= pageCount - 1

    /** 0f..1f reading progress used by library progress indicators. */
    val progress: Float
        get() = if (pageCount <= 0) 0f else ((lastPageIndex + 1).toFloat() / pageCount).coerceIn(0f, 1f)

    /** Pages remaining after the current one; the card badge reads this. */
    val pagesLeft: Int
        get() = if (pageCount <= 0) 0 else (pageCount - lastPageIndex - 1).coerceIn(0, pageCount)

    /**
     * Where opening this comic lands: the saved page while in progress,
     * page 0 otherwise. Finished books restart instead of reopening on
     * their last page, which reads as stuck.
     */
    val resumeIndex: Int
        get() = if (isInProgress) lastPageIndex else 0
}

/** In-progress books by recency, backing the continue shelf. */
fun List<Comic>.continueShelf(max: Int = 10): List<Comic> =
    filter { it.isInProgress }.sortedByDescending { it.updatedAt }.take(max)

/**
 * Most recently touched readable comic; the resume button opens it at its
 * saved page. Errored rows (tap goes to details, never the reader) and
 * untouched books (nothing to resume) never win, so the FAB can't deep-link
 * into a book the reader would immediately reject.
 */
fun List<Comic>.resumeTarget(): Comic? = filter { it.error == null && it.lastPageIndex > 0 }
    .maxByOrNull { it.updatedAt }
