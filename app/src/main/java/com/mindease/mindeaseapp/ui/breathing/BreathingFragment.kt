package com.mindease.mindeaseapp.ui.breathing

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.FragmentBreathingBinding

class BreathingFragment : Fragment() {

    private var _binding: FragmentBreathingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBreathingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        // FIX: Listener untuk Box Breathing
        binding.cardBoxBreathing.setOnClickListener {
            startBreathingSession("Box Breathing (4-4-4-4)")
        }

        // FIX: Listener untuk Cyclic Sighing
        binding.cardCyclicSighing.setOnClickListener {
            startBreathingSession("Cyclic Sighing")
        }

        // FIX: Listener untuk 4-7-8 Breathing
        binding.card478Breathing.setOnClickListener {
            startBreathingSession("4-7-8 Breathing")
        }
    }

    /**
     * Meluncurkan BreathingSessionActivity dengan jenis latihan yang dipilih.
     */
    private fun startBreathingSession(exerciseType: String) {
        val intent = Intent(requireContext(), BreathingSessionActivity::class.java).apply {
            putExtra(BreathingSessionActivity.EXTRA_EXERCISE_TYPE, exerciseType)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}