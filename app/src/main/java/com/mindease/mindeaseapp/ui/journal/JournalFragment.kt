package com.mindease.mindeaseapp.ui.journal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth // Tambahkan ini
import com.google.firebase.firestore.FirebaseFirestore // Tambahkan ini
import com.google.firebase.storage.FirebaseStorage // Tambahkan ini
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository // GANTI IMPORT INI
import com.mindease.mindeaseapp.databinding.FragmentJournalBinding

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: JournalViewModel
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)

        // 1. Setup ViewModel
        setupViewModel()

        // 2. Setup RecyclerView
        setupRecyclerView()

        // 3. Observe Data
        observeJournals()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listener untuk FAB (Tambah Jurnal Baru)
        binding.fabAddJournal.setOnClickListener {
            navigateToAddJournal()
        }
    }

    private fun setupViewModel() {
        // INISIALISASI CLOUD REPOSITORY
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val auth = FirebaseAuth.getInstance()

        // GANTI KE CLOUD REPOSITORY
        val repository = JournalCloudRepository(firestore, storage, auth)
        val factory = JournalViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[JournalViewModel::class.java]
    }

    private fun setupRecyclerView() {
        // Inisialisasi tanpa argumen
        journalAdapter = JournalAdapter()

        binding.rvJournalList.apply {
            adapter = journalAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeJournals() {
        viewModel.allJournals.observe(viewLifecycleOwner) { journals ->
            journalAdapter.submitList(journals)
            // Tampilkan atau sembunyikan empty state
            binding.tvEmptyState.visibility = if (journals.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /**
     * Fungsi untuk pindah ke AddJournalActivity.
     */
    private fun navigateToAddJournal() {
        val intent = Intent(requireContext(), AddJournalActivity::class.java)
            .apply { putExtra(AddJournalActivity.EXTRA_JOURNAL_ID, null as String?) } // Gunakan null untuk mode Add
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}