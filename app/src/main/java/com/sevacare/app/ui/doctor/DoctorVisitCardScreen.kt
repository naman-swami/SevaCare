package com.sevacare.app.ui.doctor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sevacare.app.R
import com.sevacare.app.data.model.Medicine
import com.sevacare.app.ui.components.PressablePillButton
import com.sevacare.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream

enum class ElderFeelingState {
    BETTER,
    SAME,
    WORSE,
    UNSPECIFIED
}

/**
 * Section 4.7 Doctor Visit Card.
 *
 * Requirements:
 * 1. Current medicines list with plain Hindi instructions.
 * 2. Automatic "Questions to ask your doctor" generated from duplicate/expiry/teach-back flags.
 * 3. One-tap feeling note: Better / Same / Worse.
 * 4. Shareable as an image card over WhatsApp.
 * 5. Strictly ZERO medical scoring or unprompted diagnoses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorVisitCardScreen(
    medicines: List<Medicine>,
    flags: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var feeling by remember { mutableStateOf(ElderFeelingState.UNSPECIFIED) }
    var shareSuccessMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "डॉक्टर विज़िट कार्ड / Doctor Card",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = SevaWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SevaWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SevaDeepTeal
                )
            )
        },
        containerColor = SevaCream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📋 अपनी अगली डॉक्टर विज़िट के लिए",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "यह कार्ड आप अपने डॉक्टर को दिखा सकते हैं या WhatsApp पर भेज सकते हैं।",
                        fontSize = 15.sp,
                        color = SevaBodyTeal
                    )
                }
            }

            // 1. One-tap "How I feel" note
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "आज आप कैसा महसूस कर रहे हैं?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeelingTile(
                            emoji = "😊",
                            label = stringResource(R.string.doctor_status_better),
                            isSelected = feeling == ElderFeelingState.BETTER,
                            onClick = { feeling = ElderFeelingState.BETTER },
                            modifier = Modifier.weight(1f)
                        )
                        FeelingTile(
                            emoji = "😐",
                            label = stringResource(R.string.doctor_status_same),
                            isSelected = feeling == ElderFeelingState.SAME,
                            onClick = { feeling = ElderFeelingState.SAME },
                            modifier = Modifier.weight(1f)
                        )
                        FeelingTile(
                            emoji = "😔",
                            label = stringResource(R.string.doctor_status_worse),
                            isSelected = feeling == ElderFeelingState.WORSE,
                            onClick = { feeling = ElderFeelingState.WORSE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Questions to ask doctor (Auto-generated from flags)
            val generatedQuestions = remember(medicines, flags) {
                val qList = mutableListOf<String>()
                if (flags.any { it.contains("duplicate", ignoreCase = true) }) {
                    qList.add("❓ डॉक्टर साहब, क्या मुझे एक ही साल्ट की दो अलग दवाइयाँ लेनी ज़रूरी हैं?")
                }
                if (flags.any { it.contains("expiry", ignoreCase = true) }) {
                    qList.add("❓ मेरे पास एक पत्ते की एक्सपायरी पास आ रही है, क्या नया पत्ता लिख दें?")
                }
                qList.add("❓ क्या मुझे इन दवाइयों के साथ कोई विशेष परहेज़ रखना है?")
                qList.add("❓ क्या मुझे अगली विज़िट से पहले कोई ब्लड टेस्ट कराना है?")
                qList
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "डॉक्टर से पूछने वाले सवाल:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    generatedQuestions.forEach { q ->
                        Text(
                            text = q,
                            fontSize = 15.sp,
                            color = Color(0xFF3E2723),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. Current Medicines List
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वर्तमान दवाइयाँ (${medicines.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (medicines.isEmpty()) {
                        Text(
                            text = "अभी कोई दवाई जोड़ी नहीं गई है। पत्ता स्कैन करके जोड़ें।",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    } else {
                        medicines.forEach { med ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    text = "• ${med.brandName} (${med.genericSalt})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SevaDarkText
                                )
                                Text(
                                    text = "  ${med.howToTakeHindi}",
                                    fontSize = 14.sp,
                                    color = SevaBodyTeal
                                )
                            }
                        }
                    }
                }
            }

            // 4. WhatsApp Share Button
            PressablePillButton(
                text = "WhatsApp पर डॉक्टर कार्ड भेजें",
                icon = Icons.Default.Share,
                onClick = {
                    shareDoctorCardImage(context, medicines, feeling, generatedQuestions)
                },
                isPrimary = true,
                fillColor = SevaSaffron,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeelingTile(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .background(
                color = if (isSelected) SevaSaffron.copy(alpha = 0.25f) else Color(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SevaSaffron else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 24.sp)
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = SevaDarkText
            )
        }
    }
}

private fun shareDoctorCardImage(
    context: Context,
    medicines: List<Medicine>,
    feeling: ElderFeelingState,
    questions: List<String>
) {
    try {
        val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.parseColor("#FFF8EC")) // SevaCream

        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F6B5C") // SevaDeepTeal
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        canvas.drawText("SevaCare Doctor Visit Card", 40f, 70f, paint)

        paint.textSize = 22f
        paint.color = android.graphics.Color.parseColor("#0B4F44")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Status: Patient feels ${feeling.name}", 40f, 120f, paint)

        var y = 180f
        paint.textSize = 26f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Current Prescriptions (${medicines.size}):", 40f, y, paint)
        y += 40f

        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        medicines.take(6).forEach { med ->
            canvas.drawText("• ${med.brandName} - ${med.genericSalt}", 50f, y, paint)
            y += 32f
            canvas.drawText("  Directions: ${med.howToTakeHindi}", 50f, y, paint)
            y += 40f
        }

        y += 20f
        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = android.graphics.Color.parseColor("#E65100")
        canvas.drawText("Questions to Ask:", 40f, y, paint)
        y += 36f

        paint.textSize = 19f
        paint.color = android.graphics.Color.DKGRAY
        paint.typeface = Typeface.DEFAULT
        questions.take(4).forEach { q ->
            canvas.drawText(q, 50f, y, paint)
            y += 34f
        }

        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "doctor_visit_card.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "नमस्ते डॉक्टर साहब, यह मेरा SevaCare विज़िट कार्ड है।")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Doctor Visit Card"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
