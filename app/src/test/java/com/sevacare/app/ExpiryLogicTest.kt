package com.sevacare.app

import com.sevacare.app.data.ai.ExpiryEngine
import com.sevacare.app.data.model.ExpiredMedicineEntity
import com.sevacare.app.data.model.ScheduleEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ExpiryLogicTest {

    private val zoneId = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun testPastTimeScheduleIsDetectedAsDuePassed() {
        // Schedule is for 08:00 AM today
        val schedule = ScheduleEntity(
            id = "sched-past",
            medicineId = 1,
            medicineName = "Metformin 500mg",
            timeOfDay = "08:00 AM",
            durationDays = 30
        )

        // Current time is 11:30 AM today (past due)
        val now = LocalDateTime.of(today, LocalTime.of(11, 30))
        val isPassed = ExpiryEngine.isDuePassed(schedule, now, zoneId)

        assertTrue("08:00 AM schedule should be expired when checked at 11:30 AM", isPassed)

        val expired = ExpiredMedicineEntity.from(schedule, now, outcome = "EXPIRED")
        assertEquals("sched-past", expired.scheduleId)
        assertEquals("Metformin 500mg", expired.medicineName)
        assertEquals("EXPIRED", expired.outcome)
    }

    @Test
    fun testFutureTimeScheduleRemainsUntouched() {
        // Schedule is for 08:00 PM tonight
        val schedule = ScheduleEntity(
            id = "sched-future",
            medicineId = 2,
            medicineName = "Atorvastatin 20mg",
            timeOfDay = "08:00 PM",
            durationDays = 15
        )

        // Current time is 02:00 PM today (not yet due)
        val now = LocalDateTime.of(today, LocalTime.of(14, 0))
        val isPassed = ExpiryEngine.isDuePassed(schedule, now, zoneId)

        assertFalse("08:00 PM schedule should NOT be expired when checked at 02:00 PM", isPassed)
    }

    @Test
    fun testMidnightRolloverEdgeCase() {
        // Dose scheduled for 23:59 PM before midnight
        val todayEpochMs = LocalDateTime.of(today, LocalTime.of(12, 0))
            .atZone(zoneId).toInstant().toEpochMilli()
        val lateNightSchedule = ScheduleEntity(
            id = "sched-late",
            medicineId = 3,
            medicineName = "Pantoprazole 40mg",
            timeOfDay = "11:59 PM",
            durationDays = 7,
            createdAt = todayEpochMs
        )

        // At 23:58 PM, it is not yet passed
        val beforeMidnight = LocalDateTime.of(today, LocalTime.of(23, 58))
        assertFalse("11:59 PM dose must NOT expire at 23:58", ExpiryEngine.isDuePassed(lateNightSchedule, beforeMidnight, zoneId))

        // Right at 23:59 PM, it is due/passed
        val exactDoseTime = LocalDateTime.of(today, LocalTime.of(23, 59))
        assertTrue("11:59 PM dose is due at 23:59", ExpiryEngine.isDuePassed(lateNightSchedule, exactDoseTime, zoneId))

        // After midnight rollover into next day at 00:05 AM
        val nextDay = today.plusDays(1)
        val afterMidnight = LocalDateTime.of(nextDay, LocalTime.of(0, 5))
        assertTrue("Dose from yesterday night is definitely past due the next morning", ExpiryEngine.isDuePassed(lateNightSchedule, afterMidnight, zoneId))
    }

    @Test
    fun testExplicitDueEpochMsComparison() {
        val targetEpochMs = LocalDateTime.of(today, LocalTime.of(10, 0))
            .atZone(zoneId).toInstant().toEpochMilli()

        val schedule = ScheduleEntity(
            id = "sched-epoch",
            medicineId = 4,
            medicineName = "Amlodipine 5mg",
            timeOfDay = "Morning",
            durationDays = 30,
            dueEpochMs = targetEpochMs
        )

        // Checked at 09:59 AM
        val before = LocalDateTime.of(today, LocalTime.of(9, 59))
        assertFalse(ExpiryEngine.isDuePassed(schedule, before, zoneId))

        // Checked at 10:01 AM
        val after = LocalDateTime.of(today, LocalTime.of(10, 1))
        assertTrue(ExpiryEngine.isDuePassed(schedule, after, zoneId))
    }

    @Test
    fun testTakenOutcomeIsDistinctFromExpired() {
        val schedule = ScheduleEntity(
            id = "sched-taken",
            medicineId = 5,
            medicineName = "Levothyroxine 50mcg",
            timeOfDay = "Morning (07:00 AM)",
            durationDays = 60
        )

        val now = LocalDateTime.of(today, LocalTime.of(7, 15))
        val takenHistory = ExpiredMedicineEntity.from(schedule, now, outcome = "TAKEN")

        assertEquals("TAKEN", takenHistory.outcome)
        assertNotEquals("EXPIRED", takenHistory.outcome)
    }
}
