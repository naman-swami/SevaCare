package com.sevacare.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * HardcodedStringScanTest ensures that no fake bracket translations
 * (e.g., Devanagari text followed immediately by bracketed English translation
 * like "रोकें (STOP)" or "पहचान रहे हैं (Recognizing...)") exist within UI composables.
 * All UI text must either be localized via stringResource() or loaded from database models.
 */
class HardcodedStringScanTest {

    private val fakeBracketPattern = Pattern.compile(
        """[\u0900-\u097F]+[^\"]*\([A-Za-z\s]{3,}\)"""
    )

    @Test
    fun testNoFakeBracketTranslationsInUiComposables() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val uiDir = if (File(projectDir, "app/src/main/java/com/sevacare/app/ui").exists()) {
            File(projectDir, "app/src/main/java/com/sevacare/app/ui")
        } else {
            File(projectDir, "src/main/java/com/sevacare/app/ui")
        }

        assertTrue("UI directory should exist: ${uiDir.absolutePath}", uiDir.exists())

        val violations = mutableListOf<String>()

        uiDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    // Exclude comments
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("*") && !trimmed.startsWith("/*")) {
                        // Check if line contains a Text() or label string with fake bracket translation
                        // (Exclude onboarding language picker chip "हिंदी (Hindi)" which is the canonical language selector)
                        if (fakeBracketPattern.matcher(line).find() && !line.contains("\"हिंदी (Hindi)\"") && !line.contains("\"मराठी (Marathi)\"")) {
                            violations.add("${file.name}:${index + 1} -> $trimmed")
                        }
                    }
                }
            }

        assertTrue(
            "Found fake bracket bilingual translations that should use stringResource():\n" +
                    violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
