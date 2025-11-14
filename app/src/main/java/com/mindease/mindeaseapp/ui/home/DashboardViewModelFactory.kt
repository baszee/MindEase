package com.mindease.mindeaseapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.MoodRepository
import java.lang.IllegalArgumentException

/**
 * Factory untuk membuat instance DashboardViewModel dengan MoodRepository yang sudah ada.
 * Wajib digunakan karena ViewModel membutuhkan parameter di constructor-nya.
 */
class DashboardViewModelFactory(private val repository: MoodRepository) : ViewModelProvider.Factory {

    // Perhatikan: Override 'create' ini menggunakan class token yang diperlukan
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            // Jika kelas yang diminta adalah DashboardViewModel, kembalikan instance baru
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}