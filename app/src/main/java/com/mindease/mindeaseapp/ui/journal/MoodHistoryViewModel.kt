package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.mindease.mindeaseapp.data.repository.MoodRepository

/**
 * ViewModel untuk MoodHistoryActivity.
 */
class MoodHistoryViewModel(repository: MoodRepository) : ViewModel() {

    val allMoods = repository.allMoods.asLiveData()

    // TODO: Tambahkan fungsi filtering mood berdasarkan rentang waktu di sini.
}