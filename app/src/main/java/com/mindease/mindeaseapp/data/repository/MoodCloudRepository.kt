package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mindease.mindeaseapp.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import android.util.Log

/**
 * Repository untuk menangani operasi data Mood di Cloud (Firestore).
 */
class MoodCloudRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "MoodCloudRepo"

    // Koleksi mood berada di sub-koleksi user
    private fun getUserMoodsCollection() = firestore.collection("users")
        .document(getCurrentUserId())
        .collection("moods")

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User is not logged in or UID is null.")
    }

    /**
     * Menyimpan MoodEntry baru ke Firestore.
     */
    suspend fun saveMood(mood: MoodEntry): MoodEntry {
        val userId = getCurrentUserId()

        // 1. Tentukan Document ID: Gunakan yang sudah ada (untuk update) atau buat yang baru
        val documentId = mood.documentId ?: getUserMoodsCollection().document().id

        // 2. Buat objek baru dengan userId dan documentId yang benar
        val entryToSave = mood.copy(userId = userId, documentId = documentId)

        // 3. Simpan/Timpa ke Firestore menggunakan .set() dengan ID dokumen yang spesifik
        getUserMoodsCollection().document(documentId).set(entryToSave).await()

        return entryToSave
    }

    /**
     * Mendapatkan semua moods pengguna saat ini secara real-time.
     */
    fun getAllMoods(): Flow<List<MoodEntry>> = callbackFlow {
        val listenerRegistration = getUserMoodsCollection()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val moods = snapshot?.documents?.map { document ->
                    document.toObject(MoodEntry::class.java)?.copy(documentId = document.id)
                }?.filterNotNull() ?: emptyList()

                trySend(moods)
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Mendapatkan Mood hari ini (00:00:00 - 23:59:59).
     */
    suspend fun getMoodForToday(startOfDay: Long, endOfDay: Long): MoodEntry? {
        val snapshot = getUserMoodsCollection()
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .whereLessThan("timestamp", endOfDay)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(MoodEntry::class.java)
    }

    /**
     * 🔥 BARU: Menghapus semua dokumen mood historis milik pengguna.
     * Dipanggil saat penghapusan akun.
     */
    suspend fun deleteAllMoods(): Boolean {
        return try {
            val userId = getCurrentUserId()

            // ✅ FIX: Gunakan getUserMoodsCollection() yang sudah ada
            val collectionRef = getUserMoodsCollection()

            val snapshot = collectionRef.get().await()

            // Gunakan batch untuk efisiensi
            val batch = firestore.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }

            batch.commit().await()
            Log.d(TAG, "All moods deleted successfully for user $userId")
            true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "User not logged in: ${e.message}")
            true // Anggap berhasil jika pengguna tidak ada
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all moods: ${e.message}")
            false
        }
    }
}