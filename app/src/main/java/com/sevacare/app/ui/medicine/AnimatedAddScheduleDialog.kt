package com.sevacare.app.ui.medicine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sevacare.app.R
import com.sevacare.app.data.model.Medicine
import com.sevacare.app.data.model.ScheduleDraft
import com.sevacare.app.ui.theme.SevaBodyTeal
import com.sevacare.app.ui.theme.SevaCream
import com.sevacare.app.ui.theme.SevaCreamContainer
import com.sevacare.app.ui.theme.SevaDarkText
import com.sevacare.app.ui.theme.SevaDeepTeal
import com.sevacare.app.ui.theme.SevaMint
import com.sevacare.app.ui.theme.SevaSaffron
import com.sevacare.app.ui.theme.SevaWhite

/**
 * Animated Add-to-Schedule Voice Dialog with Fixed Dialog Height.
 *
 * Implements:
 * 1. Fixed 510dp dialog height — strictly no jumping between questions.
 * 2. 3-Question sequential voice guidance with automatic TTS readout.
 * 3. In-dialog microphone button for seniors to speak their answers.
 * 4. Step 1: Timing, Step 2: Duration, Step 3: Stated Reason.
 */
@Composable
fun AnimatedAddScheduleDialog(
    medicine: Medicine,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleDraft) -> Unit,
    onSpeak: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    var selectedTiming by remember { mutableStateOf("Morning (08:00 AM)") }
    var selectedDays by remember { mutableIntStateOf(10) }
    var reasonText by remember { mutableStateOf("") }
    var activeStep by remember { mutableIntStateOf(1) } // 1: Timing, 2: Days, 3: Reason

    var isMicListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val q1Text = stringResource(R.string.ask_when_taken)
    val q2Text = stringResource(R.string.ask_duration_days)
    val q3Text = stringResource(R.string.ask_reason_optional)

    // Speak question upon active step change
    LaunchedEffect(activeStep) {
        when (activeStep) {
            1 -> onSpeak(q1Text)
            2 -> onSpeak(q2Text)
            3 -> onSpeak(q3Text)
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { isMicListening = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isMicListening = false }
                    override fun onError(error: Int) { isMicListening = false }
                    override fun onResults(results: Bundle?) {
                        isMicListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            when (activeStep) {
                                1 -> {
                                    val lower = text.lowercase()
                                    when {
                                        lower.contains("morning") || lower.contains("subah") -> selectedTiming = "Morning (08:00 AM)"
                                        lower.contains("afternoon") || lower.contains("dopahar") -> selectedTiming = "Afternoon (01:00 PM)"
                                        lower.contains("night") || lower.contains("raat") -> selectedTiming = "Night (08:00 PM)"
                                        lower.contains("twice") || lower.contains("dono") -> selectedTiming = "Twice Daily (Morning & Night)"
                                    }
                                }
                                2 -> {
                                    val digits = text.filter { it.isDigit() }
                                    if (digits.isNotBlank()) {
                                        selectedDays = digits.toInt().coerceIn(1, 365)
                                    }
                                }
                                3 -> {
                                    reasonText = text
                                }
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            speechRecognizer = recognizer
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            }
            recognizer.startListening(intent)
        } catch (_: Exception) {
            isMicListening = false
        }
    }

    fun stopListening() {
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        isMicListening = false
    }

    DisposableEffect(Unit) {
        onDispose {
            try { speechRecognizer?.destroy() } catch (_: Exception) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(510.dp) // Strictly fixed height to eliminate size jumps
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            shape = RoundedCornerShape(28.dp),
            color = SevaCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add to Schedule",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDeepTeal
                        )
                        Text(
                            text = "${medicine.brandName} (${medicine.genericSalt})",
                            fontSize = 13.sp,
                            color = SevaBodyTeal
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voice intake mic button
                        IconButton(
                            onClick = {
                                if (isMicListening) stopListening() else startListening()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(if (isMicListening) Color(0xFFFFDAD6) else SevaMint, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMicListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = if (isMicListening) "Stop voice input" else "Speak answer",
                                tint = if (isMicListening) Color.Red else SevaDeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(SevaMint, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = SevaDeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Indicator (Questions 1, 2, 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (step in 1..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(if (step <= activeStep) SevaSaffron else SevaMint)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question Content (Fixed area weight = 1f)
                Box(modifier = Modifier.weight(1f)) {
                    when (activeStep) {
                        1 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = q1Text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SevaDarkText
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val timings = listOf(
                                    "Morning (08:00 AM)",
                                    "Afternoon (01:00 PM)",
                                    "Night (08:00 PM)",
                                    "Twice Daily (Morning & Night)"
                                )

                                timings.forEach { timing ->
                                    val isSel = selectedTiming == timing
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) SevaCreamContainer else SevaWhite
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .border(
                                                width = if (isSel) 2.dp else 1.dp,
                                                color = if (isSel) SevaSaffron else SevaMint,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedTiming = timing }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AccessTime,
                                                contentDescription = null,
                                                tint = if (isSel) SevaSaffron else SevaDeepTeal,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = timing,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = SevaDarkText
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = q2Text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SevaDarkText,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(28.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (selectedDays > 1) selectedDays-- },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(SevaMint, CircleShape)
                                    ) {
                                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SevaDeepTeal)
                                    }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    Text(
                                        text = "$selectedDays days",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SevaDeepTeal
                                    )

                                    Spacer(modifier = Modifier.width(24.dp))

                                    IconButton(
                                        onClick = { if (selectedDays < 365) selectedDays++ },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(SevaMint, CircleShape)
                                    ) {
                                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SevaDeepTeal)
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf(5, 7, 10, 14, 30).forEach { days ->
                                        FilterChip(
                                            selected = selectedDays == days,
                                            onClick = { selectedDays = days },
                                            label = { Text("${days}d") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SevaSaffron,
                                                selectedLabelColor = SevaDeepTeal
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = q3Text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SevaDarkText
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = reasonText,
                                    onValueChange = { reasonText = it },
                                    placeholder = { Text("e.g. prescribed for sugar control, or skip") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    minLines = 3
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "AI indication verification checks this note against clinical indications and contraindications.",
                                    fontSize = 12.sp,
                                    color = SevaBodyTeal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeStep > 1) {
                        Button(
                            onClick = { activeStep-- },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SevaMint,
                                contentColor = SevaDeepTeal
                            )
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (activeStep < 3) {
                                activeStep++
                            } else {
                                onConfirm(
                                    ScheduleDraft(
                                        medicineId = medicine.id,
                                        medicineName = medicine.brandName,
                                        timeOfDay = selectedTiming,
                                        durationDays = selectedDays,
                                        reason = reasonText.ifBlank { null }
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SevaDeepTeal,
                            contentColor = SevaWhite
                        )
                    ) {
                        Text(if (activeStep < 3) "Next" else "Save to Schedule")
                    }
                }
            }
        }
    }
}
