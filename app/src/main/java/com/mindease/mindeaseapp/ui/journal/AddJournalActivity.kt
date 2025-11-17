package com.mindease.mindeaseapp.ui.journal

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository
import com.mindease.mindeaseapp.databinding.ActivityAddJournalBinding
import com.mindease.mindeaseapp.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date


class AddJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddJournalBinding
    private lateinit var viewModel: JournalViewModel
    private lateinit var cloudRepository: JournalCloudRepository

    private var currentJournalDocumentId: String? = null
    private var selectedMoodScore: Int = 0
    private var selectedMoodName: String = ""
    private var selectedImageUri: Uri? = null
    private var existingImagePath: String? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    companion object {
        const val EXTRA_JOURNAL_ID = "extra_journal_document_id_to_edit"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityAddJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        currentJournalDocumentId = intent.getStringExtra(EXTRA_JOURNAL_ID)

        if (currentJournalDocumentId != null) {
            loadExistingJournal(currentJournalDocumentId!!)
            binding.toolbar.title = "Edit Journal"
        } else {
            binding.toolbar.title = "Add Journal"
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
            } else {
                selectedImageUri = null
                if (existingImagePath == null) {
                    binding.ivImagePreview.visibility = View.GONE
                }
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
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val auth = FirebaseAuth.getInstance()

        val repo = JournalCloudRepository(firestore, storage, auth)
        cloudRepository = repo

        val factory = JournalViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(JournalViewModel::class.java)
    }

    private fun loadExistingJournal(documentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val journal = cloudRepository.getJournalById(documentId)
            withContext(Dispatchers.Main) {
                if (journal != null) {
                    onMoodSelected(journal.moodScore, getMoodImageView(journal.moodScore))
                    binding.etJournalContent.setText(journal.content)

                    if (journal.imageBase64 != null && journal.imageBase64.isNotEmpty()) {
                        existingImagePath = journal.imageBase64
                        binding.ivImagePreview.visibility = View.VISIBLE

                        val bitmap = base64ToBitmap(journal.imageBase64)
                        Glide.with(this@AddJournalActivity)
                            .load(bitmap)
                            .into(binding.ivImagePreview)
                    } else {
                        existingImagePath = null
                        binding.ivImagePreview.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(
                        this@AddJournalActivity,
                        "Gagal memuat jurnal.",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupMoodListeners() {
        val moodIcons = listOf(
            binding.ivMoodHappyExtreme,
            binding.ivMoodHappy,
            binding.ivMoodNeutral,
            binding.ivMoodSad,
            binding.ivMoodSadExtreme
        )

        moodIcons.forEachIndexed { index, imageView ->
            val score = 5 - index // 5, 4, 3, 2, 1
            imageView.tag = score.toString()
            imageView.setOnClickListener { view ->
                val moodScore = view.tag.toString().toIntOrNull() ?: return@setOnClickListener
                onMoodSelected(moodScore, view as ImageView)
            }
        }
        resetMoodSelection()
    }

    private fun onMoodSelected(score: Int, selectedView: ImageView?) {
        resetMoodSelection()

        if (selectedView != null) {
            selectedView.alpha = 1.0f
            val moodColor = getMoodColor(score)
            ImageViewCompat.setImageTintList(
                selectedView,
                ContextCompat.getColorStateList(this, moodColor)
            )
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
            ImageViewCompat.setImageTintList(
                imageView,
                ContextCompat.getColorStateList(this, moodColor)
            )
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

        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(
                this,
                "Anda harus login untuk menyimpan jurnal.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val action = if (currentJournalDocumentId != null) "Memperbarui" else "Menyimpan"
        Toast.makeText(this, "$action Jurnal...", Toast.LENGTH_LONG).show()
        saveOrUpdateJournalEntry(content)
    }

    private fun saveOrUpdateJournalEntry(content: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val baseJournal = JournalEntry(
            documentId = currentJournalDocumentId,
            userId = currentUser.uid,
            moodScore = selectedMoodScore,
            moodName = selectedMoodName,
            content = content,
            imageBase64 = existingImagePath,
            timestamp = Date().time
        )

        if (selectedImageUri != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val base64String = processImageToBase64(selectedImageUri!!)
                withContext(Dispatchers.Main) {
                    viewModel.saveJournalEntry(baseJournal.copy(imageBase64 = base64String), base64String)
                    showSuccessToast(base64String != null)
                }
            }
        } else {
            viewModel.saveJournalEntry(baseJournal, existingImagePath)
            showSuccessToast(existingImagePath != null)
        }
    }

    /**
     * COMPRESS & CONVERT KE BASE64 - MANUAL (TANPA LIBRARY COMPRESSOR)
     * Lebih reliable karena tidak depend on external library
     */
    private suspend fun processImageToBase64(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Load bitmap dari URI
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AddJournalActivity,
                            "Gagal membaca gambar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@withContext null
                }

                // 2. Resize gambar (compress resolusi)
                val maxWidth = 800
                val maxHeight = 600

                val width = originalBitmap.width
                val height = originalBitmap.height

                val scale = minOf(
                    maxWidth.toFloat() / width,
                    maxHeight.toFloat() / height
                )

                val scaledWidth = (width * scale).toInt()
                val scaledHeight = (height * scale).toInt()

                val resizedBitmap = Bitmap.createScaledBitmap(
                    originalBitmap,
                    scaledWidth,
                    scaledHeight,
                    true
                )

                originalBitmap.recycle()

                // 3. Compress quality & convert ke byte array
                val byteArrayOutputStream = ByteArrayOutputStream()
                var quality = 70
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)

                // Jika masih > 150KB, turunkan quality
                while (byteArrayOutputStream.size() > 150_000 && quality > 20) {
                    byteArrayOutputStream.reset()
                    quality -= 10
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                }

                resizedBitmap.recycle()

                val byteArray = byteArrayOutputStream.toByteArray()
                byteArrayOutputStream.close()

                // 4. Convert ke Base64
                val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // Log ukuran untuk debugging
                val sizeKB = byteArray.size / 1024
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AddJournalActivity,
                        "Gambar di-compress: ${sizeKB}KB",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                base64String

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AddJournalActivity,
                        "Gagal memproses gambar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                null
            }
        }
    }

    private fun showSuccessToast(hasImage: Boolean) {
        val actionMessage = if (currentJournalDocumentId != null) "diperbarui" else "disimpan"
        val imageInfo = if (hasImage) "dengan Gambar (Base64)" else ""
        Toast.makeText(
            this,
            "Jurnal berhasil $actionMessage! $imageInfo",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}