package com.mindease.mindeaseapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivityLoginBinding
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.mindease.mindeaseapp.utils.AuthResult

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inisialisasi ViewModel
        val authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // 2. Inisialisasi Google Sign-In Options dan Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("45537228218-b7dspchiv0n64m07ec2alp29f5ik2a4r.apps.googleusercontent.com") // GANTI INI dengan Web Client ID dari Firebase Console
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Inisialisasi Activity Result Launcher
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                authViewModel.signInWithGoogleCredential(credential)

            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Gagal: ${e.statusCode}", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this, "Login Firebase Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        setupListeners()
        observeViewModel()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun setupListeners() {
        // Login dengan Email/Password
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Login Tamu (Guest)
        binding.btnGuestLogin.setOnClickListener {
            authViewModel.loginAsGuest()
        }

        // Pendaftaran
        binding.tvSignUp.setOnClickListener {
            Toast.makeText(this, "Pendaftaran (TODO)", Toast.LENGTH_SHORT).show()
        }

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Membuka form Forgot Password (TODO)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.btnGuestLogin.isEnabled = false
                    binding.btnGoogleSignIn.isEnabled = false
                }
                is AuthResult.Success -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnGuestLogin.isEnabled = true
                    binding.btnGoogleSignIn.isEnabled = true
                    Toast.makeText(this, "Autentikasi Sukses! Selamat datang.", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                }
                is AuthResult.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnGuestLogin.isEnabled = true
                    binding.btnGoogleSignIn.isEnabled = true
                    Toast.makeText(this, "Gagal: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}