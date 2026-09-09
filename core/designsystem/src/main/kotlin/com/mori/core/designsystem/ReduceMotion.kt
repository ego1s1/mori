package com.mori.core.designsystem

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user disabled system animations (animator duration scale set to zero).
 * Such users get instant transitions regardless of the in-app motion setting.
 */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        isSystemReduceMotionEnabled(scale)
    }
}
