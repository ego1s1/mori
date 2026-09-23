package com.mori.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.mori.feature.reader.api.ReaderKeyInterceptor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // Shared with the MoriApp composition: the splash stays on its black bed
    // until prefs resolve, so first paint already carries the right theme
    // instead of flashing fallback colors.
    private val appViewModel: MoriAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            appViewModel.onboardingCompleted.value == null ||
                appViewModel.themePreferences.value == null
        }
        enableEdgeToEdge()
        setContent {
            MoriApp()
        }
    }

    /**
     * Reader-owned keys (volume paging) are consumed before the system sees
     * them, so handled presses never move the system volume.
     * The super call is the documented dispatch chain for this override.
     *
     * RestrictedApi is suppressed only for this override: dispatchKeyEvent
     * is the framework's own interception point, and the reader contract is
     * covered by ReaderScreen key-handler tests plus manual volume-key runs.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ReaderKeyInterceptor.handler?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
