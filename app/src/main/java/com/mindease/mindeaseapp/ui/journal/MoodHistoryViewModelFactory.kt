package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.MoodRepository
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance MoodHistoryViewModel.
 */
class MoodHistoryViewModelFactory(private val repository: MoodRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoodHistoryViewModel::class.java)) {
            return MoodHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}