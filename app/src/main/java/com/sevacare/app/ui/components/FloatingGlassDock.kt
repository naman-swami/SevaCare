package com.sevacare.app.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.ui.mascot.MascotState
import com.sevacare.app.ui.mascot.SparrowMascotView
import com.sevacare.app.ui.theme.LocalMotion
import com.sevacare.app.ui.theme.LocalSevaColors
import com.sevacare.app.ui.theme.LocalSevaHaptics
import com.sevacare.app.ui.theme.LocalSimpleMode

enum class SevaNavTab {
    HOME,
    MEDICINES,
    PAPERS,
    HELP
}

/**
 * Floating Liquid Glass Navigation Dock (Modern UI Blueprint 2026, Section 5.1 & 8.5).
 *
 * Implements:
 * - 4 labelled destinations (Home, Medicines, Papers, Help)
 * - 72dp height, floating 16-24dp from edges
 * - Tiered rendering: Glass tint + border on modern devices, solid fallback on low-end
 * - Floating 72dp Companion Voice button on the bottom right
 */
@Composable
fun FloatingGlassDock(
    currentTab: SevaNavTab,
    onTabSelected: (SevaNavTab) -> Unit,
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSevaColors.current
    val haptics = LocalSevaHaptics.current
    val simpleMode = LocalSimpleMode.current
    val isTierC = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || simpleMode

    val dockBackground = if (isTierC) {
        colors.surface1
    } else {
        colors.surface1.copy(alpha = 0.94f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── The 4-Tab Navigation Dock ────────────────────────
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = dockBackground,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .border(
                        width = 1.5.dp,
                        color = colors.outline.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(percent = 50)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockTabItem(
                        tab = SevaNavTab.HOME,
                        label = "Home",
                        icon = Icons.Default.Home,
                        isSelected = currentTab == SevaNavTab.HOME,
                        onSelect = {
                            haptics.tick()
                            onTabSelected(SevaNavTab.HOME)
                        }
                    )

                    DockTabItem(
                        tab = SevaNavTab.MEDICINES,
                        label = "Medicines",
                        icon = Icons.Default.LocalPharmacy,
                        isSelected = currentTab == SevaNavTab.MEDICINES,
                        onSelect = {
                            haptics.tick()
                            onTabSelected(SevaNavTab.MEDICINES)
                        }
                    )

                    DockTabItem(
                        tab = SevaNavTab.PAPERS,
                        label = "Papers",
                        icon = Icons.Default.Description,
                        isSelected = currentTab == SevaNavTab.PAPERS,
                        onSelect = {
                            haptics.tick()
                            onTabSelected(SevaNavTab.PAPERS)
                        }
                    )

                    DockTabItem(
                        tab = SevaNavTab.HELP,
                        label = "Help",
                        icon = Icons.Default.VolunteerActivism,
                        isSelected = currentTab == SevaNavTab.HELP,
                        onSelect = {
                            haptics.tick()
                            onTabSelected(SevaNavTab.HELP)
                        }
                    )
                }
            }

            // ── Floating 72dp Companion Voice Button ─────────────
            FloatingVoiceButton(
                onClick = {
                    haptics.confirm()
                    onOpenVoice()
                }
            )
        }
    }
}

@Composable
private fun DockTabItem(
    tab: SevaNavTab,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colors = LocalSevaColors.current
    val tint by animateColorAsState(
        targetValue = if (isSelected) colors.teal else colors.onSurfaceVariant.copy(alpha = 0.7f),
        label = "tabTint"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "$label tab${if (isSelected) ", active" else ""}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
private fun FloatingVoiceButton(
    onClick: () -> Unit
) {
    val colors = LocalSevaColors.current
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = motion.press,
        label = "talkScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = colors.saffron,
        shadowElevation = 8.dp,
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(width = 2.dp, color = colors.onSaffron.copy(alpha = 0.2f), shape = CircleShape)
            .semantics {
                contentDescription = "Talk to Companion. Opens live conversational voice session."
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = colors.onSaffron,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Talk",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSaffron
            )
        }
    }
}
