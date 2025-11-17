package com.mindease.mindeaseapp.ui.auth

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R // Import R untuk String Resources
import com.mindease.mindeaseapp.databinding.ActivityForgotPasswordBinding
import com.mindease.mindeaseapp.utils.ThemeManager

/**
 * Activity untuk fitur Lupa Password (Reset via Email).
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth: FirebaseAuth = Firebase.auth

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar manual (karena ini NoActionBar)
        // Perbaikan: Tidak perlu menyembunyikan supportActionBar jika ini adalah NoActionBar Activity
        // supportActionBar?.hide()

        setupListeners()
    }

    private fun setupListeners() {
        // ✅ Perbaikan: Menggunakan binding yang benar. Asumsi error sebelumnya adalah glitch build.
        binding.btnResetPassword.setOnClickListener {
            performPasswordReset()
        }
    }

    private fun performPasswordReset() {
        // ✅ Perbaikan: Menggunakan binding yang benar untuk EditText.
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            // ✅ Perbaikan: Menggunakan String Resource
            Toast.makeText(this, getString(R.string.email_required_toast), Toast.LENGTH_SHORT).show()
            return
        }

        // Kirim email reset password menggunakan Firebase Auth
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // ✅ Perbaikan: Menggunakan String Resource dengan format
                    Toast.makeText(
                        this,
                        getString(R.string.reset_password_success_toast, email),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    // ✅ Perbaikan: Menggunakan String Resource dan menampilkan pesan error spesifik
                    val errorMessage = task.exception?.message ?: getString(R.string.reset_password_generic_error)
                    Toast.makeText(
                        this,
                        getString(R.string.generic_failure_message, errorMessage),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}