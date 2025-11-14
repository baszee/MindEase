package com.mindease.mindeaseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Merepresentasikan satu entri jurnal detail (teks dan gambar).
 */
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Status mood saat jurnal dicatat (e.g., "Happy", "Neutral")
    val moodName: String,

    // Skor mood (1-5)
    val moodScore: Int,

    // Isi teks jurnal
    val content: String,

    // Path/URL gambar (Opsional)
    val imagePath: String? = null,

    // Waktu pencatatan
    val timestamp: Long = System.currentTimeMillis()
)