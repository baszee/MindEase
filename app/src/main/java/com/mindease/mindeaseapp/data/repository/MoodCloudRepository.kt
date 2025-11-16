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

/**
 * Repository untuk menangani operasi data Mood di Cloud (Firestore).
 */
class MoodCloudRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
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
        // .set() dengan document() akan menimpa (overwrite) dokumen jika ID sudah ada.
        getUserMoodsCollection().document(documentId).set(entryToSave).await()

        return entryToSave
    }

    /**
     * Mendapatkan semua moods pengguna saat ini secara real-time.
     * 🔥 FIX CRASH: Memastikan awaitClose adalah instruksi terakhir di callbackFlow.
     */
    fun getAllMoods(): Flow<List<MoodEntry>> = callbackFlow {
        val listenerRegistration = getUserMoodsCollection()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Menutup flow jika ada error
                    return@addSnapshotListener
                }

                // FIX: Menambahkan documentId saat mapping (penting untuk edit/delete)
                val moods = snapshot?.documents?.map { document ->
                    document.toObject(MoodEntry::class.java)?.copy(documentId = document.id)
                }?.filterNotNull() ?: emptyList()

                trySend(moods)
            }

        // ⚠️ INI HARUS MENJADI INSTRUKSI TERAKHIR UNTUK MENCEGAH CRASH!
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
}