package com.mindease.mindeaseapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivityLoginBinding
import com.mindease.mindeaseapp.ui.home.MainActivity // <-- IMPORT DITAMBAHKAN
// Anda akan membutuhkan RegisterActivity.kt nanti

class LoginActivity : AppCompatActivity() {

    // Variable binding untuk mengakses komponen UI
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Panggil fungsi untuk menyiapkan semua listener
        setupListeners()

        // TODO: Inisialisasi GoogleSignInClient dan Firebase Auth di sini
    }

    private fun setupListeners() {
        // Listener untuk tombol Login utama
        binding.btnLogin.setOnClickListener {
            performManualLogin()
        }

        // Listener untuk tombol Google Sign-In (akan diimplementasikan nanti)
        binding.btnGoogleSignIn.setOnClickListener {
            // signInWithGoogle() // TODO: Implementasi sign-in Google
            Toast.makeText(this, "Membuka Login Google...", Toast.LENGTH_SHORT).show()
        }

        // Listener untuk Guest Login
        binding.btnGuestLogin.setOnClickListener {
            performGuestLogin()
        }

        // Listener untuk Sign Up
        binding.tvSignUp.setOnClickListener {
            // TODO: Pindah ke RegisterActivity
            Toast.makeText(this, "Pindah ke halaman Sign Up", Toast.LENGTH_SHORT).show()
        }

        // Listener untuk Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implementasi Forgot Password
            Toast.makeText(this, "Membuka form Forgot Password", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Menangani proses login menggunakan Username/Password.
     */
    private fun performManualLogin() {
        val username = binding.etUsername.text.toString()
        val password = binding.etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username dan Password harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Logika Otentikasi (Panggil AuthRepository)

        // Logika sementara: anggap sukses
        Toast.makeText(this, "Login Berhasil! Mengarahkan ke Home...", Toast.LENGTH_LONG).show()
        goToMainActivity() // <-- AKTIFKAN TRANSISI
    }

    /**
     * Menangani proses masuk sebagai Guest.
     */
    private fun performGuestLogin() {
        // TODO: Simpan status Guest di SharedPreferences (user isGuest = true)
        Toast.makeText(this, "Masuk sebagai Guest. Beberapa fitur akan terbatas.", Toast.LENGTH_LONG).show()
        goToMainActivity() // <-- AKTIFKAN TRANSISI
    }

    /**
     * Fungsi helper untuk pindah ke MainActivity.
     */
    private fun goToMainActivity() {
        // Pastikan Anda sudah membuat file MainActivity.kt
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}