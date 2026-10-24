package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Log entry for a medicine flagged due to contradiction between stated intake reason
 * and medical indications/contraindications.
 */
@Entity(tableName = "suspicious_medicines")
data class SuspiciousMedicineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicineId: Int,
    val medicineName: String,
    val statedReason: String,
    val contradictionScore: Float,
    val explanation: String,
    val flaggedAt: Long = System.currentTimeMillis()
)
