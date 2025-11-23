package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.FragmentProfileBinding
import com.mindease.mindeaseapp.ui.common.SplashActivity
import com.mindease.mindeaseapp.utils.AuthResult
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleSignInClient: GoogleSignInClient

    private val authRepository: AuthRepository by lazy {
        AuthRepository(Firebase.auth, FirebaseFirestore.getInstance())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignInClient()
        displayUserProfile()
        setupLogoutListener()
        setupNavigationListeners()
        setupVerificationBanner()
    }

    override fun onResume() {
        super.onResume()
        displayUserProfile()
        setupVerificationBanner()
    }

    private fun setupGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun setupVerificationBanner() {
        viewLifecycleOwner.lifecycleScope.launch {

            if (authRepository.isGoogleUser()) {
                binding.verificationBanner.visibility = View.GONE
                return@launch
            }

            if (authRepository.isEmailPasswordUser() && !authRepository.isEmailVerified()) {
                binding.verificationBanner.visibility = View.VISIBLE

                binding.btnVerifyNow.setOnClickListener {
                    sendVerificationEmail()
                }

                binding.btnVerifyLater.setOnClickListener {
                    binding.verificationBanner.visibility = View.GONE
                }

            } else {
                binding.verificationBanner.visibility = View.GONE
            }
        }
    }

    private fun sendVerificationEmail() {
        viewLifecycleOwner.lifecycleScope.launch {
            // set awal UI
            binding.btnVerifyNow.isEnabled = false
            binding.btnVerifyNow.text = "Mengirim..."

            val result = authRepository.sendEmailVerification()

            when (result) {
                is AuthResult.Loading -> {
                    // jaga-jaga kalau repository mengirim status Loading
                    binding.btnVerifyNow.isEnabled = false
                    binding.btnVerifyNow.text = "Mengirim..."
                }

                is AuthResult.Success -> {
                    updateBannerAfterEmailSent()
                    Toast.makeText(
                        requireContext(),
                        "✅ Email verifikasi terkirim! Cek inbox Anda.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is AuthResult.Error -> {
                    val errorMsg = result.exception.message ?: "Unknown error"
                    val userMessage = when {
                        errorMsg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                            "❌ Terlalu banyak percobaan. Tunggu 5 menit."
                        errorMsg.contains("NETWORK", ignoreCase = true) ->
                            "❌ Koneksi internet bermasalah."
                        errorMsg.contains("REQUIRES_RECENT_LOGIN", ignoreCase = true) ->
                            "❌ Sesi login sudah lama. Logout dan login ulang."
                        else ->
                            "❌ Gagal mengirim email: $errorMsg"
                    }

                    Toast.makeText(requireContext(), userMessage, Toast.LENGTH_LONG).show()

                    binding.btnVerifyNow.isEnabled = true
                    binding.btnVerifyNow.text = "Verifikasi Sekarang"
                }

                else -> {
                    // fallback: jangan biarkan UI terkunci jika ada kasus tak terduga
                    binding.btnVerifyNow.isEnabled = true
                    binding.btnVerifyNow.text = "Verifikasi Sekarang"
                }
            }
        }
    }


    private fun updateBannerAfterEmailSent() {
        val banner = binding.verificationBanner

        banner.findViewById<TextView>(R.id.verification_title)?.text =
            "✅ Email Terkirim!"
        banner.findViewById<TextView>(R.id.verification_message)?.text =
            "Cek inbox Anda (termasuk Spam) dan klik link verifikasi."

        binding.btnVerifyNow.text = "Sudah Verifikasi"
        binding.btnVerifyNow.isEnabled = true
        binding.btnVerifyNow.setOnClickListener {
            checkVerificationStatus()
        }

        binding.btnVerifyLater.text = "Kirim Ulang Email"
        binding.btnVerifyLater.setOnClickListener {
            sendVerificationEmail()
        }
    }

    private fun checkVerificationStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.btnVerifyNow.isEnabled = false
            binding.btnVerifyNow.text = "Memeriksa..."

            authRepository.reloadCurrentUser()

            if (authRepository.isEmailVerified()) {
                Toast.makeText(
                    requireContext(),
                    "✅ Email berhasil diverifikasi!",
                    Toast.LENGTH_SHORT
                ).show()

                binding.verificationBanner.visibility = View.GONE

            } else {
                Toast.makeText(
                    requireContext(),
                    "❌ Email belum diverifikasi. Cek inbox dan klik link verifikasi.",
                    Toast.LENGTH_LONG
                ).show()

                binding.btnVerifyNow.isEnabled = true
                binding.btnVerifyNow.text = "Sudah Verifikasi"
            }
        }
    }

    private fun displayUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {

            authRepository.reloadCurrentUser()

            val user = authRepository.currentUser
            val profile = try {
                authRepository.getUserProfile()
            } catch (_: Exception) {
                null
            }

            if (user != null) {
                val isGuest = user.isAnonymous

                binding.tvUserName.text =
                    profile?.name ?: user.displayName ?: "User MindEase"

                binding.tvUserBio.text =
                    profile?.bio ?: "Always Be Happy"

                binding.tvUserEmail.text =
                    if (isGuest) "Guest Session"
                    else user.email ?: "No Email Found"

                binding.tvEditProfileMenu.visibility =
                    if (isGuest) View.GONE else View.VISIBLE

                val imageUrl = profile?.profileImageUrl ?: user.photoUrl?.toString()

                if (!imageUrl.isNullOrBlank() && imageUrl != "null") {
                    Glide.with(this@ProfileFragment)
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.ivProfilePicture)
                } else {
                    binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                }

            } else {
                binding.tvUserName.text = "Sesi Berakhir"
                binding.tvUserBio.text = "Silakan login ulang"
                binding.tvUserEmail.text = ""
                binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                binding.tvEditProfileMenu.visibility = View.GONE
            }
        }
    }

    private fun setupNavigationListeners() {
        binding.tvEditProfileMenu.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.tvSettingsMenu.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.tvAboutAppMenu.setOnClickListener {
            startActivity(Intent(requireContext(), AboutAppActivity::class.java))
        }
    }

    private fun setupLogoutListener() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        googleSignInClient.signOut().addOnCompleteListener {
            authRepository.logout()

            Toast.makeText(context, "Anda telah keluar.", Toast.LENGTH_SHORT).show()

            val intent = Intent(activity, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
