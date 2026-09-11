package com.mori.feature.onboarding.impl

import android.net.Uri
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences

/**
 * Link-only wizard: Welcome → Folder → Appearance. The wizard asks exactly
 * one question (where the comics live) and copies nothing; picking a folder
 * persists its tree URI and advances, and the library indexes it lazily on
 * arrival with a smooth handoff and no intermediate screens.
 */
sealed interface OnboardingUiState {
    data object Welcome : OnboardingUiState

    data object Folder : OnboardingUiState

    data class Appearance(
        val theme: ThemePreferences,
    ) : OnboardingUiState
}

sealed interface OnboardingAction {
    /** Welcome CTA. */
    data object GetStarted : OnboardingAction

    /** Leave the wizard (empty library is a valid start). */
    data object Skip : OnboardingAction

    /** Back one step. */
    data object BackStep : OnboardingAction

    /** The user picked the folder to read from. */
    data class FolderSelected(val uri: Uri) : OnboardingAction

    data class SetThemeMode(val mode: ThemeMode) : OnboardingAction

    data class SetDynamicColor(val enabled: Boolean) : OnboardingAction

    data class SetColorScheme(val scheme: ColorSchemeChoice) : OnboardingAction

    data class SetAmoled(val enabled: Boolean) : OnboardingAction

    /** Mark onboarding complete and continue to the library. */
    data object Finish : OnboardingAction
}
