package com.sevacare.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sevacare.app.data.pairing.FamilyPairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyPairDao {

    @Query("SELECT * FROM family_pair WHERE pairId = :pairId")
    suspend fun getPair(pairId: String): FamilyPairEntity?

    @Query("SELECT * FROM family_pair ORDER BY pairedAt DESC")
    fun getAllPairsFlow(): Flow<List<FamilyPairEntity>>

    @Query("SELECT * FROM family_pair ORDER BY pairedAt DESC LIMIT 1")
    suspend fun getLatestPair(): FamilyPairEntity?

    @Query("SELECT COUNT(*) FROM family_pair WHERE state = 'VERIFIED'")
    suspend fun getVerifiedCount(): Int

    @Query("SELECT * FROM family_pair WHERE state = 'VERIFIED'")
    suspend fun getVerifiedPairs(): List<FamilyPairEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pair: FamilyPairEntity)

    @Query("DELETE FROM family_pair WHERE pairId = :pairId")
    suspend fun delete(pairId: String)

    @Query("DELETE FROM family_pair")
    suspend fun clearAll()
}
