package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.EmailAuthProvider // Tambahkan import ini
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivityChangePasswordBinding
import com.mindease.mindeaseapp.ui.auth.AuthViewModel
import com.mindease.mindeaseapp.ui.auth.AuthViewModelFactory
import com.mindease.mindeaseapp.utils.AuthResult
import com.mindease.mindeaseapp.utils.ThemeManager

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verifikasi tipe pengguna sebelum inisialisasi ViewModel
        val user = Firebase.auth.currentUser

        val isEmailPasswordUser = user?.providerData?.any { info ->
            info.providerId == EmailAuthProvider.PROVIDER_ID
        } ?: false

        if (user == null || !isEmailPasswordUser) { // FIX: Blokir jika bukan Email/Pass
            Toast.makeText(this, "Mohon maaf, fitur ini hanya tersedia untuk pengguna yang masuk dengan Email & Kata Sandi.", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        // Setup ViewModel
        val authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupSaveButton()
        observeViewModel()
    }

    private fun setupSaveButton() {
        binding.btnSavePassword.setOnClickListener {
            val oldPassword = binding.etOldPassword.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmNewPassword.text.toString().trim()
            val userEmail = Firebase.auth.currentUser?.email ?: return@setOnClickListener

            // 1. Validasi Input
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Panggil ViewModel untuk ganti password
            authViewModel.changePassword(userEmail, oldPassword, newPassword)
        }
    }

    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    binding.btnSavePassword.isEnabled = false
                    binding.btnSavePassword.text = getString(R.string.loading)
                }
                is AuthResult.Success -> {
                    binding.btnSavePassword.isEnabled = true
                    binding.btnSavePassword.text = getString(R.string.save)
                    Toast.makeText(this, "Kata sandi berhasil diperbarui. Silakan login ulang.", Toast.LENGTH_LONG).show()

                    Firebase.auth.signOut()
                    finish()
                }
                is AuthResult.Error -> {
                    binding.btnSavePassword.isEnabled = true
                    binding.btnSavePassword.text = getString(R.string.save)

                    val errorMessage = when (result.exception.message) {
                        "INVALID_LOGIN_CREDENTIALS" -> "Kata sandi lama salah. Verifikasi gagal."
                        else -> "Gagal: " + result.exception.message
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}