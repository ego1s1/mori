package com.mori.feature.onboarding.impl

import android.net.Uri
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.ImportReport
import com.mori.core.model.StorageLocation
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences

sealed interface OnboardingUiState {
    data object Welcome : OnboardingUiState

    data class Storage(
        val location: StorageLocation,
        val folderName: String?,
    ) : OnboardingUiState

    data class Appearance(
        val theme: ThemePreferences,
    ) : OnboardingUiState

    data class Import(
        val location: StorageLocation,
        val folderName: String?,
    ) : OnboardingUiState

    data class Importing(val done: Int, val total: Int) : OnboardingUiState

    data class Done(val report: ImportReport) : OnboardingUiState
}

sealed interface OnboardingAction {
    /** Welcome CTA. */
    data object GetStarted : OnboardingAction

    /** Leave the wizard without importing (empty library is a valid start). */
    data object Skip : OnboardingAction

    /** Back one step. */
    data object BackStep : OnboardingAction

    data object ContinueStep : OnboardingAction

    data class SelectStorage(val location: StorageLocation) : OnboardingAction

    /** A custom folder was granted; records it as the linked source. */
    data class CustomFolderChosen(val uri: Uri, val displayName: String?) : OnboardingAction

    /** The user picked a source folder to import from now. */
    data class FolderSelected(val uri: Uri) : OnboardingAction

    /** The user picked individual archive files. */
    data class FilesSelected(val uris: List<Uri>) : OnboardingAction

    data class SetThemeMode(val mode: ThemeMode) : OnboardingAction

    data class SetDynamicColor(val enabled: Boolean) : OnboardingAction

    data class SetColorScheme(val scheme: ColorSchemeChoice) : OnboardingAction

    data class SetAmoled(val enabled: Boolean) : OnboardingAction

    data object CancelImport : OnboardingAction

    /** Back to import to pick more, from a finished import. */
    data object ImportMore : OnboardingAction

    /** Mark onboarding complete and continue to the library. */
    data object Finish : OnboardingAction
}
