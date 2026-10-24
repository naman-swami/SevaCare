package com.saathi.app

import com.saathi.app.data.ai.ConflictDetector
import com.saathi.app.data.model.ScheduleDraft
import com.saathi.app.data.model.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit verification test for ConflictDetector:
 * 1. Known interacting pair in dataset generates a ConflictReport with clinical reasons.
 * 2. Interacting pair requiring separation (e.g. Atorvastatin + Clarithromycin) generates staggered schedule.
 * 3. Disjoint pair produces empty conflict report with direct addition.
 */
class ConflictDetectorTest {

    private val detector = ConflictDetector()

    @Test
    fun testKnownInteractingPairProducesConflictReport() {
        val activeSchedules = listOf(
            ScheduleEntity(
                medicineId = 10,
                medicineName = "Warfarin",
                timeOfDay = "Morning (08:00 AM)",
                durationDays = 30
            )
        )

        val candidate = ScheduleDraft(
            medicineId = 20,
            medicineName = "Aspirin",
            timeOfDay = "Morning (08:00 AM)",
            durationDays = 15,
            reason = "Chest discomfort"
        )

        val report = detector.analyze(activeSchedules, candidate)

        assertTrue("Known interacting pair Warfarin + Aspirin must report conflicts", report.hasConflicts)
        assertEquals("Must report exactly 1 interaction", 1, report.conflicts.size)

        val conflict = report.conflicts.first()
        assertEquals("Severity must be MAJOR", "MAJOR", conflict.interaction.severity)
        assertTrue("Clinical effect must cite hemorrhage / bleeding", conflict.interaction.clinicalEffect.contains("hemorrhage", ignoreCase = true))

        val correctedCandidate = report.correctedList.last()
        assertEquals("Candidate in corrected list must have PENDING_APPROVAL status", "PENDING_APPROVAL", correctedCandidate.status)
        assertNotNull("Corrected candidate must carry adjustment reason metadata", correctedCandidate.adjustmentReason)
    }

    @Test
    fun testInteractingPairWithSeparationStaggersTiming() {
        val activeSchedules = listOf(
            ScheduleEntity(
                medicineId = 30,
                medicineName = "Atorvastatin",
                timeOfDay = "Morning (08:00 AM)",
                durationDays = 30
            )
        )

        val candidate = ScheduleDraft(
            medicineId = 40,
            medicineName = "Clarithromycin",
            timeOfDay = "Morning (08:00 AM)",
            durationDays = 7,
            reason = "Chest infection"
        )

        val report = detector.analyze(activeSchedules, candidate)

        assertTrue("Atorvastatin + Clarithromycin must be flagged", report.hasConflicts)
        val correctedCandidate = report.correctedList.last()

        // Timing should be staggered away from Morning
        assertTrue(
            "Scheduled time must be staggered to Night (08:00 PM)",
            correctedCandidate.timeOfDay.contains("Night", ignoreCase = true) || correctedCandidate.timeOfDay.contains("PM", ignoreCase = true)
        )
        assertTrue(
            "Adjustment reason must mention staggering by hours",
            correctedCandidate.adjustmentReason?.contains("staggered", ignoreCase = true) == true
        )
    }

    @Test
    fun testDisjointNonInteractingPairProducesEmptyReport() {
        val activeSchedules = listOf(
            ScheduleEntity(
                medicineId = 50,
                medicineName = "Metformin",
                timeOfDay = "Morning (08:00 AM)",
                durationDays = 30
            )
        )

        val candidate = ScheduleDraft(
            medicineId = 60,
            medicineName = "Paracetamol",
            timeOfDay = "Morning (08:00 AM)",
            durationDays = 3,
            reason = "Fever"
        )

        val report = detector.analyze(activeSchedules, candidate)

        assertFalse("Metformin + Paracetamol must have NO conflicts", report.hasConflicts)
        assertTrue("Conflicts list must be empty", report.conflicts.isEmpty())
        assertEquals("Corrected list must include both active and candidate", 2, report.correctedList.size)
        assertEquals("New candidate schedule must be ACTIVE", "ACTIVE", report.correctedList.last().status)
    }
}
