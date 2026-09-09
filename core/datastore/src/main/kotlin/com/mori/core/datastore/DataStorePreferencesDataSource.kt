package com.mori.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal class DataStorePreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MoriPreferencesDataSource {

    private val dataStore: DataStore<Preferences> by lazy {
        androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) },
        )
    }

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    override val sourceTreeUri: Flow<String?> =
        dataStore.data.map { it[SOURCE_TREE_URI] }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setSourceTreeUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(SOURCE_TREE_URI) else it[SOURCE_TREE_URI] = uri
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "mori_preferences"
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SOURCE_TREE_URI = stringPreferencesKey("source_tree_uri")
    }
}
