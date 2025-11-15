package com.mindease.mindeaseapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mindease.mindeaseapp.data.model.Quote
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Repository untuk mengambil Quotes dari Firestore.
 */
class QuoteRepository(private val firestore: FirebaseFirestore) {

    private val quoteCollection = firestore.collection("quotes")

    /**
     * Mengambil daftar semua kutipan dan mengembalikan satu kutipan acak.
     */
    suspend fun getRandomQuote(): Quote {
        return try {
            val snapshot = quoteCollection.get().await()
            val quotes = snapshot.toObjects(Quote::class.java)

            if (quotes.isNotEmpty()) {
                // Pilih kutipan secara acak dari daftar
                quotes[Random.nextInt(quotes.size)]
            } else {
                // Return default quote jika koleksi kosong
                Quote(text = "Tarik napas dalam-dalam dan tenangkan pikiranmu", author = "MindEase")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return default quote jika terjadi error (e.g., koneksi/Firestore error)
            Quote(text = "Gagal memuat kutipan. Tetap semangat!", author = "Error")
        }
    }
}