package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing an authentic recorded family audio story.
 */
@Entity(tableName = "family_stories")
data class StoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val durationMs: Long,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val familyMemberId: String = "self"
)
