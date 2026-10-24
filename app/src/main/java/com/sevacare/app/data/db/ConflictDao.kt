package com.sevacare.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sevacare.app.data.model.ConflictReportEntity
import com.sevacare.app.data.model.DrugInteractionEntity
import com.sevacare.app.data.model.SuspiciousMedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConflictDao {

    // ── Drug Interactions ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: DrugInteractionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractions(interactions: List<DrugInteractionEntity>)

    @Query("SELECT * FROM drug_interactions")
    suspend fun getAllInteractions(): List<DrugInteractionEntity>

    @Query("""
        SELECT * FROM drug_interactions 
        WHERE (LOWER(drugA) = LOWER(:saltA) AND LOWER(drugB) = LOWER(:saltB))
           OR (LOWER(drugA) = LOWER(:saltB) AND LOWER(drugB) = LOWER(:saltA))
        LIMIT 1
    """)
    suspend fun findInteraction(saltA: String, saltB: String): DrugInteractionEntity?

    // ── Suspicious Medicines ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspiciousMedicine(entry: SuspiciousMedicineEntity): Long

    @Query("SELECT * FROM suspicious_medicines ORDER BY flaggedAt DESC")
    fun getSuspiciousMedicinesFlow(): Flow<List<SuspiciousMedicineEntity>>

    @Query("SELECT * FROM suspicious_medicines ORDER BY flaggedAt DESC")
    suspend fun getAllSuspiciousMedicines(): List<SuspiciousMedicineEntity>

    // ── Conflict Reports ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflictReport(report: ConflictReportEntity): Long

    @Query("SELECT * FROM conflict_reports ORDER BY requestedAt DESC")
    fun getAllConflictReportsFlow(): Flow<List<ConflictReportEntity>>

    @Query("SELECT * FROM conflict_reports WHERE status = 'PENDING_APPROVAL' ORDER BY requestedAt DESC")
    fun getPendingConflictReportsFlow(): Flow<List<ConflictReportEntity>>

    @Query("SELECT * FROM conflict_reports WHERE id = :id LIMIT 1")
    suspend fun getConflictReportById(id: String): ConflictReportEntity?

    @Query("UPDATE conflict_reports SET status = :status, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateConflictReportStatus(id: String, status: String, resolvedAt: Long = System.currentTimeMillis())
}
