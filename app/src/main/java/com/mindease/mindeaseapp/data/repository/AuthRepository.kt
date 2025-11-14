package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.AuthCredential // FIX: Wajib ada untuk Google Sign-In
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.utils.AuthResult // Wajib diimpor
import kotlinx.coroutines.tasks.await // Wajib untuk fungsi suspend

/**
 * Repository untuk menangani semua operasi Otentikasi (Login, Register, Logout, Guest).
 */
class AuthRepository(private val auth: FirebaseAuth = Firebase.auth) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Melakukan login menggunakan kredensial pihak ketiga (misalnya, Google ID Token).
     */
    suspend fun signInWithCredential(credential: AuthCredential): AuthResult<FirebaseUser> = try {
        val result = auth.signInWithCredential(credential).await()
        val user = result.user
        if (user != null) {
            AuthResult.Success(user)
        } else {
            AuthResult.Error(Exception("Login dengan kredensial gagal: Pengguna adalah null."))
        }
    } catch (e: Exception) {
        AuthResult.Error(e)
    }

    /**
     * Melakukan pendaftaran pengguna baru.
     */
    suspend fun register(email: String, password: String): AuthResult<FirebaseUser> = try {
        // Menggunakan await() untuk mengubah operasi asinkron menjadi suspend function
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user
        if (user != null) {
            AuthResult.Success(user)
        } else {
            AuthResult.Error(Exception("Pendaftaran gagal: Pengguna adalah null."))
        }
    } catch (e: Exception) {
        AuthResult.Error(e)
    }

    /**
     * Melakukan login pengguna.
     */
    suspend fun login(email: String, password: String): AuthResult<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user
        if (user != null) {
            AuthResult.Success(user)
        } else {
            AuthResult.Error(Exception("Login gagal: Pengguna adalah null."))
        }
    } catch (e: Exception) {
        AuthResult.Error(e)
    }

    /**
     * Melakukan login tamu/anonim.
     */
    suspend fun loginAsGuest(): AuthResult<FirebaseUser> = try {
        val result = auth.signInAnonymously().await()
        val user = result.user
        if (user != null) {
            AuthResult.Success(user)
        } else {
            AuthResult.Error(Exception("Login tamu gagal: Pengguna adalah null."))
        }
    } catch (e: Exception) {
        AuthResult.Error(e)
    }

    fun logout() {
        auth.signOut()
    }
}