package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mindease.mindeaseapp.data.model.JournalEntry // <-- FIX: Import ini yang hilang
import com.mindease.mindeaseapp.data.repository.JournalRepository
import kotlinx.coroutines.launch

/**
 * ViewModel untuk JournalFragment.
 */
class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

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