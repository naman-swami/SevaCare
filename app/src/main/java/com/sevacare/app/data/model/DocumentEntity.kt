package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing an analyzed physical life-admin document.
 */
@Entity(tableName = "life_admin_documents")
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String, // "Banking", "IDs", "Bills", "Medical", "Insurance"
    val imagePath: String,
    val ocrText: String,
    val title: String,
    val deadlineDate: String? = null,
    val amountDue: String? = null,
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
