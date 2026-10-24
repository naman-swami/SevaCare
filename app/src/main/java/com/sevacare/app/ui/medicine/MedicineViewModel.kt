package com.sevacare.app.ui.medicine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import com.sevacare.app.util.VoiceEngine
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevacare.app.R
import com.sevacare.app.data.model.Medicine
import com.sevacare.app.data.repository.SevaCareRepository
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
    private val repository: SevaCareRepository
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
    private var voiceEngine: VoiceEngine? = null

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
        speakHindi("Review complete. SevaCare is always here to help.")
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
        if (voiceEngine == null) {
            voiceEngine = VoiceEngine.getInstance(context)
        }
    }

    fun speakHindi(text: String) {
        voiceEngine?.speak(text, "hi")
    }

    fun stopSpeaking() {
        voiceEngine?.stop()
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
        // Create bitmap
        val width = 900
        val height = 520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background — soft cream (#FFF8EC)
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#FFF8EC") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Border — Deep Teal (#0F6B5C)
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F6B5C")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRect(14f, 14f, (width - 14).toFloat(), (height - 14).toFloat(), borderPaint)

        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F6B5C")
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#191C1D")
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#191C1D")
            textSize = 34f
            isAntiAlias = true
        }
        val greenPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F6B5C")
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val subtlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0B4F44")
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Draw logo on top right of parchi
        try {
            val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.sevacare_logo)
            if (logoBmp != null) {
                val destRect = Rect(width - 140, 26, width - 40, 126)
                canvas.drawBitmap(logoBmp, null, destRect, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Draw text content
        canvas.drawText("📋 CHEMIST PARCHI", 50f, 80f, headerPaint)
        canvas.drawLine(40f, 110f, (width - 160).toFloat(), 110f, borderPaint.apply { strokeWidth = 2f })

        canvas.drawText("Pharmacist Note: Please dispense this medicine or equivalent generic salt:", 50f, 165f, bodyPaint)
        canvas.drawText("${medicine.brandName} (${medicine.dosageForm})", 50f, 225f, boldPaint)

        canvas.drawLine(40f, 255f, (width - 40).toFloat(), 255f, Paint().apply {
            color = android.graphics.Color.parseColor("#D4EFF2")
            strokeWidth = 2f
        })

        canvas.drawText("Active Salt: ${medicine.genericSalt}", 50f, 315f, greenPaint)
        canvas.drawText("Schedule: ${medicine.scheduleClass} • Category: ${medicine.category}", 50f, 375f, bodyPaint)

        canvas.drawLine(40f, 405f, (width - 40).toFloat(), 405f, Paint().apply {
            color = android.graphics.Color.parseColor("#D4EFF2")
            strokeWidth = 2f
        })

        canvas.drawText("Jan Aushadhi ya standard generic substitute dein", 50f, 455f, bodyPaint)
        canvas.drawText("सेवाकेयर • Powered by SevaCare", 50f, 500f, subtlePaint)

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
                putExtra(Intent.EXTRA_TEXT, "SevaCare App se Chemist Parchi — ${medicine.brandName}")
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
        voiceEngine?.stop()
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
