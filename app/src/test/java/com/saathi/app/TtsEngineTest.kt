package com.saathi.app

import com.saathi.app.data.ai.SherpaOnnxTtsEngine
import com.saathi.app.data.ai.TtsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

/**
 * Unit verification test asserting Neural TTS Engine behavior:
 * 1. Missing model file strictly causes FileNotFoundException and enters TtsState.Error.
 * 2. Valid model file warms up to TtsState.Ready.
 * 3. synthesizePcm produces valid, non-empty 16-bit linear PCM audio frames.
 * 4. speak() executes properly without crashing.
 */
class TtsEngineTest {

    @Test
    fun testMissingModelFileTriggersErrorState() = runBlocking {
        val engine = SherpaOnnxTtsEngine(context = null)
        val nonExistentFile = File("build/tmp/non_existent_neural_model_${System.currentTimeMillis()}.onnx")
        if (nonExistentFile.exists()) nonExistentFile.delete()

        try {
            engine.warmup(nonExistentFile)
            fail("Warmup on missing model file must throw FileNotFoundException")
        } catch (e: FileNotFoundException) {
            assertTrue("Exception message must reference missing file", e.message?.contains("does not exist") == true)
        }

        assertTrue("Engine state must be TtsState.Error", engine.state.value is TtsState.Error)
    }

    @Test
    fun testWarmedUpEngineProducesNonEmptyPcmAudio() = runBlocking {
        val engine = SherpaOnnxTtsEngine(context = null)

        val tempModel = File.createTempFile("test_neural_model", ".onnx")
        try {
            tempModel.writeText("ONNX_TEST_MODEL_HEADER_WEIGHTS_V1")

            engine.warmup(tempModel)
            assertEquals("Engine state must transition to Ready", TtsState.Ready, engine.state.value)

            val hindiText = "नमस्ते, आपकी दवा का समय हो गया है"
            val pcmData = engine.synthesizePcm(hindiText, "hi")

            assertNotNull("PCM byte array must not be null", pcmData)
            assertTrue("PCM byte array must not be empty", pcmData.isNotEmpty())
            assertTrue("PCM duration must scale with text length", pcmData.size >= 8000)

            // Verify valid 16-bit PCM values are present
            var hasNonZeroSamples = false
            for (i in 0 until (pcmData.size / 2)) {
                val sample = (pcmData[i * 2].toInt() and 0xFF) or (pcmData[i * 2 + 1].toInt() shl 8)
                if (sample != 0) {
                    hasNonZeroSamples = true
                    break
                }
            }
            assertTrue("PCM audio must contain non-zero acoustic waveform samples", hasNonZeroSamples)

            // Test speak invocation
            engine.speak(hindiText, "hi")
            assertEquals("State must return to Ready after speaking finishes", TtsState.Ready, engine.state.value)
        } finally {
            tempModel.delete()
        }
    }

    @Test
    fun testSynthesizeThrowsWhenNotWarmedUp() {
        val engine = SherpaOnnxTtsEngine(context = null)
        try {
            engine.synthesizePcm("Hello", "en")
            fail("Synthesize on unwarmed engine must throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue("Error must explain engine is not warmed up", e.message?.contains("not warmed up") == true)
        }
    }
}
