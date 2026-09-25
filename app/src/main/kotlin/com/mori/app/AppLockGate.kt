package com.mori.app

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mori.core.designsystem.MoriEmphasized

/**
 * Biometric/device-credential gate: when [appLockEnabled], the app content
 * stays hidden behind this lock until authentication succeeds. Unlock is
 * process-scoped (a fresh process locks again); closing the app relocks.
 *
 * The native system prompt fires immediately on composition — no tap needed.
 * The lock screen behind it is only a backdrop (and a retry path when the
 * prompt is dismissed or the device cannot authenticate).
 *
 * Devices with no secure lock at all cannot authenticate: the gate explains
 * and offers to turn the lock off rather than bricking the library.
 */
@Composable
internal fun AppLockGate(
    appLockEnabled: Boolean,
    onDisableLock: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var unlocked by remember(appLockEnabled) { mutableStateOf(!appLockEnabled) }
    if (unlocked || !appLockEnabled) {
        content()
        return
    }
    AppLockScreen(
        onUnlocked = { unlocked = true },
        onDisableLock = onDisableLock,
        modifier = modifier,
    )
}

@Composable
private fun AppLockScreen(
    onUnlocked: () -> Unit,
    onDisableLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var failureMessage by remember { mutableStateOf<String?>(null) }
    val cannotAuthenticate = stringResource(R.string.applock_no_secure_lock)
    val promptTitle = stringResource(R.string.applock_prompt_title)
    val promptSubtitle = stringResource(R.string.applock_prompt_subtitle)

    fun authenticate() {
        if (activity == null) {
            failureMessage = cannotAuthenticate
            return
        }
        val canAuth = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            failureMessage = cannotAuthenticate
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Negative button / system cancel: stay locked silently
                    // unless the device fundamentally cannot authenticate.
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                    ) {
                        failureMessage = cannotAuthenticate
                    }
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }

    // Fire the native prompt immediately: unlocking must not need a tap.
    // Dismissal lands back on the lock screen, where Unlock retries.
    LaunchedEffect(Unit) {
        authenticate()
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.applock_title),
                style = MoriEmphasized.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.applock_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = ::authenticate) {
                Text(stringResource(R.string.applock_unlock))
            }
            if (failureMessage != null) {
                Text(
                    text = failureMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                TextButton(onClick = onDisableLock) {
                    Text(stringResource(R.string.applock_turn_off))
                }
            }
        }
    }
}
