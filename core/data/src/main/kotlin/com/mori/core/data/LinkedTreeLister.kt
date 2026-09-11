package com.mori.core.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * A user document addressable for indexing: identity, display name, and a
 * change marker for the fast path.
 */
internal data class LinkedDocument(
    val uri: Uri,
    val name: String,
    val modified: Long,
)

/**
 * Lists linked-tree documents. Separated from the repository for testability:
 * SAF document queries need a content provider, which unit tests fake here
 * instead of in Robolectric shadows.
 */
internal interface LinkedTreeLister {
    suspend fun listArchives(treeUri: Uri): LinkedTreeListResult

    /** Resolves one document, or null when it is gone/unreadable. */
    suspend fun resolve(documentUri: Uri): LinkedDocument?
}

internal data class LinkedTreeListResult(
    val documents: List<LinkedDocument>,
    /**
     * True when any listing failed: the result must never read as an empty
     * folder, or pruning would wipe rows the user still owns.
     */
    val walkFailed: Boolean,
)

internal class DocumentLinkedTreeLister @Inject constructor(
    @ApplicationContext private val context: Context,
) : LinkedTreeLister {

    override suspend fun listArchives(treeUri: Uri): LinkedTreeListResult {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return LinkedTreeListResult(emptyList(), walkFailed = true)
        val out = mutableListOf<LinkedDocument>()
        var walkFailed = false
        val stack = ArrayDeque<DocumentFile>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val children = runCatching { current.listFiles().toList() }
                .getOrElse {
                    walkFailed = true
                    emptyList()
                }
            children.forEach { child ->
                if (child.isDirectory) {
                    stack.add(child)
                } else {
                    val name = child.name ?: return@forEach
                    if (isSupportedArchive(name)) {
                        out += LinkedDocument(child.uri, name, child.lastModified())
                    }
                }
            }
        }
        return LinkedTreeListResult(out, walkFailed)
    }

    override suspend fun resolve(documentUri: Uri): LinkedDocument? {
        val doc = runCatching {
            DocumentFile.fromSingleUri(context, documentUri)
        }.getOrNull() ?: return null
        val name = doc.name ?: return null
        return LinkedDocument(doc.uri, name, doc.lastModified())
    }
}

internal fun isSupportedArchive(name: String): Boolean {
    val extension = name.substringAfterLast('.', "").lowercase()
    return extension == "cbz" || extension == "zip" ||
        extension == "cbr" || extension == "rar"
}
