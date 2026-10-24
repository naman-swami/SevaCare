package com.sevacare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Section 4.12 Privacy Ledger record.
 * Tracks every single external alert dispatched with exact timestamp, type, and recipient.
 */
@Entity(tableName = "privacy_ledger")
data class PrivacyLedgerEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val alertType: String,
    val recipientPhone: String,
    val recipientName: String,
    val deliveryChannel: String, // "DIRECT_SMS" or "USER_INTENT"
    val summaryText: String
)
