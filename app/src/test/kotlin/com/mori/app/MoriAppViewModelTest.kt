package com.mori.app

import app.cash.turbine.test
import com.mori.core.model.MotionStyle
import com.mori.core.model.ThemeMode
import com.mori.core.model.ThemePreferences
import com.mori.core.testing.FakePreferencesDataSource
import com.mori.core.testing.TestDispatcherRule
import com.mori.core.testing.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Top-level app state: null until prefs emit (the splash gate), then live
 * values. The app-lock toggle writes through to prefs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MoriAppViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    @Test
    fun prefsFlowThroughOnceEmitted() = runTest {
        val preferences = FakePreferencesDataSource(
            initialTheme = ThemePreferences(mode = ThemeMode.DARK),
            initialMotion = MotionStyle.CALM,
        )
        val viewModel = MoriAppViewModel(preferences)

        // StateFlow conflates: the splash-gate null may never be observed,
        // but the persisted values always arrive.
        viewModel.themePreferences.test {
            assertEquals(ThemeMode.DARK, awaitWhere { it != null }?.mode)
        }
        viewModel.motionStyle.test {
            assertEquals(MotionStyle.CALM, awaitWhere { it != null })
        }
        viewModel.onboardingCompleted.test {
            assertEquals(false, awaitWhere { it != null })
        }
    }

    @Test
    fun setAppLockEnabledWritesThrough() = runTest {
        val preferences = FakePreferencesDataSource()
        val viewModel = MoriAppViewModel(preferences)

        viewModel.appLockEnabled.test {
            assertEquals(false, awaitItem())
            viewModel.setAppLockEnabled(true)
            assertEquals(true, awaitWhere { it == true })
        }
    }
}
