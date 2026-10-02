package com.mori.core.designsystem

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.swmansion.pulsar.Pulsar
import com.swmansion.pulsar.presets.PresetsWrapper
import com.swmansion.pulsar.types.CompatibilityMode

/** Ambient toggle controlling whether semantic haptic feedback is dispatched. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Pulsar-backed player for [MoriHaptic] events.
 *
 * Pulsar 1.3.0 (`com.swmansion:pulsar`, MIT — credited in the licenses
 * screen) resolves each system preset through the OEM-tuned path with
 * graceful fallbacks on older devices, which is what gives the crisp feeling
 * on Pixel/Samsung flagships. Every semantic event gets its own weight
 * instead of one shared tap:
 *
 * | Event | Preset | Feel |
 * |---|---|---|
 * | Select | systemSelection | light selection blip |
 * | Tick | systemSegmentTick | discrete step |
 * | FrequentTick | systemSegmentFrequentTick | light scrub ticks |
 * | ToggleOn/Off | systemToggleOn/Off | distinct on/off |
 * | Confirm | systemNotificationSuccess | success chime |
 * | PrimaryAction | systemImpactMedium | firm CTA thud |
 * | Reject | systemNotificationError | error buzz |
 * | LongPress | systemLongPress | deep press |
 *
 * Boundaries, per the Pulsar skill:
 * - Capability tiers: below [CompatibilityMode.LIMITED_SUPPORT] (budget
 *   actuators, no amplitude control) everything routes to the framework
 *   mapping, which degrades gracefully instead of buzzing blindly.
 * - Pulsar's `getPresets()` requires an `Activity` context (it casts). If the
 *   ambient context is not an Activity, or playback throws, we fall back to
 *   the framework [HapticFeedback.perform] mapping — every flow stays usable
 *   without Pulsar.
 * - No `forceHapticsSupportLevel`, no `enableHaptics(true)`: the system
 *   haptics toggle and capability tiers are respected as-is.
 * - Bounded presets only. No `RealtimeComposer` here: none of our current
 *   events (tabs, toggles, slider release, scrub ticks) need live modulation.
 * - Must be called from the click handler, not composition or `LaunchedEffect`.
 * - Silenced cleanly when [LocalHapticsEnabled] resolves to false.
 */
@Composable
fun rememberMoriHaptics(): (MoriHaptic) -> Unit {
    val enabled = LocalHapticsEnabled.current
    if (!enabled) {
        return remember { {} }
    }
    val context = LocalContext.current
    val framework = LocalHapticFeedback.current
    // Pulsar holds the context; re-create only when it changes. Construction
    // itself is cheap — preset cache fills on first play.
    val pulsar = remember(context) {
        val activity = context as? Activity ?: return@remember null
        runCatching { Pulsar(activity) }.getOrNull()
    }
    // Tier once per instance: budget devices take the framework path below.
    val pulsarCapable = remember(pulsar) {
        runCatching {
            (pulsar?.hapticSupport() ?: CompatibilityMode.NO_SUPPORT) >=
                    CompatibilityMode.LIMITED_SUPPORT
        }.getOrDefault(false)
    }
    return remember(pulsar, framework, pulsarCapable) {
        { event ->
            val played = runCatching {
                if (!pulsarCapable) return@runCatching false
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
        MoriHaptic.Confirm -> presets.systemNotificationSuccess()
        MoriHaptic.PrimaryAction -> presets.systemImpactMedium()
        MoriHaptic.Reject -> presets.systemNotificationError()
        MoriHaptic.LongPress -> presets.systemLongPress()
    }
}
