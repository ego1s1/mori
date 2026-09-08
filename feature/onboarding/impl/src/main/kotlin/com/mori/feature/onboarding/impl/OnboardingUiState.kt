package com.mori.feature.onboarding.impl

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState

    data class Ready(val message: String) : OnboardingUiState
}
