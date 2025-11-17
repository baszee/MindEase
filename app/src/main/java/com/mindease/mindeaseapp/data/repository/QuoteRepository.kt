package com.mindease.mindeaseapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mindease.mindeaseapp.data.model.Quote
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import com.mindease.mindeaseapp.R // <-- Import R

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
                // ✅ PERBAIKAN FALLBACK: Menggunakan ID String Resource.
                // DashboardFragment akan menggunakan ID ini untuk mengambil string yang benar.
                Quote(text = R.string.placeholder_quote.toString(), author = "MindEase")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // ✅ PERBAIKAN FALLBACK: Menggunakan ID String Resource.
            Quote(text = R.string.placeholder_quote.toString(), author = "Error")
        }
    }
}