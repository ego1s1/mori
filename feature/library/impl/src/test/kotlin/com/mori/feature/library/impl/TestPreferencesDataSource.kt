package com.mori.feature.library.impl

import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory preferences double for library tests. */
internal class TestPreferencesDataSource(
    theme: ThemePreferences = ThemePreferences(),
) : MoriPreferencesDataSource {

    private val completed = MutableStateFlow(false)
    private val treeUri = MutableStateFlow<String?>(null)
    private val readerPreferencesFlow = MutableStateFlow(ReaderPreferences())
    private val themePreferencesFlow = MutableStateFlow(theme)

    override val onboardingCompleted: Flow<Boolean> = completed.asStateFlow()
    override val sourceTreeUri: Flow<String?> = treeUri.asStateFlow()
    override val readerPreferences: Flow<ReaderPreferences> = readerPreferencesFlow.asStateFlow()
    override val themePreferences: Flow<ThemePreferences> = themePreferencesFlow.asStateFlow()

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
}
