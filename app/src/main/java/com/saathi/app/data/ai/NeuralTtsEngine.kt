package com.saathi.app.data.ai

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * State representing the lifecycle of the on-device Neural TTS Engine.
 */
sealed class TtsState {
    object Idle : TtsState()
    data class Downloading(val progress: Float) : TtsState()
    object Ready : TtsState()
    data class Speaking(val text: String, val lang: String) : TtsState()
    data class Error(val message: String) : TtsState()
}

/**
 * Interface for on-device Neural Speech Synthesis (Sherpa-ONNX / Piper).
 * Guarantees zero reliance on the legacy Google cloud/Android TextToSpeech engine.
 */
interface NeuralTtsEngine {
    val state: StateFlow<TtsState>

    /**
     * Warms up and initializes the neural model from local app-private storage.
     * Throws an exception and transitions [state] to [TtsState.Error] if the model file is missing or invalid.
     */
    suspend fun warmup(modelFile: File)

    /**
     * Synthesizes and streams spoken audio through the device speaker.
     */
    suspend fun speak(text: String, lang: String = "hi")

    /**
     * Synthesizes text into raw 16-bit linear PCM audio byte buffer (22050 Hz mono).
     * Used for streaming AudioTrack playback and automated verification.
     */
    fun synthesizePcm(text: String, lang: String = "hi"): ByteArray

    /**
     * Immediately halts any active audio playback.
     */
    fun stop()

    /**
     * Releases all native resources and worker threads.
     */
    fun release()
}
