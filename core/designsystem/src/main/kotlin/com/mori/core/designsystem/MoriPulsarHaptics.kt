package com.mori.core.designsystem

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.swmansion.pulsar.Pulsar
import com.swmansion.pulsar.presets.PresetsWrapper

/**
 * Pulsar-backed player for [MoriHaptic] events.
 *
 * Pulsar 1.3.0 (`com.swmansion:pulsar`) resolves each system preset through
 * the OEM-tuned path with graceful fallbacks on older devices, which is what
 * gives the crisp feeling on Pixel/Samsung flagships. The mapping preserves
 * native semantics (selection stays selection, toggles stay toggles) so the
 * feel matches platform expectations rather than inventing new textures.
 *
 * Boundaries, per the Pulsar skill:
 * - Pulsar's `getPresets()` requires an `Activity` context (it casts). If the
 *   ambient context is not an Activity, or playback throws, we fall back to
 *   the framework [HapticFeedback.perform] mapping — every flow stays usable
 *   without Pulsar.
 * - No `forceHapticsSupportLevel`, no `enableHaptics(true)`: the system
 *   haptics toggle and capability tiers are respected as-is.
 * - Bounded presets only. No `RealtimeComposer` here: none of our current
 *   events (tabs, toggles, slider release, scrub ticks) need live modulation.
 * - Must be called from the click handler, not composition or LaunchedEffect.
 */
@Composable
fun rememberMoriHaptics(): (MoriHaptic) -> Unit {
    val context = LocalContext.current
    val framework = LocalHapticFeedback.current
    // Pulsar holds the context; re-create only when it changes. Construction
    // itself is cheap — preset cache fills on first play.
    val pulsar = remember(context) {
        val activity = context as? Activity ?: return@remember null
        runCatching { Pulsar(activity) }.getOrNull()
    }
    return remember(pulsar, framework) {
        { event ->
            val played = runCatching {
                val presets = pulsar?.getPresets() ?: return@runCatching false
                event.playWith(presets)
                true
            }.getOrDefault(false)
            if (!played) framework.perform(event)
        }
    }
}

/**
 * Dispatches one semantic event to its Pulsar system preset. System presets
 * (not the playful named ones like `hammer`/`dogBark`) so UI feedback keeps
 * platform meaning and intensity on every OEM.
 */
private fun MoriHaptic.playWith(presets: PresetsWrapper) {
    when (this) {
        MoriHaptic.Select -> presets.systemSelection()
        MoriHaptic.ToggleOn -> presets.systemToggleOn()
        MoriHaptic.ToggleOff -> presets.systemToggleOff()
        MoriHaptic.Tick -> presets.systemSegmentTick()
        MoriHaptic.FrequentTick -> presets.systemSegmentFrequentTick()
        MoriHaptic.Detent -> presets.systemPrimitiveTick()
        MoriHaptic.Confirm -> presets.systemConfirm()
        MoriHaptic.Reject -> presets.systemNotificationError()
        MoriHaptic.LongPress -> presets.systemLongPress()
    }
}
