package com.saathi.app.ui.navigator

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.saathi.app.data.db.SaathiDatabase
import com.saathi.app.data.model.ChecklistItemEntity
import com.saathi.app.data.model.DocumentEntity
import com.saathi.app.data.repository.LifeAdminRepository
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
import com.saathi.app.ui.theme.SevaSafeGreenContainer
import com.saathi.app.ui.theme.SevaSaffron
import com.saathi.app.ui.theme.SevaWhite
import com.saathi.app.util.RememberPermissionGate
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Interactive Life-Admin Navigator Screen.
 *
 * Implements:
 * 1. Document intake flow: category chooser -> CameraX still capture -> ML Kit on-device TextRecognition.
 * 2. Room persistence of [DocumentEntity] and rule-generated [ChecklistItemEntity] rows.
 * 3. Interactive document cards with OCR viewer and real toggleable completion checkboxes.
 * 4. Zero static mock images or placeholder text.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LifeAdminNavigatorScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { SaathiDatabase.getInstance(context) }
    val repository = remember { LifeAdminRepository(db.lifeAdminDao(), context) }

    val documents by repository.allDocuments.collectAsState(initial = emptyList())
    val overdueItems by repository.overdueItems.collectAsState(initial = emptyList())

    val categories = remember { listOf("All", "Banking", "Bills", "Insurance", "Medical", "IDs") }
    var selectedCategory by remember { mutableStateOf("All") }

    var isCapturing by remember { mutableStateOf(false) }
    var intakeCategory by remember { mutableStateOf("Bills") }
    var selectedDocumentForDetail by remember { mutableStateOf<DocumentEntity?>(null) }

    val filteredDocuments = remember(documents, selectedCategory) {
        if (selectedCategory == "All") documents
        else documents.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    if (isCapturing) {
        CameraCaptureIntake(
            category = intakeCategory,
            onCategoryChange = { intakeCategory = it },
            onImageCaptured = { imageFile ->
                isCapturing = false
                processScannedStill(
                    context = context,
                    repository = repository,
                    imageFile = imageFile,
                    category = intakeCategory,
                    onSuccess = { doc ->
                        selectedDocumentForDetail = doc
                        onSpeak("Document recognized: ${doc.title}")
                    },
                    onError = {
                        onSpeak("Could not extract text. Please try again with brighter lighting.")
                    }
                )
            },
            onCancel = { isCapturing = false }
        )
        return
    }

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
            // Top App Bar
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
                        text = "Life-Admin Navigator",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "${documents.size} official documents organized",
                        fontSize = 15.sp,
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
                        state = if (overdueItems.isNotEmpty()) MascotState.CONCERNED else MascotState.POINTING,
                        size = 80.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (overdueItems.isNotEmpty())
                                "${overdueItems.size} Action Items Need Attention!"
                            else
                                "Documents Explained Simply",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overdueItems.isNotEmpty()) SevaDanger else SevaDarkText
                        )
                        Text(
                            text = if (overdueItems.isNotEmpty())
                                "Review your upcoming bill deadlines and policy payments below."
                            else
                                "Point your camera at bills, bank notices, or clinic receipts to extract dates & checklists.",
                            fontSize = 14.sp,
                            color = SevaBodyTeal,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Intake Button (>=88dp touch target)
            Button(
                onClick = { isCapturing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SevaDeepTeal,
                    contentColor = SevaWhite
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Scan New Document",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Capture bill, notice, or prescription",
                            fontSize = 14.sp,
                            color = SevaMint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Filter Chips
            Text(
                text = "Document Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SevaSaffron,
                            selectedLabelColor = SevaDeepTeal,
                            containerColor = SevaWhite,
                            labelColor = SevaDarkText
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Document List
            Text(
                text = "Verified Documents (${filteredDocuments.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredDocuments.isEmpty()) {
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
                            text = "No documents scanned yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the 'Scan New Document' button above. All analysis runs 100% locally on your phone.",
                            fontSize = 14.sp,
                            color = SevaBodyTeal,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                filteredDocuments.forEach { doc ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SevaWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .border(1.5.dp, SevaMint, RoundedCornerShape(20.dp))
                            .clickable { selectedDocumentForDetail = doc }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Document Image or Icon
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SevaCreamContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (File(doc.imagePath).exists()) {
                                    AsyncImage(
                                        model = File(doc.imagePath),
                                        contentDescription = doc.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Description,
                                        contentDescription = null,
                                        tint = SevaDeepTeal,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = doc.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SevaDarkText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(SevaPeach, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = doc.category,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF6E4000)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (doc.amountDue != null || doc.deadlineDate != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        doc.amountDue?.let { amt ->
                                            Text(
                                                text = amt,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SevaSafeGreen
                                            )
                                        }
                                        doc.deadlineDate?.let { date ->
                                            Text(
                                                text = "Due: $date",
                                                fontSize = 13.sp,
                                                color = SevaBodyTeal
                                            )
                                        }
                                    }
                                } else {
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(doc.createdAt))
                                    Text(
                                        text = "Scanned on $dateStr",
                                        fontSize = 13.sp,
                                        color = SevaBodyTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Document Detail Bottom Sheet
        selectedDocumentForDetail?.let { doc ->
            DocumentDetailSheet(
                document = doc,
                repository = repository,
                onDismiss = { selectedDocumentForDetail = null },
                onDelete = {
                    scope.launch {
                        repository.deleteDocument(doc.id)
                        selectedDocumentForDetail = null
                    }
                }
            )
        }
    }
}

/**
 * CameraX Still Capture View for document intake.
 */
@Composable
private fun CameraCaptureIntake(
    category: String,
    onCategoryChange: (String) -> Unit,
    onImageCaptured: (File) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessingCapture by remember { mutableStateOf(false) }

    RememberPermissionGate(
        permissions = arrayOf(Manifest.permission.CAMERA),
        rationaleTitle = "Camera Permission Required",
        rationaleMessage = "Camera access is needed to capture documents for local on-device OCR."
    ) {
        LaunchedEffect(Unit) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                } catch (e: Exception) {
                    Log.e("CameraCaptureIntake", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Top category selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intake Category: $category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaWhite
                    )
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            tint = SevaWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val intakeCategories = listOf("Bills", "Banking", "Insurance", "Medical", "IDs")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(intakeCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SevaSaffron,
                                selectedLabelColor = SevaDeepTeal,
                                containerColor = Color.White.copy(alpha = 0.3f),
                                labelColor = SevaWhite
                            )
                        )
                    }
                }
            }

            // Bottom Shutter Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessingCapture) {
                    CircularProgressIndicator(color = SevaSaffron)
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SevaSaffron)
                            .clickable {
                                val capture = imageCapture ?: return@clickable
                                isProcessingCapture = true
                                val outputFile = File(context.filesDir, "doc_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            isProcessingCapture = false
                                            onImageCaptured(outputFile)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isProcessingCapture = false
                                            Log.e("CameraCaptureIntake", "Capture failed", exception)
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Take Photo",
                            tint = SevaDeepTeal,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun processScannedStill(
    context: Context,
    repository: LifeAdminRepository,
    imageFile: File,
    category: String,
    onSuccess: (DocumentEntity) -> Unit,
    onError: () -> Unit
) {
    try {
        val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val doc = repository.processAndSaveScannedDocument(
                        imageFile = imageFile,
                        category = category,
                        ocrText = extractedText
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onSuccess(doc)
                    }
                }
            }
            .addOnFailureListener {
                onError()
            }
    } catch (_: Exception) {
        onError()
    }
}

/**
 * Detailed view of an analyzed document with interactive actionable checklist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentDetailSheet(
    document: DocumentEntity,
    repository: LifeAdminRepository,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val checklistItems by repository.getChecklistForDoc(document.id).collectAsState(initial = emptyList())
    var newCustomTaskText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SevaCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "Category: ${document.category}",
                        fontSize = 14.sp,
                        color = SevaBodyTeal
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Document",
                        tint = SevaDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row
            if (document.amountDue != null || document.deadlineDate != null || document.referenceId != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SevaCreamContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        document.amountDue?.let { amt ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Amount Due", fontSize = 12.sp, color = SevaBodyTeal)
                                Text(amt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SevaSafeGreen)
                            }
                        }
                        document.deadlineDate?.let { date ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pay By Date", fontSize = 12.sp, color = SevaBodyTeal)
                                Text(date, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SevaDarkText)
                            }
                        }
                        document.referenceId?.let { ref ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ref / A/C", fontSize = 12.sp, color = SevaBodyTeal)
                                Text(ref, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SevaDeepTeal)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Actionable Checklists Section
            Text(
                text = "Actionable Tasks & Deadlines",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (checklistItems.isEmpty()) {
                Text(
                    text = "No action items extracted. Add a custom reminder below.",
                    fontSize = 14.sp,
                    color = SevaBodyTeal
                )
            } else {
                checklistItems.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isCompleted) Color(0xFFE8F5E9) else SevaWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .border(
                                1.dp,
                                if (item.isOverdue) SevaDanger else SevaMint,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { completed ->
                                    scope.launch {
                                        repository.updateChecklistItemStatus(item.id, completed)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SevaSafeGreen,
                                    uncheckedColor = SevaDeepTeal
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    fontSize = 15.sp,
                                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (item.isCompleted) Color.Gray else SevaDarkText,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (item.isOverdue && !item.isCompleted) {
                                    Text(
                                        text = "⚠️ Urgent / Overdue Notice",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SevaDanger
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add Custom Task Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCustomTaskText,
                    onValueChange = { newCustomTaskText = it },
                    placeholder = { Text("Add custom note or task...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newCustomTaskText.isNotBlank()) {
                            scope.launch {
                                repository.addCustomChecklistItem(document.id, newCustomTaskText)
                                newCustomTaskText = ""
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SevaDeepTeal)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Task")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OCR Text Extract Viewer
            Text(
                text = "Recognized Text Extract",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SevaDeepTeal
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SevaWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = document.ocrText.ifBlank { "No legible text was found in this photo." },
                    fontSize = 13.sp,
                    color = SevaBodyTeal,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
