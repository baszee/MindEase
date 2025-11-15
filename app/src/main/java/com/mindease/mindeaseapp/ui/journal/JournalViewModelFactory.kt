package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository // GANTI IMPORT INI
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance JournalViewModel dengan JournalCloudRepository yang sudah ada.
 */
class JournalViewModelFactory(private val repository: JournalCloudRepository) : ViewModelProvider.Factory { // GANTI TIPE REPOSITORY

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // FIX: Memanggil kelas JournalViewModel yang sudah bersih
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}