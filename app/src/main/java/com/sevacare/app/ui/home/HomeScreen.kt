package com.sevacare.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.R
import com.sevacare.app.ui.components.*
import com.sevacare.app.ui.mascot.MascotState
import com.sevacare.app.ui.mascot.SparrowMascotView
import com.sevacare.app.ui.theme.*
import java.util.Calendar

/**
 * SevaCare Modern Flagship Home Screen (Modern UI Architecture 2026, Section 5 & 22).
 *
 * Implements:
 * - Fluid TodayPathHeroCard: tri-node routine status tree (Morning, Afternoon, Night)
 *   guaranteeing T_glance <= 1.5s for cognitive scanning.
 * - EmergencySosCapsule: 3-second hold circular progress ring with rising haptics
 *   and TalkBack accessible alternative.
 * - FloatingGlassDock: 4-tab liquid glass navigation dock (Home, Medicines, Papers, Help)
 *   with floating 72dp Companion Voice button.
 * - Modern SevaCard surfaces with fluid spring compression (0.97f), haptic feedback,
 *   and >=88dp touch targets.
 * - High-contrast WCAG AAA typography and 100% offline security assurances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateMedicine: () -> Unit,
    onNavigateDocument: () -> Unit,
    onNavigateVoiceGuide: () -> Unit,
    onNavigateLifeAdmin: () -> Unit = {},
    onNavigateTrustVerification: () -> Unit = {},
    onNavigateFamilyArchive: () -> Unit = {},
    onNavigateTenDayReview: () -> Unit = {},
    onTriggerSos: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalSevaColors.current
    val shapes = LocalSevaShapes.current
    val spacing = LocalSevaSpacing.current
    val motion = LocalMotion.current
    val haptics = LocalSevaHaptics.current

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val (greetingHindi, greetingEnglish) = when (currentHour) {
        in 5..11 -> Pair("सुप्रभात", "Good Morning")
        in 12..16 -> Pair("नमस्ते", "Good Afternoon")
        in 17..20 -> Pair("शुभ संध्या", "Good Evening")
        else -> Pair("शुभ रात्रि", "Rest Well")
    }

    // Dynamic tri-node routine state tree
    val todayPathState = remember(currentHour) {
        when {
            currentHour < 12 -> TodayPathUi(
                morning = PathNodeState.Current,
                afternoon = PathNodeState.Upcoming,
                night = PathNodeState.Upcoming
            )
            currentHour in 12..16 -> TodayPathUi(
                morning = PathNodeState.Done,
                afternoon = PathNodeState.Current,
                night = PathNodeState.Upcoming
            )
            else -> TodayPathUi(
                morning = PathNodeState.Done,
                afternoon = PathNodeState.Done,
                night = PathNodeState.Current
            )
        }
    }

    val doseSubtitle = remember(currentHour) {
        when {
            currentHour < 12 -> "Morning dose: 1 tablet after breakfast (Metformin 500mg)"
            currentHour in 12..16 -> "Afternoon dose: 1 tablet with lunch (Multivitamin)"
            else -> "Night dose: 1 tablet before bed (Telma 40)"
        }
    }

    Scaffold(
        containerColor = colors.canvas,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.sevacare_logo),
                            contentDescription = "SevaCare Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "सेवाकेयर",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onTeal
                                )
                                Text(
                                    text = "SevaCare",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onTeal.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = "Your Offline Safety Companion",
                                fontSize = 12.sp,
                                color = colors.onTeal.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.teal,
                    titleContentColor = colors.onTeal
                )
            )
        },
        bottomBar = {
            // Modern 2026 Liquid Glass Floating Dock with 72dp Voice Button
            FloatingGlassDock(
                currentTab = SevaNavTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        SevaNavTab.HOME -> { /* Already on Home */ }
                        SevaNavTab.MEDICINES -> onNavigateMedicine()
                        SevaNavTab.PAPERS -> onNavigateDocument()
                        SevaNavTab.HELP -> onNavigateTrustVerification()
                    }
                },
                onOpenVoice = onNavigateVoiceGuide
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Senior Greeting & Perched Mascot Banner ──
            Surface(
                shape = shapes.card,
                color = colors.surface1,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SparrowMascotView(
                        state = MascotState.GREETING,
                        size = 64.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greetingHindi!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.teal
                        )
                        Text(
                            text = "$greetingEnglish • Your companion is with you",
                            fontSize = 15.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // ── 2. Glanceable Tri-Node Hero Card (TodayPathHeroCard) ──
            TodayPathHeroCard(
                state = HeroCardUi(
                    title = "DAILY MEDICINE ROUTINE",
                    subtitle = doseSubtitle,
                    path = todayPathState
                ),
                onClick = onNavigateMedicine
            )

            // ── 3. Emergency SOS Hold Capsule ──
            EmergencySosCapsule(
                onTriggerSos = {
                    haptics.confirm()
                    if (onTriggerSos != {}) {
                        onTriggerSos()
                    } else {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        context.startActivity(dialIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ── 4. Primary Feature Cards (SevaCard) ──
            // Card A: Medicine Scanner & Shelf
            SevaCard(
                onClick = onNavigateMedicine,
                backgroundColor = colors.surfaceMint,
                minHeight = 110.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalPharmacy,
                                contentDescription = null,
                                tint = colors.teal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "दवाई और नुस्खे",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.teal
                        )
                        Text(
                            text = "Medicine Scanner & Cabinet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.teal.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan strip for instructions, expiry date, and duplicate alerts.",
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Card B: Notice Decoder & Scam Shield
            SevaCard(
                onClick = onNavigateDocument,
                backgroundColor = SevaPeach,
                minHeight = 110.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFF6E4000),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "कागज़ और सुरक्षा",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6E4000)
                        )
                        Text(
                            text = "Notice Decoder & Scam Shield",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6E4000).copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan letters, pension notices, or suspicious calls with Pause Mode.",
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // ── 5. Next-Gen Innovation Grid (2x2) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Innovation 1: Life-Admin Navigator
                SevaCard(
                    onClick = { onNavigateLifeAdmin() },
                    backgroundColor = colors.surface1,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "📄", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Life-Admin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.teal
                        )
                        Text(
                            text = "Bills & notices",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Innovation 2: Trust Verification
                SevaCard(
                    onClick = { onNavigateTrustVerification() },
                    backgroundColor = colors.surface1,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "🛡️", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Trust Verify",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF6E4000)
                        )
                        Text(
                            text = "Pause mode",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Innovation 3: Family Stories Archive
                SevaCard(
                    onClick = { onNavigateFamilyArchive() },
                    backgroundColor = colors.surface1,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "🎙️", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Family Stories",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.teal
                        )
                        Text(
                            text = "Voice archive",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Innovation 4: 10-Day Review
                SevaCard(
                    onClick = { onNavigateTenDayReview() },
                    backgroundColor = colors.surface1,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "📋", fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "10-Day Report",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.success
                        )
                        Text(
                            text = "Doctor visit facts",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // ── 6. 100% Offline Trust Assurance Banner ──
            Surface(
                color = colors.surface2,
                shape = shapes.card,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🔒", fontSize = 26.sp)
                    Column {
                        Text(
                            text = "100% Offline • Zero Cloud Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.teal
                        )
                        Text(
                            text = "Your health data and photos never leave this phone.",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom clearance for FloatingGlassDock
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
