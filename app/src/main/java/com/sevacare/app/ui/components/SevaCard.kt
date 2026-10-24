package com.sevacare.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sevacare.app.ui.theme.LocalMotion
import com.sevacare.app.ui.theme.LocalSevaColors
import com.sevacare.app.ui.theme.LocalSevaHaptics
import com.sevacare.app.ui.theme.LocalSevaShapes
import com.sevacare.app.ui.theme.LocalSevaSpacing

/**
 * Standard Modern Pressable Surface (Section 4.6).
 *
 * Implements fluid spring physics on touch (0.97f scale compression),
 * subtle haptic feedback, 28dp card radius, and a minimum 88dp touch target.
 */
@Composable
fun SevaCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalSevaColors.current.surface1,
    minHeight: Dp = 88.dp,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalMotion.current
    val haptics = LocalSevaHaptics.current
    val shapes = LocalSevaShapes.current
    val spacing = LocalSevaSpacing.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = motion.press,
        label = "cardScale"
    )

    LaunchedEffect(pressed) {
        if (pressed) haptics.tick()
    }

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = shapes.card,
        color = backgroundColor,
        shadowElevation = elevation,
        modifier = modifier
            .heightIn(min = minHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Column(
            modifier = Modifier.padding(spacing.l),
            content = content
        )
    }
}
