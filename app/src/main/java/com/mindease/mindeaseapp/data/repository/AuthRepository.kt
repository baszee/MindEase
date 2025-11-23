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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay

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
    // 🔥 EMAIL VERIFICATION HELPERS
    // ====================================================================

    /**
     * Cek apakah user saat ini adalah Email/Password user (bukan Google/Guest)
     */
    fun isEmailPasswordUser(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isAnonymous) return false
        return user.providerData.any { it.providerId == "password" }
    }

    /**
     * Cek apakah user saat ini adalah Google user
     */
    fun isGoogleUser(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { it.providerId == "google.com" }
    }

    /**
     * Cek apakah email user sudah diverifikasi
     */
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    /**
     * Kirim email verifikasi ke user (HANYA untuk Email/Password user)
     */
    suspend fun sendEmailVerification(): AuthResult<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in.")

            // 🔥 FIX: Jangan kirim verifikasi untuk Google user
            if (isGoogleUser()) {
                Log.d(TAG, "Google user tidak perlu verifikasi email")
                return AuthResult.Success(Unit)
            }

            user.sendEmailVerification().await()
            Log.d(TAG, "Verification email sent to ${user.email}")
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send verification email: ${e.message}")
            AuthResult.Error(e)
        }
    }

    /**
     * 🔥 FIX: Cek verifikasi dengan double reload untuk avoid race condition
     * Hanya untuk CRITICAL ACTION: Change Password & Delete Account
     * Google user otomatis lolos (sudah verified by Google)
     */
    suspend fun checkVerificationForCriticalAction(): Boolean {
        try {
            // Single reload
            auth.currentUser?.reload()?.await()
        } catch (e: Exception) {
            Log.e("AuthRepo", "Error reloading user: ${e.message}")
            return false
        }

        val user = auth.currentUser ?: return false

        // Guest user: Boleh langsung
        if (user.isAnonymous) return true

        // Google user: Boleh langsung (Google sudah verifikasi)
        if (isGoogleUser()) return true

        // Email/Password user: HARUS sudah verifikasi
        return user.isEmailVerified
    }

    // ====================================================================
    // 🔥 ACCOUNT LINKING (MERGE EMAIL/PASSWORD + GOOGLE)
    // ====================================================================

    /**
     * 🔥 BARU: Cek apakah email sudah terdaftar dengan provider lain
     * Return: List of sign-in methods untuk email tersebut
     */
    suspend fun checkEmailExists(email: String): List<String> {
        return try {
            val methods = auth.fetchSignInMethodsForEmail(email).await()
            methods.signInMethods ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email: ${e.message}")
            emptyList()
        }
    }

    /**
     * 🔥 BARU: Link Google credential ke akun yang sudah ada
     * Digunakan saat Google Sign-In dengan email yang sudah punya akun Email/Password
     */
    suspend fun linkGoogleToExistingAccount(credential: AuthCredential): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in.")
            val result = user.linkWithCredential(credential).await()
            Log.d(TAG, "Google account linked successfully")
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to link Google account: ${e.message}")
            AuthResult.Error(e)
        }
    }

    // ====================================================================
    // FIRESTORE USER PROFILE
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
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(if (imageUrl != null) android.net.Uri.parse(imageUrl) else user.photoUrl)
                    .build()

                user.updateProfile(profileUpdates).await()

                val userProfileData = UserProfile(
                    userId = userId,
                    name = name,
                    email = user.email,
                    bio = bio,
                    profileImageUrl = imageUrl
                )

                getUserProfileCollection().document(userId)
                    .set(userProfileData, SetOptions.merge())
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
    // AUTH UNTUK GOOGLE RE-AUTH & LINKING
    // ====================================================================

    suspend fun reauthenticateWithCredential(credential: AuthCredential): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in.")
            user.reauthenticate(credential).await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    suspend fun linkNewPassword(newPassword: String): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in.")
            val email = user.email ?: throw Exception("User does not have an email address to set password.")
            val emailCredential = EmailAuthProvider.getCredential(email, newPassword)
            val result = user.linkWithCredential(emailCredential).await()
            Log.d(TAG, "Password linked successfully to Google account")
            AuthResult.Success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to link password: ${e.message}")
            AuthResult.Error(e)
        }
    }

    // ====================================================================
    // AUTH UNTUK EMAIL/PASSWORD USER
    // ====================================================================

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

    suspend fun updatePassword(newPassword: String): AuthResult<FirebaseUser> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error(Exception("Sesi pengguna berakhir. Mohon login ulang."))
            }

            user.updatePassword(newPassword).await()
            Log.d(TAG, "Password updated successfully")

            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update password: ${e.message}")
            AuthResult.Error(e)
        }
    }

    suspend fun deleteUserAccount(): AuthResult<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error(Exception("Sesi pengguna berakhir. Tidak dapat menghapus akun."))
            }

            getUserProfileCollection().document(user.uid).delete().await()
            user.delete().await()

            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    // ====================================================================
    // AUTH STANDARD
    // ====================================================================

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