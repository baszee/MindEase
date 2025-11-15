package com.mindease.mindeaseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId // Tambahkan import ini

/**
 * Merepresentasikan satu catatan mood harian di database (Tabel Room/Firestore).
 */
@Entity(tableName = "mood_entries")
data class MoodEntry(
    // ID lokal (hanya digunakan Room, bisa diabaikan di Firestore)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID Dokumen Firebase (BARU untuk Cloud)
    @DocumentId
    val documentId: String? = null,

    // ID Pengguna Firebase (BARU untuk Cloud)
    val userId: String = "",

    // Status mood: 1 (Very Sad) hingga 5 (Very Happy)
    val score: Int,

    // Nama mood (untuk tampilan, e.g., "Happy", "Neutral")
    val moodName: String,

    // Timestamp saat mood dicatat (penting untuk riwayat)
    val timestamp: Long = System.currentTimeMillis()
) {
    // Digunakan oleh Firestore untuk membuat objek kosong
    constructor() : this(0, null, "", 0, "", 0L)
}