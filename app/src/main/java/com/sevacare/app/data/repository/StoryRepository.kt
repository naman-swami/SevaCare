package com.sevacare.app.data.repository

import android.content.Context
import com.sevacare.app.data.db.StoryDao
import com.sevacare.app.data.model.StoryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/**
 * Repository for managing authentic voice recordings in the Living Family Archive.
 */
class StoryRepository(
    private val storyDao: StoryDao,
    private val context: Context
) {

    val allStories: Flow<List<StoryEntity>> = storyDao.getAllStories()

    val storiesDirectory: File
        get() = File(context.filesDir, "stories").apply { if (!exists()) mkdirs() }

    fun createNewStoryFile(): File {
        return File(storiesDirectory, "story_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a")
    }

    suspend fun saveStory(
        file: File,
        durationMs: Long,
        title: String,
        familyMemberId: String = "self"
    ): StoryEntity {
        val story = StoryEntity(
            filePath = file.absolutePath,
            durationMs = durationMs,
            title = title.ifBlank { "Story of ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}" },
            familyMemberId = familyMemberId
        )
        storyDao.insertStory(story)
        return story
    }

    suspend fun deleteStory(story: StoryEntity) {
        try {
            val file = File(story.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignored
        }
        storyDao.deleteStory(story)
    }
}
