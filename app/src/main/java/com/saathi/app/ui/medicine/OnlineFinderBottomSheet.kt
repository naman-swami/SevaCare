package com.saathi.app.ui.medicine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.components.PressablePillButton
import com.saathi.app.ui.theme.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Section 13 & Acceptance Criterion 10 & 13:
 * Optional Online Medicine Finder (ONLINE-OPTIONAL, User-Triggered Only)
 *
 * Requirements:
 * 1. Button appears ONLY on the result card after local result is shown.
 * 2. Disclaimer prominently displayed:
 *    "Prices and availability are provided by third-party sources and may change.
 *     SevaCare is not a pharmacy and does not earn a commission from these links."
 * 3. Schedule H/H1/X notice: "This medicine needs a doctor's prescription."
 * 4. Zero-cost default with no key: Launches ACTION_VIEW to web search.
 * 5. No hardcoded prices anywhere in the core flow.
 */
data class PriceResult(
    val pharmacyName: String,
    val price: Double,
    val currency: String,
    val packSize: String,
    val url: String,
    val lastUpdated: String,
    val sourceLabel: String
)

interface PriceProvider {
    suspend fun search(drugName: String, country: String): List<PriceResult>
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineFinderBottomSheet(
    medicineName: String,
    scheduleClass: String,
    country: String = "India",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPrescriptionRequired = scheduleClass in listOf("H", "H1", "X")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SevaCream,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Where to Buy / कहाँ से खरीदें",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = medicineName,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = SevaDarkText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule requirement alert
            if (isPrescriptionRequired) {
                Surface(
                    color = SevaPeach,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ This medicine needs a doctor's prescription / इसके लिए डॉक्टर की पर्ची अनिवार्य है।",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = SevaOnSaffronText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Mandatory Section 13 Disclaimer
            Surface(
                color = SevaMint,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Prices and availability are provided by third-party sources and may change. SevaCare is not a pharmacy and does not earn a commission from these links.\n\nकीमतें और उपलब्धता बाहरी स्रोतों से हैं। SevaCare कोई फार्मेसी नहीं है और इन लिंक्स से कोई कमीशन नहीं लेता।",
                    fontSize = 13.sp,
                    color = SevaDeepTeal,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Zero-cost web search button
            PressablePillButton(
                text = "Search the Web for this Medicine\nऑनलाइन खोजें",
                onClick = {
                    val query = "$medicineName price $country"
                    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
                    context.startActivity(intent)
                    onDismiss()
                },
                isPrimary = true,
                height = 88.dp,
                fillColor = SevaDeepTeal,
                lipColor = SevaDeepTealLip,
                textColor = SevaWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            PressablePillButton(
                text = "Close / बंद करें",
                onClick = onDismiss,
                isPrimary = false,
                height = 64.dp,
                fillColor = SevaCreamSurface,
                lipColor = SevaPeach,
                textColor = SevaDarkText
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
