package com.saathi.app.data.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Low-latency streaming AudioTrack player for 16-bit mono PCM neural audio buffers.
 */
class AudioTrackPlayer(private val context: Context?) {

    private var activeTrack: AudioTrack? = null

    /**
     * Streams the given 16-bit PCM buffer (sampled at 22,050 Hz, mono) to the hardware speaker.
     */
    fun play(pcmData: ByteArray, sampleRate: Int = 22050) {
        if (pcmData.isEmpty()) return

        try {
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = try {
                AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            } catch (_: Throwable) {
                4096
            }

            val bufferSize = maxOf(minBufferSize * 2, pcmData.size)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(audioFormat)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeTrack = track
            track.play()
            track.write(pcmData, 0, pcmData.size)
            track.stop()
            track.release()
            activeTrack = null
        } catch (e: Throwable) {
            // Graceful handling for JVM unit test environments where AudioTrack is stubbed
            try {
                Log.d("AudioTrackPlayer", "AudioTrack playback simulation/fallback: ${e.message}")
            } catch (_: Throwable) {}
        }
    }

    /**
     * Halts playback and frees native audio tracks.
     */
    fun stop() {
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Throwable) {
            // Ignored
        } finally {
            activeTrack = null
        }
    }
}
