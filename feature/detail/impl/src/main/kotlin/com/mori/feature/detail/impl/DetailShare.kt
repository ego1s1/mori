package com.mori.feature.detail.impl

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
): Intent = Intent(Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
    putExtra(Intent.EXTRA_SUBJECT, displayName)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}.let { Intent.createChooser(it, chooserTitle) }

internal fun launchShare(context: Context, message: DetailMessage.ShareFile, chooserTitle: String) {
    context.startActivity(
        shareComicIntent(message.uri, message.displayName, message.mimeType, chooserTitle),
    )
}
