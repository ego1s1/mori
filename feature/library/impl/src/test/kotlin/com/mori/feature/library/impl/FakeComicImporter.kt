package com.mori.feature.library.impl

import android.net.Uri
import com.mori.core.data.ComicImporter
import com.mori.core.data.ImportCandidate
import com.mori.core.model.ImportReport

/** Importer double: configurable tree report, counts linked-folder imports. */
internal class FakeComicImporter(
    var treeReport: ImportReport = ImportReport(0, 0, 0, emptyList()),
) : ComicImporter {

    val seenTrees = mutableListOf<Uri>()

    override suspend fun importCandidates(
        candidates: List<ImportCandidate>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport = ImportReport(0, 0, 0, emptyList())

    override suspend fun importTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        seenTrees += treeUri
        return treeReport
    }

    override suspend fun importDocuments(
        uris: List<Uri>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport = ImportReport(0, 0, 0, emptyList())
}
