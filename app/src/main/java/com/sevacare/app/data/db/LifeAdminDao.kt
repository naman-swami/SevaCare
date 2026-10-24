package com.sevacare.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sevacare.app.data.model.ChecklistItemEntity
import com.sevacare.app.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeAdminDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Query("SELECT * FROM life_admin_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM life_admin_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("DELETE FROM life_admin_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity): Long

    @Query("SELECT * FROM checklist_items WHERE documentId = :docId ORDER BY createdAt ASC")
    fun getChecklistForDocument(docId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items ORDER BY createdAt DESC")
    fun getAllChecklistItems(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE isOverdue = 1 AND isCompleted = 0")
    fun getOverdueChecklistItems(): Flow<List<ChecklistItemEntity>>

    @Query("UPDATE checklist_items SET isCompleted = :completed WHERE id = :id")
    suspend fun updateChecklistItemStatus(id: String, completed: Boolean)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItemById(id: String)
}
