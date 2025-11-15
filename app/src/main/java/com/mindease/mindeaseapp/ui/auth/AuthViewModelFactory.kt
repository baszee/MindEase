package com.mindease.mindeaseapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.data.repository.AuthRepository
// import kotlin.reflect.KClass // Hapus atau tidak diperlukan jika menggunakan .java

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // FIX: Menggunakan .java eksplisit untuk menghindari ambiguitas KClass/Class
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}