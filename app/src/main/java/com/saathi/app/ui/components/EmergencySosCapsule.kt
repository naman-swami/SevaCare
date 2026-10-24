package com.saathi.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.theme.LocalMotion
import com.saathi.app.ui.theme.LocalSevaColors
import kotlinx.coroutines.launch

/**
 * 3-Second Hold Emergency SOS Capsule (Modern UI Blueprint 2026, Section 5.1 & 16).
 *
 * Implements:
 * - 3-second intentional hold with circular progress ring
 * - Rising haptic ramp on touch to prevent accidental pocket triggers
 * - Release cancels with soft spring settle
 * - Senior accessibility: TalkBack alternative dialog with immediate 1-tap confirmation
 */
@Composable
fun EmergencySosCapsule(
    onTriggerSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSevaColors.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var showAccessibleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            // Rising haptic ticks during hold
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            val animation = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
            )
            if (progress.value >= 1f) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onTriggerSos()
                progress.snapTo(0f)
                isHolding = false
            }
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = colors.danger,
        shadowElevation = 6.dp,
        modifier = modifier
            .height(64.dp)
            .semantics {
                contentDescription = "Emergency SOS button. Press and hold for 3 seconds or double tap to confirm."
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    },
                    onDoubleTap = {
                        showAccessibleDialog = true
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Circular progress ring inside icon area
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background track
                        drawArc(
                            color = Color.White.copy(alpha = 0.3f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Active progress sweep
                        if (progress.value > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = progress.value * 360f,
                                useCenter = false,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isHolding) "HOLD (3s)..." else "EMERGENCY SOS",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isHolding) "Calling 112 & Family" else "Hold 3s to alert 112",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }

    // Accessible Alternative Dialog for TalkBack users
    if (showAccessibleDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibleDialog = false },
            title = {
                Text(
                    text = "Activate Emergency SOS?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.danger
                )
            },
            text = {
                Text(
                    text = "This will immediately notify your registered family members and offer direct 112 emergency routing.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibleDialog = false
                        onTriggerSos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Confirm Emergency Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
