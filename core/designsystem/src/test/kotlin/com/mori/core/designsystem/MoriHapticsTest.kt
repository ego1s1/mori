package com.mori.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class MoriHapticsTest {

    @Test
    fun selectionUsesLightTick() {
        assertEquals(HapticFeedbackType.SegmentTick, MoriHaptic.Select.type())
        assertEquals(HapticFeedbackType.SegmentTick, MoriHaptic.Tick.type())
    }

    @Test
    fun togglesHaveDistinctOnOff() {
        assertEquals(HapticFeedbackType.ToggleOn, MoriHaptic.ToggleOn.type())
        assertEquals(HapticFeedbackType.ToggleOff, MoriHaptic.ToggleOff.type())
    }

    @Test
    fun scrubbingUsesFrequentTick() {
        assertEquals(HapticFeedbackType.SegmentFrequentTick, MoriHaptic.FrequentTick.type())
    }

    @Test
    fun completionAndFailureAreDistinct() {
        assertEquals(HapticFeedbackType.Confirm, MoriHaptic.Confirm.type())
        assertEquals(HapticFeedbackType.Reject, MoriHaptic.Reject.type())
    }

    @Test
    fun longPressMapsThrough() {
        assertEquals(HapticFeedbackType.LongPress, MoriHaptic.LongPress.type())
    }
}
