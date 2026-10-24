package com.sevacare.app.data.repository

import com.sevacare.app.data.ai.ConflictDetector
import com.sevacare.app.data.ai.ExpiryEngine
import com.sevacare.app.data.ai.IndicationVerifier
import com.sevacare.app.data.ai.MedicineKnowledgeDataset
import com.sevacare.app.data.ai.Verdict
import com.sevacare.app.data.db.ConflictDao
import com.sevacare.app.data.db.ExpiredMedicineDao
import com.sevacare.app.data.db.FamilyPairDao
import com.sevacare.app.data.db.MedicineDao
import com.sevacare.app.data.db.ScheduleDao
import com.sevacare.app.data.model.ConflictReportEntity
import com.sevacare.app.data.model.ExpiredMedicineEntity
import com.sevacare.app.data.model.Medicine
import com.sevacare.app.data.model.ScheduleDraft
import com.sevacare.app.data.model.ScheduleEntity
import com.sevacare.app.data.model.SuspiciousMedicineEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AddScheduleResult {
    data class AddedDirectly(val schedule: ScheduleEntity) : AddScheduleResult()
    data class FlaggedSuspicious(val entry: SuspiciousMedicineEntity) : AddScheduleResult()
    data class ConflictDetected(val report: ConflictReportEntity, val isFamilyPaired: Boolean) : AddScheduleResult()
}

/**
 * Repository orchestrating medicine knowledge, AI indication verification,
 * cross-medicine conflict detection, family approval routing, and schedule events.
 */
class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val scheduleDao: ScheduleDao,
    private val conflictDao: ConflictDao,
    private val familyPairDao: FamilyPairDao,
    private val indicationVerifier: IndicationVerifier = IndicationVerifier(),
    private val conflictDetector: ConflictDetector = ConflictDetector()
) {

    val activeSchedules: Flow<List<ScheduleEntity>> = scheduleDao.getActiveSchedulesFlow()
    val pendingConflicts: Flow<List<ConflictReportEntity>> = conflictDao.getPendingConflictReportsFlow()
    val suspiciousMedicines: Flow<List<SuspiciousMedicineEntity>> = conflictDao.getSuspiciousMedicinesFlow()

    private val _scheduleEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scheduleEvents: SharedFlow<Unit> = _scheduleEvents.asSharedFlow()

    suspend fun searchMedicines(query: String): List<Medicine> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()
        return medicineDao.searchMedicines(trimmed)
    }

    suspend fun lookupByBarcodeOrText(barcode: String?, ocrText: String?): Medicine? {
        val all = medicineDao.getAllMedicines()

        // 1. Try exact barcode match if present
        if (!barcode.isNullOrBlank()) {
            val byBarcode = all.firstOrNull { it.barcode == barcode.trim() }
            if (byBarcode != null) return byBarcode
        }

        // 2. Try matching brand name in OCR text
        if (!ocrText.isNullOrBlank()) {
            val cleanOcr = ocrText.lowercase()
            for (med in all) {
                val brand = med.brandName.lowercase().trim()
                if (cleanOcr.contains(brand)) {
                    return med
                }
            }
            // Fallback: match generic salt
            for (med in all) {
                val saltRoot = med.normalizedSalt.lowercase().trim()
                if (saltRoot.length >= 4 && cleanOcr.contains(saltRoot)) {
                    return med
                }
            }
        }

        return null
    }

    suspend fun addMedicineToSchedule(draft: ScheduleDraft, medicine: Medicine): AddScheduleResult {
        // Step 1: AI Indication Verification
        val verdict = indicationVerifier.verify(medicine, draft.reason)
        if (verdict is Verdict.Suspicious) {
            val suspiciousEntry = SuspiciousMedicineEntity(
                medicineId = medicine.id,
                medicineName = medicine.brandName,
                statedReason = draft.reason ?: "Unspecified",
                contradictionScore = verdict.score,
                explanation = verdict.explanation
            )
            conflictDao.insertSuspiciousMedicine(suspiciousEntry)
            return AddScheduleResult.FlaggedSuspicious(suspiciousEntry)
        }

        // Step 2: Cross-Medicine Conflict Detection
        val activeSchedulesList = scheduleDao.getActiveSchedules()
        val conflictReport = conflictDetector.analyze(activeSchedulesList, draft)

        if (conflictReport.hasConflicts) {
            val primaryConflict = conflictReport.conflicts.first()
            val interaction = primaryConflict.interaction
            val pairs = familyPairDao.getVerifiedPairs()
            val isPaired = pairs.isNotEmpty()

            val adjustedCandidate = conflictReport.correctedList.last()

            val reportEntity = ConflictReportEntity(
                candidateMedicineId = medicine.id,
                candidateMedicineName = medicine.brandName,
                conflictingMedicineName = primaryConflict.activeMedicine.medicineName,
                severity = interaction.severity,
                interactionReason = interaction.clinicalEffect,
                correctedRecommendation = interaction.recommendation,
                proposedScheduleTime = adjustedCandidate.timeOfDay,
                durationDays = draft.durationDays,
                statedReason = draft.reason,
                status = "PENDING_APPROVAL"
            )
            conflictDao.insertConflictReport(reportEntity)

            return AddScheduleResult.ConflictDetected(reportEntity, isPaired)
        }

        // Step 3: No conflicts — direct addition
        val schedule = ScheduleEntity(
            medicineId = medicine.id,
            medicineName = medicine.brandName,
            timeOfDay = draft.timeOfDay,
            durationDays = draft.durationDays,
            statedReason = draft.reason,
            status = "ACTIVE"
        )
        scheduleDao.insertSchedule(schedule)
        _scheduleEvents.tryEmit(Unit)
        return AddScheduleResult.AddedDirectly(schedule)
    }

    suspend fun approveConflictReport(reportId: String): Boolean {
        val report = conflictDao.getConflictReportById(reportId) ?: return false
        conflictDao.updateConflictReportStatus(reportId, "APPROVED")

        val schedule = ScheduleEntity(
            medicineId = report.candidateMedicineId,
            medicineName = report.candidateMedicineName,
            timeOfDay = report.proposedScheduleTime,
            durationDays = report.durationDays,
            statedReason = report.statedReason,
            status = "ACTIVE",
            adjustmentReason = "Approved with safe schedule: ${report.correctedRecommendation}"
        )
        scheduleDao.insertSchedule(schedule)
        _scheduleEvents.tryEmit(Unit)
        return true
    }

    suspend fun rejectConflictReport(reportId: String): Boolean {
        val report = conflictDao.getConflictReportById(reportId) ?: return false
        conflictDao.updateConflictReportStatus(reportId, "REJECTED")
        return true
    }

    suspend fun markMedicineTaken(scheduleId: String, expiredDao: ExpiredMedicineDao? = null): Boolean {
        val schedule = scheduleDao.getScheduleById(scheduleId) ?: return false
        scheduleDao.delete(schedule)
        expiredDao?.insert(ExpiredMedicineEntity.from(schedule, outcome = "TAKEN"))
        _scheduleEvents.tryEmit(Unit)
        return true
    }

    suspend fun markMedicineSkipped(scheduleId: String, expiredDao: ExpiredMedicineDao? = null): Boolean {
        val schedule = scheduleDao.getScheduleById(scheduleId) ?: return false
        scheduleDao.delete(schedule)
        expiredDao?.insert(ExpiredMedicineEntity.from(schedule, outcome = "SKIPPED"))
        _scheduleEvents.tryEmit(Unit)
        return true
    }

    suspend fun expireDueMedicines(
        now: java.time.LocalDateTime = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault()),
        expiredDao: ExpiredMedicineDao? = null
    ): Int {
        val sqlPassed = scheduleDao.findDueBefore(now)
        val allActive = scheduleDao.getActiveSchedules()
        val enginePassed = allActive.filter { ExpiryEngine.isDuePassed(it, now) }
        val passed = (sqlPassed + enginePassed).distinctBy { it.id }

        passed.forEach { row ->
            scheduleDao.delete(row)
            expiredDao?.insert(ExpiredMedicineEntity.from(row, expiredAt = now, outcome = "EXPIRED"))
        }
        if (passed.isNotEmpty()) {
            _scheduleEvents.tryEmit(Unit)
        }
        return passed.size
    }
}
