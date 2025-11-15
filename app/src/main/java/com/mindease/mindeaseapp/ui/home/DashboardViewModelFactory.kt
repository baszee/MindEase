package com.mindease.mindeaseapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository // FIX: Ganti ke Cloud Repository
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance DashboardViewModel dengan MoodCloudRepository yang sudah ada.
 */
class DashboardViewModelFactory(private val repository: MoodCloudRepository) : ViewModelProvider.Factory { // FIX: Menggunakan MoodCloudRepository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            // Jika kelas yang diminta adalah DashboardViewModel, kembalikan instance baru
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}