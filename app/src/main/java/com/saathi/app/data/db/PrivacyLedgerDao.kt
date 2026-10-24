package com.saathi.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saathi.app.data.model.PrivacyLedgerEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyLedgerDao {
    @Query("SELECT * FROM privacy_ledger ORDER BY timestampMs DESC")
    fun getAllEntries(): Flow<List<PrivacyLedgerEntry>>

    @Query("SELECT * FROM privacy_ledger ORDER BY timestampMs DESC")
    suspend fun getAllEntriesList(): List<PrivacyLedgerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PrivacyLedgerEntry): Long

    @Query("SELECT COUNT(*) FROM privacy_ledger")
    suspend fun getEntryCount(): Int
}
