package com.mindease.mindeaseapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivityRegisterBinding
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.mindease.mindeaseapp.utils.AuthResult
import com.mindease.mindeaseapp.utils.AnalyticsHelper // 🔥 IMPORT BARU
import com.mindease.mindeaseapp.utils.ThemeManager

/**
 * Activity untuk fitur Pendaftaran/Register.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar manual (karena ini NoActionBar)
        supportActionBar?.hide()

        // Setup ViewModel
        val authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.tvLoginLink.setOnClickListener {
            // Kembali ke LoginActivity
            finish()
        }
    }

    private fun performRegistration() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Password tidak cocok.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter.", Toast.LENGTH_SHORT).show()
            return
        }

        authViewModel.register(email, password)
    }

    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.tvLoginLink.isEnabled = false
                    Toast.makeText(this, "Mendaftar...", Toast.LENGTH_SHORT).show()
                }
                is AuthResult.Success -> {
                    Toast.makeText(this, "Pendaftaran Berhasil! Selamat datang.", Toast.LENGTH_SHORT).show()

                    // 🔥 ANALYTICS: Log Sign Up Sukses
                    AnalyticsHelper.logSignUp("email_password")

                    goToMainActivity()
                }
                is AuthResult.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.tvLoginLink.isEnabled = true
                    Toast.makeText(this, "Pendaftaran Gagal: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}