package com.saathi.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saathi.app.ui.theme.SevaSaffron

/**
 * Production Four-Bar live audio waveform visualizer.
 * Animates 4 rounded bars from real amplitude polling sampled at ~60ms intervals.
 */
@Composable
fun FourBarAudioVisualizer(
    amplitude: Int,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SevaSaffron,
    width: Dp = 100.dp,
    maxHeight: Dp = 60.dp
) {
    // Normalize raw 16-bit amplitude (0..32767) into a 0.1f .. 1.0f range
    val normalizedAmp = if (isRecording) {
        ((amplitude.toFloat() / 25000f).coerceIn(0.08f, 1.0f))
    } else {
        0.08f
    }

    val animSpec = tween<Float>(durationMillis = 60, easing = FastOutSlowInEasing)

    // Staggered responsiveness per bar for natural speech motion
    val h0 by animateFloatAsState(targetValue = if (isRecording) normalizedAmp * 0.70f else 0.08f, animationSpec = animSpec, label = "h0")
    val h1 by animateFloatAsState(targetValue = if (isRecording) normalizedAmp * 1.00f else 0.12f, animationSpec = animSpec, label = "h1")
    val h2 by animateFloatAsState(targetValue = if (isRecording) normalizedAmp * 0.85f else 0.10f, animationSpec = animSpec, label = "h2")
    val h3 by animateFloatAsState(targetValue = if (isRecording) normalizedAmp * 0.65f else 0.08f, animationSpec = animSpec, label = "h3")

    Canvas(
        modifier = modifier
            .width(width)
            .height(maxHeight)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barCount = 4
        val barWidth = (totalWidth / (barCount * 2)).coerceAtLeast(6f)
        val gap = (totalWidth - (barWidth * barCount)) / (barCount + 1)
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val barHeights = listOf(h0, h1, h2, h3)

        for (i in 0 until barCount) {
            val barH = (canvasHeight * barHeights[i]).coerceIn(barWidth, canvasHeight)
            val x = gap + i * (barWidth + gap)
            val y = (canvasHeight - barH) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}
