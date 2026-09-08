package com.mori.comic.archive

import com.mori.comic.model.MediaType

/**
 * Shared logic for deciding whether an archive entry or file should be treated as a page.
 */
internal object PageEntryNames {
    private val ignoredBasenames = setOf("thumbs.db", "comicinfo.xml")

    /** Returns the final path segment of [name], handling `/` and `\` separators. */
    fun basename(name: String): String =
        name.substringAfterLast('/').substringAfterLast('\\')

    /**
     * Whether [name] denotes a readable page.
     *
     * Excludes hidden files (a leading `.` in the basename), well-known non-page files
     * such as `Thumbs.db` and `ComicInfo.xml`, and entries whose extension is not a
     * recognized image format.
     */
    fun isPage(name: String): Boolean {
        val base = basename(name)
        val hiddenOrIgnored = base.startsWith(".") || ignoredBasenames.contains(base.lowercase())
        return !hiddenOrIgnored && MediaType.isImageName(base)
    }
}
