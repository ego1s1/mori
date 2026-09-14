package com.mori.core.data

import android.net.Uri
import com.mori.core.model.Comic
import java.io.File
import java.security.MessageDigest

/**
 * Resolves a comic to a readable file: linked rows re-materialize
 * transiently through the bounded cache, plain rows read in place. The
 * user original is never copied into the library either way.
 */
internal suspend fun LinkedArchiveCache.fileFor(comic: Comic): File =
    if (isLinkedSourcePath(comic.sourcePath)) {
        materialize(Uri.parse(comic.sourcePath), comic.sourceDisplayName)
    } else {
        File(comic.sourcePath)
    }

/** Stable hex digest for cache keys and cover ids (never a security boundary). */
internal fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
