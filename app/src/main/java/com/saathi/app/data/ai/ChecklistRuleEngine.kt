package com.saathi.app.data.ai

import com.saathi.app.data.model.ChecklistItemEntity
import java.util.UUID

/**
 * Metadata extracted from OCR text by the deterministic rule engine.
 */
data class ExtractedDocumentMetadata(
    val title: String,
    val deadlineDate: String?,
    val amountDue: String?,
    val referenceId: String?,
    val isOverdue: Boolean = false
)

/**
 * Deterministic rule engine converting on-device OCR text into actionable checklist items.
 *
 * Enforces:
 * 1. Zero fake defaults: Empty or blank OCR text strictly produces an empty checklist.
 * 2. Keyword mapping: Detects deadlines, amounts, policy renewals, utility payments, and clinic visits.
 * 3. Geriatric clarity: Action items are concise, unambiguous, and formatted for senior readability.
 */
object ChecklistRuleEngine {

    private val AMOUNT_REGEX = Regex("""(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
    private val DATE_REGEX = Regex("""\b(\d{1,2}[-/.](?:\d{1,2}|[A-Za-z]{3})[-/.](?:\d{2}|\d{4}))\b""")
    private val REF_REGEX = Regex("""(?:CA\s*(?:No\.?|#)?|Ref\s*(?:No\.?|#)?|Consumer\s*No\.?|Policy\s*(?:No\.?|#)?|A\/C\s*No\.?)\s*[:\-]?\s*([A-Za-z0-9\-]{6,20})""", RegexOption.IGNORE_CASE)

    fun extractMetadata(ocrText: String, category: String): ExtractedDocumentMetadata {
        if (ocrText.isBlank()) {
            return ExtractedDocumentMetadata(
                title = "$category Document",
                deadlineDate = null,
                amountDue = null,
                referenceId = null,
                isOverdue = false
            )
        }

        val amountMatch = AMOUNT_REGEX.find(ocrText)
        val amount = amountMatch?.let { "₹ ${it.groupValues[1]}" }

        val dateMatch = DATE_REGEX.find(ocrText)
        val date = dateMatch?.groupValues?.get(1)

        val refMatch = REF_REGEX.find(ocrText)
        val ref = refMatch?.groupValues?.get(1)

        val isOverdue = ocrText.contains("overdue", ignoreCase = true) ||
                ocrText.contains("immediate payment", ignoreCase = true) ||
                ocrText.contains("disconnection notice", ignoreCase = true)

        val title = when {
            ocrText.contains("electricity", ignoreCase = true) || ocrText.contains("power", ignoreCase = true) -> "Electricity Utility Bill"
            ocrText.contains("water", ignoreCase = true) -> "Water Utility Notice"
            ocrText.contains("insurance", ignoreCase = true) || ocrText.contains("policy", ignoreCase = true) -> "Insurance Policy Document"
            ocrText.contains("bank", ignoreCase = true) || ocrText.contains("statement", ignoreCase = true) -> "Bank Account Statement"
            ocrText.contains("hospital", ignoreCase = true) || ocrText.contains("prescription", ignoreCase = true) || ocrText.contains("clinic", ignoreCase = true) -> "Medical Clinic Prescription"
            ocrText.contains("aadhaar", ignoreCase = true) -> "Aadhaar Card Record"
            ocrText.contains("income tax", ignoreCase = true) || ocrText.contains("pan", ignoreCase = true) -> "Tax Identity Document"
            else -> "$category Document"
        }

        return ExtractedDocumentMetadata(
            title = title,
            deadlineDate = date,
            amountDue = amount,
            referenceId = ref,
            isOverdue = isOverdue
        )
    }

    fun generateChecklist(
        ocrText: String,
        category: String,
        documentId: String = UUID.randomUUID().toString()
    ): List<ChecklistItemEntity> {
        val trimmed = ocrText.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }

        val items = mutableListOf<ChecklistItemEntity>()
        val lower = trimmed.lowercase()
        val meta = extractMetadata(trimmed, category)

        // Rule 1: Bills & Due Dates
        val hasDueDate = lower.contains("due date") || lower.contains("pay by") ||
                lower.contains("last date") || lower.contains("valid till") ||
                lower.contains("before") || lower.contains("expiry")
        if (hasDueDate) {
            val dateLabel = meta.deadlineDate?.let { " before $it" } ?: " on or before the due date"
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Ensure bill payment is completed$dateLabel",
                    isOverdue = meta.isOverdue,
                    dueDate = meta.deadlineDate
                )
            )
        }

        // Rule 2: Amount Due
        if (meta.amountDue != null || lower.contains("amount due") || lower.contains("total payable") || lower.contains("premium")) {
            val amountLabel = meta.amountDue?.let { " ($it)" } ?: ""
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Verify the exact payable amount$amountLabel with your passbook",
                    isOverdue = meta.isOverdue,
                    dueDate = meta.deadlineDate
                )
            )
        }

        // Rule 3: Insurance & Policy Renewal
        if (lower.contains("premium") || lower.contains("renewal") || lower.contains("policy")) {
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Check policy renewal status and keep the premium receipt safe",
                    dueDate = meta.deadlineDate
                )
            )
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Verify nominee and family coverage details in the policy schedule"
                )
            )
        }

        // Rule 4: Banking & KYC
        if (lower.contains("kyc") || lower.contains("account") || lower.contains("nominee") || lower.contains("branch")) {
            if (lower.contains("kyc") || lower.contains("update")) {
                items.add(
                    ChecklistItemEntity(
                        documentId = documentId,
                        text = "Complete KYC verification at your bank branch if requested"
                    )
                )
            }
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Match statement transactions against your physical passbook entries"
                )
            )
        }

        // Rule 5: Medical & Prescriptions
        if (lower.contains("prescription") || lower.contains("dosage") || lower.contains("doctor") ||
            lower.contains("dr.") || lower.contains("tablet") || lower.contains("follow up")
        ) {
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Check medicine dosage schedule with doctor or pharmacist"
                )
            )
            if (lower.contains("follow up") || lower.contains("next visit")) {
                items.add(
                    ChecklistItemEntity(
                        documentId = documentId,
                        text = "Mark doctor's next follow-up appointment date on calendar"
                    )
                )
            }
        }

        // Rule 6: Government IDs & Official Notices
        if (lower.contains("aadhaar") || lower.contains("pan") || lower.contains("voter") || lower.contains("passport")) {
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Confirm official name, date of birth, and address details match your identity records"
                )
            )
        }

        // Fallback for non-empty OCR text with generic content
        if (items.isEmpty()) {
            items.add(
                ChecklistItemEntity(
                    documentId = documentId,
                    text = "Review verified text extract and archive in your records"
                )
            )
        }

        return items
    }
}
