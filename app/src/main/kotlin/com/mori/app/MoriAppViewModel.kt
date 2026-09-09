package com.mori.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.datastore.MoriPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Owns the top-level start destination: onboarding until the user completes it once,
 * then straight into the library.
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
}
