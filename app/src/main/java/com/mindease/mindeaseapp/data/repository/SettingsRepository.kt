package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mindease.mindeaseapp.data.model.UserSettings
import kotlinx.coroutines.tasks.await

/**
 * Repository untuk menangani penyimpanan dan pengambilan User Settings (preferensi).
 */
class SettingsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Menunjuk ke dokumen spesifik untuk pengaturan pernapasan
    private fun getUserSettingsDocument(userId: String) = firestore.collection("users")
        .document(userId)
        .collection("settings") // Subkoleksi untuk pengaturan
        .document("breathing_prefs") // Nama dokumen spesifik

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User is not logged in or UID is null.")
    }

    /**
     * Menyimpan UserSettings ke Firestore.
     */
    suspend fun saveSettings(settings: UserSettings) {
        val userId = getCurrentUserId()
        val settingsToSave = settings.copy(documentId = "breathing_prefs") // Pastikan ID dokumen
        getUserSettingsDocument(userId).set(settingsToSave).await()
    }

    /**
     * Mengambil UserSettings dari Firestore.
     */
    suspend fun getSettings(): UserSettings {
        return try {
            val userId = getCurrentUserId()
            val snapshot = getUserSettingsDocument(userId).get().await()

            // Jika ada data, kembalikan objek UserSettings. Jika tidak ada, kembalikan default.
            snapshot.toObject(UserSettings::class.java) ?: UserSettings()
        } catch (e: IllegalStateException) {
            // Dilempar jika user null, biarkan ditangani di Activity
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            UserSettings()
        }
    }
}