package com.mori.feature.detail.impl

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mori.core.model.ComicFormat

/**
 * Shares a comic file through the Android Sharesheet. Link-only libraries
 * address user documents by URI, so the document URI itself is shared with a
 * read grant — no copy, no FileProvider round trip. Pure construction for
 * testability; the route launches it.
 */
internal fun shareMimeType(displayName: String): String =
    ComicFormat.fromFileName(displayName)?.mimeType() ?: "*/*"

internal fun shareComicIntent(
    uri: String,
    displayName: String,
    mimeType: String,
    chooserTitle: String,
): Intent {
    // Single documents only: a folder (tree) grant would over-share the
    // whole library directory with the target app. Rows only ever hold
    // document URIs; this guards the invariant at the share boundary by
    // matching SAF's literal "tree" path segment (not a substring, so
    // authorities or file names containing "tree" still pass).
    require("tree" !in Uri.parse(uri).pathSegments) { "Refusing to share a folder grant" }
    return Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.let { Intent.createChooser(it, chooserTitle) }
}

/**
 * Launches the sharesheet; returns false when no app can handle the share
 * (e.g. a device with no send handler) so the caller can explain instead
 * of crashing.
 */
internal fun launchShare(context: Context, message: DetailMessage.ShareFile, chooserTitle: String): Boolean {
    return try {
        context.startActivity(
            shareComicIntent(message.uri, message.displayName, message.mimeType, chooserTitle),
        )
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
