package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.data.model.UserProfile
import com.mindease.mindeaseapp.utils.AuthResult
import com.mindease.mindeaseapp.utils.retryWithExponentialBackoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repository untuk menangani semua operasi Otentikasi (Login, Register, Logout, Guest) dan Profile Data di Firestore.
 */
class AuthRepository(
    val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val TAG = "AuthRepo"

    private fun getUserProfileCollection() = firestore.collection("user_profiles")

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ====================================================================
    // FIREBASE AUTH (Nama di Auth)
    // ====================================================================

    fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }

    suspend fun reloadCurrentUser() {
        try {
            auth.currentUser?.reload()?.await()
            Log.d(TAG, "FirebaseUser successfully reloaded from server.")
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading user: ${e.message}")
        }
    }

    // ====================================================================
    // FIRESTORE USER PROFILE (Bio, Image, Nama Sinkronisasi)
    // ====================================================================

    suspend fun getUserProfile(): UserProfile = retryWithExponentialBackoff(tag = "$TAG-GetProfile") {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")
        val snapshot = getUserProfileCollection().document(userId).get().await()

        return@retryWithExponentialBackoff snapshot.toObject(UserProfile::class.java)
            ?.copy(documentId = snapshot.id)
            ?: UserProfile(userId = userId)
    }

    fun updateUserProfile(name: String, bio: String, imageUrl: String? = null): Flow<AuthResult<FirebaseUser>> = flow {
        emit(AuthResult.Loading)
        try {
            val user = auth.currentUser
            val userId = user?.uid

            if (user != null && userId != null) {
                // 1. Update Firebase Auth (hanya DisplayName)
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(if (imageUrl != null) android.net.Uri.parse(imageUrl) else user.photoUrl)
                    .build()

                user.updateProfile(profileUpdates).await()

                // 2. Update Firestore User Profile (Name, Bio, Image URL)
                val userProfileData = UserProfile(
                    userId = userId,
                    name = name,
                    email = user.email,
                    bio = bio,
                    profileImageUrl = imageUrl
                )

                getUserProfileCollection().document(userId)
                    .set(userProfileData)
                    .await()

                reloadCurrentUser()

                emit(AuthResult.Success(user))
            } else {
                emit(AuthResult.Error(Exception("User not authenticated for profile update.")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile: ${e.message}")
            emit(AuthResult.Error(e))
        }
    }

    // ====================================================================
    // AUTH BARU (UNTUK GOOGLE RE-AUTH & LINKING)
    // ====================================================================

    /**
     * 🔥 BARU: Re-authenticate pengguna dengan Credential (digunakan oleh Google user untuk Delete/Change Pass).
     */
    suspend fun reauthenticateWithCredential(credential: AuthCredential): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in.")
            user.reauthenticate(credential).await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    /**
     * 🔥 BARU: Link password baru ke akun yang sudah ada (hanya digunakan oleh Google user untuk menyetel password).
     */
    suspend fun linkNewPassword(newPassword: String): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in.")

            // Email diperlukan untuk membuat kredensial email/pass
            val email = user.email ?: throw Exception("User does not have an email address to set password.")

            val emailCredential = EmailAuthProvider.getCredential(email, newPassword)

            // Link the new email credential to the existing account
            val result = user.linkWithCredential(emailCredential).await()

            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    // ====================================================================
    // AUTH LAMA (Dipertahankan)
    // ====================================================================

    /**
     * Re-authenticate pengguna dengan sandi lama (HANYA digunakan oleh Email/Pass user).
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
     * Mengganti kata sandi pengguna (HANYA digunakan oleh Email/Pass user).
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


    suspend fun deleteUserAccount(): AuthResult<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error(Exception("Sesi pengguna berakhir. Tidak dapat menghapus akun."))
            }

            // Hapus dokumen profile custom dari Firestore
            getUserProfileCollection().document(user.uid).delete().await()

            // Hapus user dari Firebase Auth
            user.delete().await()

            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

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

    suspend fun register(email: String, password: String): AuthResult<FirebaseUser> = try {
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