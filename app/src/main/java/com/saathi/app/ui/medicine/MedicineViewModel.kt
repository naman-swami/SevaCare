package com.saathi.app.ui.medicine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.repository.SaathiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * ViewModel for the Medicine Scanner screen.
 *
 * Core wow-factor features implemented here:
 * 1. Duplicate Pill Alert — session-aware salt comparison
 * 2. 5-Step Calm Disclosure — deterministic state machine (no LLM)
 * 3. Chemist Parchi — image generation for local chemist
 */
class MedicineViewModel(
    private val repository: SaathiRepository
) : ViewModel() {

    // ─── Session State ───────────────────────────────────────────

    /** All salts scanned in this session — powers the Duplicate Pill Alert */
    private val _scannedSaltSession = mutableListOf<String>()

    /** Currently identified medicine from the scan */
    private val _currentMedicine = MutableStateFlow<Medicine?>(null)
    val currentMedicine: StateFlow<Medicine?> = _currentMedicine.asStateFlow()

    /** All medicines scanned in this session (for session summary) */
    private val _sessionMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    val sessionMedicines: StateFlow<List<Medicine>> = _sessionMedicines.asStateFlow()

    /** Duplicate pill alert — populated when a duplicate salt is detected */
    private val _duplicateAlert = MutableStateFlow<DuplicatePillAlert?>(null)
    val duplicateAlert: StateFlow<DuplicatePillAlert?> = _duplicateAlert.asStateFlow()

    /** Current step in the 5-Step Calm Disclosure Protocol */
    private val _disclosureStep = MutableStateFlow(DisclosureStep.IDLE)
    val disclosureStep: StateFlow<DisclosureStep> = _disclosureStep.asStateFlow()

    /** Search results for manual medicine lookup */
    private val _searchResults = MutableStateFlow<List<Medicine>>(emptyList())
    val searchResults: StateFlow<List<Medicine>> = _searchResults.asStateFlow()

    private val _uiState = MutableStateFlow<MedicineUiState>(MedicineUiState.Idle)
    val uiState: StateFlow<MedicineUiState> = _uiState.asStateFlow()

    // TTS engine reference (set from composable context)
    private var tts: TextToSpeech? = null

    // ─── OCR Result Processing ───────────────────────────────────

    /**
     * Called when ML Kit OCR returns text from a scanned medicine strip.
     * Searches the database for matching medicines and triggers the duplicate alert if needed.
     */
    fun onOcrTextReceived(ocrText: String) {
        viewModelScope.launch {
            _uiState.value = MedicineUiState.Loading

            val results = repository.findMedicineByBrandName(ocrText)

            if (results.isEmpty()) {
                // Try broader search on the full OCR text
                val words = ocrText.split(" ", "\n").filter { it.length > 4 }
                for (word in words.take(5)) {
                    val r = repository.searchMedicines(word)
                    if (r.isNotEmpty()) {
                        processMedicineFound(r.first())
                        return@launch
                    }
                }
                _uiState.value = MedicineUiState.NotFound(ocrText)
            } else {
                processMedicineFound(results.first())
            }
        }
    }

    private suspend fun processMedicineFound(medicine: Medicine) {
        _currentMedicine.value = medicine

        // ── WOW FACTOR #2: Duplicate Pill Alert ────────────────
        val existing = _scannedSaltSession
        val salt = medicine.genericSalt.trim().lowercase()

        val duplicate = repository.checkDuplicateSalt(medicine.genericSalt, existing)

        if (duplicate != null) {
            // Found a duplicate! Alert the user immediately.
            _duplicateAlert.value = DuplicatePillAlert(
                newMedicine = medicine,
                existingMedicine = duplicate,
                commonSalt = medicine.genericSalt
            )
            _uiState.value = MedicineUiState.DuplicateDetected(medicine, duplicate)
        } else {
            // New unique medicine — add to session
            if (salt !in existing.map { it.lowercase() }) {
                _scannedSaltSession.add(medicine.genericSalt)
                _sessionMedicines.value = _sessionMedicines.value + medicine
            }
            _uiState.value = MedicineUiState.MedicineFound(medicine)
            // Auto-start the 5-step disclosure
            startCalmDisclosure()
        }
    }

    // ─── WOW FACTOR #4: 5-Step Calm Disclosure ──────────────────

    /**
     * Starts the 5-Step Calm Disclosure Protocol.
     * DESIGN: This is a deterministic state machine — no LLM, no generative text.
     * Steps 1-4 speak from pre-written, clinically reviewed Hindi strings.
     * Step 5 listens for user voice confirmation.
     *
     * Based on WHO Teach-Back Method adapted for elderly Indian literacy.
     */
    fun startCalmDisclosure() {
        _disclosureStep.value = DisclosureStep.WARM_OPEN
        speakHindi("Namaste! Yeh kaun si cheez hai aapke haath mein?")
    }

    fun advanceToContextStep() {
        val medicine = _currentMedicine.value ?: return
        _disclosureStep.value = DisclosureStep.DOCTOR_CONTEXT
        speakHindi("Doctor ne inhe kis cheez ke liye diya?")
    }

    fun advanceToExplanationStep() {
        val medicine = _currentMedicine.value ?: return
        _disclosureStep.value = DisclosureStep.EXPLANATION
        speakHindi(medicine.purposeHindi)
    }

    fun advanceToHowToTakeStep() {
        val medicine = _currentMedicine.value ?: return
        _disclosureStep.value = DisclosureStep.HOW_TO_TAKE
        speakHindi(medicine.howToTakeHindi)
    }

    fun advanceToTeachBackStep() {
        _disclosureStep.value = DisclosureStep.TEACH_BACK
        speakHindi("Achha, toh yeh kab leni hai? Batao mujhe.")
    }

    fun completeDisclosure() {
        _disclosureStep.value = DisclosureStep.COMPLETE
        speakHindi("Bahut achha! Saathi ne aapki madad ki. Dhyan rakhna.")
    }

    // ─── Manual Search ───────────────────────────────────────────

    fun searchMedicines(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = repository.searchMedicines(query)
        }
    }

    fun selectMedicineFromSearch(medicine: Medicine) {
        viewModelScope.launch {
            processMedicineFound(medicine)
        }
    }

    // ─── TTS (Text-to-Speech) ────────────────────────────────────

    fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Prefer Hindi, fallback to English
                val hindiLocale = Locale("hi", "IN")
                val result = tts?.setLanguage(hindiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                // Elder-optimized speech rate: slightly slower than default (1.0)
                tts?.setSpeechRate(0.82f)
                tts?.setPitch(1.05f) // Slightly higher pitch for clarity
            }
        }
    }

    fun speakHindi(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "saathi_tts")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    // ─── WOW FACTOR #3: Chemist Parchi ───────────────────────────

    /**
     * Generates a shareable Chemist Parchi image card.
     *
     * The "Parchi" (slip) is a physical-world UX pattern — the elder shows the
     * generated image to their local neighborhood chemist.
     *
     * This bridges the digital-offline gap that no current health app addresses.
     */
    fun generateAndShareChemistParchi(context: Context, medicine: Medicine) {
        val savings = medicine.brandMrp - medicine.janAushadhiMrp

        // Create bitmap
        val width = 900
        val height = 520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FFF8E7") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#6650A4")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRect(12f, 12f, (width - 12).toFloat(), (height - 12).toFloat(), borderPaint)

        val boldPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1C1B1F")
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1C1B1F")
            textSize = 36f
            isAntiAlias = true
        }
        val greenPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#386A20")
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val subtlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#49454F")
            textSize = 28f
            isAntiAlias = true
        }

        // Draw text content
        canvas.drawText("📋 CHEMIST PARCHI", 60f, 80f, boldPaint)
        canvas.drawLine(40f, 100f, (width - 40).toFloat(), 100f, borderPaint.apply { strokeWidth = 2f })

        canvas.drawText("Bhaiya, yeh dena:", 60f, 155f, bodyPaint)
        canvas.drawText("${medicine.janAushadhiName}", 60f, 215f, boldPaint)

        canvas.drawLine(40f, 240f, (width - 40).toFloat(), 240f, Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        })

        canvas.drawText("Brand: ₹${medicine.brandMrp.toInt()} → Jan Aushadhi: ₹${medicine.janAushadhiMrp.toInt()}", 60f, 300f, bodyPaint)
        canvas.drawText("Bachat: ₹${savings.toInt()} per patta", 60f, 360f, greenPaint)

        canvas.drawLine(40f, 390f, (width - 40).toFloat(), 390f, Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        })

        canvas.drawText("Jan Aushadhi Kendra se maangein", 60f, 440f, bodyPaint)
        canvas.drawText("Powered by Saathi App", 60f, 495f, subtlePaint)

        // Save to cache and share
        try {
            val cacheDir = File(context.cacheDir, "parchi")
            cacheDir.mkdirs()
            val file = File(cacheDir, "chemist_parchi_${medicine.brandName.replace(" ", "_")}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Saathi App se Chemist Parchi — ${medicine.brandName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Parchi Share Karein"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── Session Reset ───────────────────────────────────────────

    fun clearDuplicateAlert() {
        _duplicateAlert.value = null
    }

    fun resetSession() {
        _scannedSaltSession.clear()
        _sessionMedicines.value = emptyList()
        _currentMedicine.value = null
        _duplicateAlert.value = null
        _disclosureStep.value = DisclosureStep.IDLE
        _uiState.value = MedicineUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

// ─── State Classes ───────────────────────────────────────────────

enum class DisclosureStep {
    IDLE,
    WARM_OPEN,
    DOCTOR_CONTEXT,
    EXPLANATION,
    HOW_TO_TAKE,
    TEACH_BACK,
    COMPLETE
}

data class DuplicatePillAlert(
    val newMedicine: Medicine,
    val existingMedicine: Medicine,
    val commonSalt: String
)

sealed class MedicineUiState {
    object Idle : MedicineUiState()
    object Loading : MedicineUiState()
    data class MedicineFound(val medicine: Medicine) : MedicineUiState()
    data class DuplicateDetected(val newMed: Medicine, val existingMed: Medicine) : MedicineUiState()
    data class NotFound(val ocrText: String) : MedicineUiState()
    data class Error(val message: String) : MedicineUiState()
}
