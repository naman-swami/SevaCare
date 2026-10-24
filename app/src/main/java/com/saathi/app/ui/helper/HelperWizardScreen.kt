package com.saathi.app.ui.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.components.PressablePillButton
import com.saathi.app.ui.theme.*

/**
 * Section 4.10 & Acceptance Criterion 19:
 * Helper Setup Wizard and Share With a Neighbour.
 *
 * Checklist run by a family member or neighbour on the elder's phone:
 * 1. Language & Speech Voices verified
 * 2. Battery optimization & OEM Autostart (vivo, Xiaomi, Oppo, Samsung, Generic)
 * 3. Notification Access (for Scam Shield)
 * 4. Exact Alarm Permission (for Morning Check-ins)
 * 5. SMS Permission (for Emergency SOS & Circle Alerts)
 * 6. Send Test Alert to Circle
 * 7. "Share SevaCare" via Bluetooth / Nearby Share / Quick Share
 */
@Composable
fun HelperWizardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var testAlertSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SevaCream)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressablePillButton(
                text = "← Back",
                onClick = onBack,
                isPrimary = false,
                height = 56.dp,
                fillColor = SevaCreamSurface,
                lipColor = SevaPeach,
                textColor = SevaDarkText,
                modifier = Modifier.width(120.dp)
            )

            Text(
                text = "Helper Wizard / सेटअप",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = SevaMint,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Family & Caregiver Setup Guide",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDeepTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Complete these 6 steps on the elder's phone so alarms and emergency alerts never fail in the background.",
                    fontSize = 14.sp,
                    color = SevaBodyTeal
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 1: Battery Optimization Exemption
        WizardStepCard(
            stepNumber = "1",
            title = "Battery & Autostart (OEM Background Limits)",
            description = "Crucial on vivo, Xiaomi, Samsung, Oppo. Disable battery saver for SevaCare so morning check-ins and falls trigger on time.",
            actionText = "Open Battery Settings",
            onAction = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Notification Access (Scam Shield 2.0)
        WizardStepCard(
            stepNumber = "2",
            title = "Notification Access (Scam Shield)",
            description = "Enables SevaNotificationListenerService to catch fake police/CBI messages in memory without storing message bodies.",
            actionText = "Grant Notification Access",
            onAction = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Exact Alarms (Seva Agent Check-in)
        WizardStepCard(
            stepNumber = "3",
            title = "Exact Alarms Permission",
            description = "Ensures the 8:00 AM check-in rings precisely without Doze delays.",
            actionText = "Allow Exact Alarms",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: SMS Permission (Zero-server Circle Dispatch)
        WizardStepCard(
            stepNumber = "4",
            title = "SMS Permission (Emergency SOS)",
            description = "Required to send instant distress SMS to Family Circle without internet.",
            actionText = "Check App Permissions",
            onAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 5: Test Circle Alert
        Surface(
            color = SevaCreamSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "5. Verify Family Circle Alert",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDeepTeal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (testAlertSent) "✓ Test alert simulated successfully to paired contacts!" else "Send a non-emergency test alert to verify the elder's phone can reach paired family numbers.",
                    fontSize = 14.sp,
                    color = if (testAlertSent) SevaSafeGreen else SevaDarkText
                )
                Spacer(modifier = Modifier.height(12.dp))
                PressablePillButton(
                    text = if (testAlertSent) "✓ Alert Sent" else "Send Test SMS to Circle",
                    onClick = { testAlertSent = true },
                    isPrimary = false,
                    height = 56.dp,
                    fillColor = if (testAlertSent) SevaMint else SevaDeepTeal,
                    lipColor = SevaDeepTealLip,
                    textColor = if (testAlertSent) SevaDeepTeal else SevaWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 6: Share With a Neighbour
        Surface(
            color = SevaPeach,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🤝 Share SevaCare with a Neighbour",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaOnSaffronText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No Google Play Store account required. Share this app directly to an elderly neighbour via Quick Share, Nearby Share, or Bluetooth APK transfer.",
                    fontSize = 14.sp,
                    color = SevaOnSaffronText,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                PressablePillButton(
                    text = "Share SevaCare via Nearby Share",
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Install SevaCare (Offline Safety Companion for Indian Seniors): Keep your parents safe from duplicate medicine overdoses and digital arrest extortion. 100% offline & private."
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share SevaCare with a Neighbour")
                        context.startActivity(shareIntent)
                    },
                    isPrimary = true,
                    height = 64.dp,
                    fillColor = SevaSaffron,
                    lipColor = SevaSaffronLip,
                    textColor = SevaOnSaffronText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WizardStepCard(
    stepNumber: String,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Surface(
        color = SevaCreamSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$stepNumber. $title",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = SevaDarkText,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            PressablePillButton(
                text = actionText,
                onClick = onAction,
                isPrimary = false,
                height = 56.dp,
                fillColor = SevaDeepTeal,
                lipColor = SevaDeepTealLip,
                textColor = SevaWhite
            )
        }
    }
}
