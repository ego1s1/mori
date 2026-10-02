package com.mori.core.testing

import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.LibraryQuery
import com.mori.core.model.MotionStyle
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single in-memory [MoriPreferencesDataSource] double for all suites.
 * Constructor defaults cover every feature's previous copy; [updates]
 * logs reader-preference writes for persistence assertions.
 */
class FakePreferencesDataSource(
    initialReader: ReaderPreferences = ReaderPreferences(),
    initialTheme: ThemePreferences = ThemePreferences(),
    initialMotion: MotionStyle = MotionStyle.EXPRESSIVE,
    initialQuery: LibraryQuery = LibraryQuery(),
    overviewSeen: Boolean = true,
    initialTrees: Set<String> = emptySet(),
) : MoriPreferencesDataSource {

    private val completed = MutableStateFlow(false)
    private val treeUris = MutableStateFlow(initialTrees)
    private val readerPreferencesFlow = MutableStateFlow(initialReader)
    private val themePreferencesFlow = MutableStateFlow(initialTheme)
    private val motionStyleFlow = MutableStateFlow(initialMotion)
    private val libraryQueryFlow = MutableStateFlow(initialQuery)
    private val overviewSeenFlow = MutableStateFlow(overviewSeen)
    val updates = mutableListOf<ReaderPreferences>()

    override val onboardingCompleted: Flow<Boolean> = completed.asStateFlow()
    override val sourceTreeUris: Flow<Set<String>> = treeUris.asStateFlow()
    override val readerPreferences: Flow<ReaderPreferences> = readerPreferencesFlow.asStateFlow()
    override val themePreferences: Flow<ThemePreferences> = themePreferencesFlow.asStateFlow()
    override val motionStyle: Flow<MotionStyle> = motionStyleFlow.asStateFlow()
    override val libraryQuery: Flow<LibraryQuery> = libraryQueryFlow.asStateFlow()
    override val readerOverviewSeen: Flow<Boolean> = overviewSeenFlow.asStateFlow()

    fun isOverviewSeen(): Boolean = overviewSeenFlow.value

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        this.completed.value = completed
    }

    override suspend fun addSourceTreeUri(uri: String) {
        treeUris.value = treeUris.value + uri
    }

    override suspend fun removeSourceTreeUri(uri: String) {
        treeUris.value = treeUris.value - uri
    }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        val updated = transform(readerPreferencesFlow.value)
        updates += updated
        readerPreferencesFlow.value = updated
    }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        themePreferencesFlow.value = transform(themePreferencesFlow.value)
    }

    override suspend fun updateMotionStyle(style: MotionStyle) {
        motionStyleFlow.value = style
    }

    override suspend fun updateLibraryQuery(transform: (LibraryQuery) -> LibraryQuery) {
        libraryQueryFlow.value = transform(libraryQueryFlow.value)
    }

    override suspend fun setReaderOverviewSeen() {
        overviewSeenFlow.value = true
    }

    private val appLockFlow = MutableStateFlow(false)
    override val appLockEnabled: Flow<Boolean> = appLockFlow.asStateFlow()

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        appLockFlow.value = enabled
    }
}
