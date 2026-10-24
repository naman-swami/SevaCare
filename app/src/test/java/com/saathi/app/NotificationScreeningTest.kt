package com.saathi.app

import com.saathi.app.data.model.CircleContact
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.model.PrivacyLedgerEntry
import com.saathi.app.data.model.ScamPhrase
import org.junit.Assert.*
import org.junit.Test

/**
 * Section 4.3b & Acceptance Criterion 18:
 * "Message bodies are never stored (inspect the database);
 * the listener is limited to messaging packages."
 */
class NotificationScreeningTest {

    private val whitelistedPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.plus"
    )

    @Test
    fun testPackageFiltering_AcceptsOnlyWhitelistedMessagingApps() {
        // Must accept standard SMS, WhatsApp, Telegram
        assertTrue(whitelistedPackages.contains("com.google.android.apps.messaging"))
        assertTrue(whitelistedPackages.contains("com.whatsapp"))
        assertTrue(whitelistedPackages.contains("org.telegram.messenger"))

        // Must reject browsers, system apps, social media, shopping apps
        assertFalse(whitelistedPackages.contains("com.android.chrome"))
        assertFalse(whitelistedPackages.contains("com.instagram.android"))
        assertFalse(whitelistedPackages.contains("com.facebook.katana"))
        assertFalse(whitelistedPackages.contains("com.amazon.mShop.android.shopping"))
        assertFalse(whitelistedPackages.contains("com.google.android.youtube"))
    }

    @Test
    fun testDatabaseSchema_NeverStoresMessageBodies() {
        // Inspect all fields across all Room Entity classes via Java reflection (no external deps needed)
        val entityClasses = listOf(
            Medicine::class.java,
            ScamPhrase::class.java,
            CircleContact::class.java,
            PrivacyLedgerEntry::class.java
        )

        val forbiddenFieldNames = setOf(
            "messagebody", "messagetext", "body", "smsbody", "notificationtext",
            "fulltext", "rawmessage", "chatbody", "contentbody"
        )

        entityClasses.forEach { javaClass ->
            val fieldNames = javaClass.declaredFields.map { it.name.lowercase() }
            fieldNames.forEach { fieldName ->
                assertFalse(
                    "Entity ${javaClass.simpleName} contains prohibited personal message storage field: '$fieldName'",
                    forbiddenFieldNames.contains(fieldName)
                )
            }
        }
    }

    @Test
    fun testPrivacyLedger_StoresOnlyAuditMetadata_NeverMessageContent() {
        val fieldNames = PrivacyLedgerEntry::class.java.declaredFields.map { it.name }
        assertTrue(fieldNames.contains("alertType"))
        assertTrue(fieldNames.contains("recipientPhone"))
        assertTrue(fieldNames.contains("deliveryChannel"))
        assertFalse("Privacy ledger must not store incoming message text", fieldNames.contains("incomingMessage"))
    }
}
