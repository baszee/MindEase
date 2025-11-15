package com.mindease.mindeaseapp.data.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // Import Update
import kotlinx.coroutines.flow.Flow

/**
 * Antarmuka untuk mengakses dan memanipulasi data JournalEntry.
 */
@Dao
interface JournalDao {

    /** Menyimpan JournalEntry baru. Jika sudah ada, ganti. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntry)

    /** Memperbarui JournalEntry yang sudah ada. */
    @Update // BARU: Tambahkan fungsi update
    suspend fun updateJournal(journal: JournalEntry)

    /** Menghapus JournalEntry. */
    @Delete // BARU: Tambahkan fungsi delete
    suspend fun deleteJournal(journal: JournalEntry)

    /** Mengambil semua riwayat jurnal yang diurutkan berdasarkan waktu terbaru. */
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllJournals(): Flow<List<JournalEntry>>

    /** Mengambil jurnal spesifik berdasarkan ID. */
    @Query("SELECT * FROM journal_entries WHERE id = :journalId")
    suspend fun getJournalById(journalId: Int): JournalEntry?

    // TODO: Hapus TODO sebelumnya karena sudah diimplementasikan di atas
}