package com.sevacare.app.ui.voice

import androidx.compose.runtime.Composable
import com.sevacare.app.data.repository.SevaCareRepository

/**
 * Voice Guide companion surface.
 * Delegates seamlessly to the signature [LiveVoiceScreen] implementing the
 * 2026 Modern Android UI Architecture blueprint (Section 7).
 */
@Composable
fun VoiceGuideScreen(
    repository: SevaCareRepository,
    onBack: () -> Unit
) {
    LiveVoiceScreen(
        repository = repository,
        onBack = onBack
    )
}
