package com.saathi.app.ui.voice

import androidx.compose.runtime.Composable
import com.saathi.app.data.repository.SaathiRepository

/**
 * Voice Guide companion surface.
 * Delegates seamlessly to the signature [LiveVoiceScreen] implementing the
 * 2026 Modern Android UI Architecture blueprint (Section 7).
 */
@Composable
fun VoiceGuideScreen(
    repository: SaathiRepository,
    onBack: () -> Unit
) {
    LiveVoiceScreen(
        repository = repository,
        onBack = onBack
    )
}
