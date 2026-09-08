package com.mori.feature.onboarding.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

fun NavController.navigateToOnboarding() {
    navigate(OnboardingRoute)
}
