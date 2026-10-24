package com.sevacare.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sevacare.app.data.repository.SevaCareRepository

class VoiceGuideViewModelFactory(
    private val repository: SevaCareRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceGuideViewModel::class.java)) {
            return VoiceGuideViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
