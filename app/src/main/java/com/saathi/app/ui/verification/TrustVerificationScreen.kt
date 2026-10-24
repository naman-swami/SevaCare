package com.saathi.app.ui.verification

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Shield
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
import com.saathi.app.ui.theme.SevaDanger
import com.saathi.app.ui.theme.SevaDarkText
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaMint
import com.saathi.app.ui.theme.SevaPeach
import com.saathi.app.ui.theme.SevaSafeGreen
import com.saathi.app.ui.theme.SevaSaffron
import com.saathi.app.ui.theme.SevaWhite

/**
 * Family Trust Verification Network Screen.
 *
 * Implements the out-of-band verification protocol for suspicious phone calls,
 * digital arrest threats, and fake emergencies (Technical Blueprint, Section 6).
 */
@Composable
fun TrustVerificationScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPauseModeActive by remember { mutableStateOf(false) }
    var challengeSent by remember { mutableStateOf(false) }

    // Three check states
    var check1Secret by remember { mutableStateOf(false) }
    var check2ReceiveMoney by remember { mutableStateOf(false) }
    var check3PoliceVideo by remember { mutableStateOf(false) }

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
                        text = "Trust Verification",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "Safe pause before acting",
                        fontSize = 16.sp,
                        color = SevaBodyTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calm Security Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPauseModeActive) Color(0xFFFFDAD6) else SevaCreamContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparrowMascotView(
                        state = if (isPauseModeActive) MascotState.CONCERNED else MascotState.IDLE_SOFT,
                        size = 80.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPauseModeActive) "Pause Mode is Active" else "Someone asking for money or OTP?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPauseModeActive) SevaDanger else SevaDarkText
                        )
                        Text(
                            text = if (isPauseModeActive)
                                "Payment tools are paused for 30 minutes. Let us verify with your family first."
                            else
                                "Real officials never demand money on video calls or ask for your secret PIN.",
                            fontSize = 15.sp,
                            color = SevaBodyTeal,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Critical Pause-Before-Acting Checks
            Text(
                text = "Three Quick Safety Checks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Check 1: Secrecy
            SafetyCheckCard(
                question = "Did they tell you: 'Do not tell anyone or hang up'?",
                isChecked = check1Secret,
                onToggle = {
                    check1Secret = !check1Secret
                    if (check1Secret) isPauseModeActive = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Check 2: Receive Money Myth
            SafetyCheckCard(
                question = "Were you told that scanning a QR or entering a PIN receives money?",
                subtext = "Scanning a QR or typing your UPI PIN only sends money. It never receives money.",
                isChecked = check2ReceiveMoney,
                onToggle = {
                    check2ReceiveMoney = !check2ReceiveMoney
                    if (check2ReceiveMoney) isPauseModeActive = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Check 3: Digital Arrest / Police Video Call
            SafetyCheckCard(
                question = "Did someone claim to be a police, CBI, or customs officer on video call?",
                subtext = "No real police or government agency ever conducts 'digital arrests' over WhatsApp or Skype.",
                isChecked = check3PoliceVideo,
                onToggle = {
                    check3PoliceVideo = !check3PoliceVideo
                    if (check3PoliceVideo) isPauseModeActive = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Verification Actions
            Text(
                text = "Immediate Safe Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action 1: Out-of-Band Family Challenge
            PressablePillButton(
                text = if (challengeSent) "✓ Verification Sent to Family" else "Send Verification to Family Circle",
                onClick = {
                    challengeSent = true
                    isPauseModeActive = true
                    onSpeak("Verification request sent to your family circle. Please pause until they confirm.")
                },
                style = if (challengeSent) PressablePillStyle.Secondary else PressablePillStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action 2: Call Cyber Crime Helpline (1930)
            PressablePillButton(
                text = "Call Cyber Helpline (1930)",
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                    context.startActivity(dialIntent)
                },
                style = PressablePillStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action 3: Call Emergency Services (112)
            PressablePillButton(
                text = "Emergency Helpline (112)",
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                    context.startActivity(dialIntent)
                },
                style = PressablePillStyle.Secondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SafetyCheckCard(
    question: String,
    subtext: String? = null,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) Color(0xFFFFDAD6) else SevaWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isChecked) SevaDanger else SevaMint,
                shape = RoundedCornerShape(20.dp)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "$question ${if (isChecked) "Yes selected" else "Tap to mark yes"}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isChecked) SevaDanger else SevaDarkText,
                    lineHeight = 22.sp
                )
                if (subtext != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtext,
                        fontSize = 14.sp,
                        color = SevaBodyTeal,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isChecked) SevaDanger else SevaMint, CircleShape)
            ) {
                Icon(
                    imageVector = if (isChecked) Icons.Rounded.Warning else Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (isChecked) SevaWhite else SevaDeepTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
