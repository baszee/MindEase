package com.mindease.mindeaseapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.databinding.FragmentDashboardBinding
import com.mindease.mindeaseapp.ui.journal.MoodHistoryActivity
import java.util.Calendar
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.QuoteRepository
import com.mindease.mindeaseapp.utils.AnalyticsHelper
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.utils.LocalizationHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private var selectedMoodScore: Int? = null
    private var selectedMoodName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi Database dan ViewModel
        val firestore = FirebaseFirestore.getInstance()
        val auth = Firebase.auth

        val moodRepository = MoodCloudRepository(firestore, auth)
        val quoteRepository = QuoteRepository(firestore)
        val authRepository = AuthRepository(auth, firestore)

        val factory = DashboardViewModelFactory(moodRepository, quoteRepository, authRepository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        // 2. Siapkan Listener dan Observers
        setupMoodListeners()
        setupObservers()
        setupGreeting()

        // Listener untuk Mood History
        binding.tvMoodHistoryLink.setOnClickListener {
            val intent = Intent(requireContext(), MoodHistoryActivity::class.java)
            startActivity(intent)
            AnalyticsHelper.logScreenView("mood_history_activity", "MoodHistoryActivity")
        }

        // Memuat data
        viewModel.loadMoodForToday()
        viewModel.loadRandomQuote()

        AnalyticsHelper.logScreenView("dashboard_fragment", "DashboardFragment")
    }

    override fun onResume() {
        super.onResume()
        setupGreeting()
        viewModel.loadMoodForToday()
        viewModel.loadRandomQuote()
    }

    // ==========================================================
    // GREETING DINAMIS
    // ==========================================================

    /**
     * Menentukan waktu hari dan mengembalikan Resource ID untuk Sapaan.
     */
    private fun getTimeOfDay(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> R.string.good_morning
            in 11..14 -> R.string.good_afternoon
            in 15..17 -> R.string.good_evening
            else -> R.string.good_night
        }
    }

    /**
     * Menampilkan sapaan dinamis berdasarkan waktu dan nama pengguna.
     */
    private fun setupGreeting() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userName = viewModel.getUpdatedUserName()
            val timeOfDayResId = getTimeOfDay()
            val greeting = getString(timeOfDayResId)

            val emoji = when (timeOfDayResId) {
                R.string.good_morning -> "🌤️"
                R.string.good_afternoon -> "☀️"
                R.string.good_evening -> "🌙"
                else -> "🌌"
            }

            binding.tvGreeting.text = getString(
                R.string.greeting_format,
                greeting,
                userName,
                emoji
            )
        }
    }

    /**
     * Update prompt dinamis berdasarkan mood dan waktu.
     * Menggunakan string resources yang sudah ada dengan format %1$s.
     */
    private fun updateMoodPrompt(moodEntry: MoodEntry?) {
        val timeOfDayIndex = when (getTimeOfDay()) {
            R.string.good_morning -> 0
            R.string.good_afternoon -> 1
            R.string.good_evening -> 2
            else -> 3
        }

        if (moodEntry != null && moodEntry.score > 0) {
            // MOOD SUDAH DICATAT - Tampilkan pesan dengan nama mood
            val localizedMoodName = LocalizationHelper.getLocalizedMoodName(
                requireContext(),
                moodEntry.moodName
            )

            val messageResId = when(timeOfDayIndex) {
                0 -> R.string.dashboard_mood_logged_morning
                1 -> R.string.dashboard_mood_logged_afternoon
                2 -> R.string.dashboard_mood_logged_evening
                else -> R.string.dashboard_mood_logged_night
            }

            // Format string dengan nama mood
            binding.tvMoodPrompt.text = getString(messageResId, localizedMoodName)
        } else {
            // MOOD BELUM DICATAT - Tampilkan prompt dinamis
            val messageResId = when(timeOfDayIndex) {
                0 -> R.string.dashboard_mood_prompt_morning
                1 -> R.string.dashboard_mood_prompt_afternoon
                2 -> R.string.dashboard_mood_prompt_evening
                else -> R.string.dashboard_mood_prompt_night
            }
            binding.tvMoodPrompt.text = getString(messageResId)
        }
    }

    // ==========================================================
    // MOOD SELECTION
    // ==========================================================

    private fun setupMoodListeners() {
        val moodViews = mapOf(
            binding.ivMoodHappyExtreme to 5,
            binding.ivMoodHappy to 4,
            binding.ivMoodNeutral to 3,
            binding.ivMoodSad to 2,
            binding.ivMoodSadExtreme to 1
        )

        moodViews.forEach { entry ->
            entry.key.setOnClickListener {
                onMoodSelected(entry.value, getMoodName(entry.value))
            }
        }
    }

    private fun onMoodSelected(score: Int, moodName: String) {
        selectedMoodScore = score
        selectedMoodName = moodName

        val newMoodEntry = MoodEntry(
            score = score,
            moodName = moodName,
            timestamp = System.currentTimeMillis()
        )
        viewModel.saveMood(newMoodEntry)

        AnalyticsHelper.logMoodTracked(moodName, score)

        val localizedMoodName = LocalizationHelper.getLocalizedMoodName(requireContext(), moodName)
        Toast.makeText(
            requireContext(),
            getString(R.string.mood_logged_toast, localizedMoodName),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun resetMoodSelection() {
        val moodViewScores = mapOf(
            binding.ivMoodHappyExtreme to 5,
            binding.ivMoodHappy to 4,
            binding.ivMoodNeutral to 3,
            binding.ivMoodSad to 2,
            binding.ivMoodSadExtreme to 1
        )

        moodViewScores.forEach { entry ->
            entry.key.alpha = 0.5f
            val moodColor = getMoodColor(entry.value)
            ImageViewCompat.setImageTintList(
                entry.key,
                ContextCompat.getColorStateList(requireContext(), moodColor)
            )
        }
    }

    private fun getMoodName(score: Int): String {
        return when (score) {
            5 -> "Very Happy"
            4 -> "Happy"
            3 -> "Neutral"
            2 -> "Sad"
            else -> "Very Sad"
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

    // ==========================================================
    // OBSERVERS
    // ==========================================================

    private fun setupObservers() {
        // Observer untuk Mood Harian
        viewModel.currentDayMood.observe(viewLifecycleOwner) { mood ->
            // Update prompt dinamis
            updateMoodPrompt(mood)

            // Reset mood selection visual
            resetMoodSelection()

            if (mood != null) {
                val selectedViewId = when (mood.score) {
                    5 -> R.id.iv_mood_happy_extreme
                    4 -> R.id.iv_mood_happy
                    3 -> R.id.iv_mood_neutral
                    2 -> R.id.iv_mood_sad
                    else -> R.id.iv_mood_sad_extreme
                }

                val selectedView = view?.findViewById<ImageView>(selectedViewId)
                if (selectedView != null) {
                    selectedView.alpha = 1.0f
                    val moodColor = getMoodColor(mood.score)
                    ImageViewCompat.setImageTintList(
                        selectedView,
                        ContextCompat.getColorStateList(requireContext(), moodColor)
                    )
                }

                selectedMoodName = mood.moodName
            }
        }

        // Observer untuk Quote dengan fallback handling
        viewModel.currentQuote.observe(viewLifecycleOwner) { quote ->
            if (quote != null) {
                val quoteText = try {
                    // Jika fallback (ID string resource), ambil resource string
                    if (quote.author == "MindEase" || quote.author == "Error") {
                        getString(quote.text.toInt())
                    } else {
                        quote.text
                    }
                } catch (e: Exception) {
                    quote.text
                }

                binding.tvQuoteText.text = "\"$quoteText\""
                binding.tvQuoteAuthor.text = "— ${quote.author}"
            } else {
                // Fallback manual jika quote null
                binding.tvQuoteText.text = getString(R.string.placeholder_quote)
                binding.tvQuoteAuthor.text = "— MindEase"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}