package com.mori.core.data

import android.net.Uri
import com.mori.core.model.Comic
import java.io.File

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
