package com.mindease.mindeaseapp.data.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Kelas abstrak Room Database utama untuk aplikasi MindEase.
 * Versi diatur ke 2 karena penambahan tabel JournalEntry.
 */
@Database(
    entities = [MoodEntry::class, JournalEntry::class], // <-- TAMBAHAN: JournalEntry
    version = 2, // <-- PERUBAHAN: Versi dinaikkan
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Menyediakan akses ke DAO
    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao // <-- TAMBAHAN: Journal DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Mengembalikan instance AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Menggunakan synchronized untuk memastikan hanya satu thread yang membuat instance
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindease_database"
                )
                    // PENTING: Menambahkan fallbackToDestructiveMigration karena perubahan versi
                    // Ini akan menghapus data lama saat skema berubah (cocok untuk pengembangan awal)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}