package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// FIX: Menggunakan import untuk Cloud Repository
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository

/**
 * Factory untuk membuat instance JournalViewModel.
 * FIX: Sekarang menggunakan JournalCloudRepository
 */
class JournalViewModelFactory(
    private val repository: JournalCloudRepository // FIX: Mengganti tipe Repository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if ((modelClass.isAssignableFrom(JournalViewModel::class.java))) {
            // Memberikan instance Cloud Repository ke ViewModel
            return JournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}