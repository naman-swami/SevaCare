package com.saathi.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saathi.app.data.repository.SaathiRepository

class VoiceGuideViewModelFactory(
    private val repository: SaathiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceGuideViewModel::class.java)) {
            return VoiceGuideViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
