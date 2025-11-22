package com.mindease.mindeaseapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivityRegisterBinding
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.mindease.mindeaseapp.utils.AuthResult
import com.mindease.mindeaseapp.utils.AnalyticsHelper
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        authRepository = AuthRepository(Firebase.auth)
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
                    // 🔥 FIX: Kirim email verifikasi setelah registrasi berhasil
                    lifecycleScope.launch {
                        val sendResult = authRepository.sendEmailVerification()

                        when (sendResult) {
                            is AuthResult.Success -> {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "✅ Email verifikasi terkirim ke ${result.data.email}\n\n" +
                                            "⚠️ PERIKSA FOLDER SPAM jika tidak muncul di Inbox dalam 2 menit.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is AuthResult.Error -> {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Pendaftaran berhasil, tapi gagal mengirim email verifikasi. Anda bisa kirim ulang dari menu Profile nanti.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {}
                        }

                        AnalyticsHelper.logSignUp("email_password")
                        goToMainActivity()
                    }
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