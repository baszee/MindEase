package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.utils.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow // FIX: Import flow
import kotlinx.coroutines.tasks.await // FIX: Import await

/**
 * Repository untuk menangani semua operasi Otentikasi (Login, Register, Logout, Guest).
 */
class AuthRepository(private val auth: FirebaseAuth = Firebase.auth) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Melakukan update nama tampilan (displayName) pengguna.
     */
    fun updateProfileName(name: String): Flow<AuthResult<FirebaseUser>> = flow { // FIX: flow sudah diimport
        emit(AuthResult.Loading)
        try {
            val user = auth.currentUser
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profileUpdates).await() // FIX: await sudah diimport
                emit(AuthResult.Success(user))
            } else {
                emit(AuthResult.Error(Exception("User not authenticated for profile update.")))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error(e))
        }
    }

    /**
     * Melakukan login menggunakan kredensial pihak ketiga (misalnya, Google ID Token).
     */
    suspend fun signInWithCredential(credential: AuthCredential): AuthResult<FirebaseUser> = try {
        val result = auth.signInWithCredential(credential).await() // FIX: await sudah diimport
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
        val result = auth.createUserWithEmailAndPassword(email, password).await() // FIX: await sudah diimport
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
        val result = auth.signInWithEmailAndPassword(email, password).await() // FIX: await sudah diimport
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
        val result = auth.signInAnonymously().await() // FIX: await sudah diimport
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