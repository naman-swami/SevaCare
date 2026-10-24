package com.saathi.app.ui.medicine

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.saathi.app.R
import com.saathi.app.data.db.SaathiDatabase
import com.saathi.app.data.model.ConflictReportEntity
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.model.MedicineEntity
import com.saathi.app.data.repository.AddScheduleResult
import com.saathi.app.data.repository.MedicineRepository
import com.saathi.app.ui.components.FourBarAudioVisualizer
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
import com.saathi.app.util.RememberPermissionGate
import com.saathi.app.util.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Medicine Hub Screen.
 *
 * Implements:
 * 1. Top Search Bar: Room full-text search across 345+ seeded Indian chronic medicines.
 * 2. Bottom Circular Scan Button: CameraX viewfinder with real barcode + OCR fallback.
 * 3. Medicine Card: Name, Brief Description, Clinical Details, Schedule Class, Warnings.
 * 4. Animated Add-to-Schedule voice dialog.
 * 5. Indication Verification & Cross-Medicine Conflict Detection.
 */
@Composable
fun MedicineHubScreen(
    onBack: () -> Unit,
    onNavigatePairing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { SaathiDatabase.getInstance(context) }
    val repository = remember {
        MedicineRepository(
            medicineDao = db.medicineDao(),
            scheduleDao = db.scheduleDao(),
            conflictDao = db.conflictDao(),
            familyPairDao = db.familyPairDao()
        )
    }
    val voiceEngine = remember { VoiceEngine.getInstance(context) }

    val activeSchedules by repository.activeSchedules.collectAsState(initial = emptyList())
    val suspiciousList by repository.suspiciousMedicines.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }

    var isScanningViewfinderOpen by remember { mutableStateOf(false) }
    var isAddScheduleDialogOpen by remember { mutableStateOf(false) }

    var activeConflictReport by remember { mutableStateOf<ConflictReportEntity?>(null) }
    var isFamilyPairedForConflict by remember { mutableStateOf(false) }

    var showSuspiciousScreen by remember { mutableStateOf(false) }

    // Live search debounced
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            searchResults = repository.searchMedicines(q)
        } else {
            searchResults = emptyList()
        }
    }

    if (showSuspiciousScreen) {
        SuspiciousMedicinesScreen(
            onBack = { showSuspiciousScreen = false }
        )
        return
    }

    if (isScanningViewfinderOpen) {
        MedicineViewfinderScanner(
            onMedicineDetected = { detectedMed ->
                isScanningViewfinderOpen = false
                selectedMedicine = detectedMed
                voiceEngine.speak("${detectedMed.brandName} पहचान ली गई है। ${detectedMed.purposeHindi}")
            },
            onClose = { isScanningViewfinderOpen = false },
            repository = repository
        )
        return
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SevaCream
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(54.dp)
                                .background(SevaMint, CircleShape)
                                .semantics { contentDescription = "Go back to Home" }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                tint = SevaDeepTeal
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.medicine_hub_title),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = SevaDeepTeal
                            )
                            Text(
                                text = "${activeSchedules.size} active daily medicines",
                                fontSize = 14.sp,
                                color = SevaBodyTeal
                            )
                        }
                    }

                    // Suspicious Flagged Badge / Button
                    if (suspiciousList.isNotEmpty()) {
                        IconButton(
                            onClick = { showSuspiciousScreen = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFFDAD6), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "View suspicious flagged medicines",
                                tint = SevaDanger
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Top Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_medicine_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = SevaDeepTeal)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SevaWhite,
                        unfocusedContainerColor = SevaWhite,
                        focusedBorderColor = SevaSaffron,
                        unfocusedBorderColor = SevaMint
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Search Results Dropdown/Card
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SevaWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, SevaMint, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            searchResults.forEach { med ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedMedicine = med
                                            searchQuery = ""
                                            searchResults = emptyList()
                                            voiceEngine.speak("${med.brandName}। ${med.genericSalt}। ${med.howToTakeHindi}")
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MedicalServices,
                                        contentDescription = null,
                                        tint = SevaDeepTeal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.brandName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SevaDarkText
                                        )
                                        Text(
                                            text = "${med.genericSalt} • ${med.category}",
                                            fontSize = 13.sp,
                                            color = SevaBodyTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Selected Medicine Card Display
                selectedMedicine?.let { med ->
                    MedicineDetailCard(
                        medicine = med,
                        onAddToSchedule = { isAddScheduleDialogOpen = true },
                        onClose = { selectedMedicine = null }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Active Daily Schedule Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Daily Schedule",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "${activeSchedules.size} items",
                        fontSize = 14.sp,
                        color = SevaBodyTeal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeSchedules.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SevaWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No active medicines scheduled",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SevaDarkText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the circular camera button below to scan a medicine strip, or search above to add your prescription.",
                                fontSize = 14.sp,
                                color = SevaBodyTeal,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    activeSchedules.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SevaWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .border(1.dp, SevaMint, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.medicineName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SevaDarkText
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(SevaMint, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.timeOfDay,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SevaDeepTeal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Duration: ${item.durationDays} days" + (item.statedReason?.let { " • Note: $it" } ?: ""),
                                    fontSize = 13.sp,
                                    color = SevaBodyTeal
                                )

                                item.adjustmentReason?.let { adj ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ℹ️ $adj",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SevaSafeGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(110.dp))
            }

            // Bottom-Center Circular Scan Button (>=88dp touch target)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SevaSaffron)
                        .clickable { isScanningViewfinderOpen = true }
                        .semantics { contentDescription = "Scan medicine with camera" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = SevaDeepTeal,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Add to Schedule Animated Voice Dialog
        if (isAddScheduleDialogOpen && selectedMedicine != null) {
            AnimatedAddScheduleDialog(
                medicine = selectedMedicine!!,
                onDismiss = { isAddScheduleDialogOpen = false },
                onSpeak = { text -> voiceEngine.speak(text) },
                onConfirm = { draft ->
                    isAddScheduleDialogOpen = false
                    scope.launch {
                        val result = repository.addMedicineToSchedule(draft, selectedMedicine!!)
                        when (result) {
                            is AddScheduleResult.AddedDirectly -> {
                                voiceEngine.speak("${selectedMedicine!!.brandName} आपके शेड्यूल में सुरक्षित रूप से जोड़ दी गई है।")
                                selectedMedicine = null
                            }
                            is AddScheduleResult.FlaggedSuspicious -> {
                                voiceEngine.speak("चेतावनी: इस दवा के कारण में विरोधाभास मिला है। इसे डॉक्टर की समीक्षा के लिए सुरक्षित सूची में दर्ज किया गया है।")
                            }
                            is AddScheduleResult.ConflictDetected -> {
                                activeConflictReport = result.report
                                isFamilyPairedForConflict = result.isFamilyPaired
                                voiceEngine.speak("दवाओं में परस्पर टकराव मिला है। परिवार की मंज़ूरी के लिए विवरण भेजा गया है।")
                            }
                        }
                    }
                }
            )
        }

        // Conflict Approval Dialog
        activeConflictReport?.let { report ->
            ConflictApprovalDialog(
                report = report,
                isFamilyPaired = isFamilyPairedForConflict,
                onApprove = {
                    scope.launch {
                        repository.approveConflictReport(report.id)
                        activeConflictReport = null
                        selectedMedicine = null
                        voiceEngine.speak("सुरक्षित संशोधित शेड्यूल मंज़ूर हो गया है।")
                    }
                },
                onReject = {
                    scope.launch {
                        repository.rejectConflictReport(report.id)
                        activeConflictReport = null
                    }
                },
                onNavigatePairing = {
                    activeConflictReport = null
                    onNavigatePairing()
                },
                onDismiss = { activeConflictReport = null }
            )
        }
    }
}

/**
 * Detailed presentation of a scanned or selected medicine.
 */
@Composable
private fun MedicineDetailCard(
    medicine: MedicineEntity,
    onAddToSchedule: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SevaWhite),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, SevaMint, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.brandName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDarkText
                    )
                    Text(
                        text = medicine.genericSalt,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SevaDeepTeal
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SevaMint, CircleShape)
                ) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = SevaDeepTeal)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(SevaPeach, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Schedule ${medicine.scheduleClass}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6E4000)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(SevaMint, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = medicine.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                }
                if (medicine.requiresEmptyStomach) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFDAD6), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Empty Stomach",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDanger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description / Indication
            Text(
                text = "Primary Indication:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )
            Text(
                text = medicine.primaryIndication,
                fontSize = 15.sp,
                color = SevaDarkText,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Intake Instructions
            Text(
                text = "Clinical Intake Instructions:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )
            Text(
                text = medicine.intakeInstructions,
                fontSize = 15.sp,
                color = SevaBodyTeal
            )

            if (medicine.contraindications.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Contraindications / Caution:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDanger
                )
                Text(
                    text = medicine.contraindications,
                    fontSize = 13.sp,
                    color = SevaDarkText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddToSchedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SevaDeepTeal)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.add_to_schedule),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * CameraX Viewfinder combining ML Kit barcode scanner and on-device text OCR.
 */
@Composable
private fun MedicineViewfinderScanner(
    onMedicineDetected: (Medicine) -> Unit,
    onClose: () -> Unit,
    repository: MedicineRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isAnalyzing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    RememberPermissionGate(
        permissions = arrayOf(Manifest.permission.CAMERA),
        rationaleTitle = "Camera Required for Medicine Scan",
        rationaleMessage = "Camera access is needed to scan barcodes and read medicine blister pack labels on your phone."
    ) {
        LaunchedEffect(Unit) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val barcodeScanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_UPC_A, Barcode.FORMAT_DATA_MATRIX, Barcode.FORMAT_QR_CODE)
                        .build()
                )
                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isAnalyzing) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        isAnalyzing = true

                        // Stage 1: Barcode scan
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val firstBarcode = barcodes.firstOrNull()?.rawValue
                                if (!firstBarcode.isNullOrBlank()) {
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        val match = repository.lookupByBarcodeOrText(firstBarcode, null)
                                        if (match != null) {
                                            withContext(Dispatchers.Main) {
                                                onMedicineDetected(match)
                                            }
                                        } else {
                                            isAnalyzing = false
                                        }
                                    }
                                } else {
                                    // Stage 2: OCR Fallback for blister pack text
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val ocrText = visionText.text
                                            if (ocrText.isNotBlank()) {
                                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                                    val match = repository.lookupByBarcodeOrText(null, ocrText)
                                                    if (match != null) {
                                                        withContext(Dispatchers.Main) {
                                                            onMedicineDetected(match)
                                                        }
                                                    } else {
                                                        isAnalyzing = false
                                                    }
                                                }
                                            } else {
                                                isAnalyzing = false
                                            }
                                        }
                                        .addOnFailureListener {
                                            isAnalyzing = false
                                        }
                                }
                            }
                            .addOnFailureListener {
                                isAnalyzing = false
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Overlay Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Point camera at medicine barcode or label",
                    color = SevaWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = SevaWhite)
                }
            }
        }
    }
}
