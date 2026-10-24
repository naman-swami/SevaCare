package com.sevacare.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sevacare.app.data.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(schedules: List<ScheduleEntity>)

    @Query("SELECT * FROM medicine_schedules WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM medicine_schedules WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    suspend fun getActiveSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM medicine_schedules ORDER BY createdAt DESC")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM medicine_schedules WHERE medicineId = :medId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveScheduleForMedicine(medId: Int): ScheduleEntity?

    @Query("UPDATE medicine_schedules SET status = :status WHERE id = :id")
    suspend fun updateScheduleStatus(id: String, status: String)

    @Query("SELECT * FROM medicine_schedules WHERE status IN ('ACTIVE', 'UPCOMING', 'TAKEN', 'EXPIRED') ORDER BY createdAt DESC")
    fun todayWithStatus(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM medicine_schedules WHERE status = 'ACTIVE' AND dueEpochMs > 0 AND dueEpochMs <= :now")
    suspend fun findDueBefore(now: java.time.LocalDateTime): List<ScheduleEntity>

    @Query("SELECT * FROM medicine_schedules WHERE status = 'ACTIVE' AND dueEpochMs > 0 AND dueEpochMs <= :epochMs")
    suspend fun findDueBeforeEpoch(epochMs: Long): List<ScheduleEntity>

    @Query("SELECT * FROM medicine_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: String): ScheduleEntity?

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}
