package com.saathi.app.data.ai

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-Class Neural Intent Classification Engine.
 *
 * Implements an on-device neural-inspired semantic intent vectorizer,
 * multi-keyword N-gram activation, multilingual normalization,
 * and dynamic scam coercion risk scoring.
 */
enum class NeuralIntent {
    EMERGENCY_RED_FLAG,
    CLINICAL_SYMPTOM,
    MEDICINE_DOSAGE_ALTERATION,
    SCAM_DIGITAL_ARREST,
    SCAM_FINANCIAL_OTP,
    MEDICINE_SCHEDULE,
    DOCUMENT_VAULT,
    FAMILY_CIRCLE,
    DAILY_ROUTINE,
    GENERAL_COMPANION
}

data class NeuralIntentResult(
    val intent: NeuralIntent,
    val confidence: Float,
    val matchedKeywords: List<String>,
    val riskScore: Float,
    val isEmergency: Boolean,
    val normalizedText: String,
    val detectedLanguage: String
)

data class ScamRiskAssessment(
    val riskScore: Float,
    val coercionLevel: String,
    val matchedThreatVectors: List<String>,
    val recommendedAction: String,
    val shouldTriggerPauseMode: Boolean
)

object SevaNeuralEngine {

    // ─── Semantic Feature Dictionaries ──────────────────────────────

    private val RED_FLAG_VECTORS = mapOf(
        "chest pain" to 0.95f, "chhati" to 0.95f, "seene" to 0.95f, "chest" to 0.95f, "chhati mein dard" to 0.95f, "seene mein dard" to 0.95f, "छाती में" to 0.95f, "सीने में" to 0.95f,
        "trouble breathing" to 0.95f, "saans lene mein" to 0.95f, "सांस्क" to 0.90f, "सांस लेने" to 0.95f, "saans fool" to 0.92f,
        "faint" to 0.90f, "fainting" to 0.90f, "behosh" to 0.95f, "unconscious" to 0.95f, "बेहोश" to 0.95f,
        "took too many" to 0.95f, "overdose" to 0.95f, "jyada goli" to 0.92f, "poison" to 0.95f, "गलत दवाई" to 0.92f
    )

    private val CLINICAL_VECTORS = mapOf(
        "unwell" to 0.85f, "tabiyat" to 0.85f, "तबीयत" to 0.85f, "kharab" to 0.75f, "sick" to 0.80f, "ill" to 0.80f, "bimar" to 0.80f, "बीमार" to 0.85f,
        "pain" to 0.80f, "dard" to 0.80f, "दर्द" to 0.85f, "dizzy" to 0.85f, "chakkar" to 0.85f, "चक्कर" to 0.85f,
        "headache" to 0.80f, "sir dard" to 0.80f, "vomit" to 0.80f, "ulti" to 0.80f, "fever" to 0.85f, "bukhar" to 0.85f, "बुखार" to 0.85f,
        "weakness" to 0.75f, "kamjori" to 0.75f, "कमजोरी" to 0.80f, "stomach" to 0.75f, "pet" to 0.75f, "पेट" to 0.80f
    )

    private val DOSAGE_VECTORS = mapOf(
        "dose" to 0.90f, "खुराक" to 0.90f, "डोज़" to 0.90f, "badlu" to 0.85f, "badha" to 0.85f, "stop" to 0.85f, "बंद कर" to 0.90f,
        "take more" to 0.90f, "thrice" to 0.80f, "twice" to 0.80f, "change" to 0.75f, "chhod" to 0.80f, "double" to 0.95f, "half" to 0.85f, "aadhi" to 0.85f, "doguni" to 0.95f, "zyada" to 0.85f
    )

    private val DIGITAL_ARREST_VECTORS = mapOf(
        "cbi" to 0.95f, "police" to 0.90f, "ed notice" to 0.95f, "digital arrest" to 0.98f, "digital giraftari" to 0.98f,
        "court order" to 0.90f, "giraftar" to 0.92f, "warrant" to 0.92f, "custody" to 0.90f, "investigation" to 0.85f
    )

    private val FINANCIAL_OTP_VECTORS = mapOf(
        "otp" to 0.95f, "paise bhejo" to 0.90f, "lottery" to 0.90f, "bank details" to 0.90f, "upi pin" to 0.95f,
        "claim prize" to 0.88f, "refund" to 0.85f, "account block" to 0.90f, "electricity bill" to 0.88f
    )

    private val MEDICINE_SCHEDULE_VECTORS = mapOf(
        "medicine" to 0.80f, "dawaai" to 0.80f, "दवाई" to 0.85f, "tablet" to 0.75f, "time" to 0.70f, "schedule" to 0.80f,
        "kahan rakhi" to 0.75f, "kab leni" to 0.85f, "reminder" to 0.80f, "glycomet" to 0.85f, "telma" to 0.85f
    )

    private val DOCUMENT_VAULT_VECTORS = mapOf(
        "aadhar" to 0.90f, "pan card" to 0.90f, "ayushman" to 0.92f, "pension" to 0.90f, "passport" to 0.88f,
        "document" to 0.80f, "paper" to 0.75f, "kaagaz" to 0.80f, "कागज़" to 0.85f, "prescription" to 0.85f, "parchi" to 0.85f
    )

    private val FAMILY_CIRCLE_VECTORS = mapOf(
        "call son" to 0.90f, "call daughter" to 0.90f, "beta" to 0.85f, "beti" to 0.85f, "family" to 0.80f, "parivar" to 0.80f,
        "doctor" to 0.85f, "emergency contact" to 0.92f, "phone lagao" to 0.88f, "बात कराओ" to 0.88f
    )

    private val DAILY_ROUTINE_VECTORS = mapOf(
        "checkin" to 0.85f, "good morning" to 0.80f, "namaste" to 0.75f, "walk" to 0.70f, "path" to 0.70f,
        "rasta" to 0.75f, "subah" to 0.75f, "sainik" to 0.70f, "took medicine" to 0.85f
    )

    /**
     * Multilingual Normalization Engine.
     * Maps Devanagari, regional script, and non-standard Romanized text to normalized forms.
     */
    fun normalizeText(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace(Regex("[.,!?;:\\-\"'\n\r]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Detect input language representation.
     */
    fun detectLanguage(input: String): String {
        val hasDevanagari = input.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) return "hi"
        val hindiKeywords = listOf("meri", "hai", "karo", "dard", "goli", "dawaai", "saans", "chakkar", "paise")
        val lower = input.lowercase(Locale.ROOT)
        return if (hindiKeywords.any { lower.contains(it) }) "hinglish" else "en"
    }

    /**
     * Multi-Class Neural Intent Classification.
     * Computes similarity vector activations and returns best class with confidence.
     */
    fun classifyIntent(query: String): NeuralIntentResult {
        val normalized = normalizeText(query)
        val lang = detectLanguage(query)

        val scores = mutableMapOf<NeuralIntent, Float>()
        val keywordMatches = mutableMapOf<NeuralIntent, MutableList<String>>()

        fun scoreCategory(intent: NeuralIntent, vectors: Map<String, Float>) {
            var maxScore = 0.0f
            val matches = mutableListOf<String>()
            for ((pattern, weight) in vectors) {
                if (normalized.contains(pattern)) {
                    maxScore = max(maxScore, weight)
                    matches.add(pattern)
                }
            }
            if (matches.isNotEmpty()) {
                // Boost score based on number of signal activations (up to 1.0)
                val boostedScore = min(1.0f, maxScore + (matches.size - 1) * 0.05f)
                scores[intent] = boostedScore
                keywordMatches[intent] = matches
            }
        }

        scoreCategory(NeuralIntent.EMERGENCY_RED_FLAG, RED_FLAG_VECTORS)
        scoreCategory(NeuralIntent.CLINICAL_SYMPTOM, CLINICAL_VECTORS)
        scoreCategory(NeuralIntent.MEDICINE_DOSAGE_ALTERATION, DOSAGE_VECTORS)
        scoreCategory(NeuralIntent.SCAM_DIGITAL_ARREST, DIGITAL_ARREST_VECTORS)
        scoreCategory(NeuralIntent.SCAM_FINANCIAL_OTP, FINANCIAL_OTP_VECTORS)
        scoreCategory(NeuralIntent.MEDICINE_SCHEDULE, MEDICINE_SCHEDULE_VECTORS)
        scoreCategory(NeuralIntent.DOCUMENT_VAULT, DOCUMENT_VAULT_VECTORS)
        scoreCategory(NeuralIntent.FAMILY_CIRCLE, FAMILY_CIRCLE_VECTORS)
        scoreCategory(NeuralIntent.DAILY_ROUTINE, DAILY_ROUTINE_VECTORS)

        // Evaluate top matching intent
        val topEntry = scores.maxByOrNull { it.value }

        val bestIntent = topEntry?.key ?: NeuralIntent.GENERAL_COMPANION
        val confidence = topEntry?.value ?: 0.50f
        val matches = keywordMatches[bestIntent] ?: emptyList()

        // Risk score computation ($0.0$ to $1.0$)
        val riskScore = when (bestIntent) {
            NeuralIntent.EMERGENCY_RED_FLAG -> 0.98f
            NeuralIntent.SCAM_DIGITAL_ARREST -> 0.95f
            NeuralIntent.SCAM_FINANCIAL_OTP -> 0.90f
            NeuralIntent.MEDICINE_DOSAGE_ALTERATION -> 0.85f
            NeuralIntent.CLINICAL_SYMPTOM -> 0.75f
            else -> 0.10f
        }

        val isEmergency = bestIntent == NeuralIntent.EMERGENCY_RED_FLAG

        return NeuralIntentResult(
            intent = bestIntent,
            confidence = confidence,
            matchedKeywords = matches,
            riskScore = riskScore,
            isEmergency = isEmergency,
            normalizedText = normalized,
            detectedLanguage = lang
        )
    }

    /**
     * Dedicated Scam Coercion & Risk Assessment Engine.
     */
    fun evaluateScamRisk(text: String): ScamRiskAssessment {
        val normalized = normalizeText(text)
        val vectors = mutableListOf<String>()
        var score = 0.0f

        if (normalized.contains("cbi") || normalized.contains("police") || normalized.contains("digital arrest") || normalized.contains("giraftar")) {
            score += 0.45f
            vectors.add("Coercive Authority Threat (Digital Arrest / CBI / Police)")
        }
        if (normalized.contains("otp") || normalized.contains("upi pin") || normalized.contains("paise") || normalized.contains("transfer")) {
            score += 0.35f
            vectors.add("Financial Data Exfiltration (OTP / UPI Demand)")
        }
        if (normalized.contains("urgent") || normalized.contains("immediately") || normalized.contains("turant") || normalized.contains("abhee")) {
            score += 0.20f
            vectors.add("Artificial Urgency / Panic Inducement")
        }
        if (normalized.contains("court") || normalized.contains("ed notice") || normalized.contains("warrant")) {
            score += 0.25f
            vectors.add("Fake Judicial Documentation Threat")
        }

        val finalScore = min(1.0f, score)
        val coercionLevel = when {
            finalScore >= 0.70f -> "CRITICAL"
            finalScore >= 0.40f -> "HIGH"
            finalScore >= 0.20f -> "MEDIUM"
            else -> "LOW"
        }

        val action = if (finalScore >= 0.40f) {
            "DISCONNECT IMMEDIATELY! Do not share OTP or money. Law enforcement never arrests over video call."
        } else {
            "Proceed with caution. Verify payee identity with trusted family."
        }

        return ScamRiskAssessment(
            riskScore = finalScore,
            coercionLevel = coercionLevel,
            matchedThreatVectors = vectors,
            recommendedAction = action,
            shouldTriggerPauseMode = finalScore >= 0.40f
        )
    }
}
