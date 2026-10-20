package com.saathi.app.ui.document

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saathi.app.data.repository.SaathiRepository
import com.saathi.app.ui.theme.RukoRed
import com.saathi.app.ui.theme.RukoRedContainer
import com.saathi.app.ui.theme.RukoOnRedContainer
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SafeGreenContainer
import com.saathi.app.ui.medicine.CameraPreviewCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    repository: SaathiRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DocumentViewModel = viewModel(
        factory = DocumentViewModelFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val rukoMode by viewModel.rukoModeActive.collectAsState()
    val rukoSecondsRemaining by viewModel.rukoSecondsRemaining.collectAsState()

    var showCamera by remember { mutableStateOf(false) }
    var dismissCode by remember { mutableStateOf("") }

    // Ruko Mode countdown ticker
    LaunchedEffect(rukoMode) {
        if (rukoMode) {
            while (viewModel.rukoSecondsRemaining.value > 0) {
                delay(1000)
                viewModel.tickRuko()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Main Screen ──────────────────────────────────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("कागज जाँचें", fontWeight = FontWeight.Bold)
                            Text("Document & Notice Check", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Scan Button
                Button(
                    onClick = { showCamera = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(72.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Notice / Letter Scan Karein", style = MaterialTheme.typography.labelLarge)
                }

                // Context explainer
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚠️ Koi notice, letter, ya message mila hai?",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Saathi isko scan karke batayega — yeh asli government notice hai, ya koi dhokhadhadi.\n\n" +
                            "Digital Arrest scam: Jan-Apr 2024 mein sirf 4 mahine mein ₹120 crore ki thagi. " +
                            "70% shikaar buzurg log hain.",
                            fontSize = 16.sp, lineHeight = 24.sp
                        )
                    }
                }

                // Result
                when (val state = uiState) {
                    is DocumentUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Jaanch ho rahi hai...")
                            }
                        }
                    }
                    is DocumentUiState.SafeDocument -> {
                        SafeDocumentCard(state)
                    }
                    is DocumentUiState.ScamDetected -> {
                        ScamDetectedCard(
                            state = state,
                            onTriggerRuko = { familyPhone ->
                                viewModel.activateRukoMode()
                                // Send WhatsApp alert
                                if (familyPhone.isNotBlank()) {
                                    val msg = "⚠️ [Saathi Alert] Aapke ghar mein ek suspicious government notice scan hua hai. Abhi call karein!"
                                    val whatsappIntent = Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/$familyPhone?text=${Uri.encode(msg)}"))
                                    whatsappIntent.setPackage("com.whatsapp")
                                    try { context.startActivity(whatsappIntent) } catch (e: Exception) {
                                        // WhatsApp not installed fallback — plain SMS
                                        val smsIntent = Intent(Intent.ACTION_SENDTO,
                                            Uri.parse("smsto:$familyPhone")).apply {
                                            putExtra("sms_body", msg)
                                        }
                                        try { context.startActivity(smsIntent) } catch (e2: Exception) { }
                                    }
                                }
                            }
                        )
                    }
                    else -> {
                        // Idle
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Shield, contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(12.dp))
                                Text("Koi bhi notice scan karein",
                                    fontWeight = FontWeight.Bold, fontSize = 22.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.height(8.dp))
                                Text("CBI · ED · Police · Customs · Aadhaar · RBI — koi bhi",
                                    fontSize = 14.sp, textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // ── RUKO MODE OVERLAY ────────────────────────────────────
        // Full-screen, hard to dismiss — deliberately so.
        AnimatedVisibility(
            visible = rukoMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RukoModeOverlay(
                secondsRemaining = rukoSecondsRemaining,
                dismissCode = dismissCode,
                onDismissCodeChange = { dismissCode = it },
                onAttemptDismiss = {
                    if (dismissCode.uppercase() == "SAATHI") {
                        viewModel.deactivateRukoMode()
                        dismissCode = ""
                    }
                }
            )
        }
    }

    // Camera Dialog
    if (showCamera) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCamera = false }) {
            CameraPreviewCard(
                onTextRecognized = { text ->
                    showCamera = false
                    viewModel.analyzeDocument(text)
                },
                onDismiss = { showCamera = false }
            )
        }
    }
}

// ─── Ruko Mode Overlay ────────────────────────────────────────────

@Composable
fun RukoModeOverlay(
    secondsRemaining: Int,
    dismissCode: String,
    onDismissCodeChange: (String) -> Unit,
    onAttemptDismiss: () -> Unit
) {
    val minutesLeft = secondsRemaining / 60
    val secondsLeft = secondsRemaining % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RukoRed),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("🛑", fontSize = 72.sp)
            Text(
                "RUKO!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 56.sp,
                color = Color.White
            )
            Text(
                "KHATARA HAI",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Divider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth(0.8f))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Saathi ne yeh document mein dhokha dhadi ke nishaan dekhe hain.",
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Koi bhi paise mat bhejiye.",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Countdown Timer
            Text(
                text = String.format("%02d:%02d", minutesLeft, secondsLeft),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 80.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Intezaar karein — Ghabrana mat",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Divider(color = Color.White.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth(0.8f))

            // Dismiss code entry (makes it hard to close under panic)
            Text(
                "Bahar jaane ke liye \"SAATHI\" type karein:",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = dismissCode,
                onValueChange = onDismissCodeChange,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedBorderColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                label = { Text("Type here", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            Button(
                onClick = onAttemptDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = RukoRed
                ),
                modifier = Modifier.height(56.dp).fillMaxWidth(0.7f)
            ) {
                Text("Bahar Jaayein", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

// ─── Safe Document Card ───────────────────────────────────────────

@Composable
fun SafeDocumentCard(state: DocumentUiState.SafeDocument) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafeGreenContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = SafeGreen, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("SAAF LAGTA HAI", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = SafeGreen)
                    Text("Koi obvious scam signal nahi mila", fontSize = 14.sp, color = SafeGreen.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Lekin yaad rakho: agar koi phone kare aur turant paise maange, " +
                "ya kisi ko mat batane ko bole — toh yeh scam hoga. " +
                "Pehle apne bête / beti / parivaar ko dikhaayein.",
                fontSize = 16.sp, lineHeight = 24.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("Scan Score: ${state.score}/30 (6 se kam = safe zone)",
                fontSize = 13.sp, color = SafeGreen.copy(alpha = 0.7f))
        }
    }
}

// ─── Scam Detected Card ───────────────────────────────────────────

@Composable
fun ScamDetectedCard(
    state: DocumentUiState.ScamDetected,
    onTriggerRuko: (String) -> Unit
) {
    var familyPhone by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = RukoRedContainer),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚨", fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("KHATRA!", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp,
                        color = RukoRed)
                    Text("Scam ke nishaan mile! Score: ${state.score}/30",
                        fontSize = 14.sp, color = RukoRed.copy(alpha = 0.8f))
                }
            }

            Text("Yeh nishaan mile:", fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = RukoOnRedContainer)
            state.matchedPhrases.take(5).forEach { phrase ->
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text("🔴 ", fontSize = 16.sp)
                    Column {
                        Text(phrase.phrase, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            color = RukoOnRedContainer)
                        Text(phrase.reason, fontSize = 12.sp, color = RukoOnRedContainer.copy(alpha = 0.7f))
                    }
                }
            }

            Divider(color = RukoRed.copy(alpha = 0.2f))

            Text("Koi bhi paise mat bhejiye. Pehle parivaar ko call karein.",
                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RukoOnRedContainer,
                textAlign = TextAlign.Center)

            // Family phone for WhatsApp
            OutlinedTextField(
                value = familyPhone,
                onValueChange = { familyPhone = it },
                label = { Text("Parivaar ka WhatsApp number (91XXXXXXXXXX)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onTriggerRuko(familyPhone) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RukoRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("RUKO MODE Activate Karein", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
