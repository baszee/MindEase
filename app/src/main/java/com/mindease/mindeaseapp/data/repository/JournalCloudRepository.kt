package com.mindease.mindeaseapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.mindease.mindeaseapp.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository yang bertanggung jawab untuk menangani operasi data Jurnal di Cloud (Firestore & Storage).
 */
class JournalCloudRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    val journalCollection = firestore.collection("journals")
    private val storageReference = storage.reference.child("journal_images")

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User is not logged in or UID is null.")
    }

    /**
     * Mendapatkan semua jurnal pengguna saat ini secara real-time (Flow dari Firestore).
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

                // FIX: Menambahkan documentId saat mapping (penting untuk edit/delete)
                val journals = snapshot?.documents?.map { document ->
                    document.toObject(JournalEntry::class.java)?.copy(documentId = document.id)
                }?.filterNotNull() ?: emptyList()

                trySend(journals)
            }

        // Cleanup listener saat Flow dibatalkan
        awaitClose { listenerRegistration.remove() }
    }.catch { e ->
        e.printStackTrace()
        emit(emptyList())
    }

    /**
     * Menyimpan atau memperbarui entri jurnal (dengan upload gambar jika ada).
     */
    suspend fun saveJournal(entry: JournalEntry, imageUri: Uri?): JournalEntry {
        val userId = getCurrentUserId()
        var updatedEntry = entry.copy(userId = userId) // Pastikan userId selalu di-set

        // 1. Proses Upload/Pembaruan Gambar
        if (imageUri != null) {
            val downloadUrl = uploadImage(userId, imageUri)
            updatedEntry = updatedEntry.copy(imagePath = downloadUrl)
            // TODO: Hapus gambar lama (untuk versi mendatang)
        } else if (entry.documentId.isNullOrEmpty() && entry.imagePath.isNullOrEmpty()) {
            // Jurnal baru tanpa gambar
        } else {
            // Pertahankan imagePath yang sudah ada
        }


        // 2. Simpan atau perbarui data ke Firestore
        return if (entry.documentId.isNullOrEmpty()) {
            // FIX UTAMA: Gunakan .document() dan .set() untuk memastikan serialization field userId benar
            val newDocRef = journalCollection.document()
            updatedEntry = updatedEntry.copy(documentId = newDocRef.id)

            newDocRef.set(updatedEntry).await() // Gunakan SET

            updatedEntry // Kembalikan entry lengkap
        } else {
            // Update yang sudah ada
            journalCollection.document(entry.documentId!!).set(updatedEntry).await()
            updatedEntry
        }
    }

    /**
     * Mengunggah gambar ke Firebase Storage dan mengembalikan URL unduhan.
     */
    private suspend fun uploadImage(userId: String, imageUri: Uri): String {
        // Gunakan timestamp untuk memastikan nama file unik
        val imageFileName = "${userId}_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val imageRef = storageReference.child(imageFileName)

        val uploadTask = imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    /**
     * Mendapatkan jurnal berdasarkan documentId.
     */
    suspend fun getJournalById(documentId: String): JournalEntry? {
        val snapshot = journalCollection.document(documentId).get().await()
        return snapshot.toObject(JournalEntry::class.java)?.copy(documentId = snapshot.id)
    }

    /**
     * Menghapus jurnal dan gambar terkait dari cloud.
     */
    suspend fun deleteJournal(entry: JournalEntry) {
        // 1. Hapus gambar dari Storage jika ada
        entry.imagePath?.let { url ->
            if (url.startsWith("https://firebasestorage")) {
                try {
                    storage.getReferenceFromUrl(url).delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Hapus dokumen dari Firestore
        entry.documentId?.let { id ->
            journalCollection.document(id).delete().await()
        }
    }
}