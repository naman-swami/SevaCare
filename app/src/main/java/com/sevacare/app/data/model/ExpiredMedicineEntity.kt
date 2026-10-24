package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Historical record of a medicine intake schedule that was either
 * EXPIRED (due time passed without confirmation), TAKEN, or SKIPPED.
 */
@Entity(tableName = "expired_medicines")
data class ExpiredMedicineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scheduleId: String,
    val medicineId: Int,
    val medicineName: String,
    val timeOfDay: String,
    val scheduledAtEpochMs: Long,
    val expiredAtEpochMs: Long,
    val zoneIdAtExpiry: String = ZoneId.systemDefault().id,
    val outcome: String, // "EXPIRED", "TAKEN", "SKIPPED"
    val statedReason: String? = null,
    val durationDays: Int = 1
) {
    companion object {
        fun from(
            schedule: ScheduleEntity,
            expiredAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
            outcome: String = "EXPIRED"
        ): ExpiredMedicineEntity {
            val epochMs = expiredAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return ExpiredMedicineEntity(
                scheduleId = schedule.id,
                medicineId = schedule.medicineId,
                medicineName = schedule.medicineName,
                timeOfDay = schedule.timeOfDay,
                scheduledAtEpochMs = if (schedule.dueEpochMs > 0L) schedule.dueEpochMs else schedule.createdAt,
                expiredAtEpochMs = epochMs,
                zoneIdAtExpiry = ZoneId.systemDefault().id,
                outcome = outcome,
                statedReason = schedule.statedReason,
                durationDays = schedule.durationDays
            )
        }
    }
}
