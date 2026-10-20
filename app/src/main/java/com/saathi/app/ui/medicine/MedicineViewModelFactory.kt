package com.saathi.app.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saathi.app.data.repository.SaathiRepository

class MedicineViewModelFactory(
    private val repository: SaathiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            return MedicineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
