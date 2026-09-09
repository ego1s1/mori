package com.mori.feature.onboarding.impl

import android.net.Uri
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.ImportItem
import com.mori.core.model.ImportReport
import com.mori.core.model.ImportStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Deterministic importer double: scripted reports, optional hang for cancellation tests. */
internal class FakeComicImporter(
    var treeReport: ImportReport = ImportReport(0, 0, 0, emptyList()),
    var documentsReport: ImportReport = ImportReport(0, 0, 0, emptyList()),
    var candidatesReport: ImportReport = ImportReport(0, 0, 0, emptyList()),
    var hangImport: Boolean = false,
) : ComicImporter {

    val seenTrees = mutableListOf<Uri>()
    val seenDocuments = mutableListOf<List<Uri>>()

    override suspend fun importCandidates(
        candidates: List<ImportCandidate>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        if (hangImport) kotlinx.coroutines.suspendCancellableCoroutine<Nothing> {}
        return candidatesReport
    }

    override suspend fun importTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        seenTrees += treeUri
        if (hangImport) kotlinx.coroutines.suspendCancellableCoroutine<Nothing> {}
        return treeReport
    }

    override suspend fun importDocuments(
        uris: List<Uri>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        seenDocuments += uris
        if (hangImport) kotlinx.coroutines.suspendCancellableCoroutine<Nothing> {}
        return documentsReport
    }

    companion object {
        fun success(total: Int, succeeded: Int = total): ImportReport {
            val items = (1..total).map {
                ImportItem("comic$it.cbz", ImportStatus.SUCCEEDED, null)
            }
            return ImportReport(total, succeeded, total - succeeded, items)
        }
    }
}

/** In-memory preferences double. */
internal class FakePreferencesDataSource : MoriPreferencesDataSource {

    private val completed = MutableStateFlow(false)
    private val treeUri = MutableStateFlow<String?>(null)

    override val onboardingCompleted: Flow<Boolean> = completed.asStateFlow()
    override val sourceTreeUri: Flow<String?> = treeUri.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        this.completed.value = completed
    }

    override suspend fun setSourceTreeUri(uri: String?) {
        treeUri.value = uri
    }
}
