package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivityEditProfileBinding
import com.mindease.mindeaseapp.ui.auth.AuthViewModel
import com.mindease.mindeaseapp.ui.auth.AuthViewModelFactory
import com.mindease.mindeaseapp.utils.AuthResult

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewModel
        val authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadCurrentUserProfile()
        setupSaveButton()
        observeViewModel()
    }

    /**
     * Memuat nama dan email pengguna saat ini ke UI.
     */
    private fun loadCurrentUserProfile() {
        val user = Firebase.auth.currentUser
        binding.tvUserEmail.text = user?.email ?: "Guest Session"
        binding.etUsername.setText(user?.displayName ?: "")
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val newName = binding.etUsername.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // PENTING: Periksa sesi sebelum memanggil ViewModel/Repository
            if (Firebase.auth.currentUser == null) {
                Toast.makeText(this, "Sesi pengguna tidak valid. Silakan login ulang.", Toast.LENGTH_LONG).show()
                // Jika null, jangan panggil ViewModel, cukup kembali.
                finish()
                return@setOnClickListener
            }

            // Panggil fungsi update profile di ViewModel
            authViewModel.updateProfileName(newName)
        }
    }

    /**
     * Mengamati hasil dari update profile menggunakan LiveData.observe.
     */
    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    binding.btnSave.isEnabled = false
                    binding.btnSave.text = getString(R.string.loading)
                }
                is AuthResult.Success -> {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Toast.makeText(this, getString(R.string.profile_updated_successfully), Toast.LENGTH_SHORT).show()
                    finish() // Tutup activity dan kembali ke ProfileFragment
                }
                is AuthResult.Error -> {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    // Menampilkan pesan error yang lebih detail dari Firebase
                    Toast.makeText(this, "Gagal memperbarui: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}