package com.sevacare.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * SevaCare Spring-Based Motion Tokens (Modern UI Blueprint 2026).
 *
 * All motion is spring-based, interruptible, and answers a user action.
 * Standard character (no bounce) is enforced on safety screens;
 * expressive bounce is reserved exclusively for celebration moments.
 */
data class SevaMotionSpecs(
    // Spatial: elements that move, scale, or morph
    val press: SpringSpec<Float> = spring(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMedium
    ),
    val enter: SpringSpec<Float> = spring(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMediumLow
    ),
    val sheet: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessLow
    ),
    val settle: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    // Effects: colour and alpha transitions (no overshoot)
    val fade: TweenSpec<Float> = tween(
        durationMillis = 180,
        easing = FastOutSlowInEasing
    ),
    // Reduced motion: instantaneous transitions with identical end state
    val reduced: TweenSpec<Float> = tween(
        durationMillis = 0
    )
)

object SevaMotion {
    val Default = SevaMotionSpecs()
    val Reduced = SevaMotionSpecs(
        press = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        enter = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        sheet = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        settle = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        fade = tween(durationMillis = 0),
        reduced = tween(durationMillis = 0)
    )
}

val LocalMotion = compositionLocalOf { SevaMotion.Default }
val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun resolveMotion(): SevaMotionSpecs {
    val context = LocalContext.current
    val isReduceMotion = remember(context) {
        try {
            val animScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            animScale == 0f || transScale == 0f
        } catch (e: Exception) {
            false
        }
    }
    return if (isReduceMotion) SevaMotion.Reduced else SevaMotion.Default
}
