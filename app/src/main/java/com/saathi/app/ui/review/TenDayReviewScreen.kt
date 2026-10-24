package com.saathi.app.ui.review

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.components.PressablePillButton
import com.saathi.app.ui.components.PressablePillStyle
import com.saathi.app.ui.mascot.MascotState
import com.saathi.app.ui.mascot.SparrowMascotView
import com.saathi.app.ui.theme.SevaBodyTeal
import com.saathi.app.ui.theme.SevaCream
import com.saathi.app.ui.theme.SevaCreamContainer
import com.saathi.app.ui.theme.SevaDarkText
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaMint
import com.saathi.app.ui.theme.SevaPeach
import com.saathi.app.ui.theme.SevaSafeGreen
import com.saathi.app.ui.theme.SevaSafeGreenContainer
import com.saathi.app.ui.theme.SevaSaffron
import com.saathi.app.ui.theme.SevaWhite

/**
 * 10-Day Care Review Report Screen.
 *
 * Provides a factual, transparent routine ledger for older adults and their doctors/caregivers.
 * Strictly separates factual observations from diagnostic inferences (Technical Blueprint, Section 7).
 */
@Composable
fun TenDayReviewScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isShared by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SevaCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(64.dp)
                        .background(SevaMint, CircleShape)
                        .semantics { contentDescription = "Go back to Home" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = SevaDeepTeal,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "10-Day Care Review",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "Factual routine & doctor visit notes",
                        fontSize = 16.sp,
                        color = SevaBodyTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Companion Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SevaCreamContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparrowMascotView(
                        state = MascotState.IDLE_SOFT,
                        size = 80.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Routine Period: 12 – 21 Oct 2026",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDarkText
                        )
                        Text(
                            text = "Here are your facts for your doctor or family. Nothing here is an automated diagnosis.",
                            fontSize = 15.sp,
                            color = SevaBodyTeal,
                            lineHeight = 20.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            onSpeak("Here is your 10-day review. 28 of 30 medicine doses were taken on time. You reported dizziness on two days.")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(SevaDeepTeal, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Listen to summary",
                            tint = SevaWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Routine Facts & Adherence
            Text(
                text = "Medication Routine",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SevaWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SevaMint, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Doses Taken on Time",
                            fontSize = 17.sp,
                            color = SevaBodyTeal
                        )
                        Text(
                            text = "28 / 30 (93%)",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaSafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Snoozed Reminders",
                            fontSize = 17.sp,
                            color = SevaBodyTeal
                        )
                        Text(
                            text = "2 times",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SevaDarkText
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unconfirmed Doses",
                            fontSize = 17.sp,
                            color = SevaBodyTeal
                        )
                        Text(
                            text = "0 doses",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SevaSafeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Self-Reported Wellbeing Notes (Factual provenance)
            Text(
                text = "Self-Reported Wellbeing Notes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SevaPeach),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "• 15 Oct, 9:15 AM: Reported mild dizziness after morning routine.",
                        fontSize = 16.sp,
                        color = SevaDarkText,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 18 Oct, 8:40 PM: Noted good sleep and steady energy.",
                        fontSize = 16.sp,
                        color = SevaDarkText,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Note: These are exact records of what you stated during morning check-ins.",
                        fontSize = 13.sp,
                        color = Color(0xFF6E4000)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Questions to Ask Doctor
            Text(
                text = "Questions for Doctor Visit",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SevaWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SevaMint, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = SevaDeepTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Should Glycomet 500mg continue to be taken after food or before?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SevaDarkText,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = SevaDeepTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Mention the dizziness noted on 15 Oct to doctor during consultation.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SevaDarkText,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share Action
            PressablePillButton(
                text = if (isShared) "✓ Report Prepared for Doctor" else "Share Report with Doctor / Family",
                onClick = {
                    isShared = true
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "SevaCare 10-Day Routine Summary (12-21 Oct 2026):\n- Adherence: 28/30 doses on time (93%)\n- Self-reported notes: Mild dizziness reported 15 Oct.\n- Questions for Doctor: Glycomet timing review."
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share 10-Day Care Report"))
                },
                style = PressablePillStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
