package com.sevacare.app.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.R
import com.sevacare.app.data.model.PrivacyLedgerEntry
import com.sevacare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Section 4.12 Privacy Ledger.
 *
 * Requirements:
 * 1. Complete local transparency log listing every alert ever dispatched (time, type, recipient).
 * 2. Feature toggles for elder control.
 * 3. Verified ending stamp: "Nothing else has left your phone. / आपके फ़ोन से इसके अलावा कुछ भी बाहर नहीं गया है।"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyLedgerScreen(
    entries: List<PrivacyLedgerEntry> = emptyList(),
    onBack: () -> Unit
) {
    var checkinsEnabled by remember { mutableStateOf(true) }
    var fallDetectionEnabled by remember { mutableStateOf(true) }
    var scamScreeningEnabled by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "प्राइवेसी लेज़र / Privacy Ledger",
                        fontWeight = FontWeight.Bold,
                        color = SevaWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SevaWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SevaDeepTeal)
            )
        },
        containerColor = SevaCream
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Transparency Guarantee Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SevaDeepTeal, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% ऑन-डिवाइस गारंटी",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SevaDeepTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SevaCare आपके संदेश, फ़ोटो या स्वास्थ्य डेटा को किसी सर्वर पर नहीं भेजता। नीचे दी गई सूची आपके फ़ोन से भेजी गई एकमात्र जानकारियाँ हैं।",
                            fontSize = 14.sp,
                            color = SevaBodyTeal,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Feature Control Toggles
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.privacy_feature_controls),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SevaDarkText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FeatureToggleRow(
                            title = "Morning Check-in Alert",
                            subtitle = "नाश्ते के बाद परिवार को सूचना",
                            checked = checkinsEnabled,
                            onCheckedChange = { checkinsEnabled = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))

                        FeatureToggleRow(
                            title = "Fall Detection Service",
                            subtitle = "गिरने पर 30s अलार्म और SMS",
                            checked = fallDetectionEnabled,
                            onCheckedChange = { fallDetectionEnabled = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))

                        FeatureToggleRow(
                            title = "Scam Message Screening",
                            subtitle = "संदिग्ध संदेशों की स्थानीय जांच",
                            checked = scamScreeningEnabled,
                            onCheckedChange = { scamScreeningEnabled = it }
                        )
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = "भेजे गए अलर्ट्स का रिकॉर्ड (${entries.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SevaDeepTeal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Outbound Alert History
            if (entries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "अभी तक कोई अलर्ट नहीं भेजा गया है।",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(entries) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.alertType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SevaDeepTeal
                                )
                                Text(
                                    text = dateFormat.format(Date(entry.timestampMs)),
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recipient: ${entry.recipientName} (${entry.recipientPhone})",
                                fontSize = 14.sp,
                                color = SevaDarkText
                            )
                            Text(
                                text = "Channel: ${entry.deliveryChannel}",
                                fontSize = 13.sp,
                                color = SevaBodyTeal
                            )
                        }
                    }
                }
            }

            // Ending Seal Stamp (Section 4.12 Invariant)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“Nothing else has left your phone.”",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B5E20),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "आपके फ़ोन से इसके अलावा कुछ भी बाहर नहीं गया है।",
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SevaDarkText)
            Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SevaDeepTeal
            )
        )
    }
}
