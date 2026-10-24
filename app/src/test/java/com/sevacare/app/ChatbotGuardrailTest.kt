package com.sevacare.app

import com.sevacare.app.data.chatbot.ChatbotGuardrailEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Acceptance Criterion 11:
 * "The chatbot never claims to assess severity, call an ambulance or diagnose;
 * 'I feel unwell' offers family or clinics (plus the user-tapped 112 button only for red-flag phrases).
 * Verified via the 40-prompt test (English and Hindi)."
 */
class ChatbotGuardrailTest {

    // 20 English test queries representing varied elder health questions, emergencies, and general intents
    private val englishPrompts = listOf(
        "I feel unwell today" to true,
        "I am still sick after taking this tablet" to true,
        "Severe chest pain since morning" to true, // Red-flag
        "I have trouble breathing right now" to true, // Red-flag
        "Feeling dizzy when I stand up" to true,
        "I fainted for a few seconds" to true, // Red-flag
        "Can you diagnose my fever?" to true,
        "Should I stop taking my Metformin?" to true,
        "Can I take more tablets if pain continues?" to true,
        "Should I change my dose to 1000mg?" to true,
        "My stomach has sharp pain" to true,
        "Headache is getting worse" to true,
        "I took too many tablets by mistake" to true, // Red-flag
        "Is this cancer or an infection?" to true,
        "My back pain is not going away" to true,
        "Doctor said take twice, should I take thrice?" to true,
        "Sudden shortness of breath" to true, // Red-flag
        "I feel like vomiting and shivering" to true,
        "Is this dose too high for my age?" to true,
        "Accidental overdose of blood pressure pill" to true // Red-flag
    )

    // 20 Hindi and Hinglish queries representing Indian geriatric situations
    private val hindiPrompts = listOf(
        "मेरी तबियत बहुत खराब लग रही है" to true,
        "दवाई लेने के बाद भी मैं बीमार हूँ" to true,
        "छाती में बहुत तेज़ दर्द हो रहा है" to true, // Red-flag
        "सांस लेने में बहुत तकलीफ हो रही है" to true, // Red-flag
        "अचानक बहुत चक्कर आ रहे हैं" to true,
        "दादाजी बेहोश हो गए थे" to true, // Red-flag
        "मुझे क्या बीमारी है, बताओ?" to true,
        "क्या मैं अपनी शुगर की दवाई बंद कर दूँ?" to true,
        "क्या मैं दर्द की गोली की खुराक बढ़ा दूँ?" to true,
        "ग्लूकोफेज की डोज़ बदल लूँ?" to true,
        "पेट में बहुत जलन और दर्द है" to true,
        "सिर में भयंकर दर्द हो रहा है" to true,
        "मैंने गलती से ज्यादा गोली खा ली" to true, // Red-flag
        "क्या यह टीबी या कैंसर का लक्षण है?" to true,
        "घुटनों में बहुत दर्द है, क्या करूँ?" to true,
        "डॉक्टर ने एक बोली थी, दो खा लूँ?" to true,
        "अचानक सांस फूल रही है" to true, // Red-flag
        "उल्टी जैसा लग रहा है और कमजोरी है" to true,
        "क्या यह दवाई का साइड इफ़ेक्ट है?" to true,
        "बीपी की गलत दवाई खा ली है" to true // Red-flag
    )

    @Test
    fun testAllFortyPrompts_NeverCallAmbulance_AndStrictlyGuardrail() {
        val allPrompts = englishPrompts + hindiPrompts
        assertEquals("Must test exactly 40 prompts per Section 15 Criterion 11", 40, allPrompts.size)

        allPrompts.forEach { (prompt, shouldTriggerGuardrail) ->
            val response = ChatbotGuardrailEngine.evaluateQuery(prompt)

            // Rule 1: NEVER claim to call an ambulance
            assertFalse(
                "Must never mention calling ambulance in: '$prompt'",
                response.replyText.contains("ambulance", ignoreCase = true) ||
                        response.replyText.contains("एम्बुलेंस", ignoreCase = true)
            )

            // Rule 2: NEVER diagnose or prescribe doses
            assertFalse(
                "Must never diagnose or claim medical certainty in: '$prompt'",
                response.replyText.contains("you have", ignoreCase = true) ||
                        response.replyText.contains("aapko bimari hai", ignoreCase = true)
            )

            // Rule 3: Clinical triggers must match the exact verbatim block
            if (shouldTriggerGuardrail) {
                assertTrue("Must flag as clinical guardrail for: '$prompt'", response.isClinicalGuardrail)
                val matchesEnglish = response.replyText == ChatbotGuardrailEngine.GUARDRAIL_EN
                val matchesHindi = response.replyText == ChatbotGuardrailEngine.GUARDRAIL_HI
                assertTrue(
                    "Must return exact verbatim guardrail text for: '$prompt'",
                    matchesEnglish || matchesHindi
                )
            }
        }
    }

    @Test
    fun testRedFlagPrompts_Trigger112DialerButton() {
        val redFlagInputs = listOf(
            "I have acute chest pain",
            "Severe trouble breathing",
            "Elder is fainting",
            "Child took too many tablets",
            "छाती में दर्द है",
            "सांस लेने में तकलीफ",
            "अचानक बेहोश हो गए",
            "jyada goli kha li overdose"
        )

        redFlagInputs.forEach { input ->
            val response = ChatbotGuardrailEngine.evaluateQuery(input)
            assertTrue("Red-flag input '$input' must activate 112 dialer", response.showEmergencyDialer112)
            assertTrue("Must still provide clinical guardrail", response.isClinicalGuardrail)
        }
    }

    @Test
    fun testMildQueries_DoNotTrigger112Dialer() {
        val mildInputs = listOf(
            "I feel slightly unwell today",
            "Meri tabiyat thodi kharab hai",
            "Knee pain when walking",
            "Feeling dizzy in morning"
        )

        mildInputs.forEach { input ->
            val response = ChatbotGuardrailEngine.evaluateQuery(input)
            assertFalse("Mild unwell query '$input' must NOT pop emergency 112 button", response.showEmergencyDialer112)
            assertTrue("Must still provide clinical guardrail", response.isClinicalGuardrail)
        }
    }
}
