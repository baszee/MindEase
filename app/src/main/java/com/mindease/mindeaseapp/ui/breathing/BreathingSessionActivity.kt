@file:Suppress("DEPRECATION")

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
import android.util.Log
import com.mindease.mindeaseapp.utils.ThemeManager

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

        // Cyclic Sighing: Icon scale range (lebih besar, tetap dalam lingkaran)
        const val CYCLIC_SCALE_MIN = 0.7f
        const val CYCLIC_SCALE_MAX = 1.3f
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firestore = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        settingsRepository = SettingsRepository(firestore, auth)

        exerciseType = intent.getStringExtra(EXTRA_EXERCISE_TYPE) ?: "Box Breathing (4-4-4-4)"

        @Suppress("DEPRECATION")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.toolbar.title = exerciseType
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadSettings()
        calculateTotalCycles()
        updateUIForExercise(exerciseType)
        setupListeners()
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                binding.switchSound.isChecked = settings.isSoundEnabled
                binding.switchHaptic.isChecked = settings.isHapticEnabled
                Log.d("SETTINGS_LOAD", "Loaded settings: Sound=${settings.isSoundEnabled}, Haptic=${settings.isHapticEnabled}")
            } catch (e: IllegalStateException) {
                binding.switchSound.isEnabled = false
                binding.switchHaptic.isEnabled = false
                Toast.makeText(this@BreathingSessionActivity, "Fitur pengaturan persisten dinonaktifkan untuk Guest/belum Login.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSettings(isSoundEnabled: Boolean, isHapticEnabled: Boolean) {
        if (isRunning) return

        Log.d("SETTINGS_SAVE", "Attempting save with UI values: Sound=$isSoundEnabled, Haptic=$isHapticEnabled")

        lifecycleScope.launch {
            try {
                val settings = UserSettings(
                    isSoundEnabled = isSoundEnabled,
                    isHapticEnabled = isHapticEnabled
                )
                settingsRepository.saveSettings(settings)
                Log.d("SETTINGS_SAVE", "SAVE SUCCESSFUL. Check Firebase Console.")
            } catch (e: IllegalStateException) {
                // Guest user
            } catch (e: Exception) {
                Log.e("SETTINGS_SAVE", "SAVE FAILED: ${e.message}")
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

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchSound.isEnabled) {
                saveSettings(isChecked, binding.switchHaptic.isChecked)
            }
        }

        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchHaptic.isEnabled) {
                saveSettings(binding.switchSound.isChecked, isChecked)
            }
        }
    }

    private fun calculateTotalCycles() {
        val phases = getPhasesForExercise(exerciseType)
        val cycleDurationSeconds = phases.sumOf { it.second }

        totalCycles = (sessionDurationSeconds / cycleDurationSeconds).toInt()

        if (totalCycles == 0) totalCycles = 1

        Log.d("CYCLE_CALC", "Session: ${sessionDurationSeconds}s, Cycle duration: ${cycleDurationSeconds}s, Total cycles: $totalCycles")
    }

    private fun updateUIForExercise(type: String) {
        when (type) {
            "Box Breathing (4-4-4-4)" -> {
                // Show progress box, hide others
                binding.breathingProgressBox.visibility = View.VISIBLE
                binding.breathingProgressTriangle.visibility = View.GONE
                binding.cyclicCircleContainer.visibility = View.GONE

                // Show cycle arrows
                binding.tvCycleTop.visibility = View.VISIBLE
                binding.tvCycleLeft.visibility = View.VISIBLE
                binding.tvCycleRight.visibility = View.VISIBLE
                binding.tvCycleBottom.visibility = View.VISIBLE

                binding.tvCycleTop.text = "Inhale 4s →"
                binding.tvCycleLeft.text = "↑ Hold 4s"
                binding.tvCycleRight.text = "Hold 4s ↓"
                binding.tvCycleBottom.text = "← Exhale 4s"

                // Show countdown
                binding.tvTimerCountdown.visibility = View.VISIBLE

                binding.breathingProgressBox.updateColors()
            }
            "Cyclic Sighing" -> {
                // Show cyclic container, hide others
                binding.breathingProgressBox.visibility = View.GONE
                binding.breathingProgressTriangle.visibility = View.GONE
                binding.cyclicCircleContainer.visibility = View.VISIBLE

                // Hide arrows
                binding.tvCycleTop.visibility = View.GONE
                binding.tvCycleLeft.visibility = View.GONE
                binding.tvCycleRight.visibility = View.GONE
                binding.tvCycleBottom.visibility = View.GONE

                // HIDE countdown untuk Cyclic Sighing
                binding.tvTimerCountdown.visibility = View.GONE

                // Set icon paru-paru
                binding.cyclicLungsIcon.setImageResource(R.drawable.ic_lungs)
                binding.cyclicLungsIcon.setColorFilter(ContextCompat.getColor(this, R.color.mindease_primary))
            }
            "4-7-8 Breathing" -> {
                // Show progress triangle, hide others
                binding.breathingProgressBox.visibility = View.GONE
                binding.breathingProgressTriangle.visibility = View.VISIBLE
                binding.cyclicCircleContainer.visibility = View.GONE

                // Show cycle arrows (3 only) - FINAL: Kiri, Kanan, Bawah
                binding.tvCycleTop.visibility = View.GONE
                binding.tvCycleLeft.visibility = View.VISIBLE
                binding.tvCycleRight.visibility = View.VISIBLE
                binding.tvCycleBottom.visibility = View.VISIBLE

                // FINAL Text: Kiri = Inhale, Kanan = Hold, Bawah = Exhale
                binding.tvCycleLeft.text = "Inhale 4s ↗"
                binding.tvCycleRight.text = "Hold 7s ↘"
                binding.tvCycleBottom.text = "↓ Exhale 8s"

                // Show countdown
                binding.tvTimerCountdown.visibility = View.VISIBLE

                binding.breathingProgressTriangle.updateColors()
            }
        }

        binding.tvSessionDuration.text = formatDurationSeconds(sessionDurationSeconds)
        binding.tvInstruction.text = "Tekan 'Start Session' untuk memulai $type."
        binding.tvCycleCounter.text = "Cycle $currentCycle/$totalCycles"

        binding.btnDurationMinus.isEnabled = sessionDurationSeconds > MIN_DURATION_SECONDS
        binding.btnDurationMinus.alpha = if (binding.btnDurationMinus.isEnabled) 1.0f else 0.5f

        val durationControlsVisibility = if (isRunning) View.GONE else View.VISIBLE
        binding.btnDurationMinus.visibility = durationControlsVisibility
        binding.btnDurationPlus.visibility = durationControlsVisibility
        binding.tvSessionDuration.visibility = durationControlsVisibility

        (binding.layoutDurationControl as? ViewGroup)?.getChildAt(0)?.visibility = durationControlsVisibility

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

        // Reset animations
        when (exerciseType) {
            "Box Breathing (4-4-4-4)" -> {
                binding.breathingProgressBox.resetProgress()
            }
            "Cyclic Sighing" -> {
                binding.cyclicLungsIcon.scaleX = 1.0f
                binding.cyclicLungsIcon.scaleY = 1.0f
            }
            "4-7-8 Breathing" -> {
                binding.breathingProgressTriangle.resetProgress()
            }
        }

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

        binding.tvInstruction.text = phaseName
        binding.tvCycleCounter.text = "Cycle $currentCycle/$totalCycles"

        // Update visual feedback based on exercise type
        when (exerciseType) {
            "Box Breathing (4-4-4-4)" -> {
                highlightBoxArrows(currentPhaseIndex)
                binding.breathingProgressBox.setPhase(currentPhaseIndex)
                binding.breathingProgressBox.setProgress(0f)
            }
            "Cyclic Sighing" -> {
                startCyclicAnimation(phaseName, phaseDurationMs)
            }
            "4-7-8 Breathing" -> {
                highlightTriangleArrows(currentPhaseIndex)
                binding.breathingProgressTriangle.setPhase(currentPhaseIndex)
                binding.breathingProgressTriangle.setProgress(0f)
            }
        }

        if (binding.switchHaptic.isChecked && !phaseName.contains("Hold", ignoreCase = true)) {
            triggerHapticFeedback()
        }

        if (binding.switchSound.isChecked) {
            // TODO: Implement Sound Guidance playback logic here
        }

        timer = object : CountDownTimer(phaseDurationMs, 50) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000 + 1

                // Update countdown (hidden untuk Cyclic Sighing)
                if (binding.tvTimerCountdown.visibility == View.VISIBLE) {
                    binding.tvTimerCountdown.text = secondsRemaining.toString()
                }

                // Update progress bar
                val elapsed = phaseDurationMs - millisUntilFinished
                val progress = elapsed.toFloat() / phaseDurationMs.toFloat()

                when (exerciseType) {
                    "Box Breathing (4-4-4-4)" -> {
                        binding.breathingProgressBox.setProgress(progress)
                    }
                    "4-7-8 Breathing" -> {
                        binding.breathingProgressTriangle.setProgress(progress)
                    }
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

    /**
     * FIX FINAL: Highlight arrows untuk Box Breathing dengan mapping yang BENAR
     * Progress bar jalan: Top → Right → Bottom → Left
     * Mapping: Inhale (Top) → Hold In (Right) → Exhale (Bottom) → Hold Out (Left)
     */
    private fun highlightBoxArrows(index: Int) {
        val arrowViews = listOf(
            binding.tvCycleTop,      // 0
            binding.tvCycleRight,    // 1
            binding.tvCycleBottom,   // 2
            binding.tvCycleLeft      // 3
        )
        val inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val activeColor = ContextCompat.getColor(this, R.color.mindease_primary)

        // Reset all to inactive
        for (arrow in arrowViews) {
            arrow.setTextColor(inactiveColor)
            arrow.alpha = 0.4f
            arrow.textSize = 20f
            arrow.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // FIX FINAL: Progress bar path = Text highlight path
        // Progress: Top(0) → Right(1) → Bottom(2) → Left(3)
        // Text: Inhale(Top=0) → Hold(Right=1) → Exhale(Bottom=2) → Hold(Left=3)
        val highlightIndex = index % 4  // Direct mapping!

        if (highlightIndex < 4) {
            arrowViews[highlightIndex].setTextColor(activeColor)
            arrowViews[highlightIndex].alpha = 1.0f
            arrowViews[highlightIndex].textSize = 22f
        }
    }

    /**
     * Highlight arrows untuk 4-7-8 Triangle
     * FINAL MAPPING:
     * Progress: Left (bottom-left → top) → Right (top → bottom-right) → Bottom (bottom-right → bottom-left)
     * Text posisi: LEFT = Inhale, RIGHT = Hold, BOTTOM = Exhale
     */
    private fun highlightTriangleArrows(index: Int) {
        val arrowViews = listOf(
            binding.tvCycleLeft,     // 0 - Inhale 4s (progress di sisi kiri)
            binding.tvCycleRight,    // 1 - Hold 7s (progress di sisi kanan)
            binding.tvCycleBottom    // 2 - Exhale 8s (progress di sisi bawah)
        )
        val inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val activeColor = ContextCompat.getColor(this, R.color.mindease_primary)

        // Reset all to inactive
        for (arrow in arrowViews) {
            arrow.setTextColor(inactiveColor)
            arrow.alpha = 0.4f
            arrow.textSize = 20f
            arrow.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // FINAL Direct Mapping
        // Phase 0: Progress Left side → Text "Inhale" di LEFT
        // Phase 1: Progress Right side → Text "Hold" di RIGHT
        // Phase 2: Progress Bottom side → Text "Exhale" di BOTTOM
        val highlightIndex = index % 3

        if (highlightIndex < 3) {
            arrowViews[highlightIndex].setTextColor(activeColor)
            arrowViews[highlightIndex].alpha = 1.0f
            arrowViews[highlightIndex].textSize = 22f
        }
    }

    /**
     * Animasi untuk Cyclic Sighing: Icon paru-paru membesar/mengecil
     */
    private fun startCyclicAnimation(phaseName: String, durationMs: Long) {
        val targetScale: Float
        val startScale = binding.cyclicLungsIcon.scaleX

        targetScale = when {
            phaseName.contains("Inhale", ignoreCase = true) -> CYCLIC_SCALE_MAX
            phaseName.contains("Exhale", ignoreCase = true) -> CYCLIC_SCALE_MIN
            else -> startScale // Hold = no change
        }

        if (targetScale == startScale) return

        val scaleX = ObjectAnimator.ofFloat(binding.cyclicLungsIcon, View.SCALE_X, startScale, targetScale).apply {
            duration = durationMs
        }
        val scaleY = ObjectAnimator.ofFloat(binding.cyclicLungsIcon, View.SCALE_Y, startScale, targetScale).apply {
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

        AnalyticsHelper.logBreathingSessionCompleted(exerciseType, sessionDurationSeconds)

        binding.tvInstruction.text = "Sesi Latihan Pernapasan Selesai!"
        binding.btnStartSession.text = "Start Session"

        // Reset animations
        when (exerciseType) {
            "Box Breathing (4-4-4-4)" -> {
                binding.breathingProgressBox.resetProgress()
            }
            "Cyclic Sighing" -> {
                binding.cyclicLungsIcon.scaleX = 1.0f
                binding.cyclicLungsIcon.scaleY = 1.0f
            }
            "4-7-8 Breathing" -> {
                binding.breathingProgressTriangle.resetProgress()
            }
        }

        binding.tvTimerCountdown.text = ""

        val durationText = formatDurationSeconds(sessionDurationSeconds)
        Toast.makeText(this, "Latihan selesai! Anda telah menyelesaikan $totalCycles siklus dalam $durationText.", Toast.LENGTH_LONG).show()

        currentCycle = 1
        currentPhaseIndex = 0
        calculateTotalCycles()
        updateUIForExercise(exerciseType)
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