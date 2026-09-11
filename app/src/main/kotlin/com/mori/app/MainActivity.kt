package com.mori.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mori.feature.reader.api.ReaderKeyInterceptor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoriApp()
        }
    }

    /**
     * Reader-owned keys (volume paging) are consumed before the system sees
     * them, so handled presses never move the system volume (Mihon parity).
     * The super call is the documented dispatch chain for this override.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ReaderKeyInterceptor.handler?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
