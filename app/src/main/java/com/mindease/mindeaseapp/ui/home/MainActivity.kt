package com.mindease.mindeaseapp.ui.home

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityMainBinding
import com.mindease.mindeaseapp.ui.breathing.BreathingFragment
import com.mindease.mindeaseapp.ui.journal.JournalFragment
import com.mindease.mindeaseapp.ui.profile.ProfileFragment
import com.mindease.mindeaseapp.utils.ThemeManager // 🔥 Wajib import

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Deklarasi Fragments
    private val dashboardFragment = DashboardFragment()
    private val journalFragment = JournalFragment()
    private val breathingFragment = BreathingFragment()
    private val profileFragment = ProfileFragment()

    // Status Fragment yang sedang aktif
    private var activeFragment: Fragment = dashboardFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 FIX KRITIS: Panggil setTheme() sebelum super.onCreate()
        setTheme(ThemeManager.getThemeStyleResId(this))

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // FIREBASE COST-SAVING: AKTIFKAN OFFLINE PERSISTENCE
        setupFirestoreOfflinePersistence()

        if (savedInstanceState == null) {
            // Inisialisasi semua Fragment hanya jika Activity baru dibuat
            setupInitialFragments()
        }

        // Siapkan listener untuk Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(dashboardFragment)
                R.id.nav_journal -> switchFragment(journalFragment)
                R.id.nav_breathing -> switchFragment(breathingFragment)
                R.id.nav_profile -> switchFragment(profileFragment)
            }
            true
        }
    }

    /**
     * Menambahkan semua Fragment ke FrameLayout, hanya menampilkan Dashboard.
     */
    private fun setupInitialFragments() {
        supportFragmentManager.beginTransaction().apply {
            // Tambahkan semua Fragment
            add(R.id.fragment_container, dashboardFragment, "dashboard")
            add(R.id.fragment_container, journalFragment, "journal").hide(journalFragment)
            add(R.id.fragment_container, breathingFragment, "breathing").hide(breathingFragment)
            add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
        }.commit()

        activeFragment = dashboardFragment
    }

    /**
     * Mengganti Fragment yang ditampilkan menggunakan show() dan hide().
     */
    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment == targetFragment) return

        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(targetFragment)
        }.commit()

        activeFragment = targetFragment
    }

    /**
     * Mengaktifkan caching (Persistence) Firestore untuk mode offline.
     */
    private fun setupFirestoreOfflinePersistence() {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Konfigurasi agar data tersimpan di cache lokal
            @Suppress("DEPRECATION")
            val settings = com.google.firebase.firestore.ktx.firestoreSettings {
                isPersistenceEnabled = true
                cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
            }

            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Gagal mengaktifkan persistence: ${e.message}")
        }
    }
}