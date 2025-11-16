@file:Suppress("DEPRECATION") // FIX: Menghilangkan warning deprecation pada Vibrator

package com.mindease.mindeaseapp.ui.breathing

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mindease.mindeaseapp.databinding.ActivityBreathingSessionBinding
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.UserSettings
import com.mindease.mindeaseapp.data.repository.SettingsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import com.mindease.mindeaseapp.utils.AnalyticsHelper
import kotlinx.coroutines.launch
import kotlin.math.floor
import android.util.Log // 🔥 IMPORT LOG BARU

class BreathingSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingSessionBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var exerciseType: String
    private var isRunning = false
    private var currentPhaseIndex = 0

    private var sessionDurationSeconds = 30
    private val MIN_DURATION_SECONDS = 30
    private val DURATION_STEP_SECONDS = 30

    private var totalCycles = 0
    private var currentCycle = 1

    // Model Fase Pernapasan: Pair<Nama Fase, Durasi (detik)>

    private val boxBreathingPhases = listOf(
        Pair("Inhale", 4L),
        Pair("Hold", 4L),
        Pair("Exhale", 4L),
        Pair("Hold", 4L)
    )

    private val fourSevenEightPhases = listOf(
        Pair("Inhale 4s", 4L),
        Pair("Hold 7s", 7L),
        Pair("Exhale 8s", 8L)
    )

    private val cyclicSighingPhases = listOf(
        Pair("Inhale Cepat", 2L),
        Pair("Inhale Penuh", 2L),
        Pair("Exhale Panjang", 4L)
    )

    private lateinit var timer: CountDownTimer
    private lateinit var vibrator: Vibrator
    private var currentAnimator: AnimatorSet? = null

    companion object {
        const val EXTRA_EXERCISE_TYPE = "extra_exercise_type"
        const val HAPTIC_FEEDBACK_DURATION = 150L
        const val SCALE_MIN = 1.0f
        const val SCALE_MAX = 3.5f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Repository Settings
        val firestore = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        settingsRepository = SettingsRepository(firestore, auth)

        exerciseType = intent.getStringExtra(EXTRA_EXERCISE_TYPE) ?: "Box Breathing (4-4-4-4)"

        // Inisialisasi Vibrator
        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.toolbar.title = exerciseType
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 🔥 UTAMA: Memuat pengaturan terakhir dari Firebase
        loadSettings()
        calculateTotalCycles()
        updateUIForExercise(exerciseType)
        setupListeners()
    }

    // ==========================================================
    // FUNGSI UNTUK MENGELOLA SETTINGS (MEMUAT DAN MENYIMPAN KE FIRESTORE)
    // ==========================================================

    /**
     * Memuat pengaturan sesi pernapasan dari Firestore (atau default).
     */
    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                // Mencoba memuat pengaturan dari Firestore.
                val settings = settingsRepository.getSettings()

                // Update UI dengan nilai dari Firestore atau Default (false, false)
                binding.switchSound.isChecked = settings.isSoundEnabled
                binding.switchHaptic.isChecked = settings.isHapticEnabled

                Log.d("SETTINGS_LOAD", "Loaded settings: Sound=${settings.isSoundEnabled}, Haptic=${settings.isHapticEnabled}") // 🔥 DEBUG LOG
            } catch (e: IllegalStateException) {
                // Tangani Guest user/Not logged in (biarkan default)
                binding.switchSound.isEnabled = false
                binding.switchHaptic.isEnabled = false
                Toast.makeText(this@BreathingSessionActivity, "Fitur pengaturan persisten dinonaktifkan untuk Guest/belum Login.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Menyimpan pengaturan sesi pernapasan ke Firestore.
     */
    private fun saveSettings(isSoundEnabled: Boolean, isHapticEnabled: Boolean) {
        // Jangan simpan jika sesi sedang berjalan
        if (isRunning) return

        // 🔥 DEBUG LOG: NILAI YANG DIKIRIM OLEH UI
        Log.d("SETTINGS_SAVE", "Attempting save with UI values: Sound=$isSoundEnabled, Haptic=$isHapticEnabled")

        lifecycleScope.launch {
            try {
                val settings = UserSettings(
                    isSoundEnabled = isSoundEnabled,
                    isHapticEnabled = isHapticEnabled
                )
                settingsRepository.saveSettings(settings)
                Log.d("SETTINGS_SAVE", "SAVE SUCCESSFUL. Check Firebase Console.") // 🔥 DEBUG LOG SUCCESS
            } catch (e: IllegalStateException) {
                // Tangani Guest user/Not logged in (do nothing, switch sudah dinonaktifkan)
            } catch (e: Exception) {
                Log.e("SETTINGS_SAVE", "SAVE FAILED: ${e.message}") // 🔥 DEBUG LOG FAILURE
                Toast.makeText(this@BreathingSessionActivity, "Gagal menyimpan pengaturan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun formatDurationSeconds(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d.%02d", minutes, seconds)
    }

    private fun setupListeners() {
        binding.btnStartSession.setOnClickListener {
            if (!isRunning) {
                // 🔥 Jaminan terakhir: Simpan pengaturan sebelum memulai sesi (memenuhi requirement Anda)
                saveSettings(binding.switchSound.isChecked, binding.switchHaptic.isChecked)
                startSession()
                binding.btnStartSession.text = "Stop Session"
            } else {
                stopSession()
                binding.btnStartSession.text = "Start Session"
            }
        }

        binding.btnDurationPlus.setOnClickListener {
            if (!isRunning) {
                sessionDurationSeconds += DURATION_STEP_SECONDS
                calculateTotalCycles()
                updateUIForExercise(exerciseType)
            }
        }

        binding.btnDurationMinus.setOnClickListener {
            if (!isRunning && sessionDurationSeconds > MIN_DURATION_SECONDS) {
                sessionDurationSeconds -= DURATION_STEP_SECONDS
                calculateTotalCycles()
                updateUIForExercise(exerciseType)
            }
        }

        // 🔥 LISTENERS UTAMA: Menyimpan status switch segera setelah diubah
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            // Pastikan switch tidak dinonaktifkan (bukan Guest/logged in)
            if (binding.switchSound.isEnabled) {
                // isChecked = Nilai BARU Sound switch (misal TRUE)
                // binding.switchHaptic.isChecked = Nilai SAAT INI Haptic switch
                saveSettings(isChecked, binding.switchHaptic.isChecked)
            }
        }

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            // Pastikan switch tidak dinonaktifkan (bukan Guest/logged in)
            if (binding.switchHaptic.isEnabled) {
                // binding.switchSound.isChecked = Nilai SAAT INI Sound switch
                // isChecked = Nilai BARU Haptic switch (misal TRUE)
                saveSettings(binding.switchSound.isChecked, isChecked)
            }
        }
    }

    private fun calculateTotalCycles() {
        val phases = getPhasesForExercise(exerciseType)
        val cycleDurationSeconds = phases.sumOf { it.second }.toDouble()
        val totalSessionSeconds = sessionDurationSeconds.toDouble()

        totalCycles = floor(totalSessionSeconds / cycleDurationSeconds).toInt()

        if (totalSessionSeconds > 0 && totalCycles == 0) totalCycles = 1
    }

    private fun updateUIForExercise(type: String) {
        val showCycleArrows: Boolean
        val shapeRes: Int

        when (type) {
            "Cyclic Sighing" -> {
                showCycleArrows = false
                shapeRes = R.drawable.bg_breathing_circle
                binding.breathingShape.setImageResource(R.drawable.ic_nav_breathing)
                binding.breathingShape.setColorFilter(ContextCompat.getColor(this, R.color.mindease_primary))
            }
            "Box Breathing (4-4-4-4)", "4-7-8 Breathing" -> {
                showCycleArrows = true
                shapeRes = R.drawable.bg_breathing_rounded_box
                binding.breathingShape.setImageResource(0)
                binding.breathingShape.clearColorFilter()
            }
            else -> {
                showCycleArrows = true
                shapeRes = R.drawable.bg_breathing_rounded_box
            }
        }

        binding.breathingShape.setBackgroundResource(shapeRes)

        // Kontrol Visibilitas Panah Siklus
        binding.tvCycleTop.visibility = if (showCycleArrows) View.VISIBLE else View.GONE
        binding.tvCycleLeft.visibility = if (showCycleArrows) View.VISIBLE else View.GONE
        binding.tvCycleRight.visibility = if (showCycleArrows) View.VISIBLE else View.GONE
        // Menyembunyikan panah bawah untuk 4-7-8
        binding.tvCycleBottom.visibility = if (showCycleArrows && type != "4-7-8 Breathing") View.VISIBLE else View.GONE

        // Update Teks Panah
        if (type == "Box Breathing (4-4-4-4)") {
            binding.tvCycleTop.text = "Inhale 4s →"
            binding.tvCycleLeft.text = "↑ Hold 4s"
            binding.tvCycleRight.text = "↓ Hold 4s"
            binding.tvCycleBottom.text = "← Exhale 4s"
        } else if (type == "4-7-8 Breathing") {
            binding.tvCycleTop.text = "Inhale 4s →"
            binding.tvCycleLeft.text = "↑ Hold 7s"
            binding.tvCycleRight.text = "↓ Exhale 8s"
            // tvCycleBottom.visibility sudah diatur
        }

        // Teks Durasi
        binding.tvSessionDuration.text = formatDurationSeconds(sessionDurationSeconds)

        binding.tvInstruction.text = "Tekan 'Start Session' untuk memulai $type."
        binding.tvCycleCounter.text = "Cycle $currentCycle/$totalCycles"

        // Kontrol Tombol Minus
        binding.btnDurationMinus.isEnabled = sessionDurationSeconds > MIN_DURATION_SECONDS
        binding.btnDurationMinus.alpha = if (binding.btnDurationMinus.isEnabled) 1.0f else 0.5f

        // Nonaktifkan/Sembunyikan kontrol durasi saat sesi berjalan
        val durationControlsVisibility = if (isRunning) View.GONE else View.VISIBLE
        binding.btnDurationMinus.visibility = durationControlsVisibility
        binding.btnDurationPlus.visibility = durationControlsVisibility
        binding.tvSessionDuration.visibility = durationControlsVisibility

        (binding.layoutDurationControl as? ViewGroup)?.getChildAt(0)?.visibility = durationControlsVisibility

        // Nonaktifkan switch saat sesi berjalan (agar tidak ada saveSettings saat sesi aktif)
        // Kita juga pastikan tidak menimpa status disabled untuk Guest/belum login
        if (Firebase.auth.currentUser != null && !Firebase.auth.currentUser!!.isAnonymous) {
            binding.switchSound.isEnabled = !isRunning
            binding.switchHaptic.isEnabled = !isRunning
        }
    }

    private fun startSession() {
        isRunning = true
        currentCycle = 1
        currentPhaseIndex = 0
        updateUIForExercise(exerciseType)

        if (binding.switchHaptic.isChecked) triggerHapticFeedback()
        binding.tvInstruction.layoutParams = (binding.tvInstruction.layoutParams as? android.widget.FrameLayout.LayoutParams)?.apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
            topMargin = 16.dpToPx()
        }
        startPhaseTimer()
    }

    private fun stopSession() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        currentAnimator?.cancel()
        isRunning = false
        binding.tvInstruction.text = "Sesi dihentikan."
        binding.btnStartSession.text = "Start Session"

        currentCycle = 1
        currentPhaseIndex = 0
        calculateTotalCycles()
        updateUIForExercise(exerciseType)

        binding.breathingShape.scaleX = SCALE_MIN
        binding.breathingShape.scaleY = SCALE_MIN
        binding.tvTimerCountdown.text = ""
    }

    private fun getPhasesForExercise(type: String): List<Pair<String, Long>> {
        return when (type) {
            "Box Breathing (4-4-4-4)" -> boxBreathingPhases
            "Cyclic Sighing" -> cyclicSighingPhases
            "4-7-8 Breathing" -> fourSevenEightPhases
            else -> boxBreathingPhases
        }
    }

    private fun startPhaseTimer() {
        if (!isRunning) return

        if (currentCycle > totalCycles) {
            sessionCompleted()
            return
        }

        val phases = getPhasesForExercise(exerciseType)

        val currentPhase = phases[currentPhaseIndex]
        val phaseName = currentPhase.first
        val phaseDurationMs = currentPhase.second * 1000L

        currentAnimator?.cancel()

        // Perbarui UI Instruksi & Panah
        binding.tvInstruction.text = phaseName
        binding.tvCycleCounter.text = "Cycle $currentCycle/$totalCycles"
        highlightCycleArrow(currentPhaseIndex, exerciseType)

        if (binding.switchHaptic.isChecked && !phaseName.contains("Hold", ignoreCase = true)) {
            triggerHapticFeedback()
        }

        // PENTING: Jika Sound Guidance ON, di sini Anda akan memanggil fungsi memainkan suara.
        if (binding.switchSound.isChecked) {
            // TODO: Implement Sound Guidance playback logic here
        }

        startVisualAnimation(phaseName, phaseDurationMs)

        // Mulai Timer Hitungan Mundur
        timer = object : CountDownTimer(phaseDurationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000 + 1

                if (binding.tvTimerCountdown.visibility == View.VISIBLE) {
                    binding.tvTimerCountdown.text = secondsRemaining.toString()
                } else {
                    binding.tvTimerCountdown.text = ""
                }
            }

            override fun onFinish() {
                currentPhaseIndex++

                if (currentPhaseIndex >= phases.size) {
                    currentPhaseIndex = 0
                    currentCycle++
                }

                if (currentCycle > totalCycles) {
                    sessionCompleted()
                } else {
                    startPhaseTimer()
                }
            }
        }.start()
    }

    private fun highlightCycleArrow(index: Int, type: String) {
        val arrowViews = listOf(binding.tvCycleTop, binding.tvCycleRight, binding.tvCycleBottom, binding.tvCycleLeft)
        val baseColor = ContextCompat.getColor(this, android.R.color.black)
        val highlightColor = ContextCompat.getColor(this, R.color.mindease_primary)

        for (i in 0 until 4) {
            arrowViews[i].setTextColor(baseColor)
            if (type != "Cyclic Sighing") {
                arrowViews[i].visibility = View.VISIBLE
            } else {
                arrowViews[i].visibility = View.GONE
            }
        }

        if (type == "Cyclic Sighing") return

        val highlightIndex = when (type) {
            "Box Breathing (4-4-4-4)" -> {
                when (index % 4) {
                    0 -> 0 // Inhale -> Top
                    1 -> 3 // Hold -> Left
                    2 -> 1 // Exhale -> Right
                    3 -> 2 // Hold -> Bottom
                    else -> -1
                }
            }
            "4-7-8 Breathing" -> {
                when(index % 3) {
                    0 -> 0 // Inhale -> Top
                    1 -> 3 // Hold -> Left
                    2 -> 1 // Exhale -> Right
                    else -> -1
                }
            }
            else -> -1
        }

        if (highlightIndex != -1 && highlightIndex < 4) {
            arrowViews[highlightIndex].setTextColor(highlightColor)
        }

        if (type == "4-7-8 Breathing") {
            binding.tvCycleBottom.visibility = View.GONE
        }
    }


    private fun startVisualAnimation(phaseName: String, durationMs: Long) {
        val targetScale: Float
        val startScale: Float

        if (phaseName.contains("Inhale", ignoreCase = true)) {
            startScale = binding.breathingShape.scaleX
            targetScale = SCALE_MAX
        } else if (phaseName.contains("Exhale", ignoreCase = true)) {
            startScale = binding.breathingShape.scaleX
            targetScale = SCALE_MIN
        } else {
            return
        }

        val scaleX = ObjectAnimator.ofFloat(binding.breathingShape, View.SCALE_X, startScale, targetScale).apply {
            duration = durationMs
        }
        val scaleY = ObjectAnimator.ofFloat(binding.breathingShape, View.SCALE_Y, startScale, targetScale).apply {
            duration = durationMs
        }

        currentAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun triggerHapticFeedback() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(HAPTIC_FEEDBACK_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(HAPTIC_FEEDBACK_DURATION)
            }
        }
    }


    private fun sessionCompleted() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        currentAnimator?.cancel()
        isRunning = false

        // ANALYTICS: Log Session Completed
        AnalyticsHelper.logBreathingSessionCompleted(exerciseType, sessionDurationSeconds)

        binding.tvInstruction.text = "Sesi Latihan Pernapasan Selesai!"
        binding.btnStartSession.text = "Start Session"
        binding.breathingShape.scaleX = SCALE_MIN
        binding.breathingShape.scaleY = SCALE_MIN
        binding.tvTimerCountdown.text = ""

        val durationText = formatDurationSeconds(sessionDurationSeconds)

        Toast.makeText(this, "Latihan selesai! Anda telah menyelesaikan $totalCycles siklus dalam $durationText.", Toast.LENGTH_LONG).show()

        currentCycle = 1
        currentPhaseIndex = 0
        calculateTotalCycles()
        updateUIForExercise(exerciseType) // Panggil lagi untuk mengaktifkan switch
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::timer.isInitialized) {
            timer.cancel()
        }
        currentAnimator?.cancel()
    }
}