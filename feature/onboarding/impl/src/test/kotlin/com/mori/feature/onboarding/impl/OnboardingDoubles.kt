package com.mori.feature.onboarding.impl

import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.LibraryDisplay
import com.mori.core.model.LibraryQuery
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.MotionStyle
import com.mori.core.model.StorageUsage
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** Repository double (unused by the link-only wizard; kept for UI tests). */
internal class FakeComicsRepository : ComicsRepository {
    val linkedTrees = mutableListOf<android.net.Uri>()

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        MutableStateFlow(emptyList<Comic>()).asStateFlow()

    override fun observeComic(id: String): Flow<Comic?> =
        MutableStateFlow<Comic?>(null).asStateFlow()

    override suspend fun getComic(id: String): Comic? = null

    override suspend fun indexLinkedTree(
        treeUri: android.net.Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): com.mori.core.model.ImportReport {
        linkedTrees += treeUri
        return com.mori.core.model.ImportReport(0, 0, 0, emptyList())
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
