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
import android.util.Log

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private val currentUser = Firebase.auth.currentUser
    private var isGoogleUser = false
    private var hasPassword = false
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
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // ✅ Setup Google launcher SEBELUM lifecycle check
        setupGoogleReauthClient()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ CHECK VERIFICATION
        lifecycleScope.launch {
            if (!authRepository.checkVerificationForCriticalAction()) {
                showVerificationRequiredUI()
                return@launch
            }
            proceedWithPasswordChange()
        }
    }

    private fun showVerificationRequiredUI() {
        binding.tvOldPasswordInstruction.text = "🔒 Verifikasi Email Diperlukan\n\n" +
                "Untuk keamanan akun, silakan verifikasi email terlebih dahulu.\n\n" +
                "📧 Email verifikasi telah dikirim ke ${currentUser?.email}\n\n" +
                "⚠️ Cek folder Spam/Junk jika tidak muncul"

        binding.tilOldPassword.visibility = View.GONE
        binding.tilNewPassword.visibility = View.GONE
        binding.tilConfirmNewPassword.visibility = View.GONE

        binding.btnSavePassword.text = "Kirim Ulang Email Verifikasi"

        binding.btnSavePassword.setOnClickListener {
            resendVerificationEmail()
        }
    }

    private fun resendVerificationEmail() {
        lifecycleScope.launch {
            binding.btnSavePassword.isEnabled = false
            binding.btnSavePassword.text = "Mengirim..."

            val result = authRepository.sendEmailVerification()

            when (result) {
                is AuthResult.Success -> {
                    binding.tvOldPasswordInstruction.text = "✅ Email Verifikasi Terkirim!\n\n" +
                            "📧 Silakan cek inbox (termasuk Spam)\n\n" +
                            "🔄 Setelah klik link, kembali dan klik \"Sudah Verifikasi\""

                    binding.btnSavePassword.text = "Sudah Verifikasi - Lanjutkan"
                    binding.btnSavePassword.isEnabled = true

                    binding.btnSavePassword.setOnClickListener {
                        checkVerificationAndProceed()
                    }
                }
                is AuthResult.Error -> {
                    val errorMsg = result.exception.message ?: "Unknown error"

                    val userMessage = when {
                        errorMsg.contains("TOO_MANY_ATTEMPTS") ->
                            "❌ Terlalu banyak percobaan. Tunggu 5 menit."
                        errorMsg.contains("NETWORK") ->
                            "❌ Koneksi internet bermasalah."
                        errorMsg.contains("REQUIRES_RECENT_LOGIN") ->
                            "❌ Sesi login sudah lama. Logout dan login ulang."
                        else ->
                            "❌ Gagal: $errorMsg"
                    }

                    Toast.makeText(this@ChangePasswordActivity, userMessage, Toast.LENGTH_LONG).show()

                    binding.btnSavePassword.isEnabled = true
                    binding.btnSavePassword.text = "Kirim Ulang Email Verifikasi"
                }
                else -> {}
            }
        }
    }

    private fun checkVerificationAndProceed() {
        lifecycleScope.launch {
            binding.btnSavePassword.isEnabled = false
            binding.btnSavePassword.text = "Memeriksa..."

            authRepository.reloadCurrentUser()

            if (authRepository.checkVerificationForCriticalAction()) {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "✅ Email terverifikasi!",
                    Toast.LENGTH_SHORT
                ).show()
                proceedWithPasswordChange()
            } else {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "❌ Email belum diverifikasi. Cek inbox dan klik link verifikasi.",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnSavePassword.isEnabled = true
                binding.btnSavePassword.text = "Sudah Verifikasi - Lanjutkan"
            }
        }
    }

    private fun proceedWithPasswordChange() {
        isGoogleUser = authRepository.isGoogleUser()
        hasPassword = authRepository.isEmailPasswordUser()

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
        when {
            isGoogleUser && !hasPassword -> {
                binding.tilOldPassword.visibility = View.GONE
                binding.etOldPassword.setText(getString(R.string.dummy_pass_google))
                binding.tvOldPasswordInstruction.text = "Anda login dengan Google. Buat password untuk bisa login dengan Email/Password juga."
                binding.btnSavePassword.text = "Verifikasi Google & Set Password"
                setNewPasswordFieldsEnabled(false)
            }

            isGoogleUser && hasPassword -> {
                binding.tilOldPassword.visibility = View.GONE
                binding.etOldPassword.setText(getString(R.string.dummy_pass_google))
                binding.tvOldPasswordInstruction.text = "Anda login dengan Google. Verifikasi untuk mengubah password."
                binding.btnSavePassword.text = getString(R.string.verify_with_google)
                setNewPasswordFieldsEnabled(false)
            }

            else -> {
                binding.tvOldPasswordInstruction.text = getString(R.string.enter_your_old_password)
                setNewPasswordFieldsEnabled(true)
            }
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
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when {
                isGoogleUser && !hasPassword -> {
                    authViewModel.linkNewPasswordToGoogleUser(newPassword)
                }
                isGoogleUser && hasPassword -> {
                    authViewModel.updatePasswordDirectly(newPassword)
                }
                else -> {
                    if (oldPassword.isEmpty()) {
                        Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    authViewModel.changePassword(userEmail, oldPassword, newPassword)
                }
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

                    when {
                        isGoogleUser && !isReauthenticated -> {
                            isReauthenticated = true
                            setNewPasswordFieldsEnabled(true)
                            binding.tvOldPasswordInstruction.text =
                                if (hasPassword) "Verifikasi Sukses. Masukkan password baru Anda."
                                else "Verifikasi Sukses. Buat password untuk login dengan Email/Password."
                            Toast.makeText(this, "Verifikasi Sukses!", Toast.LENGTH_SHORT).show()
                            binding.btnSavePassword.text = getString(R.string.save)
                        }

                        isGoogleUser -> {
                            val message = if (hasPassword) {
                                "Password berhasil diubah! Anda dapat login dengan Google atau Email/Password."
                            } else {
                                "Password berhasil dibuat! Sekarang Anda dapat login dengan Google atau Email/Password."
                            }
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            finish()
                        }

                        else -> {
                            Toast.makeText(
                                this,
                                getString(R.string.password_updated_relogin),
                                Toast.LENGTH_LONG
                            ).show()
                            Firebase.auth.signOut()
                            finish()
                        }
                    }
                }

                is AuthResult.Error -> {
                    setLoadingState(false)

                    val errorMessage = when (result.exception.message) {
                        "INVALID_LOGIN_CREDENTIALS" -> getString(R.string.old_password_incorrect)
                        else -> getString(R.string.generic_failure_message, result.exception.message)
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
            when {
                isGoogleUser && !isReauthenticated -> "Verifikasi Google"
                else -> getString(R.string.save)
            }
        }
    }
}