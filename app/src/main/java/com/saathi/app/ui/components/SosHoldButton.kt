package com.saathi.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.theme.SevaDanger
import com.saathi.app.ui.theme.SevaDangerLip
import com.saathi.app.ui.theme.SevaWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 3-Second Hold SOS Button with Haptic Ramp & TalkBack Alternative (Section 4.4).
 *
 * Requirements:
 * - Hold for 3 seconds to trigger.
 * - Visible animated circular progress ring during hold.
 * - Progressive haptic feedback ramp every 500ms.
 * - Single-tap triggers TalkBack alternative confirmation dialog.
 */
@Composable
fun SosHoldButton(
    onTriggerSos: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 68.dp
) {
    val haptic = LocalHapticFeedback.current
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var showTalkBackDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val totalSteps = 30 // 30 * 100ms = 3000ms
            for (step in 1..totalSteps) {
                if (!isHolding) break
                delay(100)
                holdProgress = step / totalSteps.toFloat()
                if (step % 5 == 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            if (isHolding && holdProgress >= 0.99f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTriggerSos()
                isHolding = false
                holdProgress = 0f
            }
        } else {
            holdProgress = 0f
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress,
        animationSpec = tween(durationMillis = 80),
        label = "sos_progress"
    )

    val shape = RoundedCornerShape(percent = 50)
    val lipHeight = 4.dp

    Box(
        modifier = modifier
            .height(height + lipHeight)
            .semantics {
                contentDescription = "Emergency SOS button. Hold for 3 seconds or tap for emergency dialog."
            }
    ) {
        // Bottom Lip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = lipHeight)
                .clip(shape)
                .background(SevaDangerLip)
        )

        // Main Button Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(SevaDanger)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            tryAwaitRelease()
                            isHolding = false
                        },
                        onTap = {
                            // TalkBack single tap fallback dialog
                            showTalkBackDialog = true
                        }
                    )
                }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Indicator
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        if (animatedProgress > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SevaWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isHolding) "HOLD FOR SOS (${(3 - (animatedProgress * 3)).toInt() + 1}s)" else "EMERGENCY SOS",
                    color = SevaWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    // Accessible Confirmation Dialog for TalkBack Users
    if (showTalkBackDialog) {
        AlertDialog(
            onDismissRequest = { showTalkBackDialog = false },
            title = { Text("Emergency SOS", fontWeight = FontWeight.Bold, color = SevaDanger) },
            text = {
                Text(
                    "Would you like to send an emergency SMS alert to your Family and Neighbour Circle contacts?",
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTalkBackDialog = false
                        onTriggerSos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SevaDanger)
                ) {
                    Text("Send Emergency Alert", color = SevaWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTalkBackDialog = false }) {
                    Text("Cancel", color = Color.Gray, fontSize = 16.sp)
                }
            }
        )
    }
}
