package com.mori.feature.detail.impl

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mori.core.model.ComicFormat
import java.io.File

/**
 * Shares a comic file through the Android Sharesheet. Link-only libraries
 * address user documents by URI, so the document URI itself is shared with a
 * read grant — no copy, no FileProvider round trip. Legacy file-backed rows
 * are served through FileProvider instead: a bare filesystem path is never
 * handed out (it grants nothing and leaks device layout). Pure construction
 * for testability; the route launches it.
 */
internal fun shareMimeType(displayName: String): String =
    ComicFormat.fromFileName(displayName)?.mimeType() ?: "*/*"

/**
 * Resolves a row's [rawUri] to a shareable `content://` URI, or null when
 * the row cannot be shared (folder grants, missing files, revoked
 * providers). Never throws: every failure maps to null and the caller
 * shows the unavailable fallback instead of crashing.
 */
internal fun shareableUri(context: Context, rawUri: String): Uri? = runCatching {
    val parsed = Uri.parse(rawUri)
    if (parsed.scheme == "content") {
        // Single documents only: a folder (tree) grant would over-share the
        // whole library directory with the target app. Rows only ever hold
        // document URIs; this guards the invariant at the share boundary by
        // matching SAF's literal "tree" path segment (not a substring, so
        // authorities or file names containing "tree" still pass).
        require("tree" !in parsed.pathSegments) { "Refusing to share a folder grant" }
        return@runCatching parsed
    }
    val file = File(rawUri)
    if (!file.isFile) return@runCatching null
    FileProvider.getUriForFile(context, context.packageName + ".shares", file)
}.getOrNull()

internal fun shareComicIntent(
    context: Context,
    uri: Uri,
    displayName: String,
    mimeType: String,
    chooserTitle: String,
): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        // ClipData carries the grant to the target on versions where
        // EXTRA_STREAM alone does not propagate through the chooser.
        clipData = ClipData.newUri(context.contentResolver, displayName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, chooserTitle).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/**
 * Launches the sharesheet; returns false when the row cannot be shared or
 * no app can handle the share, so the caller can explain instead of
 * crashing. Adds NEW_TASK for non-Activity callers.
 */
internal fun launchShare(context: Context, message: DetailMessage.ShareFile, chooserTitle: String): Boolean {
    return try {
        val uri = shareableUri(context, message.uri) ?: return false
        val intent = shareComicIntent(
            context,
            uri,
            message.displayName,
            message.mimeType,
            chooserTitle,
        )
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }
}
