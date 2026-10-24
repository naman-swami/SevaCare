package com.sevacare.app.ui.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sevacare.app.data.repository.SevaCareRepository

class DocumentViewModelFactory(
    private val repository: SevaCareRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
            return DocumentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
