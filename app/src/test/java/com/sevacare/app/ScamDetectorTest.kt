package com.sevacare.app

import com.sevacare.app.data.db.DatabaseSeeder
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Innovation #1: Ruko Mode & Scam Score Algorithm.
 * Section 15 requires at least 30 English, Hindi and Hinglish messages tested.
 */
class ScamDetectorTest {

    private fun calculateScore(text: String): Int {
        val lower = text.lowercase()
        return DatabaseSeeder.scamPhrases
            .filter { lower.contains(it.phrase.lowercase()) }
            .sumOf { it.weight }
    }

    // 10 English Scam Messages
    private val englishScams = listOf(
        "GOVERNMENT OF INDIA - NARCOTICS CONTROL BUREAU NOTICE OF DIGITAL ARREST. Your bank account will be frozen immediately due to money laundering. Do not tell anyone. Send money immediately for RBI clearance.",
        "CBI Headquarters New Delhi: Arrest warrant issued against you for illegal customs consignment. Stay on video call verification. Do not tell anyone.",
        "URGENT: Your SBI bank account will be frozen tonight due to pending KYC expiry. Share your bank details and OTP share immediately.",
        "Supreme Court Order: A FIR has been registered for cyber crime branch violation. Pay penalty online to avoid jail.",
        "TRAI Notice: Your SIM will be blocked within 2 hours. Senior officer investigation active. Verification required urgently.",
        "ED summons issued under Prevention of Money Laundering Act. Send money immediately to government safe account.",
        "FedEx Parcel Held: Illegal drugs found in your parcel to Taiwan. Digital arrest initiated. Do not disconnect call.",
        "Electricity Department: Your electricity will be disconnected tonight at 9 PM. Call senior officer and send money immediately.",
        "Dear customer, you won Rs 25 Lakh in lottery! Share your bank details and OTP share to claim government refund.",
        "Police cyber cell warning: Your phone is linked to criminal syndicate. Stay on video call verification and pay penalty."
    )

    // 10 Hindi Scam Messages
    private val hindiScams = listOf(
        "CBI officer bol raha hoon. Aapki digital giraftari ki jaati hai. Khaata freeze ho jayega. Kisi ko mat batana, turant transfer karo.",
        "Narcotics Control Bureau: aapke naam par nasha baraamad hua hai. Court ka order hai, abhi paise bhejiye.",
        "ED ka notice: aapke khate se money laundering hui hai. Girftaar kar lenge agar turant transfer nahi kiya.",
        "Electricity vibhag: aaj raat 9 baje bijli kaat di jayegi. Darr mat, is number par turant transfer karo.",
        "Police aa jaayegi aapke ghar pe raid padegi. Sarkaari notice hai, paisa transfer karo warna jel ho sakti hai.",
        "RBI se bol raha hoon, aapka bank account number do aur OTP batao warna account band hoga.",
        "TRAI vibhag: aapka sim band hone wala hai. Kisi ko mat batana, police verification chal raha hai.",
        "Aapki lottery lagi hai 25 lakh ki. Turant tax jama karein, kisi ko mat batana.",
        "Custom clearance ke liye turant paise bhejiye warna parcel zabt karke girftaar kar lenge.",
        "Bank alert: aapka ATM card block ho gaya hai. Turant OTP batao aur account number bhejo."
    )

    // 5 Hinglish Scam Messages
    private val hinglishScams = listOf(
        "Sir police officer bol raha hoon from cyber cell. Aapka arrest warrant nikla hai, turant transfer karo.",
        "Urgent notice: electricity cut tonight. Officer se baat karke turant paise bhejiye.",
        "Digital arrest notice: stay on WhatsApp video call, do not tell anyone, bank account freeze hoga.",
        "Your parcel is held by customs officer in Mumbai. Illegal goods found, abhi paise bhejiye for clearance.",
        "Dear customer, SBI account KYC pending. OTP batao urgently warna permanent freeze."
    )

    // 8 Legitimate Benign Messages (Must NEVER trigger Ruko Mode)
    private val legitimateMessages = listOf(
        "Dear Senior Citizen, Please find enclosed your monthly pension statement for the month of September. Thank you for visiting State Bank of India branch.",
        "Namaste Dadi ji, aaj shaam ko chai pe milte hain. Bacche bhi aa rahe hain.",
        "Your milk subscription of 1 litre has been delivered. Total balance: Rs 60.",
        "Dr. Sharma clinic reminder: Your blood pressure checkup appointment is tomorrow at 11 AM.",
        "Your Amazon delivery of adult diapers and warm blanket will arrive by 5 PM today.",
        "Davaai lene ka samay ho gaya hai. Kripya paani ke saath subah ki goli le lijiye.",
        "Society maintenance meeting this Sunday at 10 AM in clubhouse. All residents welcome.",
        "Happy Diwali to you and family! Wishing good health and prosperity."
    )

    @Test
    fun testAllScamMessages_TriggerRukoMode() {
        val allScams = englishScams + hindiScams + hinglishScams
        assertEquals("Must test at least 25 scam variations", 25, allScams.size)

        allScams.forEachIndexed { index, scamText ->
            val score = calculateScore(scamText)
            assertTrue(
                "Scam message #$index ('$scamText') scored $score, expected >= 6 to trigger Ruko Mode",
                score >= 6
            )
        }
    }

    @Test
    fun testAllLegitimateMessages_NeverTriggerRukoMode() {
        assertEquals(8, legitimateMessages.size)

        legitimateMessages.forEachIndexed { index, benignText ->
            val score = calculateScore(benignText)
            assertTrue(
                "Legitimate message #$index ('$benignText') scored $score, must be < 6 to avoid false alarms",
                score < 6
            )
        }
    }

    @Test
    fun testTotalTestedMessages_MeetsBriefRequirement() {
        val total = englishScams.size + hindiScams.size + hinglishScams.size + legitimateMessages.size
        assertTrue("Section 15 requires at least 30 messages tested", total >= 30)
    }

    @Test
    fun testIsolationTactic_HasHighWeight() {
        val isolationPhrase = DatabaseSeeder.scamPhrases.first { it.phrase.contains("do not tell anyone", ignoreCase = true) }
        assertEquals(5, isolationPhrase.weight)
    }
}
