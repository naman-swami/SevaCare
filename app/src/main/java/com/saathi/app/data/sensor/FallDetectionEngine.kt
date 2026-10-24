package com.saathi.app.data.sensor

import kotlin.math.sqrt

data class AccelReading(
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
) {
    /** Total acceleration magnitude normalized to g-units (1.0g = 9.80665 m/s^2) */
    val gMagnitude: Float
        get() = (sqrt((x * x + y * y + z * z).toDouble()) / 9.80665).toFloat()
}

enum class FallStage {
    MONITORING,
    FREE_FALL_DETECTED,
    IMPACT_DETECTED,
    STILLNESS_DETECTED,
    ALARM_30S_COUNTDOWN,
    ALERT_SENT,
    CANCELLED
}

/**
 * Section 4.4 & Criterion 17:
 * Pure Kotlin algorithmic core for elder fall detection.
 * Testable with trace replays for walking, sitting, phone drops, and real falls.
 */
class FallDetectionEngine(
    var isCharging: Boolean = false,
    var isScreenOn: Boolean = false,
    var freeFallThresholdG: Float = 0.5f,
    var impactThresholdG: Float = 2.5f,
    var stillnessVarianceThresholdG: Float = 0.15f
) {
    var stage: FallStage = FallStage.MONITORING
        private set

    private var freeFallStartMs: Long = 0L
    private var impactTimeMs: Long = 0L
    private val stillnessBuffer = mutableListOf<AccelReading>()
    private var alarmTriggerTimeMs: Long = 0L

    var onFallConfirmedUnresponsive: (() -> Unit)? = null

    /**
     * Feed a sensor reading into the detection state machine.
     * Returns the updated FallStage.
     */
    fun processSample(reading: AccelReading): FallStage {
        // Suppress false positives while device is charging or actively used
        if (isCharging || isScreenOn) {
            stage = FallStage.MONITORING
            resetHeuristics()
            return stage
        }

        val g = reading.gMagnitude

        when (stage) {
            FallStage.MONITORING -> {
                if (g < freeFallThresholdG) {
                    freeFallStartMs = reading.timestampMs
                    stage = FallStage.FREE_FALL_DETECTED
                }
            }

            FallStage.FREE_FALL_DETECTED -> {
                val freeFallDuration = reading.timestampMs - freeFallStartMs
                if (g >= impactThresholdG && freeFallDuration >= 80L) {
                    // Transition to impact
                    impactTimeMs = reading.timestampMs
                    stillnessBuffer.clear()
                    stage = FallStage.IMPACT_DETECTED
                } else if (g >= freeFallThresholdG && freeFallDuration < 80L) {
                    // Aborted drop / jitter
                    stage = FallStage.MONITORING
                } else if (freeFallDuration > 1500L) {
                    // Unrealistically long zero-g (e.g. sensor disconnect)
                    stage = FallStage.MONITORING
                }
            }

            FallStage.IMPACT_DETECTED -> {
                stillnessBuffer.add(reading)
                val elapsedAfterImpact = reading.timestampMs - impactTimeMs

                if (elapsedAfterImpact >= 5000L) { // 5 seconds of post-impact stillness check
                    // Check if variance is below stillness threshold
                    val avgG = stillnessBuffer.map { it.gMagnitude }.average().toFloat()
                    val variance = stillnessBuffer.map {
                        val diff = it.gMagnitude - avgG
                        diff * diff
                    }.average()

                    val stdDev = sqrt(variance).toFloat()

                    // Must be resting near 1.0g with minimal movement
                    if (avgG in 0.7f..1.3f && stdDev < stillnessVarianceThresholdG) {
                        stage = FallStage.ALARM_30S_COUNTDOWN
                        alarmTriggerTimeMs = reading.timestampMs
                    } else {
                        // Movement detected (person picked up phone, or walking continued)
                        stage = FallStage.MONITORING
                        resetHeuristics()
                    }
                }
            }

            FallStage.ALARM_30S_COUNTDOWN -> {
                val countdownElapsed = reading.timestampMs - alarmTriggerTimeMs
                if (countdownElapsed >= 30_000L) { // 30 seconds expired without cancel
                    stage = FallStage.ALERT_SENT
                    onFallConfirmedUnresponsive?.invoke()
                }
            }

            FallStage.ALERT_SENT, FallStage.CANCELLED -> {
                // Terminal states until reset
            }
            else -> {}
        }

        return stage
    }

    /**
     * User pressed "I'm OK" / Cancel during the 30-second window.
     */
    fun cancelCountdown() {
        if (stage == FallStage.ALARM_30S_COUNTDOWN) {
            stage = FallStage.CANCELLED
            resetHeuristics()
        }
    }

    fun reset() {
        stage = FallStage.MONITORING
        resetHeuristics()
    }

    private fun resetHeuristics() {
        freeFallStartMs = 0L
        impactTimeMs = 0L
        stillnessBuffer.clear()
        alarmTriggerTimeMs = 0L
    }
}
