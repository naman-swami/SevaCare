package com.sevacare.app

import com.sevacare.app.data.sensor.AccelReading
import com.sevacare.app.data.sensor.FallDetectionEngine
import com.sevacare.app.data.sensor.FallStage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Acceptance Criterion 17:
 * "Fall detection: recorded sensor traces for a fall are detected while walking,
 * sitting and a phone drop are rejected; the 30-second cancel works; SMS only after no response;
 * no ambulance call."
 */
class FallDetectionTest {

    private lateinit var engine: FallDetectionEngine

    @Before
    fun setup() {
        engine = FallDetectionEngine()
    }

    @Test
    fun testRealFallTrace_TriggersCountdown_ThenSendsAlertAfter30Seconds() {
        var alertSent = false
        engine.onFallConfirmedUnresponsive = { alertSent = true }

        var t = 1000L

        // 1. Initial standing/stillness (~1g)
        for (i in 0 until 10) {
            engine.processSample(AccelReading(t, 0f, 0f, 9.8f))
            t += 50
        }
        assertEquals(FallStage.MONITORING, engine.stage)

        // 2. Free fall: < 0.5g for 150ms
        for (i in 0 until 3) {
            engine.processSample(AccelReading(t, 0.5f, 0.5f, 0.5f)) // ~0.08g
            t += 50
        }
        assertEquals(FallStage.FREE_FALL_DETECTED, engine.stage)

        // 3. Impact spike: > 2.5g (e.g. 3.2g)
        engine.processSample(AccelReading(t, 20f, 20f, 15f)) // ~3.2g
        t += 50
        assertEquals(FallStage.IMPACT_DETECTED, engine.stage)

        // 4. Post-impact stillness: ~1g resting on floor for 5 seconds
        for (i in 0 until 100) {
            engine.processSample(AccelReading(t, 0.1f, 0.1f, 9.8f))
            t += 50
        }
        assertEquals("Stillness after impact must enter 30s countdown", FallStage.ALARM_30S_COUNTDOWN, engine.stage)
        assertFalse("Alert must NOT be sent before 30 seconds expire", alertSent)

        // 5. 30 seconds elapse without cancellation
        for (i in 0 until 600) {
            engine.processSample(AccelReading(t, 0.1f, 0.1f, 9.8f))
            t += 50
        }

        assertEquals("Unresponsive elder triggers ALERT_SENT stage", FallStage.ALERT_SENT, engine.stage)
        assertTrue("Circle alert must be dispatched after 30 seconds of no response", alertSent)
    }

    @Test
    fun testRealFall_CancelledWithin30Seconds_SendsNoAlert() {
        var alertSent = false
        engine.onFallConfirmedUnresponsive = { alertSent = true }

        var t = 1000L

        // Free fall + Impact + 5s Stillness
        for (i in 0 until 3) {
            engine.processSample(AccelReading(t, 0.5f, 0.5f, 0.5f))
            t += 50
        }
        engine.processSample(AccelReading(t, 20f, 20f, 15f))
        t += 50
        for (i in 0 until 100) {
            engine.processSample(AccelReading(t, 0.1f, 0.1f, 9.8f))
            t += 50
        }

        assertEquals(FallStage.ALARM_30S_COUNTDOWN, engine.stage)

        // Elder hears "Are you okay?" and taps Cancel at 10 seconds
        t += 10_000L
        engine.cancelCountdown()

        assertEquals(FallStage.CANCELLED, engine.stage)
        assertFalse("Cancelled fall must NEVER send an alert", alertSent)
    }

    @Test
    fun testWalkingTrace_IsRejected() {
        var alertSent = false
        engine.onFallConfirmedUnresponsive = { alertSent = true }

        var t = 1000L
        // Rhythmic walking oscillations between 0.8g and 1.3g (never sustained < 0.5g)
        for (step in 0 until 50) {
            val zAcc = if (step % 2 == 0) 12.0f else 8.0f
            engine.processSample(AccelReading(t, 1.0f, 1.0f, zAcc))
            t += 50
        }

        assertEquals(FallStage.MONITORING, engine.stage)
        assertFalse(alertSent)
    }

    @Test
    fun testSittingDownTrace_IsRejected() {
        var alertSent = false
        engine.onFallConfirmedUnresponsive = { alertSent = true }

        var t = 1000L
        // Sitting down: smooth deceleration from 9.8 m/s^2 to 13 m/s^2 (~1.3g peak, never reaching 2.5g)
        engine.processSample(AccelReading(t, 0f, 0f, 9.8f)); t += 100
        engine.processSample(AccelReading(t, 0f, 0f, 11.5f)); t += 100
        engine.processSample(AccelReading(t, 0f, 0f, 13.0f)); t += 100
        engine.processSample(AccelReading(t, 0f, 0f, 9.8f)); t += 100

        assertEquals(FallStage.MONITORING, engine.stage)
        assertFalse(alertSent)
    }

    @Test
    fun testPhoneDropWithImmediatePickup_IsRejected() {
        var alertSent = false
        engine.onFallConfirmedUnresponsive = { alertSent = true }

        var t = 1000L
        // Free fall + impact
        for (i in 0 until 3) {
            engine.processSample(AccelReading(t, 0.5f, 0.5f, 0.5f)); t += 50
        }
        engine.processSample(AccelReading(t, 20f, 20f, 15f)); t += 50

        // But instead of stillness, user immediately picks up phone (high motion/variance)
        for (i in 0 until 100) {
            val motion = if (i % 2 == 0) 18.0f else 4.0f
            engine.processSample(AccelReading(t, motion, motion, motion)); t += 50
        }

        // Must reject fall and return to MONITORING
        assertEquals(FallStage.MONITORING, engine.stage)
        assertFalse(alertSent)
    }
}
