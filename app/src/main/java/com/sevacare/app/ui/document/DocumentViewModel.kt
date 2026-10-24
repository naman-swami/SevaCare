package com.sevacare.app.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevacare.app.data.model.ScamPhrase
import com.sevacare.app.data.repository.SevaCareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Document Check screen.
 *
 * WOW FACTOR #1: Ruko Mode
 * When the scam score crosses the threshold (≥6), activates a full-screen
 * mandatory 30-minute pause overlay. The elder cannot dismiss it without
 * typing "SEVACARE", which they are unlikely to do under scammer pressure.
 *
 * This directly exploits the scammer's own tactic (manufactured urgency)
 * against them — inserting a 30-minute calm delay frustrates the scammer's
 * timeline and gives the elder a chance to call a family member.
 */
class DocumentViewModel(
    private val repository: SevaCareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Idle)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private val _rukoModeActive = MutableStateFlow(false)
    val rukoModeActive: StateFlow<Boolean> = _rukoModeActive.asStateFlow()

    // 30 minutes = 1800 seconds
    private val _rukoSecondsRemaining = MutableStateFlow(1800)
    val rukoSecondsRemaining: StateFlow<Int> = _rukoSecondsRemaining.asStateFlow()

    fun analyzeDocument(ocrText: String) {
        viewModelScope.launch {
            _uiState.value = DocumentUiState.Loading
            val result = repository.analyzeForScamSignals(ocrText)
            if (result.shouldTriggerRukoMode) {
                _uiState.value = DocumentUiState.ScamDetected(
                    score = result.score,
                    matchedPhrases = result.matchedPhrases,
                    scannedText = ocrText
                )
            } else {
                _uiState.value = DocumentUiState.SafeDocument(
                    score = result.score,
                    scannedText = ocrText
                )
            }
        }
    }

    fun activateRukoMode() {
        _rukoSecondsRemaining.value = 1800 // Reset to 30 minutes
        _rukoModeActive.value = true
    }

    fun deactivateRukoMode() {
        _rukoModeActive.value = false
    }

    /** Called every second by a LaunchedEffect in the composable */
    fun tickRuko() {
        val current = _rukoSecondsRemaining.value
        if (current > 0) {
            _rukoSecondsRemaining.value = current - 1
        } else {
            // Time expired — auto dismiss
            _rukoModeActive.value = false
        }
    }
}

sealed class DocumentUiState {
    object Idle : DocumentUiState()
    object Loading : DocumentUiState()
    data class SafeDocument(val score: Int, val scannedText: String) : DocumentUiState()
    data class ScamDetected(
        val score: Int,
        val matchedPhrases: List<ScamPhrase>,
        val scannedText: String
    ) : DocumentUiState()
}
