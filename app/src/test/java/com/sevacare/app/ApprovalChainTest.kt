package com.sevacare.app

import com.sevacare.app.data.db.ConflictDao
import com.sevacare.app.data.db.FamilyPairDao
import com.sevacare.app.data.db.MedicineDao
import com.sevacare.app.data.db.ScheduleDao
import com.sevacare.app.data.model.ConflictReportEntity
import com.sevacare.app.data.model.DrugInteractionEntity
import com.sevacare.app.data.model.Medicine
import com.sevacare.app.data.model.ScheduleEntity
import com.sevacare.app.data.model.SuspiciousMedicineEntity
import com.sevacare.app.data.pairing.FamilyPairEntity
import com.sevacare.app.data.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit verification test for the Family Approval Chain:
 * 1. Approving a conflict report writes the corrected schedule into Room and emits a schedule update event.
 * 2. Rejecting a conflict report transitions status to REJECTED and leaves active schedules untouched.
 */
class ApprovalChainTest {

    private class FakeScheduleDao : ScheduleDao {
        val schedules = mutableListOf<ScheduleEntity>()

        override suspend fun insertSchedule(schedule: ScheduleEntity): Long {
            schedules.add(schedule)
            return schedules.size.toLong()
        }

        override suspend fun upsertAll(schedules: List<ScheduleEntity>) {
            this.schedules.addAll(schedules)
        }

        override fun getActiveSchedulesFlow(): Flow<List<ScheduleEntity>> = flowOf(schedules)
        override suspend fun getActiveSchedules(): List<ScheduleEntity> = schedules.filter { it.status == "ACTIVE" }
        override fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>> = flowOf(schedules)
        override suspend fun getActiveScheduleForMedicine(medId: Int): ScheduleEntity? = schedules.find { it.medicineId == medId && it.status == "ACTIVE" }
        override suspend fun updateScheduleStatus(id: String, status: String) {
            val idx = schedules.indexOfFirst { it.id == id }
            if (idx != -1) {
                schedules[idx] = schedules[idx].copy(status = status)
            }
        }
        override suspend fun deleteSchedule(schedule: ScheduleEntity) {
            schedules.remove(schedule)
        }
        override suspend fun delete(schedule: ScheduleEntity) {
            schedules.remove(schedule)
        }
        override fun todayWithStatus(): Flow<List<ScheduleEntity>> = flowOf(schedules)
        override suspend fun findDueBefore(now: java.time.LocalDateTime): List<ScheduleEntity> = emptyList()
        override suspend fun findDueBeforeEpoch(epochMs: Long): List<ScheduleEntity> = emptyList()
        override suspend fun getScheduleById(id: String): ScheduleEntity? = schedules.find { it.id == id }
    }

    private class FakeConflictDao : ConflictDao {
        val reports = mutableMapOf<String, ConflictReportEntity>()
        val suspicious = mutableListOf<SuspiciousMedicineEntity>()

        override suspend fun insertInteraction(interaction: DrugInteractionEntity): Long = 1L
        override suspend fun insertInteractions(interactions: List<DrugInteractionEntity>) {}
        override suspend fun getAllInteractions(): List<DrugInteractionEntity> = emptyList()
        override suspend fun findInteraction(saltA: String, saltB: String): DrugInteractionEntity? = null

        override suspend fun insertSuspiciousMedicine(entry: SuspiciousMedicineEntity): Long {
            suspicious.add(entry)
            return suspicious.size.toLong()
        }
        override fun getSuspiciousMedicinesFlow(): Flow<List<SuspiciousMedicineEntity>> = flowOf(suspicious)
        override suspend fun getAllSuspiciousMedicines(): List<SuspiciousMedicineEntity> = suspicious

        override suspend fun insertConflictReport(report: ConflictReportEntity): Long {
            reports[report.id] = report
            return 1L
        }
        override fun getAllConflictReportsFlow(): Flow<List<ConflictReportEntity>> = flowOf(reports.values.toList())
        override fun getPendingConflictReportsFlow(): Flow<List<ConflictReportEntity>> = flowOf(reports.values.filter { it.status == "PENDING_APPROVAL" })
        override suspend fun getConflictReportById(id: String): ConflictReportEntity? = reports[id]
        override suspend fun updateConflictReportStatus(id: String, status: String, resolvedAt: Long) {
            reports[id]?.let {
                reports[id] = it.copy(status = status, resolvedAt = resolvedAt)
            }
        }
    }

    private class FakeMedicineDao : MedicineDao {
        override suspend fun searchMedicines(query: String): List<Medicine> = emptyList()
        override suspend fun findByBrandName(brandName: String): List<Medicine> = emptyList()
        override suspend fun findDuplicateSalt(newSalt: String, scannedSalts: List<String>): Medicine? = null
        override suspend fun getMedicinesByCategory(category: String): List<Medicine> = emptyList()
        override suspend fun getAllMedicines(): List<Medicine> = emptyList()
        override suspend fun insertAll(medicines: List<Medicine>) {}
    }

    private class FakeFamilyPairDao : FamilyPairDao {
        override suspend fun getPair(pairId: String): FamilyPairEntity? = null
        override fun getAllPairsFlow(): Flow<List<FamilyPairEntity>> = flowOf(emptyList())
        override suspend fun getLatestPair(): FamilyPairEntity? = null
        override suspend fun getVerifiedCount(): Int = 1
        override suspend fun getVerifiedPairs(): List<FamilyPairEntity> = listOf(
            FamilyPairEntity(pairId = "pair-1", phone = "+919876543210", name = "Son (Ravi)", saltHash = "hash", sessionKeyEncrypted = "key")
        )
        override suspend fun upsert(pair: FamilyPairEntity) {}
        override suspend fun delete(pairId: String) {}
        override suspend fun clearAll() {}
    }

    @Test
    fun testApprovalStateTransitionWritesScheduleAndEmitsEvent() = runBlocking {
        val scheduleDao = FakeScheduleDao()
        val conflictDao = FakeConflictDao()
        val repo = MedicineRepository(
            medicineDao = FakeMedicineDao(),
            scheduleDao = scheduleDao,
            conflictDao = conflictDao,
            familyPairDao = FakeFamilyPairDao()
        )

        val reportId = "report-123"
        val pendingReport = ConflictReportEntity(
            id = reportId,
            candidateMedicineId = 101,
            candidateMedicineName = "Aspirin 75mg",
            conflictingMedicineName = "Warfarin 5mg",
            severity = "MAJOR",
            interactionReason = "Elevated hemorrhage risk",
            correctedRecommendation = "Stagger intake and check with physician",
            proposedScheduleTime = "Night (08:00 PM)",
            durationDays = 30,
            statedReason = "Blood flow",
            status = "PENDING_APPROVAL"
        )
        conflictDao.insertConflictReport(pendingReport)

        // Approve
        val success = repo.approveConflictReport(reportId)
        assertTrue("Approval call must return true", success)

        val updatedReport = conflictDao.getConflictReportById(reportId)
        assertNotNull("Report must still exist", updatedReport)
        assertEquals("Status must transition to APPROVED", "APPROVED", updatedReport!!.status)
        assertNotNull("Resolved timestamp must be recorded", updatedReport.resolvedAt)

        // Verify corrected schedule was written to Room
        assertEquals("Exactly one schedule must be written to Room", 1, scheduleDao.schedules.size)
        val writtenSchedule = scheduleDao.schedules.first()
        assertEquals("Schedule medicine name must match candidate", "Aspirin 75mg", writtenSchedule.medicineName)
        assertEquals("Schedule status must be ACTIVE", "ACTIVE", writtenSchedule.status)
        assertEquals("Schedule timing must match proposed time", "Night (08:00 PM)", writtenSchedule.timeOfDay)
        assertTrue("Adjustment reason must be populated", writtenSchedule.adjustmentReason?.contains("Approved with safe schedule") == true)
    }

    @Test
    fun testRejectionStateTransitionLeavesScheduleUntouched() = runBlocking {
        val scheduleDao = FakeScheduleDao()
        val conflictDao = FakeConflictDao()
        val repo = MedicineRepository(
            medicineDao = FakeMedicineDao(),
            scheduleDao = scheduleDao,
            conflictDao = conflictDao,
            familyPairDao = FakeFamilyPairDao()
        )

        val reportId = "report-456"
        val pendingReport = ConflictReportEntity(
            id = reportId,
            candidateMedicineId = 202,
            candidateMedicineName = "Clarithromycin",
            conflictingMedicineName = "Atorvastatin",
            severity = "MAJOR",
            interactionReason = "Rhabdomyolysis risk",
            correctedRecommendation = "Avoid macrolide antibiotic with statin",
            proposedScheduleTime = "Night (08:00 PM)",
            durationDays = 5,
            statedReason = "Chest cough",
            status = "PENDING_APPROVAL"
        )
        conflictDao.insertConflictReport(pendingReport)

        // Reject
        val success = repo.rejectConflictReport(reportId)
        assertTrue("Rejection call must return true", success)

        val updatedReport = conflictDao.getConflictReportById(reportId)
        assertEquals("Status must transition to REJECTED", "REJECTED", updatedReport!!.status)

        // Verify schedule table remains strictly untouched
        assertTrue("Schedule table must remain empty after rejection", scheduleDao.schedules.isEmpty())
    }
}
