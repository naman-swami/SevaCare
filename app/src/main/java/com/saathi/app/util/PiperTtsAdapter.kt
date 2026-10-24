package com.saathi.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Piper Offline VITS Neural Text-to-Speech Adapter (Sherpa-ONNX Architecture).
 *
 * Designed to load pre-trained Piper ONNX models (vits-piper-hi_IN / vits-piper-en_US-lessac)
 * from app assets or internal storage, synthesize PCM Float samples, and play via AudioTrack
 * with USAGE_ASSISTANCE_ACCESSIBILITY routing.
 */
data class PiperVoiceConfig(
    val modelPath: String,
    val tokensPath: String,
    val dataDirPath: String,
    val noiseScale: Float = 0.667f,
    val noiseScaleW: Float = 0.8f,
    val lengthScale: Float = 1.22f // 1.0f / 0.82f for calm senior pacing
)

class PiperTtsAdapter(private val context: Context) {

    var isModelAvailable: Boolean = false
        private set

    init {
        checkModelAvailability()
    }

    private fun checkModelAvailability() {
        // Checks if Piper assets have been bundled in assets or storage
        try {
            val assetList = context.assets.list("piper") ?: emptyArray()
            isModelAvailable = assetList.any { it.endsWith(".onnx") }
        } catch (e: Exception) {
            isModelAvailable = false
        }
    }

    /**
     * Plays raw PCM audio samples using low-latency AudioTrack.
     */
    suspend fun playPcmSamples(samples: FloatArray, sampleRate: Int = 22050) = withContext(Dispatchers.Default) {
        if (samples.isEmpty()) return@withContext

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize.coerceAtLeast(samples.size * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }
}
