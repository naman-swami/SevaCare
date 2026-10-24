package com.sevacare.app.data.db

import androidx.room.*
import com.sevacare.app.data.model.CircleContact
import kotlinx.coroutines.flow.Flow

@Dao
interface CircleContactDao {
    @Query("SELECT * FROM circle_contacts ORDER BY isPrimary DESC, id ASC")
    fun getAllContacts(): Flow<List<CircleContact>>

    @Query("SELECT * FROM circle_contacts ORDER BY isPrimary DESC, id ASC")
    suspend fun getAllContactsDirect(): List<CircleContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: CircleContact): Long

    @Update
    suspend fun updateContact(contact: CircleContact)

    @Delete
    suspend fun deleteContact(contact: CircleContact)

    @Query("SELECT COUNT(*) FROM circle_contacts")
    suspend fun getContactCount(): Int
}
