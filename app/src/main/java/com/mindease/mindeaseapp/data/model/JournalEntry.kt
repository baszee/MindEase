package com.mindease.mindeaseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId // Tambahkan import ini

/**
 * Merepresentasikan satu entri jurnal detail (teks dan gambar).
 * Catatan: Tetap menggunakan Room untuk struktur lokal, tapi kita akan
 * memetakan ke Firestore dengan DocumentId.
 */
@Entity(tableName = "journal_entries")
data class JournalEntry(
    // ID lokal (hanya digunakan Room, bisa diabaikan di Firestore)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID Dokumen Firebase (BARU)
    @DocumentId
    val documentId: String? = null,

    // ID Pengguna Firebase (BARU)
    val userId: String = "",

    // Status mood saat jurnal dicatat (e.g., "Happy", "Neutral")
    val moodName: String,

    // Skor mood (1-5)
    val moodScore: Int,

    // Isi teks jurnal
    val content: String,

    // Path/URL gambar (SEKARANG BERISI URL CLOUD JIKA ADA)
    val imagePath: String? = null,

    // Waktu pencatatan
    val timestamp: Long = System.currentTimeMillis()
) {
    // Digunakan oleh Firestore untuk membuat objek kosong
    constructor() : this(0, null, "", "", 0, "", null, 0L)
}