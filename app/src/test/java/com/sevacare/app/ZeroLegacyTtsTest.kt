package com.sevacare.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architectural Lint Test:
 * Statically asserts ZERO imports of the legacy Google Android TextToSpeech engine
 * across all Kotlin source files in app/src/main.
 */
class ZeroLegacyTtsTest {

    @Test
    fun assertZeroLegacyTtsImportsInMainSourceSet() {
        val projectRoots = listOf(
            File("app/src/main/java"),
            File("C:/Naman Swami/SevaCare/app/src/main/java"),
            File("../app/src/main/java")
        )

        val mainDir = projectRoots.firstOrNull { it.exists() && it.isDirectory }
        assertTrue("Main source directory must exist for scanning", mainDir != null)

        val offendingFiles = mutableListOf<String>()

        mainDir!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import ") && trimmed.contains("android.speech.tts")) {
                        offendingFiles.add("${file.name}:${index + 1} -> $trimmed")
                    }
                }
            }

        assertEquals(
            "Legacy Google android.speech.tts imports found! Zero-mock neural TTS policy violated:\n" +
                    offendingFiles.joinToString("\n"),
            0,
            offendingFiles.size
        )
    }
}
