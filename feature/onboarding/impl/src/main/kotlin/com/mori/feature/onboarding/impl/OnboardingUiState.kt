package com.mori.feature.onboarding.impl

import android.net.Uri
import com.mori.core.model.ImportReport

sealed interface OnboardingUiState {
    data object Welcome : OnboardingUiState

    data class Importing(val done: Int, val total: Int) : OnboardingUiState

    data class Done(val report: ImportReport) : OnboardingUiState
}

sealed interface OnboardingAction {
    /** The user picked a source folder; the Route takes persistable permission first. */
    data class FolderSelected(val uri: Uri) : OnboardingAction

    /** The user picked individual archive files. */
    data class FilesSelected(val uris: List<Uri>) : OnboardingAction

    data object CancelImport : OnboardingAction

    /** Back to welcome to pick more, from a finished import. */
    data object ImportMore : OnboardingAction

    /** Mark onboarding complete and continue to the library. */
    data object Finish : OnboardingAction
}
