package com.saathi.app.ui.medicine

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.saathi.app.R
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.repository.SaathiRepository
import com.saathi.app.ui.theme.DuplicateAmber
import com.saathi.app.ui.theme.DuplicateAmberContainer
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SafeGreenContainer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(
    repository: SaathiRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MedicineViewModel = viewModel(
        factory = MedicineViewModelFactory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val currentMedicine by viewModel.currentMedicine.collectAsState()
    val duplicateAlert by viewModel.duplicateAlert.collectAsState()
    val disclosureStep by viewModel.disclosureStep.collectAsState()
    val sessionMedicines by viewModel.sessionMedicines.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var showCamera by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.saathi_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Column {
                            Text("दवाई देखें", fontWeight = FontWeight.Bold)
                            Text("Medicine Scanner", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    if (sessionMedicines.isNotEmpty()) {
                        IconButton(onClick = { viewModel.resetSession() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "New Session", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Duplicate Alert Banner (if active) ───────────────
            duplicateAlert?.let { alert ->
                DuplicatePillAlertBanner(
                    alert = alert,
                    onDismiss = { viewModel.clearDuplicateAlert() },
                    onSpeak = { viewModel.speakHindi(it) }
                )
            }

            // ── Search / Scan Buttons ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scan button
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            showCamera = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Strip", style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Manual Search ─────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchMedicines(it)
                },
                label = { Text("Ya naam type karein (e.g. Metformin)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // ── Search Results ────────────────────────────────────
            if (searchResults.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    searchResults.forEach { med ->
                        OutlinedCard(
                            onClick = {
                                searchQuery = ""
                                viewModel.selectMedicineFromSearch(med)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(med.brandName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(med.genericSalt, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Main Medicine Card ────────────────────────────────
            when (val state = uiState) {
                is MedicineUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is MedicineUiState.MedicineFound -> {
                    MedicineDetailCard(
                        medicine = state.medicine,
                        disclosureStep = disclosureStep,
                        onNextStep = {
                            when (disclosureStep) {
                                DisclosureStep.WARM_OPEN -> viewModel.advanceToContextStep()
                                DisclosureStep.DOCTOR_CONTEXT -> viewModel.advanceToExplanationStep()
                                DisclosureStep.EXPLANATION -> viewModel.advanceToHowToTakeStep()
                                DisclosureStep.HOW_TO_TAKE -> viewModel.advanceToTeachBackStep()
                                DisclosureStep.TEACH_BACK -> viewModel.completeDisclosure()
                                else -> {}
                            }
                        },
                        onSpeak = { viewModel.speakHindi(it) },
                        onShareParchi = { viewModel.generateAndShareChemistParchi(context, state.medicine) }
                    )
                }
                is MedicineUiState.DuplicateDetected -> {
                    MedicineDetailCard(
                        medicine = state.newMed,
                        disclosureStep = disclosureStep,
                        onNextStep = {},
                        onSpeak = { viewModel.speakHindi(it) },
                        onShareParchi = { viewModel.generateAndShareChemistParchi(context, state.newMed) }
                    )
                }
                is MedicineUiState.NotFound -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Yeh dawaai nahi mili", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("OCR mein mila: \"${state.ocrText.take(50)}...\"", fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("Upar search box mein naam type karein.", textAlign = TextAlign.Center)
                        }
                    }
                }
                else -> {
                    // Idle state — show instructions
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalPharmacy, contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(12.dp))
                            Text("Dawaai ka patta scan karein", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Text("Ya neeche search box mein naam type karein",
                                textAlign = TextAlign.Center, fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // ── Session Summary ───────────────────────────────────
            if (sessionMedicines.isNotEmpty()) {
                SessionSummaryCard(medicines = sessionMedicines)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Camera Dialog ─────────────────────────────────────────────
    if (showCamera) {
        Dialog(onDismissRequest = { showCamera = false }) {
            CameraPreviewCard(
                onTextRecognized = { text ->
                    showCamera = false
                    viewModel.onOcrTextReceived(text)
                },
                onDismiss = { showCamera = false }
            )
        }
    }
}

// ─── Medicine Detail Card ─────────────────────────────────────────

@Composable
fun MedicineDetailCard(
    medicine: Medicine,
    disclosureStep: DisclosureStep,
    onNextStep: () -> Unit,
    onSpeak: (String) -> Unit,
    onShareParchi: () -> Unit
) {
    val savings = medicine.brandMrp - medicine.janAushadhiMrp

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SafeGreenContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(medicine.brandName, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = SafeGreen)
                    Text(medicine.genericSalt, fontSize = 14.sp, color = SafeGreen.copy(alpha = 0.7f))
                }
            }

            Divider(color = SafeGreen.copy(alpha = 0.2f))

            // Schedule class badge
            val (badgeColor, badgeText) = when (medicine.scheduleClass) {
                "H1" -> Pair(MaterialTheme.colorScheme.error, "⚠️ Prescription Required")
                "H" -> Pair(DuplicateAmber, "Rx — Doctor's Prescription")
                "OTC" -> Pair(SafeGreen, "✅ OTC — Bina prescription ke milti hai")
                else -> Pair(SafeGreen, medicine.scheduleClass)
            }

            // Purpose
            Text("Kya kaam karti hai:", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(medicine.purposeHindi, fontSize = 18.sp, lineHeight = 26.sp)

            // How to take
            Text("Kaise leni hai:", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(medicine.howToTakeHindi, fontSize = 18.sp, lineHeight = 26.sp)

            Divider(color = SafeGreen.copy(alpha = 0.2f))

            // Price comparison
            Text("💰 Sasta Alternative:", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Brand price:", fontSize = 14.sp)
                    Text("₹${medicine.brandMrp.toInt()}", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.error)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterVertically))
                Column {
                    Text("Jan Aushadhi:", fontSize = 14.sp)
                    Text("₹${medicine.janAushadhiMrp.toInt()}", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = SafeGreen)
                }
                Column {
                    Text("Bachat:", fontSize = 14.sp)
                    Text("₹${savings.toInt()}", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = SafeGreen)
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // TTS
                OutlinedButton(
                    onClick = { onSpeak(medicine.purposeHindi + " " + medicine.howToTakeHindi) },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Suniye")
                }
                // Chemist Parchi
                Button(
                    onClick = onShareParchi,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Parchi")
                }
            }

            // 5-Step Disclosure Progress
            if (disclosureStep != DisclosureStep.IDLE && disclosureStep != DisclosureStep.COMPLETE) {
                DisclosureStepCard(step = disclosureStep, medicine = medicine, onNext = onNextStep)
            }
        }
    }
}

// ─── 5-Step Disclosure UI ─────────────────────────────────────────

@Composable
fun DisclosureStepCard(
    step: DisclosureStep,
    medicine: Medicine,
    onNext: () -> Unit
) {
    val (stepNumber, stepText, stepTitle) = when (step) {
        DisclosureStep.WARM_OPEN -> Triple(1, "Namaste! Yeh kaun si cheez hai aapke haath mein?", "Parichay")
        DisclosureStep.DOCTOR_CONTEXT -> Triple(2, "Doctor ne inhe kis cheez ke liye diya?", "Doctor ka advice")
        DisclosureStep.EXPLANATION -> Triple(3, medicine.purposeHindi, "Kya kaam karti hai")
        DisclosureStep.HOW_TO_TAKE -> Triple(4, medicine.howToTakeHindi, "Kaise leni hai")
        DisclosureStep.TEACH_BACK -> Triple(5, "Achha, toh yeh kab leni hai? Batao mujhe.", "Aapki samajh (Teach-Back)")
        else -> Triple(0, "", "")
    }

    if (stepNumber == 0) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Step $stepNumber/5",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(stepTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Text(stepText, fontSize = 18.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (step == DisclosureStep.TEACH_BACK) "Samajh gaye ✓" else "Aage →")
            }
        }
    }
}

// ─── Duplicate Pill Alert Banner ─────────────────────────────────

@Composable
fun DuplicatePillAlertBanner(
    alert: DuplicatePillAlert,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val alertText = "Dono mein ek hi cheez hai — ${alert.commonSalt}. Dono saath mat lijiye!"

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = DuplicateAmberContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔴", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "EK HI NAMAK!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = DuplicateAmber
                    )
                    Text("Duplicate Pill Alert", fontSize = 13.sp, color = DuplicateAmber)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                alertText,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = DuplicateAmber,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${alert.existingMedicine.brandName} + ${alert.newMedicine.brandName} — dono mein: ${alert.commonSalt}",
                fontSize = 14.sp,
                color = DuplicateAmber.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSpeak(alertText) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Suniye")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuplicateAmber)) {
                    Text("Samajh Gaye")
                }
            }
        }
    }
}

// ─── Session Summary ──────────────────────────────────────────────

@Composable
fun SessionSummaryCard(medicines: List<Medicine>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Is Session Mein Scan Ki Gayi Dawaaiyan (${medicines.size})",
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            medicines.forEach { med ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", fontSize = 16.sp)
                    Text("${med.brandName} (${med.genericSalt.take(30)})", fontSize = 16.sp)
                }
            }
        }
    }
}

// ─── Camera Preview ───────────────────────────────────────────────

@Composable
fun CameraPreviewCard(
    onTextRecognized: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var isProcessing by remember { mutableStateOf(false) }

    // ML Kit recognizers — both Latin and Devanagari
    val latinRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val devanagariRecognizer = remember { TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        Box {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Scan ho raha hai...", color = Color.White)
                } else {
                    Button(
                        onClick = {
                            isProcessing = true
                            imageCapture.takePicture(executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val inputImage = InputImage.fromMediaImage(
                                            image.image!!,
                                            image.imageInfo.rotationDegrees
                                        )
                                        // Run both Latin and Devanagari recognizers
                                        latinRecognizer.process(inputImage)
                                            .addOnSuccessListener { latinResult ->
                                                devanagariRecognizer.process(inputImage)
                                                    .addOnSuccessListener { devResult ->
                                                        val combinedText = latinResult.text + "\n" + devResult.text
                                                        image.close()
                                                        onTextRecognized(combinedText)
                                                    }
                                                    .addOnFailureListener {
                                                        image.close()
                                                        onTextRecognized(latinResult.text)
                                                    }
                                            }
                                            .addOnFailureListener {
                                                image.close()
                                                isProcessing = false
                                            }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        isProcessing = false
                                    }
                                }
                            )
                        },
                        modifier = Modifier.height(64.dp).fillMaxWidth(0.7f)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Karein", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(0.7f)) {
                        Text("Wapas Jaayein", color = Color.White)
                    }
                }
            }
        }
    }
}
