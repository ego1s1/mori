package com.mori.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mori.core.model.PageFit
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ReadingDirection
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal class DataStorePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : MoriPreferencesDataSource {

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    override val sourceTreeUri: Flow<String?> =
        dataStore.data.map { it[SOURCE_TREE_URI] }

    override val readerPreferences: Flow<ReaderPreferences> =
        dataStore.data.map { prefs ->
            ReaderPreferences(
                direction = prefs[READING_DIRECTION]?.let {
                    runCatching { ReadingDirection.valueOf(it) }.getOrDefault(ReadingDirection.LEFT_TO_RIGHT)
                } ?: ReadingDirection.LEFT_TO_RIGHT,
                pageFit = prefs[PAGE_FIT]?.let {
                    runCatching { PageFit.valueOf(it) }.getOrDefault(PageFit.WIDTH)
                } ?: PageFit.WIDTH,
                cropMargins = prefs[CROP_MARGINS] ?: false,
                volumeKeys = prefs[VOLUME_KEYS] ?: false,
                keepScreenOn = prefs[KEEP_SCREEN_ON] ?: true,
                showPageCounter = prefs[SHOW_PAGE_COUNTER] ?: true,
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setSourceTreeUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(SOURCE_TREE_URI) else it[SOURCE_TREE_URI] = uri
        }
    }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        val updated = transform(readerPreferences.first())
        dataStore.edit {
            it[READING_DIRECTION] = updated.direction.name
            it[PAGE_FIT] = updated.pageFit.name
            it[CROP_MARGINS] = updated.cropMargins
            it[VOLUME_KEYS] = updated.volumeKeys
            it[KEEP_SCREEN_ON] = updated.keepScreenOn
            it[SHOW_PAGE_COUNTER] = updated.showPageCounter
        }
    }

    override val themePreferences: Flow<ThemePreferences> =
        dataStore.data.map { prefs ->
            ThemePreferences(
                mode = prefs[THEME_MODE]?.let {
                    runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
                } ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
                amoled = prefs[AMOLED] ?: false,
            )
        }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        val updated = transform(themePreferences.first())
        dataStore.edit {
            it[THEME_MODE] = updated.mode.name
            it[DYNAMIC_COLOR] = updated.dynamicColor
            it[AMOLED] = updated.amoled
        }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SOURCE_TREE_URI = stringPreferencesKey("source_tree_uri")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val PAGE_FIT = stringPreferencesKey("page_fit")
        val CROP_MARGINS = booleanPreferencesKey("crop_margins")
        val VOLUME_KEYS = booleanPreferencesKey("volume_keys")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_PAGE_COUNTER = booleanPreferencesKey("show_page_counter")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED = booleanPreferencesKey("amoled")
    }
}
