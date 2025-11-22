@file:Suppress("DEPRECATION")

package com.mindease.mindeaseapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.mindease.mindeaseapp.utils.AnalyticsHelper
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setupGoogleSignInClient()

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            setGoogleSignInLoading(false)

            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // 🔥 FIX: Cek apakah email sudah terdaftar dengan Email/Password
                lifecycleScope.launch {
                    val email = account.email ?: ""
                    val existingMethods = authRepository.checkEmailExists(email)

                    if (existingMethods.contains("password")) {
                        // Email sudah terdaftar dengan Email/Password
                        // Login dulu dengan credential, lalu link
                        handleGoogleSignInWithExistingAccount(credential, email)
                    } else {
                        // Email belum terdaftar atau baru pertama kali
                        authViewModel.signInWithGoogleCredential(credential)
                    }
                }

            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Gagal: ${e.statusCode}", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this, "Login Firebase Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        setupListeners()
        observeViewModel()
    }

    // 🔥 BARU: Handle Google Sign-In dengan akun Email/Password yang sudah ada
    private fun handleGoogleSignInWithExistingAccount(credential: com.google.firebase.auth.AuthCredential, email: String) {
        // Tampilkan dialog konfirmasi
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Akun Sudah Terdaftar")
            .setMessage("Email $email sudah terdaftar dengan Email/Password.\n\nGabungkan akun agar Anda bisa login dengan Google atau Email/Password?")
            .setPositiveButton("Gabungkan") { _, _ ->
                // Login dengan Google, lalu link
                authViewModel.signInWithGoogleCredential(credential)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Login dibatalkan", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()
    }

    private fun signInWithGoogle() {
        setGoogleSignInLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun setGoogleSignInLoading(isLoading: Boolean) {
        binding.btnGoogleSignIn.isEnabled = !isLoading
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGuestLogin.isEnabled = !isLoading

        if (isLoading) {
            binding.btnGoogleSignIn.alpha = 0.5f
        } else {
            binding.btnGoogleSignIn.alpha = 1.0f
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGuestLogin.setOnClickListener {
            authViewModel.loginAsGuest()
        }

        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
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

                    val user = Firebase.auth.currentUser
                    val method = when {
                        user?.isAnonymous == true -> "guest"
                        user != null && user.providerData.any { it.providerId == "google.com" } -> "google"
                        else -> "email_password"
                    }
                    AnalyticsHelper.logLogin(method)

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