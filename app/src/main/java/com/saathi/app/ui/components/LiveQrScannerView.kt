package com.saathi.app.ui.components

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaSaffron
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live CameraX QR Scanner View backed by ML Kit Barcode Scanning.
 * Real frame-by-frame analysis with corner reticle overlay, haptic feedback,
 * and zero mock timers.
 */
@Composable
fun LiveQrScannerView(
    onPayloadDecoded: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
    instructionText: String = "Scan the QR code on the elder's screen to connect securely",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    val hasScanned = remember { AtomicBoolean(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            barcodeScanner.close()
            cameraExecutor.shutdown()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scannerPulse")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserScan"
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null || hasScanned.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val firstQr = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                val rawValue = firstQr?.rawValue
                                if (!rawValue.isNullOrBlank() && hasScanned.compareAndSet(false, true)) {
                                    view.post {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        onPayloadDecoded(rawValue)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("LiveQrScanner", "Barcode analysis failed", e)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("LiveQrScanner", "Camera bind failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Visual Corner-Bracket Reticle & Laser Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = size.minDimension * 0.7f
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2f
            val cornerLength = 48f
            val strokeWidth = 8f
            val bracketColor = Color(0xFFF5A623) // Saffron

            // Draw Dimmed Outside Mask
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size = size
            )

            // Clear center transparently by redrawing viewfinder box cut-out
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(24f, 24f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Top-Left corner
            val path = Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top + 16f)
                quadraticTo(left, top, left + 16f, top)
                lineTo(left + cornerLength, top)

                // Top-Right corner
                moveTo(left + boxSize - cornerLength, top)
                lineTo(left + boxSize - 16f, top)
                quadraticTo(left + boxSize, top, left + boxSize, top + 16f)
                lineTo(left + boxSize, top + cornerLength)

                // Bottom-Right corner
                moveTo(left + boxSize, top + boxSize - cornerLength)
                lineTo(left + boxSize, top + boxSize - 16f)
                quadraticTo(left + boxSize, top + boxSize, left + boxSize - 16f, top + boxSize)
                lineTo(left + boxSize - cornerLength, top + boxSize)

                // Bottom-Left corner
                moveTo(left + cornerLength, top + boxSize)
                lineTo(left + 16f, top + boxSize)
                quadraticTo(left, top + boxSize, left, top + boxSize - 16f)
                lineTo(left, top + boxSize - cornerLength)
            }
            drawPath(path, color = bracketColor, style = Stroke(width = strokeWidth))

            // Animated Laser Line
            val laserY = top + (boxSize * laserOffset)
            drawLine(
                color = Color(0xFF0F6B5C).copy(alpha = 0.85f),
                start = Offset(left + 12f, laserY),
                end = Offset(left + boxSize - 12f, laserY),
                strokeWidth = 6f
            )
        }

        // Top Navigation & Flashlight Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(52.dp))
            }

            IconButton(
                onClick = {
                    isTorchOn = !isTorchOn
                    cameraControl?.enableTorch(isTorchOn)
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(if (isTorchOn) SevaSaffron else Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flashlight",
                    tint = if (isTorchOn) Color.Black else Color.White
                )
            }
        }

        // Bottom Instruction Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = instructionText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
