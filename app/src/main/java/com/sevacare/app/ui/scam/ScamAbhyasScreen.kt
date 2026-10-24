package com.sevacare.app.ui.scam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.ui.components.PressablePillButton
import com.sevacare.app.ui.theme.*

/**
 * Section 4.9 & Acceptance Criterion 19:
 * Scam Abhyas (Weekly Prebunking Practice)
 *
 * Three short, calm voice role-plays with Chhotu in the elder's language:
 * 1. Fake CBI Call
 * 2. Fake Electricity Cut-off
 * 3. Fake Relative in Trouble
 *
 * Designed for seniors:
 * - 60-second total duration
 * - Radically calm tone (zero panic/fear-mongering)
 * - Reinforces muscle memory: "Hang up and call my family"
 */
data class AbhyasScenario(
    val id: Int,
    val title: String,
    val scenarioText: String,
    val options: List<String>,
    val correctIndex: Int,
    val feedbackText: String
)

@Composable
fun ScamAbhyasScreen(
    onBack: () -> Unit
) {
    val scenarios = remember {
        listOf(
            AbhyasScenario(
                id = 1,
                title = "अभ्यास 1: नकली CBI कॉल / Fake CBI Call",
                scenarioText = "फ़ोन पर आवाज़: 'मैं CBI हेडक्वार्टर से बोल रहा हूँ। आपका बैंक खाता मनी लॉन्ड्रिंग में फंसा है। अभी वीडियो कॉल पर रहिए और जुर्माना भरिए वरना अरेस्ट वारंट निकलेगा!'",
                options = listOf(
                    "तुरंत पैसे भेज दें / Send money immediately",
                    "फ़ोन काटें और परिवार को बताएं / Hang up and call family",
                    "डर कर अपनी बैंक डिटेल्स दे दें / Share bank details"
                ),
                correctIndex = 1,
                feedbackText = "शाबाश! कोई भी असली पुलिस या CBI कभी फ़ोन पर पैसे नहीं मांगती। हमेशा फ़ोन काटें और परिवार को बताएं।"
            ),
            AbhyasScenario(
                id = 2,
                title = "अभ्यास 2: बिजली कटने का मैसेज / Fake Electricity Notice",
                scenarioText = "मैसेज: 'बिजली विभाग अलर्ट: आज रात 9 बजे आपकी बिजली काट दी जाएगी। बिल तुरंत इस नंबर पर कॉल करके जमा करें!'",
                options = listOf(
                    "मैसेज को अनदेखा करें और रसीद देखें / Ignore and verify from family",
                    "घबराकर तुरंत बताए नंबर पर पैसे भेजें / Send money in panic",
                    "अनजान लिंक पर क्लिक करके OTP दें / Click link and give OTP"
                ),
                correctIndex = 0,
                feedbackText = "बिल्कुल सही! बिजली विभाग ऐसे पर्सनल नंबर से धमकी भरे मैसेज नहीं भेजता।"
            ),
            AbhyasScenario(
                id = 3,
                title = "अभ्यास 3: रिश्तेदार मुसीबत में / Fake Relative in Trouble",
                scenarioText = "कॉल: 'चाचाजी, मेरा एक्सीडेंट हो गया है, पुलिस ने पकड़ लिया है। किसी को मत बताना, तुरंत 25,000 रुपये इस UPI पर भेज दो!'",
                options = listOf(
                    "तुरंत बिना सोचे UPI कर दें / Transfer without thinking",
                    "परिवार का गुप्त कोड वर्ड पूछें / Ask family code word",
                    "पेंशन के पैसे निकाल कर दे दें / Empty pension savings"
                ),
                correctIndex = 1,
                feedbackText = "बहुत बढ़िया! आजकल AI से आवाज़ की नकल करके ऐसे झांसे दिए जाते हैं। अपना फैमिली कोड वर्ड पूछने से तुरंत पोल खुल जाती है।"
            )
        )
    }

    var currentIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var showSuccessFeedback by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    val currentScenario = scenarios[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SevaCream)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressablePillButton(
                text = "← Back",
                onClick = onBack,
                isPrimary = false,
                height = 56.dp,
                fillColor = SevaCreamSurface,
                lipColor = SevaPeach,
                textColor = SevaDarkText,
                modifier = Modifier.width(120.dp)
            )

            Text(
                text = "Scam Abhyas / अभ्यास",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isCompleted) {
            Surface(
                color = SevaMint,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 बहुत खूब! / Well Done!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "आपने तीनों अभ्यास पूरे कर लिए। आप किसी भी डिजिटल फ्रॉड या डराने वाले कॉल से सुरक्षित रहने के लिए पूरी तरह तैयार हैं।",
                        fontSize = 18.sp,
                        color = SevaDarkText,
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    PressablePillButton(
                        text = "Back to Safety Home / मुख्य पेज",
                        onClick = onBack,
                        isPrimary = true,
                        height = 72.dp,
                        fillColor = SevaDeepTeal,
                        lipColor = SevaDeepTealLip,
                        textColor = SevaWhite
                    )
                }
            }
        } else {
            // Progress Indicator
            LinearProgressIndicator(
                progress = { (currentIndex + 1) / scenarios.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = SevaDeepTeal,
                trackColor = SevaCreamSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = SevaCreamSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = currentScenario.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentScenario.scenarioText,
                        fontSize = 18.sp,
                        color = SevaDarkText,
                        lineHeight = 26.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "आप क्या करेंगे? / What would you do?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            currentScenario.options.forEachIndexed { index, optionText ->
                val isSelected = selectedOption == index
                val isCorrect = index == currentScenario.correctIndex

                val buttonColor = when {
                    showSuccessFeedback && isCorrect -> SevaMint
                    showSuccessFeedback && isSelected && !isCorrect -> SevaPeach
                    else -> SevaCreamSurface
                }
                val lipCol = when {
                    showSuccessFeedback && isCorrect -> SevaDeepTeal
                    showSuccessFeedback && isSelected && !isCorrect -> SevaSaffronLip
                    else -> SevaPeach
                }

                PressablePillButton(
                    text = optionText,
                    onClick = {
                        selectedOption = index
                        showSuccessFeedback = true
                    },
                    isPrimary = false,
                    height = 72.dp,
                    fillColor = buttonColor,
                    lipColor = lipCol,
                    textColor = if (showSuccessFeedback && isCorrect) SevaDeepTeal else SevaDarkText
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showSuccessFeedback) {
                Surface(
                    color = if (selectedOption == currentScenario.correctIndex) SevaMint else SevaPeach,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedOption == currentScenario.correctIndex)
                                "✓ " + currentScenario.feedbackText
                            else
                                "⚠️ फिर से सोचें: हमेशा रुकें, फ़ोन काटें और अपने परिवार या पड़ोसी साथी से बात करें।",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SevaDeepTeal,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                PressablePillButton(
                    text = if (currentIndex < scenarios.size - 1) "Next Practice / अगला अभ्यास" else "Finish Practice / पूरा करें",
                    onClick = {
                        if (currentIndex < scenarios.size - 1) {
                            currentIndex++
                            selectedOption = null
                            showSuccessFeedback = false
                        } else {
                            isCompleted = true
                        }
                    },
                    isPrimary = true,
                    height = 72.dp,
                    fillColor = SevaSaffron,
                    lipColor = SevaSaffronLip,
                    textColor = SevaOnSaffronText
                )
            }
        }
    }
}
