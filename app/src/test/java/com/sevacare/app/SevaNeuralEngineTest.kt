package com.sevacare.app

import com.sevacare.app.data.agent.InMemorySevaAgentStorage
import com.sevacare.app.data.agent.SevaAgentManager
import com.sevacare.app.data.ai.NeuralIntent
import com.sevacare.app.data.ai.SevaNeuralEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SevaNeuralEngineTest {

    @Test
    fun testNeuralIntentClassification_EmergencyRedFlags() {
        val emergencyQueries = listOf(
            "Severe chest pain since morning",
            "chhati mein dard ho raha hai",
            "I have trouble breathing right now",
            "Elder fainted and unconscious",
            "jyada goli kha li overdose"
        )

        emergencyQueries.forEach { query ->
            val result = SevaNeuralEngine.classifyIntent(query)
            assertEquals("Query '$query' must classify as EMERGENCY_RED_FLAG", NeuralIntent.EMERGENCY_RED_FLAG, result.intent)
            assertTrue("Query '$query' must set isEmergency flag", result.isEmergency)
            assertTrue("Query '$query' must yield risk score >= 0.90", result.riskScore >= 0.90f)
            assertTrue("Confidence must be > 0.80", result.confidence > 0.80f)
        }
    }

    @Test
    fun testNeuralIntentClassification_ClinicalSymptoms() {
        val clinicalQueries = listOf(
            "I feel unwell and dizzy today",
            "meri tabiyat kharab hai aur chakkar aa rahe hain",
            "stomach headache and fever",
            "pet mein dard aur bukhar hai"
        )

        clinicalQueries.forEach { query ->
            val result = SevaNeuralEngine.classifyIntent(query)
            assertEquals("Query '$query' must classify as CLINICAL_SYMPTOM", NeuralIntent.CLINICAL_SYMPTOM, result.intent)
            assertFalse("Clinical query '$query' should not trigger red flag emergency", result.isEmergency)
            assertTrue("Query '$query' must yield risk score >= 0.70", result.riskScore >= 0.70f)
        }
    }

    @Test
    fun testNeuralIntentClassification_DigitalArrestScam() {
        val scamQueries = listOf(
            "Police CBI ED notice digital arrest warrant",
            "digital giraftari warrant for bank account",
            "court order court warrant investigation"
        )

        scamQueries.forEach { query ->
            val result = SevaNeuralEngine.classifyIntent(query)
            assertEquals("Query '$query' must classify as SCAM_DIGITAL_ARREST", NeuralIntent.SCAM_DIGITAL_ARREST, result.intent)
            assertTrue("Query '$query' must yield risk score >= 0.90", result.riskScore >= 0.90f)
        }
    }

    @Test
    fun testNeuralIntentClassification_FinancialOtpScam() {
        val otpQueries = listOf(
            "share bank OTP immediately for refund",
            "enter upi pin to claim lottery prize",
            "paise bhejo for electricity bill"
        )

        otpQueries.forEach { query ->
            val result = SevaNeuralEngine.classifyIntent(query)
            assertEquals("Query '$query' must classify as SCAM_FINANCIAL_OTP", NeuralIntent.SCAM_FINANCIAL_OTP, result.intent)
            assertTrue("Query '$query' must yield risk score >= 0.85", result.riskScore >= 0.85f)
        }
    }

    @Test
    fun testNeuralScamRiskEvaluator() {
        val criticalCoercionText = "Urgently share OTP or CBI police will digital arrest you in 10 mins"
        val assessment = SevaNeuralEngine.evaluateScamRisk(criticalCoercionText)

        assertEquals("CRITICAL", assessment.coercionLevel)
        assertTrue("Risk score must be >= 0.70", assessment.riskScore >= 0.70f)
        assertTrue("Must trigger Pause Mode", assessment.shouldTriggerPauseMode)
        assertTrue("Must detect multiple threat vectors", assessment.matchedThreatVectors.size >= 2)
    }

    @Test
    fun testLanguageDetectionAndNormalization() {
        val hindiText = "मेरी तबियत खराब है"
        val hinglishText = "meri tabiyat kharab hai aur dard hai"
        val englishText = "I have a headache and severe pain"

        assertEquals("hi", SevaNeuralEngine.detectLanguage(hindiText))
        assertEquals("hinglish", SevaNeuralEngine.detectLanguage(hinglishText))
        assertEquals("en", SevaNeuralEngine.detectLanguage(englishText))

        val normalized = SevaNeuralEngine.normalizeText(" Hello, World!   THIS IS A TEST.  ")
        assertEquals("hello world this is a test", normalized)
    }

    @Test
    fun testSevaAgentManager_AnalyticsAndActionPath() {
        val storage = InMemorySevaAgentStorage()
        val agent = SevaAgentManager(storage, context = null)

        val cal = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 1, 8, 0, 0) }
        for (day in 1..7) {
            agent.recordRoutineTimestamp(cal.timeInMillis + (day * 24L * 60 * 60 * 1000))
        }

        val analytics = agent.getRoutineAnalytics()
        assertNotNull(analytics.meanMinute)
        assertEquals(7, analytics.totalRecordedDays)
        assertTrue("Confidence score should be high for consistent checkins", analytics.confidenceScore >= 0.90f)

        val actionPath = agent.generateTodayActionPath(currentHour = 9)
        assertEquals(3, actionPath.size)
        assertEquals("ACT_01", actionPath[0].id)
    }
}
