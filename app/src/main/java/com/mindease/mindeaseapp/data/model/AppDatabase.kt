package com.mindease.mindeaseapp.data.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MoodEntry::class, JournalEntry::class],
    version = 3,  // ← UBAH INI ke 3 (atau 4, 5, dst setiap ada perubahan)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindease_database"
                )
                    .fallbackToDestructiveMigration()  // ← INI PENTING, sudah ada kan?
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}