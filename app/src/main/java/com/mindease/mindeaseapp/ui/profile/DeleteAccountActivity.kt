package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository
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
import com.google.firebase.auth.AuthCredential // 🔥 FIX: Import AuthCredential
import androidx.activity.result.ActivityResultLauncher // 🔥 FIX: Import ini
import androidx.activity.result.contract.ActivityResultContracts // 🔥 FIX: Import ini


class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var journalCloudRepository: JournalCloudRepository

    // FIX: ActivityResultLauncher sudah diimpor
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

        val authRepository = AuthRepository(Firebase.auth)
        val authFactory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, authFactory)[AuthViewModel::class.java]

        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        journalCloudRepository = JournalCloudRepository(firestore, storage, authRepository.auth)

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
            setupEmailPassUserUI()
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupDeleteButton()
        observeViewModel()
    }

    private fun setupGoogleReauthClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // FIX: Menggunakan ActivityResultContracts.StartActivityForResult()
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

        CoroutineScope(Dispatchers.IO).launch { // FIX: CoroutineScope perlu Dispatchers.IO
            val reauthResult = if (credential != null) {
                authViewModel.repository.reauthenticateWithCredential(credential)
            } else {
                authViewModel.repository.reauthenticateUser(userEmail, verificationPassword!!)
            }

            if (reauthResult is AuthResult.Success) {
                val isDataDeleted = deleteUserJournalData()

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

    private suspend fun deleteUserJournalData(): Boolean {
        return try {
            val userId = currentUser!!.uid
            val snapshot = journalCloudRepository.journalCollection
                .whereEqualTo("userId", userId)
                .get().await()

            for (document in snapshot.documents) {
                val entry = document.toObject(JournalEntry::class.java)?.copy(documentId = document.id)
                entry?.let { journalCloudRepository.deleteJournal(it) }
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
            if (isGoogleUser) "VERIFIKASI GOOGLE" else getString(R.string.delete_account)
        }
    }
}