package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.SettingsRepository
import com.mindease.mindeaseapp.databinding.ActivityDeleteAccountBinding
import com.mindease.mindeaseapp.ui.auth.AuthViewModel
import com.mindease.mindeaseapp.ui.auth.AuthViewModelFactory
import com.mindease.mindeaseapp.ui.auth.LoginActivity
import com.mindease.mindeaseapp.ui.common.SplashActivity
import com.mindease.mindeaseapp.utils.AuthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts


class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var journalCloudRepository: JournalCloudRepository
    private lateinit var moodCloudRepository: MoodCloudRepository
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private var isGoogleUser = false

    private val currentUser = Firebase.auth.currentUser

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(Firebase.auth)
        val authFactory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, authFactory)[AuthViewModel::class.java]

        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val auth = Firebase.auth

        journalCloudRepository = JournalCloudRepository(firestore, storage, auth)
        moodCloudRepository = MoodCloudRepository(firestore, auth)
        settingsRepository = SettingsRepository(firestore, auth)

        if (currentUser == null || currentUser.isAnonymous) {
            handleGuestAccess()
            return
        }

        isGoogleUser = currentUser.providerData.any { info ->
            info.providerId == GoogleAuthProvider.PROVIDER_ID
        }

        if (isGoogleUser) {
            setupGoogleReauthClient()
            setupGoogleUserUI()
        } else {
            // 🔥 BARU: Cek verifikasi untuk Email/Password user
            checkEmailVerificationForEmailUser()
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupDeleteButton()
        observeViewModel()
    }

    // 🔥 BARU: Paksa verifikasi untuk Email/Password user
    private fun checkEmailVerificationForEmailUser() {
        lifecycleScope.launch {
            val isVerified = authRepository.checkVerificationForCriticalAction()

            if (!isVerified) {
                // Email/Password user BELUM verifikasi → BLOKIR akses
                showVerificationRequiredUI()
            } else {
                // Sudah verifikasi → Tampilkan UI normal
                setupEmailPassUserUI()
            }
        }
    }

    // 🔥 BARU: UI jika verifikasi diperlukan
    private fun showVerificationRequiredUI() {
        binding.tvUserWarning.text = "🔒 Verifikasi Email Diperlukan\n\n" +
                "Untuk keamanan akun Anda, kami telah mengirim email verifikasi ke ${currentUser?.email}.\n\n" +
                "Silakan cek inbox Anda dan klik link verifikasi, lalu kembali ke halaman ini."

        binding.tilVerificationPassword.visibility = View.GONE
        binding.etVerificationPassword.visibility = View.GONE

        binding.btnDeleteAccountConfirm.text = "Kirim Ulang Email Verifikasi"

        binding.btnDeleteAccountConfirm.setOnClickListener {
            resendVerificationEmail()
        }
    }

    // 🔥 BARU: Kirim ulang email verifikasi
    private fun resendVerificationEmail() {
        lifecycleScope.launch {
            binding.btnDeleteAccountConfirm.isEnabled = false
            binding.btnDeleteAccountConfirm.text = "Mengirim..."

            val result = authRepository.sendEmailVerification()

            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(
                        this@DeleteAccountActivity,
                        "✅ Email verifikasi telah dikirim! Cek inbox Anda.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Tutup activity, user harus verifikasi dulu
                    finish()
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        this@DeleteAccountActivity,
                        "❌ Gagal mengirim email: ${result.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    binding.btnDeleteAccountConfirm.isEnabled = true
                    binding.btnDeleteAccountConfirm.text = "Kirim Ulang Email Verifikasi"
                }
                else -> {}
            }
        }
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

    private fun setupGoogleUserUI() {
        binding.tvUserWarning.text = getString(R.string.delete_reauth_required)
        binding.tilVerificationPassword.visibility = View.GONE
        binding.etVerificationPassword.setText(getString(R.string.dummy_pass_google))
        binding.btnDeleteAccountConfirm.text = getString(R.string.verify_with_google)
    }

    private fun setupEmailPassUserUI() {
        binding.tvUserWarning.text = getString(R.string.verify_password_for_email, currentUser?.email ?: "")
        binding.tilVerificationPassword.visibility = View.VISIBLE
    }

    private fun handleGuestAccess() {
        binding.tvUserWarning.text = getString(R.string.guest_cannot_delete_account)
        binding.etVerificationPassword.visibility = View.GONE
        binding.tilVerificationPassword.visibility = View.GONE
        binding.btnDeleteAccountConfirm.text = getString(R.string.login_register_action)

        binding.btnDeleteAccountConfirm.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupDeleteButton() {
        binding.btnDeleteAccountConfirm.setOnClickListener {
            if (isGoogleUser) {
                signInWithGoogleForReauth()
            } else {
                val verificationPassword = binding.etVerificationPassword.text.toString().trim()

                if (verificationPassword.isEmpty()) {
                    Toast.makeText(this, getString(R.string.password_required_toast), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                handleDeleteProcess(verificationPassword, null)
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

            handleDeleteProcess(null, credential)

        } catch (e: ApiException) {
            Toast.makeText(this, getString(R.string.google_sign_in_failed, e.statusCode.toString()), Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.generic_failure_message, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun handleDeleteProcess(verificationPassword: String?, credential: AuthCredential?) {
        val userEmail = currentUser?.email ?: return

        setLoadingState(true)

        CoroutineScope(Dispatchers.IO).launch {
            val reauthResult = if (credential != null) {
                authViewModel.repository.reauthenticateWithCredential(credential)
            } else {
                authViewModel.repository.reauthenticateUser(userEmail, verificationPassword!!)
            }

            if (reauthResult is AuthResult.Success) {
                val isDataDeleted = deleteAllUserData()

                withContext(Dispatchers.Main) {
                    if (isDataDeleted) {
                        authViewModel.deleteUserAccount()
                    } else {
                        setLoadingState(false)
                        Toast.makeText(this@DeleteAccountActivity, getString(R.string.failed_to_delete_journal_data), Toast.LENGTH_LONG).show()
                    }
                }
            } else if (reauthResult is AuthResult.Error) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    Toast.makeText(this@DeleteAccountActivity, getString(R.string.password_verification_failed, reauthResult.exception.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun deleteAllUserData(): Boolean {
        return try {
            val userId = currentUser!!.uid

            // 1. Hapus Journals
            val journalSnapshot = journalCloudRepository.journalCollection
                .whereEqualTo("userId", userId)
                .get().await()

            for (document in journalSnapshot.documents) {
                val entry = document.toObject(JournalEntry::class.java)?.copy(documentId = document.id)
                entry?.let { journalCloudRepository.deleteJournal(it) }
            }

            // 2. Hapus Moods
            val moodDeleted = moodCloudRepository.deleteAllMoods()
            if (!moodDeleted) {
                android.util.Log.e("DeleteAccount", "Failed to delete moods")
                return false
            }

            // 3. Hapus Settings
            val settingsDeleted = settingsRepository.deleteUserSettings()
            if (!settingsDeleted) {
                android.util.Log.e("DeleteAccount", "Failed to delete settings")
                return false
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun observeViewModel() {
        authViewModel.deleteResult.observe(this) { result ->
            when (result) {
                is AuthResult.Success -> {
                    setLoadingState(false)
                    Toast.makeText(this, getString(R.string.account_deleted_successfully), Toast.LENGTH_LONG).show()

                    val intent = Intent(this, SplashActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is AuthResult.Error -> {
                    setLoadingState(false)
                    Toast.makeText(this, getString(R.string.generic_failure_message, result.exception.message), Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.etVerificationPassword.isEnabled = !isLoading
        binding.btnDeleteAccountConfirm.isEnabled = !isLoading

        binding.btnDeleteAccountConfirm.text = if (isLoading) getString(R.string.loading) else {
            if (isGoogleUser) getString(R.string.verify_with_google) else getString(R.string.delete_account)
        }
    }
}