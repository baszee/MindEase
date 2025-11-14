package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope // <-- TAMBAHAN IMPORT
import com.mindease.mindeaseapp.data.model.JournalEntry // <-- TAMBAHAN IMPORT
import com.mindease.mindeaseapp.data.repository.JournalRepository
import kotlinx.coroutines.launch // <-- TAMBAHAN IMPORT

/**
 * ViewModel untuk JournalFragment.
 * Menyediakan akses ke daftar jurnal yang tersimpan.
 */
class JournalViewModel(private val repository: JournalRepository) : ViewModel() { // <-- Tambahkan 'private val'

    // Daftar semua jurnal, dikonversi dari Flow ke LiveData
    val allJournals = repository.allJournals.asLiveData()

    /**
     * Menyimpan entri jurnal baru ke database.
     */
    fun insertJournalEntry(journal: JournalEntry) {
        // Meluncurkan operasi database di background thread
        viewModelScope.launch {
            repository.insert(journal)
        }
    }

    // TODO: Tambahkan fungsi untuk menghapus jurnal
}