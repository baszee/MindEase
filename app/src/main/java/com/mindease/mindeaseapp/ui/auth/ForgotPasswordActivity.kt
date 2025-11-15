package com.mindease.mindeaseapp.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.databinding.ActivityForgotPasswordBinding // Menggunakan binding yang sudah dibuat

/**
 * Activity untuk fitur Lupa Password (Reset via Email).
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth: FirebaseAuth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar manual (karena ini NoActionBar)
        supportActionBar?.hide()

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnResetPassword.setOnClickListener {
            performPasswordReset()
        }
    }

    private fun performPasswordReset() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Mohon masukkan alamat email Anda.", Toast.LENGTH_SHORT).show()
            return
        }

        // Kirim email reset password menggunakan Firebase Auth
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Link reset password telah dikirim ke $email. Silakan cek email Anda.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Gagal mengirim link reset: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}