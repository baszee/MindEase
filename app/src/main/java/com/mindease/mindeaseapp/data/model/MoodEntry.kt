package com.mindease.mindeaseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Merepresentasikan satu catatan mood harian di database (Tabel Room).
 */
@Entity(tableName = "mood_entries")
data class MoodEntry(
    // Kunci utama (primary key) harus unik untuk setiap entri
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Status mood: 1 (Very Sad) hingga 5 (Very Happy)
    val score: Int,

    // Nama mood (untuk tampilan, e.g., "Happy", "Neutral")
    val moodName: String,

    // Timestamp saat mood dicatat (penting untuk riwayat)
    val timestamp: Long = System.currentTimeMillis()
)