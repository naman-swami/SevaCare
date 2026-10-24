package com.sevacare.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sevacare.app.ui.theme.LocalMotion
import com.sevacare.app.ui.theme.LocalSevaColors
import com.sevacare.app.ui.theme.LocalSimpleMode
import kotlinx.coroutines.delay

/**
 * State machine representing live companion voice states (Section 7.2).
 */
sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data class Listening(val level: Float = 0f, val partial: String = "") : VoiceUiState
    data object Thinking : VoiceUiState
    data class Speaking(val level: Float = 0f, val caption: String = "") : VoiceUiState
    data class Error(val message: String) : VoiceUiState
    data object Paused : VoiceUiState
}

private const val ORB_SHADER_SRC = """
uniform float2 resolution;
uniform float timeSec;
uniform float level;      // smoothed audio level 0..1
uniform float breath;     // 0..1 idle breath
uniform float hueMix;     // 0 = teal (listening), 1 = saffron (speaking)
uniform float think;      // 0..1 thinking rotation amount

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p); float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), f.x),
               mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), f.x), f.y);
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * resolution) / resolution.y;
    float t = timeSec * 0.25;
    float rBase = 0.30 + 0.09 * level + 0.012 * breath;
    float acc = 0.0;
    
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float ang = t * (0.4 + 0.15 * fi) + fi * 1.7 + think * (t * 2.0);
        float2 c = 0.16 * float2(cos(ang), sin(ang))
                 + 0.05 * float2(noise(float2(t + fi, 2.0 * fi)),
                                  noise(float2(3.0 * fi, t - fi)));
        float wobble = 1.0 + 0.35 * level * noise(float2(fragCoord.x * 0.01, t * 3.0));
        float d = length(uv - c) / (rBase * wobble);
        acc += exp(-d * d * 3.0);
    }
    float core = smoothstep(rBase + 0.22, rBase - 0.10, length(uv));
    float glow = clamp(acc * 0.55 + core, 0.0, 1.0);

    half3 teal = half3(0.059, 0.420, 0.361);
    half3 saffron = half3(0.961, 0.651, 0.137);
    half3 cream = half3(1.0, 0.973, 0.925);
    half3 tint = mix(teal, saffron, clamp(hueMix, 0.0, 1.0));
    half3 col = mix(tint, cream, pow(glow, 2.2));
    return half4(col, glow);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createOrbShader(): RuntimeShader = RuntimeShader(ORB_SHADER_SRC)

/**
 * The signature voice visualization surface (Modern UI Blueprint 2026, Section 7).
 *
 * Implements:
 * - AGSL RuntimeShader organic fluid blobs on API 33+
 * - Smooth level-reactive Canvas radial gradient fallback below API 33 or in Simple Mode
 * - 5-second gentle idle breathing loop (disabled when reduced motion is requested)
 * - Accessibility announcements without frame-by-frame spam
 */
@Composable
fun VoiceOrb(
    state: VoiceUiState,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    renderScale: Float = 1f
) {
    val motion = LocalMotion.current
    val colors = LocalSevaColors.current

    val targetLevel = when (state) {
        is VoiceUiState.Listening -> state.level.coerceIn(0f, 1f)
        is VoiceUiState.Speaking -> state.level.coerceIn(0f, 1f)
        else -> 0f
    }
    val level by animateFloatAsState(targetLevel, motion.settle, label = "orbLevel")
    val hueMix by animateFloatAsState(
        targetValue = if (state is VoiceUiState.Speaking) 1f else 0f,
        animationSpec = motion.fade,
        label = "orbHue"
    )
    val think by animateFloatAsState(
        targetValue = if (state is VoiceUiState.Thinking) 1f else 0f,
        animationSpec = motion.fade,
        label = "orbThink"
    )

    // Idle breath only when motion is permitted
    val breath = if (reducedMotion) 0f else rememberBreath()
    val timeSec = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reducedMotion) {
        while (!reducedMotion) {
            timeSec.floatValue += 0.016f
            delay(16)
        }
    }

    val simpleMode = LocalSimpleMode.current
    val agslAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !simpleMode
    val shader = remember(agslAvailable) {
        if (agslAvailable) {
            try {
                createOrbShader()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val stateDescription = when (state) {
        is VoiceUiState.Idle -> "Companion is ready. Tap to speak."
        is VoiceUiState.Listening -> "Listening to your voice."
        is VoiceUiState.Thinking -> "Thinking and checking."
        is VoiceUiState.Speaking -> "Companion is speaking."
        is VoiceUiState.Error -> "Voice notice: ${state.message}"
        is VoiceUiState.Paused -> "Voice session paused."
    }

    Canvas(
        modifier = modifier
            .size(280.dp)
            .graphicsLayer {
                if (renderScale in 0.1f..0.99f) {
                    scaleX = 1f / renderScale
                    scaleY = 1f / renderScale
                }
            }
            .semantics {
                contentDescription = stateDescription
            }
    ) {
        if (shader != null) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("timeSec", timeSec.floatValue)
            shader.setFloatUniform("level", level)
            shader.setFloatUniform("breath", breath)
            shader.setFloatUniform("hueMix", hueMix)
            shader.setFloatUniform("think", think)
            drawRect(brush = ShaderBrush(shader))
        } else {
            // High-quality Canvas radial gradient fallback
            val r = (size.minDimension / 2f) * (0.78f + 0.18f * level + 0.02f * breath)
            val primaryTint = if (hueMix > 0.5f) colors.saffron else colors.teal
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryTint.copy(alpha = 0.95f),
                        colors.canvas.copy(alpha = 0.0f)
                    ),
                    radius = r
                ),
                radius = r
            )
        }
    }
}

/**
 * The only continuous decorative animation permitted in the application.
 * Returns a gentle 0..1 sine wave cycle over 5 seconds.
 */
@Composable
fun rememberBreath(): Float {
    val transition = rememberInfiniteTransition(label = "breathTransition")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathFloat"
    )
    return breath
}
