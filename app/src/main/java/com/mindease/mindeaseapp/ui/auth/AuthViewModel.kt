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

class AuthViewModel(val repository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<AuthResult<FirebaseUser>>()
    val loginResult: LiveData<AuthResult<FirebaseUser>> = _loginResult

    private val _deleteResult = MutableLiveData<AuthResult<Unit>>()
    val deleteResult: LiveData<AuthResult<Unit>> = _deleteResult

    /**
     * Digunakan untuk Re-authentication user yang menggunakan Google/Pihak Ketiga (sebelum Delete/Link)
     */
    fun reauthenticateUserWithCredential(credential: AuthCredential) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.reauthenticateWithCredential(credential)
        }
    }

    /**
     * Digunakan oleh Google User untuk pertama kali menyetel password.
     */
    fun linkNewPasswordToGoogleUser(newPassword: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.linkNewPassword(newPassword)
        }
    }

    /**
     * 🔥 BARU: Update password langsung tanpa re-auth (untuk Google user yang sudah punya password)
     */
    fun updatePasswordDirectly(newPassword: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.updatePassword(newPassword)
        }
    }

    fun updateUserProfile(name: String, bio: String, imageUrl: String? = null) {
        viewModelScope.launch {
            repository.updateUserProfile(name, bio, imageUrl).collect { result ->
                _loginResult.value = result
            }
        }
    }

    /**
     * Mengganti kata sandi pengguna (HANYA UNTUK EMAIL/PASS USER)
     */
    fun changePassword(email: String, oldPassword: String, newPassword: String) {
        _loginResult.value = AuthResult.Loading
        viewModelScope.launch {
            // 1. Re-authenticate
            val reauthResult = repository.reauthenticateUser(email, oldPassword)

            if (reauthResult is AuthResult.Success) {
                // 2. Jika re-auth sukses, update password
                _loginResult.value = repository.updatePassword(newPassword)
            } else if (reauthResult is AuthResult.Error) {
                // Re-auth gagal (sandi lama salah, dll.)
                _loginResult.value = reauthResult
            }
        }
    }

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

    fun deleteUserAccount() {
        _deleteResult.value = AuthResult.Loading
        viewModelScope.launch {
            _deleteResult.value = repository.deleteUserAccount()
        }
    }
}