package com.saathi.app.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saathi.app.R
import com.saathi.app.data.ai.NeuralIntent
import com.saathi.app.data.ai.SevaNeuralEngine
import com.saathi.app.data.repository.SaathiRepository
import com.saathi.app.ui.components.VoiceOrb
import com.saathi.app.ui.components.VoiceUiState
import com.saathi.app.ui.mascot.MascotState
import com.saathi.app.ui.mascot.SparrowMascotView
import com.saathi.app.ui.theme.*

/**
 * Signature Live Voice Conversation Screen (Modern UI Architecture 2026).
 *
 * Implements:
 * - Fluid VoiceOrb visualizer driven by real PCM / RMS audio level
 * - Perched companion mascot dynamically synchronized with conversational state
 * - Clear, high-contrast streaming captions (22sp) for senior cognitive scanning
 * - Explicit conversational state labels ("Listening - Mic ON", "Thinking", "Speaking", "Idle")
 * - Interactive Medicine & Cyber Threat assessment cards grounded in local SQLite
 * - High-contrast immediate STOP tap-to-interrupt button (>=88dp target)
 * - Accessible fallback Keyboard mode with intelligent dynamic query chips
 * - Emergency 112 red alert dialer banner with zero delay
 * - TalkBack live region polite announcements for all state transitions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveVoiceScreen(
    repository: SaathiRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val colors = LocalSevaColors.current
    val shapes = LocalSevaShapes.current
    val motion = LocalMotion.current
    val haptics = LocalSevaHaptics.current
    val isReducedMotion = LocalReducedMotion.current

    val viewModel: VoiceGuideViewModel = viewModel(
        factory = VoiceGuideViewModelFactory(repository)
    )

    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val spokenText by viewModel.spokenText.collectAsState()
    val saathiResponse by viewModel.saathiResponse.collectAsState()
    val showEmergencyDialer112 by viewModel.showEmergencyDialer112.collectAsState()
    val brainResponse by viewModel.lastBrainResponse.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.startListening(context)
        }
    }

    var isHindiLanguage by remember { mutableStateOf(true) }
    var showKeyboardSheet by remember { mutableStateOf(false) }
    var typedQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.initVoice(context)
        viewModel.setLanguage(isHindiLanguage)
    }

    // Determine current VoiceUiState
    val currentVoiceState: VoiceUiState = remember(isListening, isSpeaking, audioLevel, spokenText, saathiResponse, showEmergencyDialer112) {
        when {
            showEmergencyDialer112 -> VoiceUiState.Error("Medical emergency alert triggered.")
            isSpeaking -> VoiceUiState.Speaking(level = audioLevel.coerceAtLeast(0.6f), caption = saathiResponse)
            isListening -> VoiceUiState.Listening(level = audioLevel, partial = spokenText)
            else -> VoiceUiState.Idle
        }
    }

    // Synchronize Mascot State
    val mascotState = when {
        showEmergencyDialer112 -> MascotState.HELP_NEEDED
        isSpeaking -> MascotState.SPEAKING
        isListening -> MascotState.LISTENING
        else -> MascotState.IDLE_SOFT
    }

    // Semantic announcement for TalkBack
    val stateAnnouncement = when {
        showEmergencyDialer112 -> "Emergency warning. Help is available."
        isSpeaking -> "Speaking. $saathiResponse"
        isListening -> "Listening for your voice. Microphone is on."
        else -> "Companion is ready. Tap start to talk or tap keyboard."
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = stateAnnouncement
            },
        containerColor = colors.canvas,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceMint,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Text(
                            text = stringResource(R.string.voice_companion_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ink
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.stopListening()
                            viewModel.stopSpeaking()
                            onBack()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .semantics { contentDescription = "Close Voice Guide and return home" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    // Language Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = colors.surface2,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable {
                                isHindiLanguage = !isHindiLanguage
                                viewModel.setLanguage(isHindiLanguage)
                                haptics.toggle(isHindiLanguage)
                            }
                            .semantics {
                                contentDescription = if (isHindiLanguage) "Switched to Hindi. Tap for English." else "Switched to English. Tap for Hindi."
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "हिं",
                                fontSize = 14.sp,
                                fontWeight = if (isHindiLanguage) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isHindiLanguage) colors.teal else colors.onSurfaceVariant
                            )
                            Text(
                                text = "|",
                                fontSize = 13.sp,
                                color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "EN",
                                fontSize = 14.sp,
                                fontWeight = if (!isHindiLanguage) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (!isHindiLanguage) colors.teal else colors.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.canvas,
                    titleContentColor = colors.ink
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Section 1: Top Status Banner & Emergency Dial ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Emergency Alert Banner
                AnimatedVisibility(visible = showEmergencyDialer112) {
                    Surface(
                        shape = shapes.card,
                        color = colors.danger,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "चिकित्सा आपातकाल / EMERGENCY",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "लक्षण गंभीर हैं। कृपया तुरंत 112 पर कॉल करें।",
                                fontSize = 15.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                    context.startActivity(dialIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = colors.danger,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Call 112 Immediately",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.danger
                                )
                            }
                        }
                    }
                }

                // Explicit State Indicator Pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = when {
                        isListening -> colors.surfaceMint.copy(alpha = 0.5f)
                        isSpeaking -> colors.saffron.copy(alpha = 0.25f)
                        else -> colors.surface2
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isListening) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = colors.teal,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.voice_listening),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.teal
                            )
                        } else if (isSpeaking) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = colors.ink,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.voice_speaking),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "100% Offline • Neural Care Companion",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Section 2: Visual Centerpiece (VoiceOrb + Sparrow Mascot) ──
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .semantics { contentDescription = "Live Voice Orb Visualizer" },
                contentAlignment = Alignment.Center
            ) {
                VoiceOrb(
                    state = currentVoiceState,
                    reducedMotion = isReducedMotion,
                    modifier = Modifier.fillMaxSize()
                )

                // The Sparrow Companion Mascot perched at the center
                Surface(
                    shape = CircleShape,
                    color = colors.surface1.copy(alpha = 0.90f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        SparrowMascotView(
                            state = mascotState,
                            size = 80.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 3: Live Streaming Captions (22sp) ──
            Surface(
                shape = shapes.card,
                color = colors.surface1,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val activeCaption = when {
                        isSpeaking -> saathiResponse
                        isListening && spokenText.isNotBlank() -> spokenText
                        isListening -> if (isHindiLanguage) "आपकी आवाज़ सुन रहा हूँ..." else "Listening to your voice..."
                        else -> saathiResponse
                    }

                    Text(
                        text = activeCaption,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Interactive Payloads: Matched Medicine Card ──
            brainResponse?.matchedMedicine?.let { med ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = shapes.card,
                    color = colors.surfaceMint.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.teal.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "💊", fontSize = 20.sp)
                            Text(
                                text = med.brandName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.teal
                            )
                        }
                        Text(
                            text = "Active Salt: ${med.genericSalt}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                        Text(
                            text = "Generic Jan Aushadhi: ${med.janAushadhiName}",
                            fontSize = 13.sp,
                            color = colors.success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Interactive Payloads: Scam Assessment Card ──
            brainResponse?.scamAssessment?.let { assessment ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = shapes.card,
                    color = colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.danger.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 20.sp)
                            Text(
                                text = "THREAT LEVEL: ${assessment.coercionLevel}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.danger
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = assessment.recommendedAction,
                            fontSize = 13.sp,
                            color = colors.ink,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 4: Dynamic Senior Follow-Up Suggestions ──
            val suggestions = brainResponse?.followUpSuggestions ?: listOf(
                if (isHindiLanguage) "दवाई का समय क्या है?" else "When to take medicine?",
                if (isHindiLanguage) "Scam call जांचें" else "Spot scam call"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isHindiLanguage) "जल्दी पूछें / Follow-up:" else "Quick Questions / Follow-up:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.take(2).forEach { suggestion ->
                        SuggestionChip(
                            onClick = {
                                haptics.tick()
                                viewModel.processUserQuery(suggestion)
                            },
                            label = { Text(suggestion, fontSize = 13.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = colors.surface2,
                                labelColor = colors.ink
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 5: Bottom Interactive Controls (>=88dp) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Action: Keyboard Text Input Fallback
                Surface(
                    onClick = {
                        haptics.tick()
                        showKeyboardSheet = !showKeyboardSheet
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface2,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .semantics {
                            contentDescription = "Open keyboard text input fallback"
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keyboard",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ink
                        )
                    }
                }

                // Primary Action: Large High-Contrast Mic / Stop Action (>=88dp target)
                val isBusy = isListening || isSpeaking
                val actionColor by animateColorAsState(
                    targetValue = if (isBusy) colors.danger else colors.saffron,
                    label = "actionColor"
                )

                Surface(
                    onClick = {
                        if (isBusy) {
                            haptics.confirm()
                            viewModel.stopListening()
                            viewModel.stopSpeaking()
                        } else {
                            if (!hasAudioPermission) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                haptics.confirm()
                                viewModel.startListening(context)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = actionColor,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(88.dp)
                        .semantics {
                            contentDescription = if (isBusy) "Stop talking or listening immediately" else "Start talking to companion"
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBusy) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isBusy) Color.White else colors.onSaffron,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBusy) stringResource(R.string.voice_btn_stop) else stringResource(R.string.voice_btn_talk),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBusy) Color.White else colors.onSaffron
                        )
                    }
                }
            }

            // Optional Keyboard Text Input Tray
            AnimatedVisibility(visible = showKeyboardSheet) {
                Surface(
                    shape = shapes.card,
                    color = colors.surface1,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.voice_prompt_type_question),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ink
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = typedQuery,
                                onValueChange = { typedQuery = it },
                                placeholder = { Text(stringResource(R.string.voice_example_placeholder)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (typedQuery.isNotBlank()) {
                                            focusManager.clearFocus()
                                            viewModel.processUserQuery(typedQuery)
                                            typedQuery = ""
                                            showKeyboardSheet = false
                                        }
                                    }
                                )
                            )
                            Button(
                                onClick = {
                                    if (typedQuery.isNotBlank()) {
                                        focusManager.clearFocus()
                                        viewModel.processUserQuery(typedQuery)
                                        typedQuery = ""
                                        showKeyboardSheet = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.teal),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send question")
                            }
                        }
                    }
                }
            }
        }
    }
}
