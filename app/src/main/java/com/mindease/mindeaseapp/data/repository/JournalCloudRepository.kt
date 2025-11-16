package com.mindease.mindeaseapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage // Pertahankan import dan dependency agar Activity tidak error saat init
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.utils.retryWithExponentialBackoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import android.util.Log // Import Log

/**
 * Repository yang bertanggung jawab untuk menangani operasi data Jurnal di Cloud (Firestore).
 * 🔥 FIX: Hanya menangani penyimpanan Base64 di Firestore (tanpa Storage Upload).
 */
class JournalCloudRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    private val TAG = "JournalRepo"

    val journalCollection = firestore.collection("journals")

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User is not logged in or UID is null.")
    }

    /**
     * Mendapatkan semua jurnal pengguna saat ini secara real-time.
     */
    fun getAllJournals(): Flow<List<JournalEntry>> = callbackFlow {
        val userId = getCurrentUserId()
        val listenerRegistration = journalCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val journals = snapshot?.documents?.map { document ->
                    document.toObject(JournalEntry::class.java)?.copy(documentId = document.id)
                }?.filterNotNull() ?: emptyList()

                trySend(journals)
            }

        awaitClose { listenerRegistration.remove() }
    }.catch { e ->
        e.printStackTrace()
        emit(emptyList())
    }

    /**
     * Menyimpan atau memperbarui entri jurnal dengan Base64.
     * 🔥 FIX: Hanya menyimpan ke Firestore.
     */
    suspend fun saveJournal(entry: JournalEntry): JournalEntry = retryWithExponentialBackoff(tag = "SaveJournal") {
        val userId = getCurrentUserId()
        var updatedEntry = entry.copy(userId = userId)

        // 1. Simpan atau perbarui data ke Firestore
        return@retryWithExponentialBackoff if (entry.documentId.isNullOrEmpty()) {
            // New Entry
            val newDocRef = journalCollection.document()
            updatedEntry = updatedEntry.copy(documentId = newDocRef.id)

            newDocRef.set(updatedEntry).await()

            updatedEntry
        } else {
            // Update Existing
            journalCollection.document(entry.documentId!!).set(updatedEntry).await()
            updatedEntry
        }
    }

    /**
     * Menghapus jurnal.
     * 🔥 FIX: Hanya menghapus dokumen Firestore.
     */
    suspend fun deleteJournal(entry: JournalEntry) = retryWithExponentialBackoff(tag = "DeleteJournal") {
        // Cukup hapus dokumen dari Firestore.
        entry.documentId?.let { id ->
            journalCollection.document(id).delete().await()
        }
    }

    /**
     * Mendapatkan jurnal berdasarkan documentId.
     */
    suspend fun getJournalById(documentId: String): JournalEntry? = retryWithExponentialBackoff(tag = "GetJournal") {
        val snapshot = journalCollection.document(documentId).get().await()
        // FIX: Menggunakan JournalEntry yang baru
        return@retryWithExponentialBackoff snapshot.toObject(JournalEntry::class.java)?.copy(documentId = snapshot.id)
    }
}