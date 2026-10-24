package com.saathi.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.saathi.app.ui.theme.SevaCream
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaSaffron
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Section 9 Guided Live Scanner (No Shutter Button).
 *
 * Implements:
 * 1. Full-screen CameraX PreviewView with 15fps ImageAnalysis.
 * 2. Real-time Laplacian variance (sharpness >= 120) and mean luminance (brightness >= 40)
 *    computed directly on downsampled Y-plane buffer (zero heap copies).
 * 3. 800ms stability detector triggering automatic hands-free capture.
 * 4. Contextual spoken / visual guidance updated at most once every 3 seconds.
 * 5. 8-second fallback button ("Scan now / अभी स्कैन करें") if auto-capture does not lock.
 * 6. Low-light flash toggle that surfaces when ambient luminance < 40.
 */
@Composable
fun GuidedScannerView(
    onTextRecognized: (String) -> Unit,
    onSpeakGuidance: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isLowLight by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var guidanceText by remember { mutableStateOf("दवाई के पत्ते को फ़्रेम में लाएं\n(Bring strip into frame)") }
    var showManualScanButton by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    // ML Kit text recognizers
    val latinRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val devanagariRecognizer = remember { TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Flags for auto-capture lock
    val hasTriggeredCapture = remember { AtomicBoolean(false) }
    var lastGuidanceSpokenTime by remember { mutableLongStateOf(0L) }

    // Timer for 8-second manual fallback button
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            secondsElapsed++
            if (secondsElapsed >= 8 && !showManualScanButton) {
                showManualScanButton = true
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    // Viewfinder pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "scannerPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    fun processCapturedImage(inputImage: InputImage, onComplete: () -> Unit) {
        latinRecognizer.process(inputImage)
            .addOnSuccessListener { latinResult ->
                devanagariRecognizer.process(inputImage)
                    .addOnSuccessListener { devResult ->
                        val combinedText = (latinResult.text + "\n" + devResult.text).trim()
                        onComplete()
                        onTextRecognized(combinedText)
                    }
                    .addOnFailureListener {
                        onComplete()
                        onTextRecognized(latinResult.text)
                    }
            }
            .addOnFailureListener {
                onComplete()
                isProcessing = false
                hasTriggeredCapture.set(false)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    // Low-resolution keep-only-latest image analysis
                    var stableFrameCount = 0
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            if (hasTriggeredCapture.get() || isProcessing) {
                                return@setAnalyzer
                            }

                            val yPlane = imageProxy.planes[0]
                            val yBuffer = yPlane.buffer
                            val width = imageProxy.width
                            val height = imageProxy.height
                            val rowStride = yPlane.rowStride

                            // 1. Compute Mean Brightness
                            val brightness = computeBrightness(yBuffer, width, height, rowStride)
                            val isDark = brightness < 40.0
                            isLowLight = isDark

                            // 2. Compute Laplacian Sharpness
                            val sharpness = computeLaplacianVariance(yBuffer, width, height, rowStride)

                            // 3. Guidance logic
                            val now = SystemClock.elapsedRealtime()
                            val shouldUpdateGuidance = (now - lastGuidanceSpokenTime) > 3000L

                            if (isDark) {
                                if (shouldUpdateGuidance) {
                                    guidanceText = "अंधेरा है, फ़्लैश चालू करें\n(Too dark. Turn on flash)"
                                    onSpeakGuidance("It is too dark. Turn on the flash")
                                    lastGuidanceSpokenTime = now
                                }
                                stableFrameCount = 0
                            } else if (sharpness < 120.0) {
                                if (shouldUpdateGuidance) {
                                    guidanceText = "फ़ोन को स्थिर रखें\n(Hold the phone steady)"
                                    lastGuidanceSpokenTime = now
                                }
                                stableFrameCount = 0
                            } else {
                                // Sharp & bright -> increment stability
                                stableFrameCount++
                                if (shouldUpdateGuidance && stableFrameCount < 10) {
                                    guidanceText = "स्थिर रखें... स्कैन हो रहा है\n(Holding steady...)"
                                    lastGuidanceSpokenTime = now
                                }

                                // 800ms stable condition (~12 frames at 15fps)
                                if (stableFrameCount >= 12 && hasTriggeredCapture.compareAndSet(false, true)) {
                                    view.post {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        isProcessing = true
                                        guidanceText = "पहचान रहे हैं...\n(Recognizing medicine...)"
                                    }

                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val inputImage = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        processCapturedImage(inputImage) {
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
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
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Framing Box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .height(280.dp)
                .border(
                    width = 4.dp,
                    color = if (isProcessing) SevaDeepTeal.copy(alpha = borderAlpha) else SevaSaffron.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        // Top Controls: Single Back Arrow & Flash Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Flash Toggle (surfaces when dark or user can toggle)
            if (isLowLight || isTorchOn) {
                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isTorchOn) SevaSaffron else Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (isTorchOn) Color.Black else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Bottom Guidance & Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Guidance Pill
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = guidanceText,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            // Processing indicator or manual fallback button
            if (isProcessing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SevaCream),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SevaDeepTeal,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "छोटू पढ़ रहा है... (Analyzing...)",
                            color = SevaDeepTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else if (showManualScanButton) {
                // Section 9: 8-second fallback button
                PressablePillButton(
                    text = "अभी स्कैन करें (Scan Now)",
                    onClick = {
                        hasTriggeredCapture.set(true)
                        isProcessing = true
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    },
                    isPrimary = true,
                    height = 64.dp,
                    fillColor = SevaSaffron,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

/**
 * Computes mean brightness on downscaled Y-plane buffer.
 */
private fun computeBrightness(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int): Double {
    buffer.rewind()
    val step = 8
    var sum = 0L
    var count = 0
    for (y in 0 until height step step) {
        val rowOffset = y * rowStride
        for (x in 0 until width step step) {
            val pixel = buffer.get(rowOffset + x).toInt() and 0xFF
            sum += pixel
            count++
        }
    }
    return if (count == 0) 0.0 else sum.toDouble() / count
}

/**
 * Computes variance of Laplacian on downscaled Y-plane for sharpness determination.
 * Calibrated threshold: >= 120.0
 */
private fun computeLaplacianVariance(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int): Double {
    buffer.rewind()
    val step = 4
    var sum = 0.0
    var sumSq = 0.0
    var count = 0

    for (y in step until height - step step step) {
        val rowOffset = y * rowStride
        val topOffset = (y - step) * rowStride
        val botOffset = (y + step) * rowStride

        for (x in step until width - step step step) {
            val center = buffer.get(rowOffset + x).toInt() and 0xFF
            val top = buffer.get(topOffset + x).toInt() and 0xFF
            val bot = buffer.get(botOffset + x).toInt() and 0xFF
            val left = buffer.get(rowOffset + (x - step)).toInt() and 0xFF
            val right = buffer.get(rowOffset + (x + step)).toInt() and 0xFF

            val lap = (top + bot + left + right - 4 * center).toDouble()
            sum += lap
            sumSq += lap * lap
            count++
        }
    }

    if (count == 0) return 0.0
    val mean = sum / count
    return (sumSq / count) - (mean * mean)
}
