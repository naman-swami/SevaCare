package com.saathi.app.data.repository

import android.content.Context
import com.saathi.app.data.ai.ChecklistRuleEngine
import com.saathi.app.data.db.LifeAdminDao
import com.saathi.app.data.model.ChecklistItemEntity
import com.saathi.app.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

/**
 * Repository orchestrating on-device document intake, OCR indexing, and actionable checklists.
 */
class LifeAdminRepository(
    private val lifeAdminDao: LifeAdminDao,
    private val context: Context
) {

    val allDocuments: Flow<List<DocumentEntity>> = lifeAdminDao.getAllDocuments()
    val allChecklistItems: Flow<List<ChecklistItemEntity>> = lifeAdminDao.getAllChecklistItems()
    val overdueItems: Flow<List<ChecklistItemEntity>> = lifeAdminDao.getOverdueChecklistItems()

    val documentsDirectory: File
        get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    fun createNewDocumentFile(): File {
        return File(documentsDirectory, "doc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
    }

    fun getChecklistForDoc(docId: String): Flow<List<ChecklistItemEntity>> {
        return lifeAdminDao.getChecklistForDocument(docId)
    }

    suspend fun processAndSaveScannedDocument(
        imageFile: File,
        category: String,
        ocrText: String
    ): DocumentEntity {
        val meta = ChecklistRuleEngine.extractMetadata(ocrText, category)
        val doc = DocumentEntity(
            category = category,
            imagePath = imageFile.absolutePath,
            ocrText = ocrText,
            title = meta.title,
            deadlineDate = meta.deadlineDate,
            amountDue = meta.amountDue,
            referenceId = meta.referenceId
        )

        lifeAdminDao.insertDocument(doc)

        val checklistItems = ChecklistRuleEngine.generateChecklist(ocrText, category, doc.id)
        if (checklistItems.isNotEmpty()) {
            lifeAdminDao.insertChecklistItems(checklistItems)
        }

        return doc
    }

    suspend fun updateChecklistItemStatus(itemId: String, completed: Boolean) {
        lifeAdminDao.updateChecklistItemStatus(itemId, completed)
    }

    suspend fun addCustomChecklistItem(documentId: String, text: String, dueDate: String? = null) {
        val item = ChecklistItemEntity(
            documentId = documentId,
            text = text.trim(),
            dueDate = dueDate
        )
        lifeAdminDao.insertChecklistItem(item)
    }

    suspend fun deleteDocument(documentId: String) {
        lifeAdminDao.deleteDocumentById(documentId)
    }
}
