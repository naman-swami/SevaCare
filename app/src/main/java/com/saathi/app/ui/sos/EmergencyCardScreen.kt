package com.saathi.app.ui.sos

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.saathi.app.R
import com.saathi.app.data.model.CircleContact
import com.saathi.app.data.model.Medicine
import com.saathi.app.ui.components.PressablePillButton
import com.saathi.app.ui.theme.*
import java.io.File
import java.io.FileOutputStream

/**
 * Section 4.11 Emergency Card.
 *
 * Requirements:
 * 1. 100% offline card reachable from the SOS screen.
 * 2. Elder's name, Circle contacts, current medicines.
 * 3. Optional blood group and allergies (stored only on the device).
 * 4. Shareable as an image card.
 * 5. Strictly ZERO unprompted medical diagnoses or automated triage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCardScreen(
    elderName: String = "Elder / Papa",
    bloodGroup: String = "B+ (Positive)",
    allergies: String = "None reported (Penicillin sensitivity noted)",
    contacts: List<CircleContact> = emptyList(),
    medicines: List<Medicine> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "आपातकालीन कार्ड / Emergency Card",
                        fontWeight = FontWeight.Bold,
                        color = SevaWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SevaWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SevaDanger)
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
            // Profile & Critical Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = SevaDanger, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = elderName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SevaDarkText)
                            Text(text = "Blood Group: $bloodGroup", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = SevaDanger)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Allergies: $allergies", fontSize = 15.sp, color = SevaBodyTeal)
                }
            }

            // Circle Emergency Contacts
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📞 " + stringResource(R.string.emergency_circle_contacts),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (contacts.isEmpty()) {
                        Text(text = "No contacts paired yet.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• ${contact.name} (${contact.role.name})", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(text = contact.phone, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SevaDeepTeal)
                            }
                        }
                    }
                }
            }

            // Active Prescriptions
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💊 " + stringResource(R.string.emergency_current_medicines),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SevaDeepTeal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (medicines.isEmpty()) {
                        Text(text = "No medicines registered.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        medicines.take(6).forEach { med ->
                            Text(
                                text = "• ${med.brandName} (${med.genericSalt})",
                                fontSize = 15.sp,
                                color = SevaDarkText,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Share Emergency Card Image
            PressablePillButton(
                text = "आपातकालीन कार्ड शेयर करें",
                icon = Icons.Default.Share,
                onClick = {
                    shareEmergencyCard(context, elderName, bloodGroup, allergies, contacts, medicines)
                },
                isPrimary = true,
                fillColor = SevaDanger,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun shareEmergencyCard(
    context: Context,
    elderName: String,
    bloodGroup: String,
    allergies: String,
    contacts: List<CircleContact>,
    medicines: List<Medicine>
) {
    try {
        val bitmap = Bitmap.createBitmap(800, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#C62828") // SevaDanger
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        canvas.drawText("SevaCare Emergency Medical Card", 40f, 70f, paint)

        paint.textSize = 24f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Name: $elderName", 40f, 130f, paint)
        canvas.drawText("Blood Group: $bloodGroup", 40f, 170f, paint)
        canvas.drawText("Allergies: $allergies", 40f, 210f, paint)

        var y = 280f
        paint.color = android.graphics.Color.parseColor("#0F6B5C")
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Emergency Contacts:", 40f, y, paint)
        y += 40f

        paint.typeface = Typeface.DEFAULT
        paint.color = android.graphics.Color.BLACK
        contacts.forEach { c ->
            canvas.drawText("• ${c.name} (${c.role.name}): ${c.phone}", 50f, y, paint)
            y += 36f
        }

        y += 30f
        paint.color = android.graphics.Color.parseColor("#0F6B5C")
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Active Prescriptions:", 40f, y, paint)
        y += 40f

        paint.typeface = Typeface.DEFAULT
        paint.color = android.graphics.Color.DKGRAY
        medicines.take(6).forEach { m ->
            canvas.drawText("• ${m.brandName} - ${m.genericSalt}", 50f, y, paint)
            y += 34f
        }

        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "emergency_card.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "🚨 SevaCare Emergency Card for $elderName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Emergency Card"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
