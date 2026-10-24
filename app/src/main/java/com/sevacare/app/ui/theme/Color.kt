package com.sevacare.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SevaCare Official Design Tokens (Upgrade Brief v2, Section 5)
 *
 * Mathematical Contrast Standards:
 * - Base Cream (#FFF8EC): Background
 * - Saffron (#F5A623): Fills and accents ONLY (never text on cream; ratio ~1.92:1)
 * - Deep Teal (#0F6B5C): Fills, large headers, and primary icon tints (6.07:1 on Cream)
 * - Body Text Teal (#0B4F44): Body copy (8.98:1 on Cream, exceeds WCAG AAA 7:1)
 * - Dark Text (#1E1E1E): Primary high-contrast text (15.79:1 on Cream)
 * - Saffron Button Text (#2B1B00): Text on saffron button fills (8.23:1 on Saffron, WCAG AAA)
 * - Danger Red (#C62828): SOS, Ruko Mode warnings, expired badges (5.62:1 with White)
 * - Soft Tints: Peach (#FFE3C2), Mint (#D6EFE8)
 * - Button 3D Press Lips: Derived by darkening fill by ~25%
 */

// Core Palette
val SevaCream = Color(0xFFFFF8EC)
val SevaCreamSurface = Color(0xFFFFF3DE)
val SevaCreamContainer = Color(0xFFFFECC8)

val SevaSaffron = Color(0xFFF5A623)
val SevaSaffronLip = Color(0xFFB87B17) // 25% darker for 4dp press lip
val SevaOnSaffronText = Color(0xFF2B1B00) // WCAG AAA on Saffron

val SevaDeepTeal = Color(0xFF0F6B5C)
val SevaDeepTealLip = Color(0xFF0A4A40) // 25% darker
val SevaBodyTeal = Color(0xFF0B4F44) // WCAG AAA Body Text (8.98:1)
val SevaDarkText = Color(0xFF1E1E1E) // WCAG AAA Body Text (15.79:1)

val SevaDanger = Color(0xFFC62828)
val SevaDangerLip = Color(0xFF921B1B) // 25% darker
val SevaWhite = Color(0xFFFFFFFF)

// Soft Supporting Tints
val SevaPeach = Color(0xFFFFE3C2)
val SevaMint = Color(0xFFD6EFE8)
val SevaSafeGreen = Color(0xFF1B7340)
val SevaSafeGreenContainer = Color(0xFFD7F2E2)
val SevaDuplicateAmber = Color(0xFF8A5300)
val SevaDuplicateAmberContainer = Color(0xFFFFECC7)

// Backward-compatibility aliases for existing UI code during migration
val SevaCareTealPrimary = SevaDeepTeal
val SevaCareTealDark = SevaBodyTeal
val SevaCareTealLight = Color(0xFF188E7C)
val SevaCareTealContainer = SevaMint
val SevaCareOnTealContainer = SevaBodyTeal

val SevaCareAmberAccent = SevaSaffron
val SevaCareAmberDark = SevaSaffronLip
val SevaCareAmberContainer = SevaCreamContainer
val SevaCareOnAmberContainer = SevaOnSaffronText

val PauseRed = SevaDanger
val PauseRedContainer = Color(0xFFFFDAD6)
val PauseOnRedContainer = Color(0xFF410002)

val RukoRed = PauseRed
val RukoRedContainer = PauseRedContainer
val RukoOnRedContainer = PauseOnRedContainer

val SafeGreen = SevaSafeGreen
val SafeGreenContainer = SevaSafeGreenContainer

val DuplicateAmber = SevaDuplicateAmber
val DuplicateAmberContainer = SevaDuplicateAmberContainer

val SevaCareCreamBg = SevaCream
val SevaCareSurface = SevaCreamSurface
val SevaCareSurfaceVariant = Color(0xFFF3E7D3)
