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
    /** True when the user has started but not finished this comic. */
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
        get() = (pageCount - lastPageIndex - 1).coerceAtLeast(0)
}

/** In-progress books by recency, backing the continue shelf. */
fun List<Comic>.continueShelf(max: Int = 10): List<Comic> =
    filter { it.isInProgress }.sortedByDescending { it.updatedAt }.take(max)

/** Most recently touched comic; the resume button opens it at its saved page. */
fun List<Comic>.resumeTarget(): Comic? = maxByOrNull { it.updatedAt }
