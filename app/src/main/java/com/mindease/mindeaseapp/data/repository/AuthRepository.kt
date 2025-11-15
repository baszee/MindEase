package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.utils.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk menangani semua operasi Otentikasi (Login, Register, Logout, Guest).
 */
class AuthRepository(val auth: FirebaseAuth = Firebase.auth) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Melakukan update nama tampilan (displayName) pengguna.
     */
    fun updateProfileName(name: String): Flow<AuthResult<FirebaseUser>> = flow {
        emit(AuthResult.Loading)
        try {
            val user = auth.currentUser
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profileUpdates).await()
                emit(AuthResult.Success(user))
            } else {
                emit(AuthResult.Error(Exception("User not authenticated for profile update.")))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error(e))
        }
    }

    /**
     * Re-authenticate pengguna dengan sandi lama.
     */
    suspend fun reauthenticateUser(email: String, oldPassword: String): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser
            if (user == null || user.isAnonymous) {
                return AuthResult.Error(Exception("Pengguna tidak ditemukan atau adalah Guest."))
            }

            val credential = EmailAuthProvider.getCredential(email, oldPassword)
            user.reauthenticate(credential).await()

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    /**
     * Mengganti kata sandi pengguna setelah berhasil re-authenticate.
     */
    suspend fun updatePassword(newPassword: String): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error(Exception("Sesi pengguna berakhir. Mohon login ulang."))
            }

            user.updatePassword(newPassword).await()

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }


    /**
     * Menghapus akun pengguna yang sudah terotentikasi.
     */
    suspend fun deleteUserAccount(): AuthResult<Unit> { // FIX: Diubah menjadi block body
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error(Exception("Sesi pengguna berakhir. Tidak dapat menghapus akun."))
            }

            // Hapus user dari Firebase Auth
            user.delete().await()

            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

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