package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.mindease.mindeaseapp.databinding.ActivityChangePasswordBinding
import com.mindease.mindeaseapp.ui.auth.AuthViewModel
import com.mindease.mindeaseapp.ui.auth.AuthViewModelFactory
import com.mindease.mindeaseapp.utils.AuthResult
import com.mindease.mindeaseapp.utils.ThemeManager
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private val currentUser = Firebase.auth.currentUser
    private var isGoogleUser = false
    private var isReauthenticated = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (currentUser == null || currentUser.isAnonymous) {
            Toast.makeText(
                this,
                getString(R.string.session_invalid_relogin),
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        authRepository = AuthRepository(Firebase.auth)

        lifecycleScope.launch {
            if (!authRepository.checkVerificationForCriticalAction()) {
                showVerificationRequiredDialog()
                return@launch
            }

            proceedWithPasswordChange()
        }
    }

    private fun showVerificationRequiredDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Email Belum Diverifikasi")
            .setMessage("Untuk keamanan akun Anda, silakan verifikasi email terlebih dahulu sebelum mengubah password.")
            .setPositiveButton("Kirim Email Verifikasi") { _, _ ->
                sendVerificationEmail()
            }
            .setNegativeButton("Batal") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun sendVerificationEmail() {
        lifecycleScope.launch {
            val result = authRepository.sendEmailVerification()
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "✅ Email verifikasi telah dikirim! Silakan cek inbox Anda, lalu kembali ke sini.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }

                is AuthResult.Error -> {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "❌ Gagal mengirim email: ${result.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }

                else -> {}
            }
        }
    }

    private fun proceedWithPasswordChange() {
        isGoogleUser = currentUser!!.providerData.any { info ->
            info.providerId == GoogleAuthProvider.PROVIDER_ID
        }

        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        setupGoogleReauthClient()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupInitialUI()
        setupSaveButton()
        observeViewModel()
    }

    private fun setupGoogleReauthClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGoogleSignInResult(result.data)
        }
    }

    private fun setupInitialUI() {
        if (isGoogleUser) {
            binding.tilOldPassword.visibility = View.GONE
            binding.etOldPassword.setText(getString(R.string.dummy_pass_google))
            binding.tvOldPasswordInstruction.text = getString(R.string.delete_reauth_required)
            binding.btnSavePassword.text = getString(R.string.verify_with_google)
            setNewPasswordFieldsEnabled(false)
        } else {
            binding.tvOldPasswordInstruction.text = getString(R.string.enter_your_old_password)
            setNewPasswordFieldsEnabled(true)
        }
    }

    private fun setNewPasswordFieldsEnabled(isEnabled: Boolean) {
        binding.tilNewPassword.isEnabled = isEnabled
        binding.tilConfirmNewPassword.isEnabled = isEnabled
    }

    private fun setupSaveButton() {
        binding.btnSavePassword.setOnClickListener {
            val userEmail = currentUser?.email ?: return@setOnClickListener

            if (isGoogleUser && !isReauthenticated) {
                signInWithGoogleForReauth()
                return@setOnClickListener
            }

            val oldPassword = binding.etOldPassword.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmNewPassword.text.toString().trim()

            if (newPassword.length < 6) {
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (isGoogleUser) {
                authViewModel.linkNewPasswordToGoogleUser(newPassword)
            } else {
                if (oldPassword.isEmpty()) {
                    Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                authViewModel.changePassword(userEmail, oldPassword, newPassword)
            }
        }
    }

    private fun signInWithGoogleForReauth() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authViewModel.reauthenticateUserWithCredential(credential)

        } catch (e: ApiException) {
            Toast.makeText(
                this,
                getString(R.string.google_sign_in_failed, e.statusCode.toString()),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.generic_failure_message, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    setLoadingState(true)
                }

                is AuthResult.Success -> {
                    setLoadingState(false)

                    if (isGoogleUser) {
                        if (!isReauthenticated) {
                            isReauthenticated = true
                            setNewPasswordFieldsEnabled(true)
                            binding.tvOldPasswordInstruction.text =
                                getString(R.string.set_new_password_prompt)
                            Toast.makeText(
                                this,
                                "Verifikasi Sukses. Masukkan kata sandi baru Anda.",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.btnSavePassword.text = getString(R.string.save)

                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.set_password_success),
                                Toast.LENGTH_LONG
                            ).show()
                            Firebase.auth.signOut()
                            finish()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.password_updated_relogin),
                            Toast.LENGTH_LONG
                        ).show()
                        Firebase.auth.signOut()
                        finish()
                    }
                }

                is AuthResult.Error -> {
                    setLoadingState(false)

                    val errorMessage = when (result.exception.message) {
                        "INVALID_LOGIN_CREDENTIALS" -> getString(R.string.old_password_incorrect)
                        else -> getString(
                            R.string.generic_failure_message,
                            result.exception.message
                        )
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }

                else -> {}
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnSavePassword.isEnabled = !isLoading
        binding.btnSavePassword.text = if (isLoading) {
            getString(R.string.loading)
        } else {
            if (isGoogleUser && !isReauthenticated) {
                getString(R.string.verify_with_google)
            } else {
                getString(R.string.save)
            }
        }
    }
}
