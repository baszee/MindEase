package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// FIX: Mengubah import agar kompatibel dengan MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance MoodHistoryViewModel.
 * FIX: Sekarang menggunakan MoodCloudRepository
 */
class MoodHistoryViewModelFactory(private val repository: MoodCloudRepository) : ViewModelProvider.Factory { // FIX: Mengganti tipe Repository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if ((modelClass.isAssignableFrom(MoodHistoryViewModel::class.java))) {
            return MoodHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}