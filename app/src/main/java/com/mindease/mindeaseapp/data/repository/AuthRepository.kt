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
import com.google.firebase.firestore.FirebaseFirestore // 🔥 Import Firestore

/**
 * Repository untuk menangani semua operasi Otentikasi (Login, Register, Logout, Guest) dan Profile Data di Firestore.
 */
class AuthRepository(
    val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // 🔥 Tambahkan Firestore
) {

    private val TAG = "AuthRepo"

    // Koleksi baru untuk menyimpan data profile detail pengguna
    private fun getUserProfileCollection() = firestore.collection("user_profiles")

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ====================================================================
    // FIREBASE AUTH (Nama di Auth)
    // ====================================================================

    /**
     * Mengambil nama pengguna saat ini (untuk digunakan setelah reload).
     */
    fun getCurrentUserName(): String? {
        // Ambil nama dari objek currentUser yang sudah di-reload
        return auth.currentUser?.displayName
    }

    /**
     * Memaksa reload data pengguna dari server (PENTING untuk sinkronisasi nama).
     */
    suspend fun reloadCurrentUser() {
        try {
            auth.currentUser?.reload()?.await()
            Log.d(TAG, "FirebaseUser successfully reloaded from server.")
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading user: ${e.message}")
        }
    }

    // ====================================================================
    // FIRESTORE USER PROFILE (Bio, Image, Nama Sinkronisasi) 🔥 BARU
    // ====================================================================

    /**
     * Mendapatkan profil pengguna dari Firestore.
     */
    suspend fun getUserProfile(): UserProfile = retryWithExponentialBackoff(tag = "$TAG-GetProfile") {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")
        val snapshot = getUserProfileCollection().document(userId).get().await()

        return@retryWithExponentialBackoff snapshot.toObject(UserProfile::class.java)
            ?.copy(documentId = snapshot.id)
            ?: UserProfile(userId = userId) // Kembalikan default jika dokumen tidak ada
    }

    /**
     * Melakukan update Name, Bio, dan Image URL di Firebase Auth dan Firestore.
     * Catatan: Image URL harus berupa String, proses upload harus dilakukan di ViewModel/Activity.
     */
    fun updateUserProfile(name: String, bio: String, imageUrl: String? = null): Flow<AuthResult<FirebaseUser>> = flow {
        emit(AuthResult.Loading)
        try {
            val user = auth.currentUser
            val userId = user?.uid

            if (user != null && userId != null) {
                // 1. Update Firebase Auth (hanya DisplayName)
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    // URL foto harus berupa Uri. Parse string URL jika ada.
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

                // Simpan/Timpa ke Firestore
                getUserProfileCollection().document(userId)
                    .set(userProfileData)
                    .await()

                // 3. Reload user untuk memastikan sinkronisasi ke objek lokal
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
    // AUTH LAMA (Dipertahankan)
    // ====================================================================

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
    suspend fun deleteUserAccount(): AuthResult<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return AuthResult.Error(Exception("Sesi pengguna berakhir. Tidak dapat menghapus akun."))
            }

            // 🔥 BARU: Hapus dokumen profile custom dari Firestore
            getUserProfileCollection().document(user.uid).delete().await()

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