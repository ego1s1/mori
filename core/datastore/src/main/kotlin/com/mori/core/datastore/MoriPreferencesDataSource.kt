package com.mori.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Persisted user preferences. Implemented by DataStore; faked in tests.
 */
interface MoriPreferencesDataSource {
    /** True once the user completes onboarding (folder selected/imported). */
    val onboardingCompleted: Flow<Boolean>

    /** The last-selected SAF source tree URI, if the user granted one for rescans. */
    val sourceTreeUri: Flow<String?>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setSourceTreeUri(uri: String?)
}
