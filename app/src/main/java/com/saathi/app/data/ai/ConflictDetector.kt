package com.saathi.app.data.ai

import com.saathi.app.data.model.DrugInteractionEntity
import com.saathi.app.data.model.ScheduleDraft
import com.saathi.app.data.model.ScheduleEntity

data class DetectedConflict(
    val activeMedicine: ScheduleEntity,
    val candidateDraft: ScheduleDraft,
    val interaction: DrugInteractionEntity
)

data class ConflictReport(
    val hasConflicts: Boolean,
    val conflicts: List<DetectedConflict>,
    val correctedList: List<ScheduleEntity>,
    val summaryText: String
)

/**
 * Cross-Medicine Conflict Detection Engine.
 *
 * Implements:
 * 1. Pairwise interaction matrix evaluation across active and candidate regimens.
 * 2. Staggered time-slot generation for interacting pairs requiring separation.
 * 3. Self-explaining clinical reason metadata for family review.
 */
class ConflictDetector(
    private val dataset: MedicineKnowledgeDataset = MedicineKnowledgeDataset
) {

    fun analyze(
        activeSchedules: List<ScheduleEntity>,
        candidate: ScheduleDraft
    ): ConflictReport {
        val detected = mutableListOf<DetectedConflict>()

        for (active in activeSchedules) {
            val interaction = dataset.findInteraction(active.medicineName, candidate.medicineName)
            if (interaction != null) {
                detected.add(
                    DetectedConflict(
                        activeMedicine = active,
                        candidateDraft = candidate,
                        interaction = interaction
                    )
                )
            }
        }

        if (detected.isEmpty()) {
            val newSchedule = ScheduleEntity(
                medicineId = candidate.medicineId,
                medicineName = candidate.medicineName,
                timeOfDay = candidate.timeOfDay,
                durationDays = candidate.durationDays,
                statedReason = candidate.reason,
                status = "ACTIVE"
            )
            return ConflictReport(
                hasConflicts = false,
                conflicts = emptyList(),
                correctedList = activeSchedules + newSchedule,
                summaryText = "No drug-drug conflicts detected with active medicines."
            )
        }

        // Build corrected list with staggered timings and reason metadata
        val correctedList = buildCorrectedSchedules(activeSchedules, candidate, detected)
        val highestSeverity = detected.maxByOrNull { severityWeight(it.interaction.severity) }?.interaction?.severity ?: "MAJOR"
        val summary = "Detected ${detected.size} drug conflict(s) [$highestSeverity]. Corrected schedule generated with safe spacing."

        return ConflictReport(
            hasConflicts = true,
            conflicts = detected,
            correctedList = correctedList,
            summaryText = summary
        )
    }

    private fun buildCorrectedSchedules(
        active: List<ScheduleEntity>,
        candidate: ScheduleDraft,
        conflicts: List<DetectedConflict>
    ): List<ScheduleEntity> {
        val result = active.toMutableList()

        val primaryConflict = conflicts.first()
        val interaction = primaryConflict.interaction

        val adjustedTime = if (interaction.minSeparationHours > 0) {
            // Safely space out the candidate intake
            val lower = primaryConflict.activeMedicine.timeOfDay.lowercase()
            when {
                lower.contains("morning") || lower.contains("breakfast") || lower.contains("am") -> "Night (08:00 PM)"
                lower.contains("afternoon") || lower.contains("lunch") || lower.contains("night") || lower.contains("pm") -> "Morning (08:00 AM)"
                else -> "Night (08:00 PM)"
            }
        } else {
            candidate.timeOfDay
        }

        val adjustmentReason = if (interaction.minSeparationHours > 0) {
            "Intake staggered by ${interaction.minSeparationHours} hours to avoid interaction with ${primaryConflict.activeMedicine.medicineName} (${interaction.clinicalEffect})."
        } else {
            "Requires family approval: co-administration with ${primaryConflict.activeMedicine.medicineName} has risk of ${interaction.clinicalEffect}."
        }

        val adjustedCandidate = ScheduleEntity(
            medicineId = candidate.medicineId,
            medicineName = candidate.medicineName,
            timeOfDay = adjustedTime,
            durationDays = candidate.durationDays,
            statedReason = candidate.reason,
            status = "PENDING_APPROVAL",
            adjustmentReason = adjustmentReason
        )

        result.add(adjustedCandidate)
        return result
    }

    private fun severityWeight(severity: String): Int {
        return when (severity.uppercase()) {
            "CONTRAINDICATED" -> 4
            "MAJOR" -> 3
            "MODERATE" -> 2
            "MINOR" -> 1
            else -> 0
        }
    }
}
