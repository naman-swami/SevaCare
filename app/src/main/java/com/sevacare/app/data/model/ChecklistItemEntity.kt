package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing an actionable task generated from document OCR or user input.
 */
@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val text: String,
    val isCompleted: Boolean = false,
    val isOverdue: Boolean = false,
    val dueDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
