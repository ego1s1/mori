package com.mori.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicImporter
import com.mori.core.datastore.MoriPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val importer: ComicImporter,
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Welcome)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var importJob: Job? = null

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.FolderSelected -> startImport { onProgress ->
                importer.importTree(action.uri, onProgress)
            }
            is OnboardingAction.FilesSelected -> startImport { onProgress ->
                importer.importDocuments(action.uris, onProgress)
            }
            OnboardingAction.CancelImport -> {
                importJob?.cancel()
            }
            OnboardingAction.ImportMore -> {
                _uiState.value = OnboardingUiState.Welcome
            }
            OnboardingAction.Finish -> {
                viewModelScope.launch {
                    preferences.setOnboardingCompleted(true)
                }
            }
        }
    }

    private fun startImport(
        run: suspend ((done: Int, total: Int) -> Unit) -> com.mori.core.model.ImportReport,
    ) {
        if (importJob?.isActive == true) return
        _uiState.value = OnboardingUiState.Importing(done = 0, total = 0)
        importJob = viewModelScope.launch {
            try {
                val report = run { done, total ->
                    val current = _uiState.value
                    if (current is OnboardingUiState.Importing) {
                        _uiState.value = current.copy(done = done, total = total)
                    }
                }
                _uiState.value = OnboardingUiState.Done(report)
            } catch (e: CancellationException) {
                _uiState.value = OnboardingUiState.Welcome
                throw e
            }
        }
    }
}
