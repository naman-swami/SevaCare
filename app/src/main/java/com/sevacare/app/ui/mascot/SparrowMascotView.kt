package com.sevacare.app.ui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.ui.theme.SevaBodyTeal
import com.sevacare.app.ui.theme.SevaCream
import com.sevacare.app.ui.theme.SevaDanger
import com.sevacare.app.ui.theme.SevaDeepTeal
import com.sevacare.app.ui.theme.SevaPeach
import com.sevacare.app.ui.theme.SevaSafeGreen
import com.sevacare.app.ui.theme.SevaSaffron
import com.sevacare.app.ui.theme.SevaWhite
import kotlinx.coroutines.delay

/**
 * 15 Semantic Mascot Animation States defined in SevaCare Animation Architecture (Oct 2026).
 * Domain logic emits states; renderer maps to presentation without reversing dependency.
 */
enum class MascotState(
    val description: String,
    val spokenSummary: String,
    val badgeColor: Color
) {
    IDLE_SOFT(
        description = "Sparrow companion resting peacefully",
        spokenSummary = "I am here with you.",
        badgeColor = SevaDeepTeal
    ),
    GREETING(
        description = "Sparrow companion welcoming with a friendly wing wave",
        spokenSummary = "Welcome! Good to see you.",
        badgeColor = SevaSaffron
    ),
    LISTENING(
        description = "Sparrow companion listening attentively with microphone active",
        spokenSummary = "I am listening. Please speak clearly.",
        badgeColor = SevaDeepTeal
    ),
    THINKING(
        description = "Sparrow companion processing your request on device",
        spokenSummary = "Working on it, one moment please.",
        badgeColor = SevaSaffron
    ),
    SPEAKING(
        description = "Sparrow companion speaking guidance aloud",
        spokenSummary = "Speaking aloud now.",
        badgeColor = SevaDeepTeal
    ),
    POINTING(
        description = "Sparrow companion pointing toward the next action button",
        spokenSummary = "Please look at the highlighted button below.",
        badgeColor = SevaDeepTeal
    ),
    DEMONSTRATING(
        description = "Sparrow companion showing how this feature works step by step",
        spokenSummary = "Here is how to do this.",
        badgeColor = SevaPeach
    ),
    WAITING_CONFIRMATION(
        description = "Sparrow companion waiting for your decision",
        spokenSummary = "Please confirm when you are ready.",
        badgeColor = SevaSaffron
    ),
    REMINDER(
        description = "Sparrow companion offering a gentle scheduled reminder",
        spokenSummary = "A scheduled reminder needs your attention.",
        badgeColor = SevaSaffron
    ),
    GENTLE_SUCCESS(
        description = "Sparrow companion giving an affirmative cheer nod",
        spokenSummary = "Completed successfully!",
        badgeColor = SevaSafeGreen
    ),
    CONCERNED(
        description = "Sparrow companion maintaining calm concern for safety",
        spokenSummary = "Let us pause and check this together carefully.",
        badgeColor = SevaDanger
    ),
    OFFLINE_RETRY(
        description = "Sparrow companion showing offline state with local tools ready",
        spokenSummary = "No internet right now. Local tools are working.",
        badgeColor = SevaDeepTeal
    ),
    HELP_NEEDED(
        description = "Sparrow companion showing alert status with emergency options",
        spokenSummary = "Support options are ready on screen.",
        badgeColor = SevaDanger
    ),
    PAUSED_REDUCED_MOTION(
        description = "Sparrow companion in still mode with motion paused",
        spokenSummary = "Still companion mode.",
        badgeColor = SevaDeepTeal
    ),
    ERROR_FALLBACK(
        description = "Sparrow companion in safe static fallback mode",
        spokenSummary = "Companion is ready.",
        badgeColor = SevaDeepTeal
    )
}

/**
 * Production Jetpack Compose Sparrow Mascot View.
 * Renders an animated or high-contrast static stylized sparrow companion.
 *
 * Senior Usability Features:
 * - Decoupled from care/domain logic: unidirectional presentation
 * - Touch target: >= 88dp for primary interactive tap
 * - Respects system reduced-motion (degrades to static vector immediately)
 * - Accessible TalkBack semantics describing state and next step
 * - High contrast: Warm brown, cream feathers, saffron scarf, deep teal outline
 */
@Composable
fun SparrowMascotView(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    reduceMotion: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isInspection = LocalInspectionMode.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemReduceMotion = remember(context) {
        try {
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
            android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
    val effectiveMotion = !reduceMotion && !systemReduceMotion && state != MascotState.PAUSED_REDUCED_MOTION && !isInspection

    // Infinite breathing bob animation
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_ambient")
    val bobOffset by if (effectiveMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (state == MascotState.LISTENING || state == MascotState.THINKING) -6f else -3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bob"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Wing flap angle animation for celebration/pointing
    val wingAngle by if (effectiveMotion && (state == MascotState.GENTLE_SUCCESS || state == MascotState.GREETING || state == MascotState.REMINDER)) {
        infiniteTransition.animateFloat(
            initialValue = -12f,
            targetValue = 18f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wing"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Eye blink cycle: triggers every ~4.5 seconds
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(effectiveMotion) {
        if (effectiveMotion) {
            while (true) {
                delay(4500)
                isBlinking = true
                delay(160)
                isBlinking = false
            }
        }
    }

    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "${state.description}. ${state.spokenSummary}"
            }
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .defaultMinSize(minWidth = 88.dp, minHeight = 88.dp)
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // Authentic Mascot Character Art with Semantic State Physics & Dynamics
            Image(
                painter = painterResource(id = R.drawable.sparrow_mascot_art),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        translationY = bobOffset * 2f
                        rotationZ = when (state) {
                            MascotState.LISTENING -> 8f
                            MascotState.THINKING -> -6f
                            MascotState.POINTING -> 5f
                            MascotState.CONCERNED -> -7f
                            MascotState.GREETING, MascotState.GENTLE_SUCCESS, MascotState.REMINDER -> wingAngle * 0.35f
                            else -> 0f
                        }
                        scaleX = when (state) {
                            MascotState.GENTLE_SUCCESS -> 1.08f
                            MascotState.POINTING -> 1.04f
                            else -> 1f
                        }
                        scaleY = when (state) {
                            MascotState.GENTLE_SUCCESS -> 1.08f
                            MascotState.SPEAKING -> 1.03f + (bobOffset * 0.005f)
                            else -> 1f + (bobOffset * 0.004f)
                        }
                    }
            )

            // State Badge Indicator overlay at bottom-right
            StateBadgeIndicator(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Procedural Vector Drawing for the Sparrow Mascot:
 * - Rounded geometric body (warm brown #8D5B28 + soft cream belly #FFF3DE)
 * - Saffron scarf (#F5A623) with deep teal outline
 * - Large kind dark eyes with specular white highlight (blinking support)
 * - Stylized beak with speech pulse
 * - Dual wings with state-dependent articulation
 */
private fun DrawScope.drawSparrowMascot(
    state: MascotState,
    bobOffset: Float,
    wingAngle: Float,
    isBlinking: Boolean
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = (h / 2f) + bobOffset

    val strokeWidth = w * 0.045f
    val outlineColor = Color(0xFF0F6B5C) // Deep Teal
    val bodyColor = Color(0xFF8D5B28)    // Warm Sparrow Brown
    val bellyColor = Color(0xFFFFF3DE)   // Soft Cream
    val saffronColor = Color(0xFFF5A623) // Saffron Accent
    val beakColor = Color(0xFFE88A1A)    // Gold Beak
    val eyeColor = Color(0xFF1E1E1E)     // Dark Kind Eyes

    // 1. Sparrow Tail
    val tailPath = Path().apply {
        moveTo(cx - (w * 0.22f), cy + (h * 0.26f))
        lineTo(cx - (w * 0.38f), cy + (h * 0.36f))
        lineTo(cx - (w * 0.16f), cy + (h * 0.34f))
        close()
    }
    drawPath(tailPath, bodyColor)
    drawPath(tailPath, outlineColor, style = Stroke(width = strokeWidth))

    // 2. Main Rounded Body
    val bodyRadius = w * 0.30f
    drawCircle(
        color = bodyColor,
        radius = bodyRadius,
        center = Offset(cx, cy + (h * 0.05f))
    )
    drawCircle(
        color = outlineColor,
        radius = bodyRadius,
        center = Offset(cx, cy + (h * 0.05f)),
        style = Stroke(width = strokeWidth)
    )

    // 3. Cream Belly Patch
    val bellyRadius = w * 0.20f
    drawCircle(
        color = bellyColor,
        radius = bellyRadius,
        center = Offset(cx + (w * 0.05f), cy + (h * 0.12f))
    )

    // 4. Head
    val headRadius = w * 0.22f
    val headCenter = Offset(cx, cy - (h * 0.16f))
    drawCircle(
        color = bodyColor,
        radius = headRadius,
        center = headCenter
    )
    drawCircle(
        color = outlineColor,
        radius = headRadius,
        center = headCenter,
        style = Stroke(width = strokeWidth)
    )

    // 5. Left & Right Eyes (Blinking support)
    val eyeRadius = w * 0.055f
    val leftEyeCenter = Offset(cx - (w * 0.09f), cy - (h * 0.18f))
    val rightEyeCenter = Offset(cx + (w * 0.09f), cy - (h * 0.18f))

    if (isBlinking || state == MascotState.PAUSED_REDUCED_MOTION) {
        // Closed eye slits
        drawLine(
            color = outlineColor,
            start = Offset(leftEyeCenter.x - eyeRadius, leftEyeCenter.y),
            end = Offset(leftEyeCenter.x + eyeRadius, leftEyeCenter.y),
            strokeWidth = strokeWidth * 0.9f
        )
        drawLine(
            color = outlineColor,
            start = Offset(rightEyeCenter.x - eyeRadius, rightEyeCenter.y),
            end = Offset(rightEyeCenter.x + eyeRadius, rightEyeCenter.y),
            strokeWidth = strokeWidth * 0.9f
        )
    } else {
        // Open kind eyes with specular highlight
        drawCircle(color = eyeColor, radius = eyeRadius, center = leftEyeCenter)
        drawCircle(color = eyeColor, radius = eyeRadius, center = rightEyeCenter)
        // Specular white dots
        drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(leftEyeCenter.x - (eyeRadius * 0.3f), leftEyeCenter.y - (eyeRadius * 0.3f)))
        drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(rightEyeCenter.x - (eyeRadius * 0.3f), rightEyeCenter.y - (eyeRadius * 0.3f)))
    }

    // 6. Beak
    val beakPath = Path().apply {
        moveTo(cx - (w * 0.05f), cy - (h * 0.12f))
        lineTo(cx, cy - (h * 0.05f))
        lineTo(cx + (w * 0.05f), cy - (h * 0.12f))
        close()
    }
    drawPath(beakPath, beakColor)
    drawPath(beakPath, outlineColor, style = Stroke(width = strokeWidth * 0.8f))

    // 7. Signature Saffron Scarf
    val scarfPath = Path().apply {
        moveTo(cx - (w * 0.18f), cy - (h * 0.03f))
        quadraticBezierTo(cx, cy + (h * 0.02f), cx + (w * 0.18f), cy - (h * 0.03f))
        lineTo(cx + (w * 0.22f), cy + (h * 0.04f))
        quadraticBezierTo(cx, cy + (h * 0.09f), cx - (w * 0.22f), cy + (h * 0.04f))
        close()
    }
    drawPath(scarfPath, saffronColor)
    drawPath(scarfPath, outlineColor, style = Stroke(width = strokeWidth * 0.8f))

    // 8. Wings (with articulation)
    val leftWingPath = Path().apply {
        moveTo(cx - (w * 0.26f), cy + (h * 0.05f))
        quadraticBezierTo(cx - (w * 0.45f) + (wingAngle * 0.4f), cy + (h * 0.12f), cx - (w * 0.24f), cy + (h * 0.28f))
        close()
    }
    drawPath(leftWingPath, bodyColor)
    drawPath(leftWingPath, outlineColor, style = Stroke(width = strokeWidth))

    val rightWingPath = Path().apply {
        val pointingReach = if (state == MascotState.POINTING) (w * 0.15f) else 0f
        moveTo(cx + (w * 0.26f), cy + (h * 0.05f))
        quadraticBezierTo(cx + (w * 0.45f) + pointingReach - (wingAngle * 0.4f), cy + (h * 0.12f), cx + (w * 0.24f), cy + (h * 0.28f))
        close()
    }
    drawPath(rightWingPath, bodyColor)
    drawPath(rightWingPath, outlineColor, style = Stroke(width = strokeWidth))

    // 9. Pill-shaped Feet
    val footColor = Color(0xFFC76F16)
    drawRoundRect(
        color = footColor,
        topLeft = Offset(cx - (w * 0.12f), cy + (h * 0.32f)),
        size = Size(w * 0.08f, h * 0.06f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = footColor,
        topLeft = Offset(cx + (w * 0.04f), cy + (h * 0.32f)),
        size = Size(w * 0.08f, h * 0.06f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
}

/**
 * Clean State Badge Indicator positioned on the mascot.
 */
@Composable
private fun StateBadgeIndicator(
    state: MascotState,
    modifier: Modifier = Modifier
) {
    val icon = when (state) {
        MascotState.LISTENING -> Icons.Rounded.Mic
        MascotState.THINKING -> Icons.Rounded.Lightbulb
        MascotState.SPEAKING -> Icons.Rounded.VolumeUp
        MascotState.REMINDER -> Icons.Rounded.Notifications
        MascotState.GENTLE_SUCCESS -> Icons.Rounded.CheckCircle
        MascotState.CONCERNED -> Icons.Rounded.Warning
        MascotState.HELP_NEEDED -> Icons.Rounded.Warning
        MascotState.OFFLINE_RETRY -> Icons.Rounded.WifiOff
        MascotState.POINTING, MascotState.DEMONSTRATING -> Icons.Rounded.Info
        else -> null
    }

    if (icon != null) {
        Box(
            modifier = modifier
                .size(28.dp)
                .background(state.badgeColor, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SevaWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
