package com.sevacare.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sevacare.app.data.model.ExpiredMedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpiredMedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpiredMedicineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExpiredMedicineEntity>)

    @Query("SELECT * FROM expired_medicines ORDER BY expiredAtEpochMs DESC")
    fun getAllHistoryFlow(): Flow<List<ExpiredMedicineEntity>>

    @Query("SELECT * FROM expired_medicines WHERE outcome = 'EXPIRED' ORDER BY expiredAtEpochMs DESC")
    fun getExpiredOnlyFlow(): Flow<List<ExpiredMedicineEntity>>

    @Query("SELECT * FROM expired_medicines WHERE outcome = 'TAKEN' ORDER BY expiredAtEpochMs DESC")
    fun getTakenOnlyFlow(): Flow<List<ExpiredMedicineEntity>>

    @Query("SELECT * FROM expired_medicines ORDER BY expiredAtEpochMs DESC")
    suspend fun getAllHistory(): List<ExpiredMedicineEntity>

    @Query("DELETE FROM expired_medicines WHERE id = :id")
    suspend fun deleteHistoryItem(id: String)
}
