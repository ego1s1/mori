package com.mori.core.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mori.core.model.ImportItem
import com.mori.core.model.ImportReport
import com.mori.core.model.ImportStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * A single importable comic discovered through SAF.
 *
 * @property displayName file name shown to the user and used for the library copy.
 * @property sizeBytes known size or `-1` when the provider does not report one.
 * @property open opens a fresh stream for the content; called at most once per import.
 */
data class ImportCandidate(
    val displayName: String,
    val sizeBytes: Long,
    val open: () -> java.io.InputStream,
)

/**
 * Copies user-selected comics into app-private storage and reports per-file results.
 *
 * Lives in `core:data` so onboarding and future rescan flows share one implementation.
 */
interface ComicImporter {
    suspend fun importCandidates(
        candidates: List<ImportCandidate>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportReport

    suspend fun importTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportReport

    suspend fun importDocuments(
        uris: List<Uri>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportReport
}

internal class AppComicImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDir: File,
) : ComicImporter {

    override suspend fun importCandidates(
        candidates: List<ImportCandidate>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport = withContext(Dispatchers.IO) {
        libraryDir.mkdirs()
        // Fail the whole batch upfront when the device clearly cannot hold it,
        // instead of dying file-by-file with raw stream errors.
        val needed = candidates.filter { it.sizeBytes > 0 }.sumOf { it.sizeBytes }
        if (needed > 0 && libraryDir.usableSpace < needed) {
            return@withContext ImportReport(
                total = candidates.size,
                succeeded = 0,
                failed = candidates.size,
                items = candidates.map {
                    ImportItem(it.displayName, ImportStatus.FAILED, "Not enough storage space")
                },
            )
        }
        var succeeded = 0
        var failed = 0
        val items = mutableListOf<ImportItem>()
        candidates.forEachIndexed { index, candidate ->
            coroutineContext.ensureActive()
            try {
                copyCandidate(candidate)
                succeeded += 1
                items += ImportItem(candidate.displayName, ImportStatus.SUCCEEDED, null)
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                failed += 1
                items += ImportItem(candidate.displayName, ImportStatus.FAILED, e.message)
            }
            onProgress(index + 1, candidates.size)
        }
        ImportReport(candidates.size, succeeded, failed, items)
    }

    override suspend fun importTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return ImportReport(0, 0, 0, emptyList())
        return importCandidates(collectArchives(root), onProgress)
    }

    override suspend fun importDocuments(
        uris: List<Uri>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        val candidates = uris.mapNotNull { uri ->
            val file = DocumentFile.fromSingleUri(context, uri) ?: return@mapNotNull null
            val name = file.name ?: return@mapNotNull null
            if (!isSupportedArchive(name)) return@mapNotNull null
            ImportCandidate(name, file.length()) {
                context.contentResolver.openInputStream(uri)
                    ?: throw java.io.IOException("Unable to open $name")
            }
        }
        return importCandidates(candidates, onProgress)
    }

    private fun collectArchives(root: DocumentFile): List<ImportCandidate> {
        val out = mutableListOf<ImportCandidate>()
        val stack = ArrayDeque<DocumentFile>()
        stack.add(root)
        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            val children = runCatching { current.listFiles().toList() }.getOrDefault(emptyList())
            children.forEach { child ->
                if (child.isDirectory) {
                    stack.add(child)
                } else {
                    val name = child.name ?: return@forEach
                    if (!isSupportedArchive(name)) return@forEach
                    val uri = child.uri
                    out += ImportCandidate(name, child.length()) {
                        context.contentResolver.openInputStream(uri)
                            ?: throw java.io.IOException("Unable to open $name")
                    }
                }
            }
        }
        return out
    }

    private fun copyCandidate(candidate: ImportCandidate) {
        val sanitized = sanitize(candidate.displayName)
        val base = File(libraryDir, sanitized)
        if (base.exists() && candidate.sizeBytes >= 0 && base.length() == candidate.sizeBytes) {
            return // Idempotent re-import of the identical file.
        }
        val dest = uniqueDestination(libraryDir, sanitized)
        candidate.open().use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun uniqueDestination(dir: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var candidate = File(dir, name)
        var counter = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base ($counter).$ext")
            counter += 1
        }
        return candidate
    }

    private fun sanitize(name: String): String =
        name.replace('/', '_').replace('\\', '_').trim().ifBlank { "comic" }

    private companion object {
        fun isSupportedArchive(name: String): Boolean {
            val extension = name.substringAfterLast('.', "").lowercase()
            return extension == "cbz" || extension == "zip" ||
                extension == "cbr" || extension == "rar"
        }
    }
}
