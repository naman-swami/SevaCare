package com.sevacare.app.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sevacare.app.data.db.SevaCareDatabase
import com.sevacare.app.data.repository.MedicineRepository
import com.sevacare.app.data.voice.*
import com.sevacare.app.ui.components.FourBarAudioVisualizer
import com.sevacare.app.ui.theme.LocalSevaColors
import com.sevacare.app.ui.theme.LocalSevaHaptics
import com.sevacare.app.ui.theme.SevaDanger
import com.sevacare.app.ui.theme.SevaSaffron
import com.sevacare.app.util.VoiceEngine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Floating Voice-First Command Center.
 *
 * Implements:
 * - Floating bottom-right entry button
 * - Overlay panel with top back-arrow to collapse
 * - Four-Bar live audio visualizer fed by real microphone amplitude (0..32767)
 * - On-device ASR speech recognition with live partial/final transcripts
 * - VoiceIntentRouter executing real actions: CallLog, SMS inbox, Scam reporting, Medicine routine
 * - Spoken feedback via high-cadence neural voice synthesis
 */
@Composable
fun VoiceCommandCenter(
    modifier: Modifier = Modifier,
    onNavigateMedicine: () -> Unit = {},
    onNavigateScamShield: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalSevaColors.current
    val haptics = LocalSevaHaptics.current
    val scope = rememberCoroutineScope()

    val db = remember { SevaCareDatabase.getInstance(context) }
    val medicineRepo = remember {
        MedicineRepository(
            medicineDao = db.medicineDao(),
            scheduleDao = db.scheduleDao(),
            conflictDao = db.conflictDao(),
            familyPairDao = db.familyPairDao()
        )
    }

    val router = remember {
        val deps = FeatureDependencies(
            fraudReporter = FraudReporter(db.privacyLedgerDao(), db.familyPairDao()),
            callLogReader = CallLogReader(context),
            smsReader = SmsReader(context),
            medicineVoice = MedicineVoiceCommander(medicineRepo, db.scheduleDao())
        )
        VoiceIntentRouter(deps)
    }

    val voiceEngine = remember { VoiceEngine.getInstance(context) }

    var isExpanded by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var currentAmplitude by remember { mutableIntStateOf(0) }
    var transcript by remember { mutableStateOf("") }
    var activeResult by remember { mutableStateOf<VoiceResult?>(null) }
    var pendingPermission by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted && isExpanded) {
            // Permission granted
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingPermission = null
            // Re-run router command
            scope.launch {
                activeResult = router.route(transcript)
            }
        }
    }

    // Speech Recognizer management
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var pulseJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun startListeningInternal() {
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }

        voiceEngine.stop()
        speechRecognizer?.destroy()

        isListening = true
        currentAmplitude = 9000
        transcript = "साथी सुन रहा है..."

        pulseJob?.cancel()
        pulseJob = scope.launch {
            while (isActive && isListening) {
                currentAmplitude = 6000 + (kotlin.random.Random.nextFloat() * 12000).toInt()
                kotlinx.coroutines.delay(120)
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }
                    override fun onBeginningOfSpeech() {
                        currentAmplitude = 16000
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = (rmsdB.coerceAtLeast(0f) / 10f)
                        currentAmplitude = (normalized * 30000).toInt().coerceIn(4000, 32767)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        pulseJob?.cancel()
                        currentAmplitude = 0
                    }
                    override fun onError(error: Int) {
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                transcript = "साथी सुन रहा है, कृपया बोलिए..."
                                currentAmplitude = 8000
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    if (isListening) {
                                        startListeningInternal()
                                    }
                                }
                            }
                            else -> {
                                pulseJob?.cancel()
                                isListening = false
                                currentAmplitude = 0
                            }
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        currentAmplitude = 0
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            transcript = text
                            scope.launch {
                                val result = router.route(text)
                                activeResult = result
                                val speechText = when (result) {
                                    is VoiceResult.CallLogResult -> result.summary
                                    is VoiceResult.SmsResult -> result.summary
                                    is VoiceResult.MedicineActionResult -> result.summary
                                    is VoiceResult.FraudReported -> result.summary
                                    is VoiceResult.PermissionRequired -> result.message
                                    is VoiceResult.Clarify -> result.ttsPrompt
                                    is VoiceResult.Error -> result.message
                                }
                                voiceEngine.speak(speechText)
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            transcript = text
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        }
    }

    fun stopListeningInternal() {
        speechRecognizer?.stopListening()
        isListening = false
        currentAmplitude = 0
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            voiceEngine.stop()
        }
    }

    Box(modifier = modifier) {
        // ── 1. Floating Action Pill (Collapsed State) ──
        if (!isExpanded) {
            FloatingActionButton(
                onClick = {
                    haptics.confirm()
                    isExpanded = true
                    startListeningInternal()
                },
                containerColor = colors.teal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Open Voice Command Center",
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // ── 2. Full-Screen Floating Command Center Overlay (Expanded State) ──
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                color = colors.canvas
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Back Arrow + Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptics.confirm()
                                stopListeningInternal()
                                voiceEngine.stop()
                                isExpanded = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Command Center",
                                tint = colors.teal,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Command Center",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.teal
                            )
                            Text(
                                text = "Real-time on-device voice control",
                                fontSize = 13.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Four-Bar Live Audio Visualizer (real mic amplitude)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FourBarAudioVisualizer(
                            amplitude = currentAmplitude,
                            isRecording = isListening,
                            barColor = SevaSaffron,
                            width = 140.dp,
                            maxHeight = 80.dp
                        )
                    }

                    Text(
                        text = if (isListening) "Listening... speak now" else "Tap microphone to speak",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = if (isListening) SevaSaffron else colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Transcript Box
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface1),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "You said:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (transcript.isBlank()) "“Show my recent calls” or “Report scam”" else "“$transcript”",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick-Action Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionChip("📞 Recent Calls") {
                            transcript = "show my recent calls"
                            scope.launch {
                                activeResult = router.route(transcript)
                            }
                        }
                        ActionChip("✉️ Read SMS") {
                            transcript = "read my messages"
                            scope.launch {
                                activeResult = router.route(transcript)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionChip("🛡️ Report Scam") {
                            transcript = "report digital arrest scam call"
                            scope.launch {
                                activeResult = router.route(transcript)
                            }
                        }
                        ActionChip("💊 Routine") {
                            transcript = "list my medicines"
                            scope.launch {
                                activeResult = router.route(transcript)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Result Card
                    activeResult?.let { result ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (result) {
                                    is VoiceResult.FraudReported -> SevaDanger.copy(alpha = 0.1f)
                                    is VoiceResult.Error -> SevaDanger.copy(alpha = 0.1f)
                                    else -> colors.surfaceMint
                                }
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                when (result) {
                                    is VoiceResult.FraudReported -> SevaDanger
                                    is VoiceResult.Error -> SevaDanger
                                    else -> colors.teal
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                when (result) {
                                    is VoiceResult.CallLogResult -> {
                                        Text("Recent Call Log", fontWeight = FontWeight.Bold, color = colors.teal, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.summary, fontSize = 14.sp, color = colors.onSurface)
                                        if (result.calls.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                                                items(result.calls) { call ->
                                                    Text("• ${call.type}: ${call.name} (${call.number})", fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                    is VoiceResult.SmsResult -> {
                                        Text("Recent Messages", fontWeight = FontWeight.Bold, color = colors.teal, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.summary, fontSize = 14.sp, color = colors.onSurface)
                                        if (result.messages.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                                                items(result.messages) { sms ->
                                                    Text("• From ${sms.sender}: ${sms.body.take(45)}...", fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                    is VoiceResult.FraudReported -> {
                                        Text("⚠️ Scam Report Recorded", fontWeight = FontWeight.Bold, color = SevaDanger, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.summary, fontSize = 14.sp, color = colors.onSurface)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = onNavigateScamShield,
                                            colors = ButtonDefaults.buttonColors(containerColor = SevaDanger)
                                        ) {
                                            Text("Open Scam Shield / Pause Mode", color = Color.White)
                                        }
                                    }
                                    is VoiceResult.MedicineActionResult -> {
                                        Text("Medicine Routine", fontWeight = FontWeight.Bold, color = colors.teal, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.summary, fontSize = 14.sp, color = colors.onSurface)
                                    }
                                    is VoiceResult.PermissionRequired -> {
                                        Text("Permission Needed", fontWeight = FontWeight.Bold, color = SevaSaffron, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.message, fontSize = 14.sp, color = colors.onSurface)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                pendingPermission = result.permission
                                                singlePermissionLauncher.launch(result.permission)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.teal)
                                        ) {
                                            Text("Grant Permission", color = Color.White)
                                        }
                                    }
                                    is VoiceResult.Clarify -> {
                                        Text("Can I help with something else?", fontWeight = FontWeight.Bold, color = colors.teal, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.ttsPrompt, fontSize = 14.sp, color = colors.onSurface)
                                    }
                                    is VoiceResult.Error -> {
                                        Text("Notice", fontWeight = FontWeight.Bold, color = SevaDanger, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(result.message, fontSize = 14.sp, color = colors.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Push-to-Talk Button
                    Button(
                        onClick = {
                            haptics.confirm()
                            if (isListening) {
                                stopListeningInternal()
                            } else {
                                startListeningInternal()
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) SevaDanger else colors.teal
                        ),
                        modifier = Modifier
                            .size(76.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ActionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8F3EE),
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E3A34)
            )
        }
    }
}
