package com.saathi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Report generated when drug-drug conflicts are detected, requiring family approval.
 */
@Entity(tableName = "conflict_reports")
data class ConflictReportEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val candidateMedicineId: Int,
    val candidateMedicineName: String,
    val conflictingMedicineName: String,
    val severity: String, // "MAJOR", "MODERATE", "CONTRAINDICATED"
    val interactionReason: String,
    val correctedRecommendation: String,
    val proposedScheduleTime: String,
    val durationDays: Int,
    val statedReason: String?,
    val status: String = "PENDING_APPROVAL", // "PENDING_APPROVAL", "APPROVED", "REJECTED"
    val requestedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)
