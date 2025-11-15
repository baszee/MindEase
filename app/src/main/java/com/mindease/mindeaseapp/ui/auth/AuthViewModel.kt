package com.mindease.mindeaseapp.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.utils.AuthResult
import kotlinx.coroutines.launch

// FIX: Pastikan ADA 'private val' di sini
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<AuthResult<FirebaseUser>>()
    val loginResult: LiveData<AuthResult<FirebaseUser>> = _loginResult

    // Fungsi baru untuk Update Profile Name
    fun updateProfileName(name: String) {
        viewModelScope.launch {
            repository.updateProfileName(name).collect { result ->
                _loginResult.value = result
            }
        }
    }

    // Fungsi baru untuk Google Sign-In Credential (dari langkah sebelumnya)
    fun signInWithGoogleCredential(credential: AuthCredential) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.signInWithCredential(credential)
        }
    }

    fun register(email: String, password: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.register(email, password)
        }
    }

    fun login(email: String, password: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.login(email, password)
        }
    }

    fun loginAsGuest() {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.loginAsGuest()
        }
    }
}