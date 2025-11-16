package com.mindease.mindeaseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude // 🔥 Tambah import ini

/**
 * Merepresentasikan satu entri jurnal detail (teks dan gambar Base64).
 */
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @DocumentId
    val documentId: String? = null,

    val userId: String = "",
    val moodName: String,
    val moodScore: Int,
    val content: String,

    // 🔥 FIX: Path URL diganti menjadi BASE64 STRING
    val imageBase64: String? = null,

    val timestamp: Long = System.currentTimeMillis()
) {
    // Digunakan oleh Firestore untuk membuat objek kosong
    constructor() : this(0, null, "", "", 0, "", null, 0L)
}