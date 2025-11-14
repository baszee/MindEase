package com.mindease.mindeaseapp.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Antarmuka untuk mengakses dan memanipulasi data JournalEntry.
 */
@Dao
interface JournalDao {

    /** Menyimpan JournalEntry baru. Jika sudah ada, ganti. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntry)

    /** Mengambil semua riwayat jurnal yang diurutkan berdasarkan waktu terbaru. */
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllJournals(): Flow<List<JournalEntry>>

    /** Mengambil jurnal spesifik berdasarkan ID. */
    @Query("SELECT * FROM journal_entries WHERE id = :journalId")
    suspend fun getJournalById(journalId: Int): JournalEntry?

    // TODO: Tambahkan fungsi delete jika diperlukan di masa depan
}