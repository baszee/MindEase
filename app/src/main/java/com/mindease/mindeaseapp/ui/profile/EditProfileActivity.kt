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
import com.mindease.mindeaseapp.utils.ThemeManager

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = Firebase.auth.currentUser

        // FIX: Blokir akses Edit Profile jika user adalah Guest
        if (user == null || user.isAnonymous) {
            Toast.makeText(this, "Mohon maaf, Akun Tamu tidak dapat mengubah profil.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup ViewModel
        val authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadCurrentUserProfile()
        setupSaveButton()
        observeViewModel()
    }

    private fun loadCurrentUserProfile() {
        val user = Firebase.auth.currentUser
        // Email tidak akan null jika sudah melewati cek di onCreate
        binding.tvUserEmail.text = user?.email ?: "No Email (Signed In)"
        binding.etUsername.setText(user?.displayName ?: "")
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val newName = binding.etUsername.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (Firebase.auth.currentUser == null) {
                Toast.makeText(this, "Sesi pengguna tidak valid. Silakan login ulang.", Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }

            authViewModel.updateProfileName(newName)
        }
    }

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
                    finish()
                }
                is AuthResult.Error -> {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Toast.makeText(this, "Gagal: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}