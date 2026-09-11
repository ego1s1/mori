package com.mori.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Link-only step wizard: Welcome → Folder → Appearance. Theme choices persist
 * immediately so quitting mid-wizard never loses them; Skip finishes without
 * linking. Nothing is ever copied or indexed here — the library picks up the
 * persisted tree on arrival.
 */
@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    private enum class Step { WELCOME, FOLDER, APPEARANCE }

    private val step = MutableStateFlow(Step.WELCOME)

    val uiState: StateFlow<OnboardingUiState> = combine(
        step,
        preferences.themePreferences,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState.Welcome,
    )

    private fun toUiState(step: Step, theme: ThemePreferences): OnboardingUiState =
        when (step) {
            Step.WELCOME -> OnboardingUiState.Welcome
            Step.FOLDER -> OnboardingUiState.Folder
            Step.APPEARANCE -> OnboardingUiState.Appearance(theme)
        }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.GetStarted -> step.value = Step.FOLDER
            OnboardingAction.Skip -> finish()
            OnboardingAction.BackStep -> step.value = when (step.value) {
                Step.WELCOME -> Step.WELCOME
                Step.FOLDER -> Step.WELCOME
                Step.APPEARANCE -> Step.FOLDER
            }
            is OnboardingAction.FolderSelected -> {
                viewModelScope.launch {
                    preferences.setSourceTreeUri(action.uri.toString())
                }
                step.value = Step.APPEARANCE
            }
            is OnboardingAction.SetThemeMode -> updateTheme { it.copy(mode = action.mode) }
            is OnboardingAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is OnboardingAction.SetColorScheme -> updateTheme {
                it.copy(colorScheme = action.scheme, dynamicColor = false)
            }
            is OnboardingAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            OnboardingAction.Finish -> finish()
        }
    }

    private fun updateTheme(transform: (ThemePreferences) -> ThemePreferences) {
        viewModelScope.launch {
            preferences.updateThemePreferences(transform)
        }
    }

    private fun finish() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
        }
    }
}
