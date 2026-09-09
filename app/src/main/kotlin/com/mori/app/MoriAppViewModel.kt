package com.mori.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Owns top-level app state: the onboarding gate and the persisted theme.
 */
@HiltViewModel
class MoriAppViewModel @Inject constructor(
    preferences: MoriPreferencesDataSource,
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean?> = preferences.onboardingCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val themePreferences: StateFlow<ThemePreferences?> = preferences.themePreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
