package com.sevacare.app.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevacare.app.data.ai.BrainResponse
import com.sevacare.app.data.ai.SevaCareBrain
import com.sevacare.app.data.repository.SevaCareRepository
import com.sevacare.app.util.VoiceEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Intelligent AI Care Companion Voice Guide ViewModel (The "Nervous System").
 *
 * Implements:
 * 1. Resilient SpeechRecognizer with automatic fallback (no offline-only crashes).
 * 2. Continuous aliveness pulsing for VoiceOrb and FourBarVisualizer while listening.
 * 3. Graceful handling of silence/timeout errors (codes 6 & 7) with polite prompts.
 * 4. Grounded clinical guardrails via [SevaCareBrain].
 */
class VoiceGuideViewModel(
    private val repository: SevaCareRepository
) : ViewModel() {

    private val brain = SevaCareBrain(repository)
    private var voiceEngine: VoiceEngine? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _sevaCareResponse = MutableStateFlow(
        "Welcome to SevaCare! Press the microphone button or ask any question about your medicine schedule, official notices, or suspicious calls."
    )
    val sevaCareResponse: StateFlow<String> = _sevaCareResponse.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _showEmergencyDialer112 = MutableStateFlow(false)
    val showEmergencyDialer112: StateFlow<Boolean> = _showEmergencyDialer112.asStateFlow()

    private val _lastBrainResponse = MutableStateFlow<BrainResponse?>(null)
    val lastBrainResponse: StateFlow<BrainResponse?> = _lastBrainResponse.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isHindiLanguage: Boolean = true
    private var pulseJob: Job? = null
    private var lastContext: Context? = null

    fun setLanguage(isHindi: Boolean) {
        this.isHindiLanguage = isHindi
    }

    fun initVoice(context: Context) {
        lastContext = context.applicationContext
        voiceEngine = VoiceEngine.getInstance(context)

        viewModelScope.launch {
            voiceEngine?.isSpeaking?.collect { speaking ->
                _isSpeaking.value = speaking
                if (!speaking && !_isListening.value) {
                    _audioLevel.value = 0f
                }
            }
        }

        setupSpeechRecognizer(context)
    }

    private fun setupSpeechRecognizer(context: Context) {
        try {
            speechRecognizer?.destroy()
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener(context))
                }
            }
        } catch (_: Exception) {}
    }

    private fun createListener(context: Context) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            startAlivenessPulse()
        }

        override fun onBeginningOfSpeech() {
            stopAlivenessPulse()
            _audioLevel.value = 0.5f
        }

        override fun onRmsChanged(rmsdB: Float) {
            val level = (rmsdB / 10f).coerceIn(0.15f, 1.0f)
            _audioLevel.value = level
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            stopAlivenessPulse()
            _audioLevel.value = 0f
        }

        override fun onError(error: Int) {
            stopAlivenessPulse()
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Microphone heard silence; don't collapse UI instantly
                    _spokenText.value = if (isHindiLanguage) "साथी सुन रहा है, कृपया बोलिए..." else "Listening... Please speak."
                    _audioLevel.value = 0.2f
                    viewModelScope.launch {
                        delay(1200)
                        if (_isListening.value) {
                            try {
                                startListeningInternal(context)
                            } catch (_: Exception) {
                                _isListening.value = false
                                _audioLevel.value = 0f
                            }
                        }
                    }
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT -> {
                    // Transient client error; recreate recognizer
                    setupSpeechRecognizer(context)
                    _isListening.value = false
                    _audioLevel.value = 0f
                }
                else -> {
                    _isListening.value = false
                    _audioLevel.value = 0f
                }
            }
        }

        override fun onResults(results: Bundle?) {
            stopAlivenessPulse()
            _isListening.value = false
            _audioLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                _spokenText.value = text
                processUserQuery(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                _spokenText.value = partial
                _audioLevel.value = 0.65f
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun startAlivenessPulse() {
        pulseJob?.cancel()
        pulseJob = viewModelScope.launch {
            while (isActive && _isListening.value) {
                // Subtle undulating wave level so the orb and visualizer breathe with aliveness
                val base = 0.22f + Random.nextFloat() * 0.28f
                _audioLevel.value = base
                delay(120)
            }
        }
    }

    private fun stopAlivenessPulse() {
        pulseJob?.cancel()
        pulseJob = null
    }

    fun startListening(context: Context) {
        lastContext = context.applicationContext
        voiceEngine?.stop()
        _isSpeaking.value = false
        _isListening.value = true
        _spokenText.value = if (isHindiLanguage) "साथी सुन रहा है..." else "Listening..."
        startAlivenessPulse()

        startListeningInternal(context)
    }

    private fun startListeningInternal(context: Context) {
        try {
            if (speechRecognizer == null) {
                setupSpeechRecognizer(context)
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindiLanguage) "hi-IN" else "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            _isListening.value = false
            stopAlivenessPulse()
        }
    }

    fun stopListening() {
        stopAlivenessPulse()
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _audioLevel.value = 0f
    }

    fun stopSpeaking() {
        voiceEngine?.stop()
        _isSpeaking.value = false
        _audioLevel.value = 0f
    }

    fun dismissEmergencyDialer() {
        _showEmergencyDialer112.value = false
    }

    fun processUserQuery(query: String) {
        viewModelScope.launch {
            _showEmergencyDialer112.value = false
            _isListening.value = false
            stopAlivenessPulse()

            val response = brain.reason(query)
            _lastBrainResponse.value = response

            val answerText = if (isHindiLanguage) response.spokenHindi else response.spokenEnglish
            _sevaCareResponse.value = answerText

            if (response.isEmergency) {
                _showEmergencyDialer112.value = true
            }

            voiceEngine?.speak(answerText)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAlivenessPulse()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        voiceEngine?.stop()
    }
}
