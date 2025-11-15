package com.mindease.mindeaseapp.ui.journal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalRepository
import com.mindease.mindeaseapp.databinding.ActivityDetailJournalBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailJournalBinding
    private lateinit var repository: JournalRepository
    private lateinit var viewModel: JournalViewModel

    private var currentJournal: JournalEntry? = null

    companion object {
        const val EXTRA_JOURNAL_ID = "extra_journal_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 1. Inisialisasi Repository dan ViewModel
        val journalDao = AppDatabase.getDatabase(applicationContext).journalDao()
        repository = JournalRepository(journalDao)
        val factory = JournalViewModelFactory(repository)
        // FIX: Menggunakan get(JournalViewModel::class.java)
        viewModel = ViewModelProvider(this, factory).get(JournalViewModel::class.java)

        // 2. Ambil ID dari Intent
        val journalId = intent.getIntExtra(EXTRA_JOURNAL_ID, -1)

        if (journalId != -1) {
            loadJournalDetail(journalId)
        } else {
            Toast.makeText(this, "ID Jurnal tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Listener untuk tombol delete
        binding.btnDeleteJournal.setOnClickListener {
            currentJournal?.let { showDeleteConfirmationDialog(it) }
        }

        // Listener untuk tombol edit
        binding.btnEditJournal.setOnClickListener {
            currentJournal?.let { navigateToEdit(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        currentJournal?.id?.let { loadJournalDetail(it) }
    }

    private fun loadJournalDetail(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val journal = repository.getJournalById(id)
            withContext(Dispatchers.Main) {
                if (journal != null) {
                    currentJournal = journal
                    displayJournal(journal)
                } else {
                    Toast.makeText(this@DetailJournalActivity, "Entri jurnal tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    /**
     * Menampilkan data jurnal ke UI.
     */
    private fun displayJournal(journal: JournalEntry) {
        binding.apply {
            // Data Teks
            tvMoodNameDetail.text = journal.moodName
            tvDateDetail.text = formatDate(journal.timestamp)
            tvContentDetail.text = journal.content

            // Ikon Mood
            ivMoodIconDetail.setImageResource(getMoodIconResource(journal.moodScore))

            // Terapkan warna mood pada ikon detail
            val moodColor = getMoodColor(journal.moodScore)
            val colorStateList = ContextCompat.getColorStateList(this@DetailJournalActivity, moodColor)
            ImageViewCompat.setImageTintList(ivMoodIconDetail, colorStateList)

            // Memuat gambar penuh, mempertahankan rasio aspek
            if (journal.imagePath != null && journal.imagePath.isNotEmpty()) {
                ivJournalImageDetail.visibility = View.VISIBLE
                tvImageLabel.visibility = View.VISIBLE

                Glide.with(this@DetailJournalActivity)
                    .load(Uri.parse(journal.imagePath))
                    .placeholder(R.drawable.ic_add_image)
                    .into(ivJournalImageDetail)
            } else {
                ivJournalImageDetail.visibility = View.GONE
                tvImageLabel.visibility = View.GONE
            }
        }
    }

    /**
     * Navigasi ke AddJournalActivity dalam mode Edit.
     */
    private fun navigateToEdit(journal: JournalEntry) {
        val intent = Intent(this, AddJournalActivity::class.java).apply {
            putExtra(AddJournalActivity.EXTRA_JOURNAL_ID, journal.id)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(journal: JournalEntry) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(getString(R.string.confirm_delete_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteJournalEntry(journal)
                Toast.makeText(this, getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }


    private fun formatDate(timestamp: Long): String {
        val locale = Locale.Builder().setLanguage("id").setRegion("ID").build()
        val sdf = SimpleDateFormat("dd MMMM yyyy", locale)
        return sdf.format(Date(timestamp))
    }

    private fun getMoodIconResource(score: Int): Int {
        return when (score) {
            5 -> R.drawable.ic_mood_happy_extreme
            4 -> R.drawable.ic_mood_happy
            3 -> R.drawable.ic_mood_neutral
            2 -> R.drawable.ic_mood_sad
            1 -> R.drawable.ic_mood_sad_extreme
            else -> R.drawable.ic_mood_neutral
        }
    }

    private fun getMoodColor(score: Int): Int {
        return when (score) {
            5 -> R.color.mood_very_happy
            4 -> R.color.mood_happy
            3 -> R.color.mood_neutral
            2 -> R.color.mood_sad
            else -> R.color.mood_very_sad
        }
    }
}