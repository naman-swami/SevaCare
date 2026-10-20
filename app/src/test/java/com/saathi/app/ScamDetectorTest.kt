package com.saathi.app

import com.saathi.app.data.db.DatabaseSeeder
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Innovation #1: Ruko Mode & Scam Score Algorithm.
 * Tests detection of Digital Arrest, impersonation tactics, and isolation triggers.
 */
class ScamDetectorTest {

    private fun calculateScore(text: String): Int {
        val lower = text.lowercase()
        return DatabaseSeeder.scamPhrases
            .filter { lower.contains(it.phrase.lowercase()) }
            .sumOf { it.weight }
    }

    @Test
    fun testDigitalArrestNotice_TriggersRukoMode() {
        val fakeNoticeText = """
            GOVERNMENT OF INDIA - NARCOTICS CONTROL BUREAU
            NOTICE OF DIGITAL ARREST
            Your bank account will be frozen immediately due to money laundering.
            Do not tell anyone. Send money immediately for RBI clearance.
        """.trimIndent()

        val score = calculateScore(fakeNoticeText)

        // Threshold for Ruko Mode is score >= 6
        assertTrue("Scam score ($score) should be >= 6 to trigger Ruko Mode", score >= 6)
    }

    @Test
    fun testHindiDigitalArrest_TriggersRukoMode() {
        val fakeHindiNotice = """
            CBI officer bol raha hoon.
            Aapki digital giraftari ki jaati hai.
            Khaata freeze ho jayega. Kisi ko mat batana, turant transfer karo.
        """.trimIndent()

        val score = calculateScore(fakeHindiNotice)
        assertTrue("Hindi scam score ($score) should be >= 6 to trigger Ruko Mode", score >= 6)
    }

    @Test
    fun testLegitimateLetter_DoesNotTriggerRukoMode() {
        val normalText = """
            Dear Senior Citizen,
            Please find enclosed your monthly pension statement for the month of September.
            Thank you for visiting State Bank of India branch.
        """.trimIndent()

        val score = calculateScore(normalText)
        assertTrue("Legitimate bank letter score ($score) should be < 6", score < 6)
    }

    @Test
    fun testIsolationTactic_HasHighWeight() {
        val isolationPhrase = DatabaseSeeder.scamPhrases.first { it.phrase.contains("do not tell anyone", ignoreCase = true) }
        assertEquals(5, isolationPhrase.weight)
    }
}
