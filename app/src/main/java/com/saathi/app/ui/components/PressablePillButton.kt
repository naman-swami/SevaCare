package com.saathi.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaDeepTealLip
import com.saathi.app.ui.theme.SevaOnSaffronText
import com.saathi.app.ui.theme.SevaSaffron
import com.saathi.app.ui.theme.SevaSaffronLip
import com.saathi.app.ui.theme.SevaWhite

enum class PressablePillStyle {
    PRIMARY, SECONDARY, DANGER, SUCCESS,
    Primary, Secondary, Danger, Success
}

/**
 * Elder-Friendly 3D Pressable Pill Button (Section 5).
 *
 * Features:
 * - Thick pill shape with a darker-color bottom lip.
 * - Collapses 4dp on press with an animated spring/tween.
 * - Haptic tick on touch down.
 * - Primary buttons >= 88dp height, secondary >= 64dp.
 * - Single-thumb click navigation (no drag/swipe required).
 */
@Composable
fun PressablePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    height: Dp = if (isPrimary) 88.dp else 64.dp,
    fillColor: Color = if (isPrimary) SevaSaffron else SevaDeepTeal,
    lipColor: Color = if (isPrimary) SevaSaffronLip else SevaDeepTealLip,
    textColor: Color = if (isPrimary) SevaOnSaffronText else SevaWhite,
    fontSize: Int = if (isPrimary) 22 else 18,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val lipHeight = 6.dp
    val pressCollapseOffset: Dp by animateDpAsState(
        targetValue = if (isPressed && enabled) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "press_collapse"
    )

    val shape = RoundedCornerShape(percent = 50) // Pill shape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + lipHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Bottom Lip (Darker Shadow Base)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = lipHeight)
                .clip(shape)
                .background(if (enabled) lipColor else Color.Gray.copy(alpha = 0.4f))
        )

        // Top Main Button Surface (Collapses 4dp on press)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressCollapseOffset)
                .clip(shape)
                .background(if (enabled) fillColor else Color.LightGray)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(if (isPrimary) 36.dp else 28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Text(
                    text = text,
                    color = textColor,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun PressablePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PressablePillStyle = PressablePillStyle.PRIMARY,
    enabled: Boolean = true
) {
    val isPrimary = style == PressablePillStyle.PRIMARY || style == PressablePillStyle.Primary
    val fillColor = when (style) {
        PressablePillStyle.PRIMARY, PressablePillStyle.Primary -> SevaSaffron
        PressablePillStyle.SECONDARY, PressablePillStyle.Secondary -> SevaDeepTeal
        PressablePillStyle.DANGER, PressablePillStyle.Danger -> Color(0xFFB3261E)
        PressablePillStyle.SUCCESS, PressablePillStyle.Success -> Color(0xFF2E6930)
    }
    val lipColor = when (style) {
        PressablePillStyle.PRIMARY, PressablePillStyle.Primary -> SevaSaffronLip
        PressablePillStyle.SECONDARY, PressablePillStyle.Secondary -> SevaDeepTealLip
        PressablePillStyle.DANGER, PressablePillStyle.Danger -> Color(0xFF7A1B15)
        PressablePillStyle.SUCCESS, PressablePillStyle.Success -> Color(0xFF1E4820)
    }
    val textColor = when (style) {
        PressablePillStyle.PRIMARY, PressablePillStyle.Primary -> SevaOnSaffronText
        else -> SevaWhite
    }
    PressablePillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        isPrimary = isPrimary,
        fillColor = fillColor,
        lipColor = lipColor,
        textColor = textColor,
        enabled = enabled
    )
}
