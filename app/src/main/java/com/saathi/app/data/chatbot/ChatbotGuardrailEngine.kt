package com.saathi.app.data.chatbot

data class ChatbotResponse(
    val replyText: String,
    val showEmergencyDialer112: Boolean = false,
    val isClinicalGuardrail: Boolean = false,
    val isScamAlert: Boolean = false
)

/**
 * Section 10 & Acceptance Criterion 11:
 * Deterministic, guardrailed chatbot router.
 *
 * Guaranteed Safety Rules:
 * 1. NEVER diagnoses.
 * 2. NEVER advises on dosage or dose change.
 * 3. NEVER claims to call an ambulance or dispatch emergency services.
 * 4. Verbatim clinical guardrail block triggered for unwell, dizzy, pain, diagnosis queries.
 * 5. Red-flag phrases (chest pain, trouble breathing, fainting, overdose) activate the 112 dialer button.
 */
object ChatbotGuardrailEngine {

    const val GUARDRAIL_EN = "I am not a doctor and cannot tell how serious this is. Would you like me to call a family member, or show nearby clinics?"
    const val GUARDRAIL_HI = "मैं डॉक्टर नहीं हूँ और यह नहीं बता सकता कि यह कितना गंभीर है। क्या मैं आपके परिवार के किसी सदस्य को फ़ोन लगाऊँ, या पास के क्लिनिक दिखाऊँ?"

    private val RED_FLAGS = listOf(
        "chest pain", "chhati mein dard", "seene mein dard", "छाती में", "सीने में",
        "trouble breathing", "saans lene mein", "सांस लेने", "saans fool", "सांस फूल", "shortness of breath",
        "faint", "fainted", "fainting", "faints", "behosh", "unconscious", "बेहोश",
        "took too many", "jyada goli", "ज्यादा गोली", "overdose", "poison", "galat dawaai", "गलत दवाई"
    )

    private val CLINICAL_TRIGGERS = listOf(
        "feel unwell", "unwell", "tabiyat", "तबीयत", "तबियत", "kharab", "still sick", "sick", "ill", "bimar", "बीमार", "bimari", "बीमारी",
        "pain", "dard", "दर्द", "dizzy", "chakkar", "चक्कर",
        "breathing", "diagnosis", "diagnose", "लक्षण", "symptom", "infection", "cancer", "कैंसर",
        "dose", "खुराक", "डोज़", "badlu", "badha", "kam", "change",
        "should i stop", "stop", "बंद कर", "chhod", "take more", "दो खा", "thrice", "twice",
        "stomach", "pet", "पेट", "headache", "sir dard", "सिर", "vomit", "vomiting", "ulti", "उल्टी", "shiver", "shivering",
        "side effect", "साइड", "fever", "bukhar", "बुखार", "weakness", "kamjori", "कमजोरी"
    )

    fun evaluateQuery(query: String, isScamQuery: Boolean = false): ChatbotResponse {
        val lower = query.lowercase().trim()

        // 1. Red-Flag Check -> Verbatim block + Emergency 112 dialer
        val isRedFlag = RED_FLAGS.any { lower.contains(it.lowercase()) }
        val isClinical = isRedFlag || CLINICAL_TRIGGERS.any { lower.contains(it.lowercase()) }

        if (isClinical) {
            val isEnglish = isAsciiEnglish(query)
            val reply = if (isEnglish) GUARDRAIL_EN else GUARDRAIL_HI
            return ChatbotResponse(
                replyText = reply,
                showEmergencyDialer112 = isRedFlag,
                isClinicalGuardrail = true
            )
        }

        // 2. Scam / Digital Arrest / Extortion check
        if (isScamQuery || lower.contains("cbi") || lower.contains("police") || lower.contains("ed notice") ||
            lower.contains("digital arrest") || lower.contains("digital giraftari") || lower.contains("giraftar") ||
            lower.contains("otp maang") || lower.contains("paise bhejo") || lower.contains("court order")
        ) {
            return ChatbotResponse(
                replyText = "ध्यान से सुनिए! Police, CBI या court कभी भी फ़ोन पर पैसे नहीं मांगते और ना ही वीडियो कॉल पर अरेस्ट करते हैं। यह 100% फ्रॉड है। तुरंत फ़ोन काट दीजिए और अपने परिवार को बताइए।",
                isScamAlert = true
            )
        }

        // 3. Fallback intent
        val fallback = if (isAsciiEnglish(query)) {
            "I can help with medicines, papers and calling your family."
        } else {
            "मैं दवाई, ज़रूरी कागज़ात और परिवार को फ़ोन करने में मदद कर सकता हूँ।"
        }
        return ChatbotResponse(replyText = fallback)
    }

    private fun isAsciiEnglish(text: String): Boolean {
        return text.none { it in '\u0900'..'\u097F' }
    }
}
