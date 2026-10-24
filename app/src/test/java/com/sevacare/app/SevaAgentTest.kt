package com.sevacare.app

import com.sevacare.app.data.agent.CheckInResponse
import com.sevacare.app.data.agent.DeviationRung
import com.sevacare.app.data.agent.InMemorySevaAgentStorage
import com.sevacare.app.data.agent.SevaAgentManager
import com.sevacare.app.data.model.CircleContact
import com.sevacare.app.data.model.CircleRole
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Acceptance Criterion 15:
 * "Seva Agent: 7 baseline days then two missed check-ins sends exactly one Circle alert;
 * cancelling at rung 1 sends none."
 */
class SevaAgentTest {

    private lateinit var storage: InMemorySevaAgentStorage
    private lateinit var agent: SevaAgentManager
    private val testContacts = listOf(
        CircleContact(id = 1, name = "Son", phone = "+919876543210", role = CircleRole.FAMILY, isPrimary = true),
        CircleContact(id = 2, name = "Neighbour Sharma Ji", phone = "+919876543211", role = CircleRole.NEIGHBOUR_BUDDY)
    )

    @Before
    fun setup() {
        storage = InMemorySevaAgentStorage()
        agent = SevaAgentManager(storage, context = null)
    }

    @Test
    fun testSevenBaselineDays_ThenTwoMissedCheckins_SendsExactlyOneAlert() {
        var alertCount = 0
        var alertMessageReceived = ""
        agent.onCircleAlertTriggered = { _, msg ->
            alertCount++
            alertMessageReceived = msg
        }

        val baseCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 8, 0, 0)
        }

        // 1. Simulate 7 baseline days where elder checks in on time (8:00 AM)
        for (day in 1..7) {
            val checkInTime = baseCalendar.timeInMillis + ((day - 1) * 24L * 60 * 60 * 1000)
            val rung = agent.recordCheckInResponse(CheckInResponse.TOOK_IT, testContacts, checkInTime)
            assertEquals("Baseline day $day should be normal", DeviationRung.NORMAL, rung)
            assertEquals(0, alertCount)
        }

        // Verify baseline median learned (8:00 AM = 480 minutes)
        val medianMinutes = agent.getMedianRoutineMinute()
        assertNotNull("Median routine should be computed after 7 days", medianMinutes)
        assertEquals(480, medianMinutes)

        // 2. Day 8 morning: First missed check-in -> Rung 1 (Louder on-device prompt)
        val day8Time = baseCalendar.timeInMillis + (7L * 24 * 60 * 60 * 1000)
        val firstMissRung = agent.onCheckInMissed(testContacts, day8Time)
        assertEquals("First missed checkin must enter Rung 1", DeviationRung.RUNG_1_LOUD_PROMPT, firstMissRung)
        assertEquals("No circle alert should be sent at rung 1", 0, alertCount)

        // 3. 10 minutes later: Second missed check-in -> Rung 2 (Circle alert dispatched)
        val day8TimePlus10Min = day8Time + (10L * 60 * 1000)
        val secondMissRung = agent.onCheckInMissed(testContacts, day8TimePlus10Min)
        assertEquals("Second missed checkin must enter Rung 2", DeviationRung.RUNG_2_CIRCLE_ALERT, secondMissRung)
        assertEquals("Exactly one circle alert must be sent", 1, alertCount)
        assertTrue(alertMessageReceived.contains("Papa has not checked in"))
    }

    @Test
    fun testCancellingAtRung1_SendsZeroCircleAlerts() {
        var alertCount = 0
        agent.onCircleAlertTriggered = { _, _ -> alertCount++ }

        val timeNow = System.currentTimeMillis()

        // 1. First missed check-in -> enters Rung 1
        val rung1 = agent.onCheckInMissed(testContacts, timeNow)
        assertEquals(DeviationRung.RUNG_1_LOUD_PROMPT, rung1)

        // 2. Elder wakes up and taps Cancel / Acknowledged
        agent.cancelAlertLadder()
        assertEquals(0, agent.consecutiveMissedCheckIns)

        // 3. Verify no alert was dispatched to family
        assertEquals(0, alertCount)
    }

    @Test
    fun testPausedCheckIns_SendZeroAlerts() {
        var alertCount = 0
        agent.onCircleAlertTriggered = { _, _ -> alertCount++ }

        val timeNow = System.currentTimeMillis()

        // Pause check-ins for 5 days (e.g. traveling to village)
        agent.pauseCheckInsUntil(days = 5, fromTimeMs = timeNow)
        assertTrue(agent.isPaused(timeNow + 1000L))

        // Missed check-ins during pause return PAUSED state and send 0 alerts
        val rung = agent.onCheckInMissed(testContacts, timeNow + 3600_000L)
        assertEquals(DeviationRung.PAUSED, rung)
        assertEquals(0, alertCount)
    }
}
