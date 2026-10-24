package com.saathi.app

import com.saathi.app.data.scam.CommunityScamFeedVerifier
import com.saathi.app.data.scam.ScamFeedItem
import com.saathi.app.data.scam.SignedCommunityFeed
import org.junit.Assert.*
import org.junit.Test

/**
 * Section 4.3e & Acceptance Criterion 18:
 * "A tampered community feed fails signature verification."
 */
class CommunityScamFeedTest {

    @Test
    fun testAuthenticBundledFeed_PassesVerification() {
        val authenticFeed = CommunityScamFeedVerifier.getBundledDemoFeed()
        val isValid = CommunityScamFeedVerifier.verifyFeed(authenticFeed)

        assertTrue("Authentic signed community feed must pass signature verification", isValid)
        assertTrue("Must contain demo entries", authenticFeed.items.isNotEmpty())
    }

    @Test
    fun testTamperedFeed_FailsVerification() {
        val authenticFeed = CommunityScamFeedVerifier.getBundledDemoFeed()

        // Attacker attempts to inject malicious or forged phone number into feed
        val tamperedItems = authenticFeed.items.toMutableList()
        tamperedItems.add(
            ScamFeedItem(
                id = "MALICIOUS-01",
                pincodePrefix = "110",
                reportedNumber = "+919999999999",
                scamPattern = "Injected fraudulent report",
                reportedDate = "2026-10-21",
                isDemo = false
            )
        )

        val tamperedFeed = SignedCommunityFeed(
            version = authenticFeed.version,
            timestamp = authenticFeed.timestamp,
            items = tamperedItems,
            signatureHex = authenticFeed.signatureHex // Original signature no longer matches!
        )

        val isTamperedValid = CommunityScamFeedVerifier.verifyFeed(tamperedFeed)
        assertFalse("Tampered feed must FAIL cryptographic verification", isTamperedValid)
    }

    @Test
    fun testModifiedContent_FailsVerification() {
        val authenticFeed = CommunityScamFeedVerifier.getBundledDemoFeed()

        // Attacker alters existing item's reported number
        val modifiedItems = authenticFeed.items.mapIndexed { idx, item ->
            if (idx == 0) item.copy(reportedNumber = "+910000000000") else item
        }

        val tamperedFeed = authenticFeed.copy(items = modifiedItems)
        val isTamperedValid = CommunityScamFeedVerifier.verifyFeed(tamperedFeed)

        assertFalse("Modified item content must fail signature verification", isTamperedValid)
    }
}
