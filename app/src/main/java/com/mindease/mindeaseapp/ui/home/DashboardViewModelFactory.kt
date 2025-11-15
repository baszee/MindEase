package com.mindease.mindeaseapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.QuoteRepository // Import Quote Repository
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance DashboardViewModel dengan MoodCloudRepository dan QuoteRepository.
 */
class DashboardViewModelFactory(
    private val moodRepository: MoodCloudRepository,
    private val quoteRepository: QuoteRepository // Tambahkan QuoteRepository di Factory
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            // FIX: Berikan kedua repository ke ViewModel
            return DashboardViewModel(moodRepository, quoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}