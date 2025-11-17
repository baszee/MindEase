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
import com.mindease.mindeaseapp.databinding.ActivityDeleteAccountBinding // FIX: Import Binding Class
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

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteAccountBinding // FIX: Menggunakan Binding Class
    private lateinit var authViewModel: AuthViewModel
    private lateinit var journalCloudRepository: JournalCloudRepository

    private val currentUser = Firebase.auth.currentUser

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root) // FIX: Menggunakan binding.root

        // 1. Verifikasi Pengguna dan Cek Tipe Login
        if (currentUser == null || currentUser.isAnonymous) {
            handleGuestAccess()
            return
        }

        // Setup ViewModel
        val authRepository = AuthRepository(Firebase.auth)
        val authFactory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, authFactory)[AuthViewModel::class.java]

        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        // FIX: Mengakses authRepository.auth yang sekarang val publik
        journalCloudRepository = JournalCloudRepository(firestore, storage, authRepository.auth)

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() } // FIX: Akses melalui binding

        binding.tvUserWarning.text = "Mohon verifikasi kata sandi Anda untuk menghapus akun: ${currentUser.email}" // FIX: Akses melalui binding

        setupDeleteButton()
        observeViewModel()
    }

    /**
     * Fungsi untuk menangani pengguna Guest/Anonim.
     */
    private fun handleGuestAccess() {
        binding.tvUserWarning.text = "Mohon maaf, Anda tidak dapat menghapus akun karena sedang menggunakan Sesi Tamu. Silakan Daftar atau Masuk untuk memiliki kontrol penuh atas data Anda." // FIX: Akses melalui binding
        binding.etVerificationPassword.visibility = View.GONE // FIX: Akses melalui binding
        binding.tilVerificationPassword.visibility = View.GONE // FIX: Akses melalui binding
        binding.btnDeleteAccountConfirm.text = "MASUK / DAFTAR" // FIX: Akses melalui binding

        binding.btnDeleteAccountConfirm.setOnClickListener { // FIX: Akses melalui binding
            // Arahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }


    private fun setupDeleteButton() {
        binding.btnDeleteAccountConfirm.setOnClickListener { // FIX: Akses melalui binding
            val verificationPassword = binding.etVerificationPassword.text.toString().trim() // FIX: Akses melalui binding

            if (verificationPassword.isEmpty()) {
                Toast.makeText(this, "Mohon masukkan sandi Anda.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mulai proses penghapusan
            handleDeleteProcess(verificationPassword)
        }
    }

    private fun handleDeleteProcess(verificationPassword: String) {
        val userEmail = currentUser?.email ?: return

        setLoadingState(true)

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Re-authenticate User
            // FIX: Mengakses repository melalui val publik di ViewModel
            val reauthResult = authViewModel.repository.reauthenticateUser(userEmail, verificationPassword)

            if (reauthResult is AuthResult.Success) {
                // 2. Jika Re-auth sukses, hapus semua data cloud (Jurnal)
                val isDataDeleted = deleteUserJournalData()

                withContext(Dispatchers.Main) {
                    if (isDataDeleted) {
                        // 3. Hapus akun Auth (Dipanggil setelah data dihapus)
                        authViewModel.deleteUserAccount()
                    } else {
                        // Gagal menghapus data
                        setLoadingState(false)
                        Toast.makeText(this@DeleteAccountActivity, "Gagal menghapus data Jurnal. Coba lagi.", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (reauthResult is AuthResult.Error) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    Toast.makeText(this@DeleteAccountActivity, "Verifikasi Sandi Gagal. ${reauthResult.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Hapus semua data Jurnal (Firestore dan Storage) milik pengguna saat ini.
     */
    private suspend fun deleteUserJournalData(): Boolean {
        return try {
            val userId = currentUser!!.uid
            // FIX: Mengakses journalCollection melalui val publik di Repository
            val snapshot = journalCloudRepository.journalCollection
                .whereEqualTo("userId", userId)
                .get().await() // FIX: await sudah dikenali

            // Hapus setiap dokumen dan gambarnya secara individual
            for (document in snapshot.documents) {
                val entry = document.toObject(JournalEntry::class.java)?.copy(documentId = document.id)
                entry?.let { journalCloudRepository.deleteJournal(it) }
            }
            true // Semua data berhasil diproses (dihapus/diabaikan)
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
                    Toast.makeText(this, "Akun dan semua data berhasil dihapus.", Toast.LENGTH_LONG).show()

                    // Navigasi ke splash screen setelah penghapusan sukses
                    val intent = Intent(this, SplashActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is AuthResult.Error -> {
                    setLoadingState(false)
                    Toast.makeText(this, "Gagal menghapus akun: " + result.exception.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.etVerificationPassword.isEnabled = !isLoading // FIX: Akses melalui binding
        binding.btnDeleteAccountConfirm.isEnabled = !isLoading // FIX: Akses melalui binding
        binding.btnDeleteAccountConfirm.text = if (isLoading) getString(R.string.loading) else "Hapus Akun Saya" // FIX: Akses melalui binding
    }
}