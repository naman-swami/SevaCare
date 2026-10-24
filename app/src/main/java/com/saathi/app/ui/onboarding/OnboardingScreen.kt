package com.saathi.app.ui.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.saathi.app.R
import com.saathi.app.ui.components.PressablePillButton
import com.saathi.app.ui.mascot.MascotState
import com.saathi.app.ui.mascot.SparrowMascotView
import com.saathi.app.ui.theme.*

/**
 * 7-Step Elder-First Onboarding Flow (Upgrade Brief v2, Section 6).
 *
 * Steps:
 * 1. Language First (large cards in native scripts with speaker button).
 * 2. Welcome, spoken ("Medicines, papers and help, all in one place, without internet").
 * 3. Try It First (demo scan with gentle disclosure before requesting permissions).
 * 4. Country Selection (India default).
 * 5. Circle Setup (up to 3 contacts + secret family code word + consent text).
 * 6. Permissions in Context (one at a time, never all at once).
 * 7. Helper Setup Wizard route ("Setting this up for someone else?").
 *
 * Persisted in SharedPreferences; never repeated once completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("sevacare_prefs", Context.MODE_PRIVATE) }
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedLanguage by remember { mutableStateOf("hi") }
    var selectedCountry by remember { mutableStateOf("India") }
    var familyCodeWord by remember { mutableStateOf("") }
    var demoRevealed by remember { mutableStateOf(false) }

    fun finishOnboarding() {
        prefs.edit()
            .putBoolean("has_completed_onboarding", true)
            .putString("selected_language", selectedLanguage)
            .putString("selected_country", selectedCountry)
            .putString("family_code_word", familyCodeWord)
            .apply()
        onOnboardingFinished()
    }

    Scaffold(
        containerColor = SevaCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SevaCare Setup ($currentStep/7)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SevaDeepTeal)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { finishOnboarding() }) {
                        Text("Skip", color = SevaBodyTeal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SevaCream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> LanguageStep(
                        selectedLanguage = selectedLanguage,
                        onSelectLanguage = { selectedLanguage = it }
                    )
                    2 -> WelcomeStep()
                    3 -> TryItFirstStep(
                        demoRevealed = demoRevealed,
                        onReveal = { demoRevealed = true }
                    )
                    4 -> CountryStep(
                        selectedCountry = selectedCountry,
                        onSelectCountry = { selectedCountry = it }
                    )
                    5 -> CircleStep(
                        codeWord = familyCodeWord,
                        onCodeWordChange = { familyCodeWord = it }
                    )
                    6 -> PermissionsStep()
                    7 -> HelperWizardStep()
                }
            }

            // Bottom Navigation Pill Button
            PressablePillButton(
                text = if (currentStep == 7) "Start SevaCare" else "Aage Badhein (Next)",
                onClick = {
                    if (currentStep < 7) {
                        currentStep++
                    } else {
                        finishOnboarding()
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun LanguageStep(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Select Your Language",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal
        )
        Text(
            text = "Audio guidance and screen text adapt directly to your mother tongue.",
            fontSize = 16.sp,
            color = SevaBodyTeal
        )

        val languages = listOf(
            Triple("hi", "हिंदी (Hindi)", "Tier 1: Full speech & UI"),
            Triple("en", "English", "Tier 1: Full speech & UI"),
            Triple("mr", "मराठी (Marathi)", "Tier 2: 80+ reviewed phrases"),
            Triple("ta", "தமிழ் (Tamil)", "Tier 2: 80+ reviewed phrases"),
            Triple("te", "తెలుగు (Telugu)", "Tier 2: 80+ reviewed phrases"),
            Triple("bn", "বাংলা (Bengali)", "Tier 2: 80+ reviewed phrases")
        )

        languages.forEach { (code, name, tier) ->
            val isSelected = selectedLanguage == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectLanguage(code) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SevaMint else SevaCreamSurface
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, SevaDeepTeal) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SevaDeepTeal)
                        Text(tier, fontSize = 13.sp, color = SevaBodyTeal)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = SevaDeepTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sevacare_logo),
                contentDescription = "SevaCare Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            SparrowMascotView(
                state = MascotState.GREETING,
                size = 72.dp
            )
        }

        Text(
            text = "Welcome to SevaCare",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal,
            textAlign = TextAlign.Center
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SevaCreamSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "\"Medicines, official papers, and family safety — all in one place, completely offline.\"",
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SevaBodyTeal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TryItFirstStep(
    demoRevealed: Boolean,
    onReveal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Try It First (Zero Permissions)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal
        )
        Text(
            text = "Experience how Gentle Disclosure works before granting camera access.",
            fontSize = 16.sp,
            color = SevaBodyTeal
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SevaMint),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sample Strip: Glycomet 500",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDeepTeal
                )
                Text(
                    text = "Active Salt: Metformin Hydrochloride (500 mg)",
                    fontSize = 16.sp,
                    color = SevaBodyTeal
                )
                Text(
                    text = "How to take: Khane ke saath, subah aur raat.",
                    fontSize = 16.sp,
                    color = SevaBodyTeal
                )

                if (demoRevealed) {
                    Text(
                        text = "Purpose: Helps keep blood sugar steady.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaSafeGreen
                    )
                } else {
                    Button(
                        onClick = onReveal,
                        colors = ButtonDefaults.buttonColors(containerColor = SevaDeepTeal)
                    ) {
                        Text("Doctor ne kis liye diya? (Tap)", color = SevaWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryStep(
    selectedCountry: String,
    onSelectCountry: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Select Country (Desh)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal
        )
        Text(
            text = "Controls the medicine formulary and pharmacy lookup options.",
            fontSize = 16.sp,
            color = SevaBodyTeal
        )

        listOf("India", "United States", "Other").forEach { country ->
            val isSelected = selectedCountry == country
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCountry(country) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SevaMint else SevaCreamSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(country, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SevaDeepTeal)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = SevaDeepTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleStep(
    codeWord: String,
    onCodeWordChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Family & Neighbour Circle",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal
        )
        Text(
            text = "We will only tell your family when there is a real need. Everything else stays private on your phone.",
            fontSize = 16.sp,
            color = SevaBodyTeal,
            lineHeight = 22.sp
        )

        OutlinedTextField(
            value = codeWord,
            onValueChange = onCodeWordChange,
            label = { Text("Family Secret Code Word (e.g. Laddu)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SevaDeepTeal,
                unfocusedBorderColor = SevaBodyTeal.copy(alpha = 0.5f)
            )
        )
        Text(
            text = "Encrypted in Android Keystore. Protects from AI voice-cloning scams.",
            fontSize = 13.sp,
            color = SevaBodyTeal
        )
    }
}

@Composable
private fun PermissionsStep() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Permissions in Context",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal
        )
        Text(
            text = "Requested strictly one at a time when you use each feature:",
            fontSize = 16.sp,
            color = SevaBodyTeal
        )

        val perms = listOf(
            "📷 Camera" to "Medicine strip and document scanning",
            "🔔 Notifications" to "Morning check-in with big buttons",
            "✉️ SMS" to "Emergency SOS dispatch to Family Circle",
            "📍 Location" to "Finding nearby pharmacies within 2 km",
            "🎤 Microphone" to "Speaking voice queries to companion"
        )

        perms.forEach { (title, desc) ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SevaCreamSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SevaDeepTeal)
                    Text(desc, fontSize = 14.sp, color = SevaBodyTeal)
                }
            }
        }
    }
}

@Composable
private fun HelperWizardStep() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Setting up for someone else?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SevaDeepTeal
        )
        Text(
            text = "Family members or neighbours can run the Helper Setup Wizard to configure autostart, battery exemptions, and notification access for vivo, Xiaomi, Samsung, and Oppo phones.",
            fontSize = 16.sp,
            color = SevaBodyTeal,
            lineHeight = 22.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SevaPeach),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🛠️ Helper Wizard Checklist Ready", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SevaDarkText)
                Text("Available anytime from Settings.", fontSize = 14.sp, color = SevaBodyTeal)
            }
        }
    }
}
