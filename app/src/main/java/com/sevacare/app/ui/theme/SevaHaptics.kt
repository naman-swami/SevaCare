package com.sevacare.app.ui.theme

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Contextual Spatial Haptics Wrapper (Modern UI Blueprint 2026, Section 8.6).
 *
 * Mapped semantically:
 * - tick(): scanner bounding box lock, card press, tab switch
 * - confirm(): successful medication record, verified document save
 * - reject(): genuine system errors or critical warnings (never for quiz missteps)
 * - toggle(on): switch states
 * - ramp(progress): rising intensity ticks for SOS hold
 */
class SevaHaptics(private val view: android.view.View) {

    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm() {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun reject() {
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun toggle(on: Boolean) {
        if (on) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun pulseProgress(progress: Float) {
        // Subtle micro-tick during hold
        if (progress > 0.05f) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}

val LocalSevaHaptics = compositionLocalOf<SevaHaptics> {
    error("No SevaHaptics provided in CompositionLocalProvider")
}

@Composable
fun rememberSevaHaptics(): SevaHaptics {
    val view = LocalView.current
    return remember(view) { SevaHaptics(view) }
}
