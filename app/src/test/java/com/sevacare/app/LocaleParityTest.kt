package com.sevacare.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * CI Verification Test: LocaleParityTest
 *
 * Asserts 100% key parity across all supported language resource sets:
 * - values/ (English base)
 * - values-hi/ (Hindi)
 * - values-bn/ (Bengali)
 * - values-ta/ (Tamil)
 * - values-te/ (Telugu)
 * - values-mr/ (Marathi)
 *
 * Prevents missing translations and build failures in localized environments.
 */
class LocaleParityTest {

    private val locales = listOf(
        "values",
        "values-hi",
        "values-bn",
        "values-ta",
        "values-te",
        "values-mr"
    )

    private fun findResDirectory(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
            File("../app/src/main/res"),
            File("C:/Naman Swami/SevaCare/app/src/main/res")
        )
        for (candidate in candidates) {
            if (candidate.exists() && File(candidate, "values/strings.xml").exists()) {
                return candidate
            }
        }
        throw IllegalStateException("Unable to locate app/src/main/res directory")
    }

    private fun parseStringKeys(xmlFile: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        val stringNodes = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            if (node is Element) {
                keys.add(node.getAttribute("name"))
            }
        }
        return keys
    }

    @Test
    fun testAllLocalesHaveIdenticalKeySets() {
        val resDir = findResDirectory()
        val baseFile = File(resDir, "values/strings.xml")
        assertTrue("Base values/strings.xml must exist", baseFile.exists())

        val baseKeys = parseStringKeys(baseFile)
        assertTrue("Base strings.xml must have keys", baseKeys.isNotEmpty())

        for (locale in locales) {
            val localeFile = File(resDir, "$locale/strings.xml")
            assertTrue("Locale file must exist: $locale/strings.xml", localeFile.exists())

            val localeKeys = parseStringKeys(localeFile)
            val missingInLocale = baseKeys - localeKeys
            val extraInLocale = localeKeys - baseKeys

            val errorMsg = buildString {
                if (missingInLocale.isNotEmpty()) {
                    appendLine("Missing keys in $locale: $missingInLocale")
                }
                if (extraInLocale.isNotEmpty()) {
                    appendLine("Extra keys in $locale: $extraInLocale")
                }
            }

            assertEquals(
                "Locale parity failure for $locale:\n$errorMsg",
                emptySet<String>(),
                missingInLocale
            )
            assertEquals(
                "Extra keys in $locale:\n$errorMsg",
                emptySet<String>(),
                extraInLocale
            )
            assertEquals(
                "Key count mismatch for $locale",
                baseKeys.size,
                localeKeys.size
            )
        }
    }
}
