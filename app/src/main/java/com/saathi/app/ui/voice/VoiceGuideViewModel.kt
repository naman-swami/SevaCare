package com.saathi.app.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saathi.app.data.repository.SaathiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Voice Guide companion designed for Indian elderly users.
 *
 * SAFETY PRINCIPLE:
 * All responses are grounded in verified deterministic templates and local database rows.
 * No unconstrained generative AI is used for medical advice or legal guidance.
 * Speech rate is tuned to 0.82f for clear geriatric auditory comprehension.
 */
class VoiceGuideViewModel(
    private val repository: SaathiRepository
) : ViewModel() {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _saathiResponse = MutableStateFlow<String>(
        "Namaste Dadi ji / Baba ji! Main Saathi hoon. Mic dabayein aur kuch bhi poochein — dawaai ke baare mein ya kisi anjaan phone call ke baare mein."
    )
    val saathiResponse: StateFlow<String> = _saathiResponse.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    fun initVoice(context: Context) {
        // Initialize SpeechRecognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { _isListening.value = false }
                    override fun onError(error: Int) {
                        _isListening.value = false
                    }
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _spokenText.value = text
                            processUserQuery(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        // Initialize TextToSpeech
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindiLocale = Locale("hi", "IN")
                val res = tts?.setLanguage(hindiLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(0.82f) // Elderly-friendly cadence
                tts?.setPitch(1.05f)
            }
        }
    }

    fun startListening(context: Context) {
        tts?.stop()
        _isSpeaking.value = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye, Saathi sun raha hai...")
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun processUserQuery(query: String) {
        viewModelScope.launch {
            val lower = query.lowercase()

            // 1. Check for Scam / Police / CBI / Digital Arrest mentions
            val scamAnalysis = repository.analyzeForScamSignals(query)
            if (scamAnalysis.score >= 3 || lower.contains("police") || lower.contains("cbi") ||
                lower.contains("ed") || lower.contains("giraftar") || lower.contains("arrest") ||
                lower.contains("paise maang") || lower.contains("otp")) {
                val reply = "Dhyan se suniye! Police, CBI ya court kabhi bhi phone par paise nahi maangte aur na hi video call par arrest karte hain. Yeh 100% scam hai. Foran phone kaat dijiye aur apne bachhon ko batayein."
                _saathiResponse.value = reply
                speakHindi(reply)
                return@launch
            }

            // 2. Check for Medicine mentions in database
            val medicines = repository.searchMedicines(query)
            if (medicines.isNotEmpty()) {
                val med = medicines.first()
                val reply = "${med.brandName}: ${med.purposeHindi} Yeh lene ka tareeka: ${med.howToTakeHindi}. Jan Aushadhi mein yeh lagbhag ${med.janAushadhiMrp.toInt()} rupaye mein milti hai jabki branded ${med.brandMrp.toInt()} rupaye ki aati hai."
                _saathiResponse.value = reply
                speakHindi(reply)
                return@launch
            }

            // 3. Common Elderly Health Queries
            when {
                lower.contains("khana") || lower.contains("khane ke") || lower.contains("pehle") || lower.contains("baad") -> {
                    val reply = "Aamtaur par pet ki dawaai jaise Pan 40 ya Omez khali pet subah li jaati hai. Sugar aur BP ki dawaai aksar naashte ke baad ya khane ke saath li jaati hai. Dawaai scan karke Saathi se bilkul sahi niyam jaan sakte hain."
                    _saathiResponse.value = reply
                    speakHindi(reply)
                }
                lower.contains("duplicate") || lower.contains("ek hi") || lower.contains("dono") -> {
                    val reply = "Jab do alag brand ki dawaai mein ek hi namak ya salt hota hai, jaise Glycomet aur Glucophage dono mein Metformin hota hai, dono saath lene se overdose ho sakta hai. Saathi mein patta scan karein, yeh turant alert deta hai."
                    _saathiResponse.value = reply
                    speakHindi(reply)
                }
                lower.contains("jan aushadhi") || lower.contains("sasti") || lower.contains("parchi") -> {
                    val reply = "Pradhan Mantri Jan Aushadhi Kendra par vahi zaroori dawaai 50 se 90 percent saste daam par milti hai. Saathi app se Chemist Parchi banakar apne mohalle ke chemist ko dikha sakte hain."
                    _saathiResponse.value = reply
                    speakHindi(reply)
                }
                else -> {
                    val reply = "Maine suna: \"$query\". Dawaai ka naam bolkar poochein (jaise Telma ya Metformin), ya koi notice / call ki jaankari lene ke liye poochein."
                    _saathiResponse.value = reply
                    speakHindi(reply)
                }
            }
        }
    }

    fun speakHindi(text: String) {
        tts?.stop()
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "saathi_voice_guide")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
