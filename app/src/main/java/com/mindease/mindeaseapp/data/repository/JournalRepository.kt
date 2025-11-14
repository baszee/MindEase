package com.mindease.mindeaseapp.data.repository

import com.mindease.mindeaseapp.data.model.JournalDao
import com.mindease.mindeaseapp.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository yang bertanggung jawab untuk menangani operasi data Jurnal.
 * Bertindak sebagai perantara antara ViewModel dan JournalDao.
 */
class JournalRepository(private val journalDao: JournalDao) {

    // Mendapatkan semua jurnal yang dicatat sebagai Flow (untuk pembaruan real-time)
    val allJournals: Flow<List<JournalEntry>> = journalDao.getAllJournals()

    /**
     * Menyimpan entri jurnal baru di database.
     */
    suspend fun insert(journal: JournalEntry) {
        journalDao.insertJournal(journal)
    }

    /**
     * Mendapatkan jurnal berdasarkan ID.
     */
    suspend fun getJournalById(id: Int): JournalEntry? {
        return journalDao.getJournalById(id)
    }
}