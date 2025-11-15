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

class AuthViewModel(val repository: AuthRepository) : ViewModel() { // FIX: DIUBAH MENJADI val

    private val _loginResult = MutableLiveData<AuthResult<FirebaseUser>>()
    val loginResult: LiveData<AuthResult<FirebaseUser>> = _loginResult

    private val _deleteResult = MutableLiveData<AuthResult<Unit>>() // LiveData Khusus untuk hasil Delete
    val deleteResult: LiveData<AuthResult<Unit>> = _deleteResult // BARU

    fun updateProfileName(name: String) {
        viewModelScope.launch {
            repository.updateProfileName(name).collect { result ->
                _loginResult.value = result
            }
        }
    }

    /**
     * Mengganti kata sandi pengguna (BARU)
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

    /**
     * Menghapus akun pengguna dari Firebase Auth (dipanggil setelah data Jurnal dibersihkan).
     */
    fun deleteUserAccount() {
        _deleteResult.value = AuthResult.Loading
        viewModelScope.launch {
            _deleteResult.value = repository.deleteUserAccount()
        }
    }
}