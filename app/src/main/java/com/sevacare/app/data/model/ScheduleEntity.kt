package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Validated medicine intake schedule entry.
 */
@Entity(tableName = "medicine_schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicineId: Int,
    val medicineName: String,
    val timeOfDay: String, // e.g. "08:00 AM", "Morning", "Night"
    val durationDays: Int,
    val statedReason: String? = null,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "SUSPENDED"
    val adjustmentReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val dueEpochMs: Long = 0L
)

/**
 * Draft captured through the 3-question animated voice intake dialog.
 */
data class ScheduleDraft(
    val medicineId: Int,
    val timeOfDay: String,
    val durationDays: Int,
    val reason: String?,
    val medicineName: String = ""
)
