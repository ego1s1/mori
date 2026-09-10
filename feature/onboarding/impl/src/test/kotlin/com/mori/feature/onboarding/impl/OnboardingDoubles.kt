package com.mori.feature.onboarding.impl

import android.net.Uri
import com.mori.core.data.ComicImporter
import com.mori.core.data.ComicsRepository
import com.mori.core.data.ImportCandidate
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryDisplay
import com.mori.core.model.LibraryQuery
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.MotionStyle
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemePreferences
import com.mori.core.model.ImportItem
import com.mori.core.model.ImportReport
import com.mori.core.model.ImportStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

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

/** Repository double tracking index refreshes after import. */
internal class FakeComicsRepository : ComicsRepository {
    var refreshCalls = 0

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        MutableStateFlow(emptyList<Comic>()).asStateFlow()

    override fun observeComic(id: String): Flow<Comic?> =
        MutableStateFlow<Comic?>(null).asStateFlow()

    override suspend fun getComic(id: String): Comic? = null

    override suspend fun refreshLibrary(): IndexReport {
        refreshCalls += 1
        return IndexReport(0, 0, 0)
    }

    override suspend fun refreshComic(id: String): Comic? = null

    override suspend fun removeComic(id: String) = Unit

    override suspend fun saveProgress(id: String, pageIndex: Int) = Unit

    override suspend fun toggleBookmark(id: String) = Unit

    override suspend fun clearThumbnailCache() = Unit

    override suspend fun storageUsage(): StorageUsage = StorageUsage(0, 0L, 0L)
}

/** In-memory preferences double. */
internal class FakePreferencesDataSource : MoriPreferencesDataSource {

    private val completed = MutableStateFlow(false)
    private val treeUri = MutableStateFlow<String?>(null)
    private val readerPreferencesFlow = MutableStateFlow(ReaderPreferences())
    private val themePreferencesFlow = MutableStateFlow(ThemePreferences())

    override val onboardingCompleted: Flow<Boolean> = completed.asStateFlow()
    override val sourceTreeUri: Flow<String?> = treeUri.asStateFlow()
    override val readerPreferences: Flow<ReaderPreferences> = readerPreferencesFlow.asStateFlow()
    override val themePreferences: Flow<ThemePreferences> = themePreferencesFlow.asStateFlow()
    private val motionStyleFlow = MutableStateFlow(MotionStyle.EXPRESSIVE)
    override val motionStyle: Flow<MotionStyle> = motionStyleFlow.asStateFlow()
    private val libraryDisplayFlow = MutableStateFlow(LibraryDisplay())
    override val libraryDisplay: Flow<LibraryDisplay> = libraryDisplayFlow.asStateFlow()
    private val overviewSeenFlow = MutableStateFlow(true)
    override val readerOverviewSeen: Flow<Boolean> = overviewSeenFlow.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        this.completed.value = completed
    }

    override suspend fun setSourceTreeUri(uri: String?) {
        treeUri.value = uri
    }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        readerPreferencesFlow.value = transform(readerPreferencesFlow.value)
    }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        themePreferencesFlow.value = transform(themePreferencesFlow.value)
    }

    override suspend fun updateMotionStyle(style: MotionStyle) {
        motionStyleFlow.value = style
    }

    override suspend fun updateLibraryDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        libraryDisplayFlow.value = transform(libraryDisplayFlow.value)
    }

    override suspend fun setReaderOverviewSeen() {
        overviewSeenFlow.value = true
    }
}
