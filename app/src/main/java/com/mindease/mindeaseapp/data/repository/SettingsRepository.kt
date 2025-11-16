package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mindease.mindeaseapp.data.model.UserSettings
import com.mindease.mindeaseapp.utils.retryWithExponentialBackoff
import kotlinx.coroutines.tasks.await
import android.util.Log // 🔥 FIX: IMPORT LOG

/**
 * Repository untuk menangani penyimpanan dan pengambilan User Settings (preferensi).
 */
class SettingsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "SettingsRepository" // 🔥 FIX: DEFINISI TAG

    // Menunjuk ke dokumen spesifik untuk pengaturan pernapasan
    private fun getUserSettingsDocument(userId: String) = firestore.collection("users")
        .document(userId)
        .collection("settings")
        .document("breathing_prefs")

    private fun getCurrentUserId(): String {
        // Logika ini harus dipertahankan untuk melempar IllegalStateException jika user null
        return auth.currentUser?.uid ?: throw IllegalStateException("User is not logged in or UID is null.")
    }

    /**
     * Menyimpan UserSettings ke Firestore menggunakan set(merge) untuk menjamin persistensi.
     */
    suspend fun saveSettings(settings: UserSettings) = retryWithExponentialBackoff(tag = TAG) {
        val userId = getCurrentUserId()

        val dataToUpdate = mapOf(
            "isSoundEnabled" to settings.isSoundEnabled,
            "isHapticEnabled" to settings.isHapticEnabled
        )

        Log.d(TAG, "SAVING settings for $userId: Sound=${settings.isSoundEnabled}, Haptic=${settings.isHapticEnabled}")

        // Gunakan set(data, SetOptions.merge()) untuk menjamin persistensi
        getUserSettingsDocument(userId).set(dataToUpdate, SetOptions.merge()).await()

        Log.d(TAG, "SAVE SUCCESSFUL for $userId")
    }

    /**
     * Mengambil UserSettings dari Firestore.
     */
    suspend fun getSettings(): UserSettings = retryWithExponentialBackoff(tag = TAG) {
        return@retryWithExponentialBackoff try {
            val userId = getCurrentUserId()
            val snapshot = getUserSettingsDocument(userId).get().await()

            // Jika ada data, kembalikan objek UserSettings. Jika tidak ada, kembalikan default.
            val settings = snapshot.toObject(UserSettings::class.java) ?: UserSettings()

            Log.d(TAG, "LOADING settings for $userId: Sound=${settings.isSoundEnabled}, Haptic=${settings.isHapticEnabled}")
            settings
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            // Mengembalikan default jika ada error jaringan atau Firestore
            UserSettings()
        }
    }
}