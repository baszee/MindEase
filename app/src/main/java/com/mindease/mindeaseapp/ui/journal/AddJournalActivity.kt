package com.mindease.mindeaseapp.ui.journal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalRepository
import com.mindease.mindeaseapp.databinding.ActivityAddJournalBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat

class AddJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddJournalBinding
    private lateinit var viewModel: JournalViewModel // FIX: Class reference sudah benar
    private lateinit var repository: JournalRepository

    private var currentJournalId: Int? = null
    private var selectedMoodScore: Int = 0
    private var selectedMoodName: String = ""
    private var selectedImageUri: Uri? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    companion object {
        const val EXTRA_JOURNAL_ID = "extra_journal_id_to_edit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        currentJournalId = intent.getIntExtra(EXTRA_JOURNAL_ID, -1).takeIf { it != -1 }

        if (currentJournalId != null) {
            loadExistingJournal(currentJournalId!!)
            binding.toolbar.title = "Edit Journal"
        } else {
            binding.toolbar.title = "Add Journal"
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedImageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
            } else {
                selectedImageUri = null
                binding.ivImagePreview.visibility = View.GONE
            }
        }


        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupMoodListeners()
        binding.btnSaveJournal.setOnClickListener {
            showSaveConfirmationDialog()
        }

        binding.btnAddPicture.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun setupViewModel() {
        val journalDao = AppDatabase.getDatabase(applicationContext).journalDao()
        val repo = JournalRepository(journalDao)
        repository = repo

        val factory = JournalViewModelFactory(repo)
        // FIX: Menggunakan get(JournalViewModel::class.java)
        viewModel = ViewModelProvider(this, factory).get(JournalViewModel::class.java)
    }

    private fun loadExistingJournal(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val journal = repository.getJournalById(id)
            withContext(Dispatchers.Main) {
                if (journal != null) {
                    onMoodSelected(journal.moodScore, getMoodImageView(journal.moodScore))
                    binding.etJournalContent.setText(journal.content)

                    if (journal.imagePath != null && journal.imagePath.isNotEmpty()) {
                        val uri = Uri.parse(journal.imagePath)
                        selectedImageUri = uri
                        binding.ivImagePreview.visibility = View.VISIBLE
                        Glide.with(this@AddJournalActivity).load(uri).into(binding.ivImagePreview)
                    }
                } else {
                    Toast.makeText(this@AddJournalActivity, "Gagal memuat jurnal.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun getMoodImageView(score: Int): ImageView? {
        return when (score) {
            5 -> binding.ivMoodHappyExtreme
            4 -> binding.ivMoodHappy
            3 -> binding.ivMoodNeutral
            2 -> binding.ivMoodSad
            1 -> binding.ivMoodSadExtreme
            else -> null
        }
    }

    private fun setupMoodListeners() {
        val moodIcons = listOf(
            binding.ivMoodHappyExtreme, binding.ivMoodHappy, binding.ivMoodNeutral,
            binding.ivMoodSad, binding.ivMoodSadExtreme
        )

        moodIcons.forEach { imageView ->
            imageView.setOnClickListener { view ->
                val score = view.tag.toString().toIntOrNull() ?: return@setOnClickListener
                onMoodSelected(score, view as ImageView)
            }
        }
        resetMoodSelection()
    }

    private fun onMoodSelected(score: Int, selectedView: ImageView?) {
        resetMoodSelection()

        if (selectedView != null) {
            selectedView.alpha = 1.0f
            val moodColor = getMoodColor(score)
            ImageViewCompat.setImageTintList(selectedView, ContextCompat.getColorStateList(this, moodColor))
        }

        selectedMoodScore = score
        selectedMoodName = when (score) {
            5 -> "Very Happy"
            4 -> "Happy"
            3 -> "Neutral"
            2 -> "Sad"
            1 -> "Very Sad"
            else -> ""
        }
        binding.tvMoodName.text = selectedMoodName
    }

    private fun resetMoodSelection() {
        val moodViewScores = mapOf(
            binding.ivMoodHappyExtreme to 5,
            binding.ivMoodHappy to 4,
            binding.ivMoodNeutral to 3,
            binding.ivMoodSad to 2,
            binding.ivMoodSadExtreme to 1
        )

        moodViewScores.forEach { (imageView, score) ->
            imageView.alpha = 0.5f
            val moodColor = getMoodColor(score)
            ImageViewCompat.setImageTintList(imageView, ContextCompat.getColorStateList(this, moodColor))
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

    private fun showSaveConfirmationDialog() {
        val content = binding.etJournalContent.text.toString()

        if (selectedMoodScore == 0) {
            Toast.makeText(this, "Mohon pilih Mood Anda hari ini.", Toast.LENGTH_SHORT).show()
            return
        }

        if (content.isBlank()) {
            Toast.makeText(this, "Tuliskan sedikit tentang hari Anda.", Toast.LENGTH_SHORT).show()
            return
        }

        val action = if (currentJournalId != null) "Memperbarui" else "Menyimpan"
        Toast.makeText(this, "$action Jurnal...", Toast.LENGTH_LONG).show()
        saveOrUpdateJournalEntry(content)
    }

    private fun saveOrUpdateJournalEntry(content: String) {
        val entry = JournalEntry(
            id = currentJournalId ?: 0,
            moodScore = selectedMoodScore,
            moodName = selectedMoodName,
            content = content,
            imagePath = selectedImageUri?.toString(),
            timestamp = Date().time
        )

        if (currentJournalId != null) {
            viewModel.updateJournalEntry(entry)
            Toast.makeText(this, "Jurnal berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertJournalEntry(entry)
            Toast.makeText(this, "Jurnal berhasil disimpan!", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}