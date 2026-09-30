package com.mori.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Semantic haptic events for the app, mapped to platform-tuned effects.
 *
 * One seam for all touch feedback so intensity stays consistent: light ticks
 * for selection, distinct on/off for toggles, a confirm pulse only when an
 * action completes, and reject only on failure. The framework resolves each
 * type to the OEM-tuned effect (Pixel, Samsung) and honors the system
 * haptics toggle, so there is deliberately no app-level switch or custom
 * waveform here — custom waveforms ignore user settings and vary wildly
 * across actuators.
 *
 * Usage: `val haptics = LocalHapticFeedback.current` then
 * `haptics.perform(MoriHaptic.TabSelect)` inside the click handler, not in
 * composition or `LaunchedEffect`.
 */
enum class MoriHaptic {
    /** Tab, chip, segmented option, FAB, or card selected. */
    Select,

    /** Switch or toggle moved to on. */
    ToggleOn,

    /** Switch or toggle moved to off. */
    ToggleOff,

    /** Discrete step: slider detent, scrubbed page, frequent tick. */
    Tick,

    /** Frequent steps while scrubbing (pages, percentages). */
    FrequentTick,

    /** An action completed: refresh done, shelf created, cache cleared. */
    Confirm,

    /** Highest-priority CTA fired: resume FAB, menu Read. Heavier impact. */
    PrimaryAction,

    /** An action failed or was rejected. */
    Reject,

    /** Long-press that opens an action (menus, reordering). */
    LongPress,
}

/**
 * Pure mapping from semantic event to platform effect. Pure so it stays
 * unit-testable without a device or Robolectric.
 */
fun MoriHaptic.type(): HapticFeedbackType = when (this) {
    MoriHaptic.Select -> HapticFeedbackType.SegmentTick
    MoriHaptic.ToggleOn -> HapticFeedbackType.ToggleOn
    MoriHaptic.ToggleOff -> HapticFeedbackType.ToggleOff
    MoriHaptic.Tick -> HapticFeedbackType.SegmentTick
    MoriHaptic.FrequentTick -> HapticFeedbackType.SegmentFrequentTick
    MoriHaptic.Confirm -> HapticFeedbackType.Confirm
    MoriHaptic.PrimaryAction -> HapticFeedbackType.Confirm
    MoriHaptic.Reject -> HapticFeedbackType.Reject
    MoriHaptic.LongPress -> HapticFeedbackType.LongPress
}

/** Performs the semantic event on this haptic channel. */
fun HapticFeedback.perform(event: MoriHaptic) {
    performHapticFeedback(event.type())
}
