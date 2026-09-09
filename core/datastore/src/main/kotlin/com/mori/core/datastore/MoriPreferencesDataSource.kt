package com.mori.core.datastore

import com.mori.core.model.ReaderPreferences
import com.mori.core.model.MotionStyle
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Persisted user preferences. Implemented by DataStore; faked in tests.
 */
interface MoriPreferencesDataSource {
    /** True once the user completes onboarding (folder selected/imported). */
    val onboardingCompleted: Flow<Boolean>

    /** The last-selected SAF source tree URI, if the user granted one for rescans. */
    val sourceTreeUri: Flow<String?>

    /** Reader preferences (direction, fit, toggles). */
    val readerPreferences: Flow<ReaderPreferences>

    /** App theme preferences (mode, dynamic color, AMOLED black). */
    val themePreferences: Flow<ThemePreferences>

    /** Motion personality (spring physics vs calm fades). */
    val motionStyle: Flow<MotionStyle>

    /** True once the reader's first-launch overview has faded (chrome shown 2s). */
    val readerOverviewSeen: Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setSourceTreeUri(uri: String?)

    suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences)

    suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences)

    suspend fun updateMotionStyle(style: MotionStyle)

    suspend fun setReaderOverviewSeen()
}
