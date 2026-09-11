package com.mori.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicImporter
import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.StorageLocation
import com.mori.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Step wizard: Welcome → Storage → Appearance → Import, then the transient
 * Importing/Done phases. Theme and storage choices persist immediately so
 * quitting mid-wizard never loses them; Skip finishes without importing.
 */
@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val importer: ComicImporter,
    private val repository: ComicsRepository,
    private val preferences: MoriPreferencesDataSource,
) : ViewModel() {

    private enum class Step { WELCOME, STORAGE, APPEARANCE, IMPORT }

    private sealed interface ImportPhase {
        data class Progress(val done: Int, val total: Int, val link: Boolean) : ImportPhase
        data class Finished(val report: com.mori.core.model.ImportReport) : ImportPhase
    }

    private val step = MutableStateFlow(Step.WELCOME)
    private val importPhase = MutableStateFlow<ImportPhase?>(null)
    private val linkMode = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> = combine(
        step,
        preferences.themePreferences,
        preferences.storageLocation,
        linkMode,
        importPhase,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState.Welcome,
    )

    private fun toUiState(
        step: Step,
        theme: ThemePreferences,
        location: StorageLocation,
        link: Boolean,
        phase: ImportPhase?,
    ): OnboardingUiState {
        phase?.let {
            return when (it) {
                is ImportPhase.Progress -> OnboardingUiState.Importing(it.done, it.total, it.link)
                is ImportPhase.Finished -> OnboardingUiState.Done(it.report)
            }
        }
        return when (step) {
            Step.WELCOME -> OnboardingUiState.Welcome
            Step.STORAGE -> OnboardingUiState.Storage(location)
            Step.APPEARANCE -> OnboardingUiState.Appearance(theme)
            Step.IMPORT -> OnboardingUiState.Import(location, link)
        }
    }

    private var importJob: Job? = null

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.GetStarted -> step.value = Step.STORAGE
            OnboardingAction.Skip -> finish()
            OnboardingAction.BackStep -> step.value = when (step.value) {
                Step.WELCOME -> Step.WELCOME
                Step.STORAGE -> Step.WELCOME
                Step.APPEARANCE -> Step.STORAGE
                Step.IMPORT -> Step.APPEARANCE
            }
            OnboardingAction.ContinueStep -> step.value = when (step.value) {
                Step.WELCOME -> Step.STORAGE
                Step.STORAGE -> Step.APPEARANCE
                Step.APPEARANCE -> Step.IMPORT
                Step.IMPORT -> Step.IMPORT
            }
            is OnboardingAction.SelectStorage -> viewModelScope.launch {
                preferences.setStorageLocation(action.location)
            }
            is OnboardingAction.FolderSelected -> {
                if (linkMode.value) {
                    // Link: persist the tree and index in place — zero copies.
                    viewModelScope.launch {
                        preferences.setSourceTreeUri(action.uri.toString())
                    }
                    startImport(link = true) { onProgress ->
                        repository.indexLinkedTree(action.uri, onProgress)
                    }
                } else {
                    // Copy: duplicates land in app storage and no tree lingers
                    // behind to surprise later rescans.
                    viewModelScope.launch {
                        preferences.setSourceTreeUri(null)
                    }
                    startImport(link = false) { onProgress ->
                        importer.importTree(action.uri, onProgress)
                    }
                }
            }
            is OnboardingAction.SetLinkMode -> {
                linkMode.value = action.link
            }
            is OnboardingAction.FilesSelected -> startImport(link = false) { onProgress ->
                importer.importDocuments(action.uris, onProgress)
            }
            OnboardingAction.CancelImport -> {
                importJob?.cancel()
            }
            is OnboardingAction.SetThemeMode -> updateTheme { it.copy(mode = action.mode) }
            is OnboardingAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is OnboardingAction.SetColorScheme -> updateTheme {
                it.copy(colorScheme = action.scheme, dynamicColor = false)
            }
            is OnboardingAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            OnboardingAction.ImportMore -> {
                importPhase.value = null
                step.value = Step.IMPORT
            }
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

    private fun startImport(
        link: Boolean,
        run: suspend ((done: Int, total: Int) -> Unit) -> com.mori.core.model.ImportReport,
    ) {
        if (importJob?.isActive == true) return
        step.value = Step.IMPORT
        importPhase.value = ImportPhase.Progress(done = 0, total = 0, link = link)
        var lastIndexed = 0
        importJob = viewModelScope.launch {
            try {
                val report = run { done, total ->
                    val current = importPhase.value
                    if (current is ImportPhase.Progress) {
                        importPhase.value = current.copy(done = done, total = total)
                    }
                    // Index incrementally so the shelf fills live behind the
                    // progress screen instead of appearing only at the end.
                    if (shouldRefreshIndex(done, total, lastIndexed)) {
                        lastIndexed = done
                        launch { runCatching { repository.refreshLibrary() } }
                    }
                }
                // Copying alone leaves the library empty: index the new files now
                // so the shelf is populated when onboarding finishes. A failed
                // index must not fail the import itself; rescan stays available.
                if (report.succeeded > 0) {
                    runCatching { repository.refreshLibrary() }
                }
                importPhase.value = ImportPhase.Finished(report)
            } catch (e: CancellationException) {
                importPhase.value = null
                throw e
            }
        }
    }
}

/**
 * Refresh the index at most every [INDEX_REFRESH_EVERY] files (plus always on
 * the final tick): each refresh re-walks the library, so per-file refreshes
 * would turn an N-file import quadratic.
 */
internal fun shouldRefreshIndex(done: Int, total: Int, lastRefreshDone: Int): Boolean =
    total > 0 && (done >= total || done - lastRefreshDone >= INDEX_REFRESH_EVERY)

private const val INDEX_REFRESH_EVERY = 10
