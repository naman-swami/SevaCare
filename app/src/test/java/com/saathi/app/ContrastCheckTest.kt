package com.saathi.app

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Unit tests verifying SevaCare WCAG 2.1 relative luminance and contrast ratios (Section 5).
 * Fails the build if:
 * - Body text on background is below 7.0:1 (WCAG AAA)
 * - Large text/headers on background is below 4.5:1 (WCAG AA)
 * - White text on Danger button is below 4.5:1
 * - Text on Saffron button is below 7.0:1
 */
class ContrastCheckTest {

    private data class RgbColor(val r: Int, val g: Int, val b: Int)

    private fun relativeLuminance(color: RgbColor): Double {
        fun channelLuminance(channel: Int): Double {
            val c = channel / 255.0
            return if (c <= 0.04045) {
                c / 12.92
            } else {
                ((c + 0.055) / 1.055).pow(2.4)
            }
        }
        val r = channelLuminance(color.r)
        val g = channelLuminance(color.g)
        val b = channelLuminance(color.b)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(colorA: RgbColor, colorB: RgbColor): Double {
        val lumA = relativeLuminance(colorA)
        val lumB = relativeLuminance(colorB)
        val l1 = max(lumA, lumB)
        val l2 = min(lumA, lumB)
        return (l1 + 0.05) / (l2 + 0.05)
    }

    // SevaCare Palette Hex Definitions
    private val cream = RgbColor(0xFF, 0xF8, 0xEC)       // #FFF8EC
    private val bodyTeal = RgbColor(0x0B, 0x4F, 0x44)    // #0B4F44
    private val darkText = RgbColor(0x1E, 0x1E, 0x1E)    // #1E1E1E
    private val deepTeal = RgbColor(0x0F, 0x6B, 0x5C)    // #0F6B5C
    private val saffron = RgbColor(0xF5, 0xA6, 0x23)     // #F5A623
    private val onSaffronText = RgbColor(0x2B, 0x1B, 0x00)// #2B1B00
    private val dangerRed = RgbColor(0xC6, 0x28, 0x28)   // #C62828
    private val white = RgbColor(0xFF, 0xFF, 0xFF)       // #FFFFFF

    @Test
    fun testBodyTextTealOnCream_MeetsWcagAaa() {
        val ratio = contrastRatio(bodyTeal, cream)
        println("Body Teal (#0B4F44) on Cream (#FFF8EC) Contrast Ratio: $ratio")
        assertTrue("Body Teal on Cream must be >= 7.0:1 (WCAG AAA). Actual: $ratio", ratio >= 7.0)
    }

    @Test
    fun testDarkTextOnCream_MeetsWcagAaa() {
        val ratio = contrastRatio(darkText, cream)
        println("Dark Text (#1E1E1E) on Cream (#FFF8EC) Contrast Ratio: $ratio")
        assertTrue("Dark Text on Cream must be >= 7.0:1 (WCAG AAA). Actual: $ratio", ratio >= 7.0)
    }

    @Test
    fun testDeepTealHeadersOnCream_MeetsWcagAaLarge() {
        val ratio = contrastRatio(deepTeal, cream)
        println("Deep Teal (#0F6B5C) on Cream (#FFF8EC) Contrast Ratio: $ratio")
        assertTrue("Deep Teal on Cream must be >= 4.5:1 (WCAG AA Large). Actual: $ratio", ratio >= 4.5)
    }

    @Test
    fun testOnSaffronTextOnSaffronButton_MeetsWcagAaa() {
        val ratio = contrastRatio(onSaffronText, saffron)
        println("Dark Text (#2B1B00) on Saffron (#F5A623) Contrast Ratio: $ratio")
        assertTrue("Text on Saffron must be >= 7.0:1 (WCAG AAA). Actual: $ratio", ratio >= 7.0)
    }

    @Test
    fun testWhiteTextOnDangerButton_MeetsWcagAa() {
        val ratio = contrastRatio(white, dangerRed)
        println("White (#FFFFFF) on Danger Red (#C62828) Contrast Ratio: $ratio")
        assertTrue("White on Danger Red must be >= 4.5:1. Actual: $ratio", ratio >= 4.5)
    }

    @Test
    fun testSaffronNeverUsedForTextOnCream() {
        val ratio = contrastRatio(saffron, cream)
        println("Saffron (#F5A623) on Cream (#FFF8EC) Contrast Ratio: $ratio")
        // Saffron on Cream is around 1.92:1, which is completely illegible for text.
        // This test asserts the rule: Saffron is an accent/fill ONLY, never text!
        assertTrue("Saffron on Cream is indeed below 3.0:1 ($ratio), confirming it must NEVER be used as text", ratio < 3.0)
    }
}
