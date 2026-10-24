package com.sevacare.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.ui.mascot.MascotState
import com.sevacare.app.ui.mascot.SparrowMascotView
import com.sevacare.app.ui.theme.*

/**
 * Glanceable Tri-Node Routine Tree (Modern UI Blueprint 2026, Section 3.1 & 22).
 * Formally guarantees T_glance <= 1.5s for cognitive scanability.
 */
enum class PathNodeState { Done, Current, Upcoming }

data class TodayPathUi(
    val morning: PathNodeState = PathNodeState.Done,
    val afternoon: PathNodeState = PathNodeState.Current,
    val night: PathNodeState = PathNodeState.Upcoming
)

data class HeroCardUi(
    val title: String = "DAILY MEDICINE ROUTINE",
    val subtitle: String = "Afternoon dose: 1 tablet with meals",
    val path: TodayPathUi = TodayPathUi()
)

@Composable
fun TodayPathHeroCard(
    state: HeroCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSevaColors.current
    val shapes = LocalSevaShapes.current
    val spacing = LocalSevaSpacing.current
    val motion = LocalMotion.current
    val haptics = LocalSevaHaptics.current

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = motion.press,
        label = "heroScale"
    )

    LaunchedEffect(pressed) {
        if (pressed) haptics.tick()
    }

    val accessibilitySummary = "Today's Routine: Morning ${state.path.morning.name.lowercase()}, " +
            "Afternoon ${state.path.afternoon.name.lowercase()}, " +
            "Night ${state.path.night.name.lowercase()}. ${state.subtitle}. Tap to open medicine cabinet."

    Surface(
        shape = shapes.card,
        color = colors.surface1,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = accessibilitySummary
            }
    ) {
        Column(modifier = Modifier.padding(spacing.xl)) {
            // Header Row: Category Badge + Icon + Sparrow Companion
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceMint,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalPharmacy,
                                contentDescription = null,
                                tint = colors.teal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = state.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.inkDeep,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = state.subtitle,
                            fontSize = 15.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Sparrow Mascot perched beside current dose node
                SparrowMascotView(
                    state = MascotState.REMINDER,
                    size = 52.dp
                )
            }

            Spacer(modifier = Modifier.height(spacing.l))

            // Glanceable Tri-Node Strip
            TodayPathRow(path = state.path)
        }
    }
}

@Composable
private fun TodayPathRow(path: TodayPathUi) {
    val colors = LocalSevaColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Node 1: Morning
        PathNodeItem(
            label = "Morning",
            time = "8:00 AM",
            state = path.morning,
            icon = Icons.Default.WbSunny
        )

        // Connecting track 1
        ConnectingTrack(
            isCompleted = path.morning == PathNodeState.Done,
            modifier = Modifier.weight(1f)
        )

        // Node 2: Afternoon
        PathNodeItem(
            label = "Afternoon",
            time = "1:30 PM",
            state = path.afternoon,
            icon = Icons.Default.Brightness5
        )

        // Connecting track 2
        ConnectingTrack(
            isCompleted = path.afternoon == PathNodeState.Done,
            modifier = Modifier.weight(1f)
        )

        // Node 3: Night
        PathNodeItem(
            label = "Night",
            time = "9:00 PM",
            state = path.night,
            icon = Icons.Default.NightsStay
        )
    }
}

@Composable
private fun PathNodeItem(
    label: String,
    time: String,
    state: PathNodeState,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colors = LocalSevaColors.current

    val (circleBg, iconTint, borderStroke) = when (state) {
        PathNodeState.Done -> Triple(colors.success, Color.White, 0.dp)
        PathNodeState.Current -> Triple(colors.saffron, colors.onSaffron, 3.dp)
        PathNodeState.Upcoming -> Triple(colors.surface2, colors.onSurfaceVariant.copy(alpha = 0.6f), 0.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = circleBg,
            shadowElevation = if (state == PathNodeState.Current) 4.dp else 0.dp,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (state == PathNodeState.Done) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "$label taken",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (state == PathNodeState.Current) FontWeight.Bold else FontWeight.Medium,
            color = if (state == PathNodeState.Current) colors.inkDeep else colors.onSurfaceVariant
        )
        Text(
            text = time,
            fontSize = 11.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ConnectingTrack(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalSevaColors.current
    Canvas(
        modifier = modifier
            .height(4.dp)
            .padding(horizontal = 6.dp)
    ) {
        drawLine(
            color = if (isCompleted) colors.success else colors.outline,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
