package com.saathi.app

import com.saathi.app.util.StoryRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit verification test for StoryRecorder:
 * 1. Output file creation and recording lifecycle.
 * 2. Amplitude polling produces valid non-negative range.
 * 3. File exists on disk and is non-zero in size after stop.
 */
class StoryRecorderTest {

    @Test
    fun testRecordingLifecycleAndFileGeneration() {
        val tempDir = File("build/tmp/story_tests_${System.currentTimeMillis()}").apply { mkdirs() }
        val outputFile = File(tempDir, "test_family_story.m4a")

        val recorder = StoryRecorder(context = null)

        try {
            assertFalse("Recorder must not be active before start", recorder.isCurrentlyRecording())
            assertEquals("Amplitude before start must be 0", 0, recorder.pollAmplitude())

            recorder.start(outputFile)
            assertTrue("Recorder must be active after start", recorder.isCurrentlyRecording())

            // Poll amplitude during recording
            val amp = recorder.pollAmplitude()
            assertTrue("Amplitude must be non-negative: $amp", amp >= 0)

            // Simulate recording duration
            Thread.sleep(120)

            val (resultFile, durationMs) = recorder.stop()

            assertFalse("Recorder must not be active after stop", recorder.isCurrentlyRecording())
            assertNotNull("Output file must not be null", resultFile)
            assertTrue("File must exist on disk: ${outputFile.path}", outputFile.exists())
            assertTrue("File size must be greater than zero: ${outputFile.length()} bytes", outputFile.length() > 0)
            assertTrue("Duration must be recorded: $durationMs ms", durationMs >= 100)
        } finally {
            if (outputFile.exists()) outputFile.delete()
            tempDir.delete()
        }
    }
}
