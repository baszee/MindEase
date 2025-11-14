package com.mindease.mindeaseapp.ui.breathing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivityBreathingSessionBinding
import com.mindease.mindeaseapp.R

class BreathingSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingSessionBinding
    private lateinit var exerciseType: String

    companion object {
        const val EXTRA_EXERCISE_TYPE = "extra_exercise_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ambil jenis latihan dari Intent
        exerciseType = intent.getStringExtra(EXTRA_EXERCISE_TYPE) ?: "Box Breathing"

        // 2. Setup Toolbar
        binding.toolbar.title = exerciseType
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 3. Update UI berdasarkan jenis latihan (Sesuai Desain PDF)
        updateUIForExercise(exerciseType)

        // FIX: Implementasi Logika Start Session (Placeholder)
        binding.btnStartSession.setOnClickListener {
            Toast.makeText(this, "Sesi $exerciseType dimulai!", Toast.LENGTH_SHORT).show()
            // TODO: Di sini nanti ditambahkan logika CountDownTimer/Animation
        }
    }

    /**
     * Menyesuaikan tampilan berdasarkan jenis latihan yang dipilih.
     */
    private fun updateUIForExercise(type: String) {
        // Logika sederhana untuk menyesuaikan tampilan, meskipun animasinya belum diimplementasi
        val duration = when (type) {
            "Box Breathing (4-4-4-4)" -> "4-4-4-4 Cycles"
            "4-7-8 Breathing" -> "4-7-8 Cycles"
            else -> "Varied Cycles"
        }

        binding.toolbar.title = type
        binding.tvSessionDuration.text = "Session Duration: 5 Minutes"
        binding.tvInstruction.text = "Tekan 'Start Session' untuk memulai."
        binding.tvCycleCounter.text = duration

        // TODO: Sesuaikan logika Instruction berdasarkan type
    }
}