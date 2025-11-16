package com.mindease.mindeaseapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.QuoteRepository
import com.mindease.mindeaseapp.data.repository.AuthRepository // 🔥 Import AuthRepository
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance DashboardViewModel dengan MoodCloudRepository, QuoteRepository, dan AuthRepository.
 */
class DashboardViewModelFactory(
    private val moodRepository: MoodCloudRepository,
    private val quoteRepository: QuoteRepository,
    private val authRepository: AuthRepository // 🔥 Tambahkan AuthRepository di Factory
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            // FIX: Berikan SEMUA repository ke ViewModel
            return DashboardViewModel(moodRepository, quoteRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}