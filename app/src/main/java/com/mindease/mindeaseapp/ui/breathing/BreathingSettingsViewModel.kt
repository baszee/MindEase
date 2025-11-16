package com.mindease.mindeaseapp.ui.breathing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindease.mindeaseapp.data.model.UserSettings
import com.mindease.mindeaseapp.data.repository.SettingsRepository
import kotlinx.coroutines.launch

/**
 * ViewModel untuk memuat dan menyimpan pengaturan sesi pernapasan (Sound/Haptic).
 */
class BreathingSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableLiveData<UserSettings>()
    val settings: LiveData<UserSettings> = _settings

    fun fetchBreathingSettings() {
        viewModelScope.launch {
            try {
                // Memuat pengaturan terakhir dari Firestore
                val currentSettings = settingsRepository.getSettings()
                _settings.value = currentSettings
            } catch (e: Exception) {
                // Log error atau tampilkan pesan Toast jika gagal memuat
                e.printStackTrace()
                // Gunakan nilai default jika gagal (opsional)
                _settings.value = UserSettings()
            }
        }
    }

    /**
     * Menyimpan pengaturan ke Repository.
     * Menggunakan nama parameter isSoundEnabled dan isHapticEnabled untuk mencocokkan UserSettings.
     */
    fun saveBreathingSettings(isSoundEnabled: Boolean, isHapticEnabled: Boolean) {
        viewModelScope.launch {
            try {
                // Salin nilai pengaturan saat ini dan perbarui hanya bagian breathing
                val currentSettings = _settings.value ?: UserSettings()
                // 🔥 FIX: Menggunakan properti isSoundEnabled dan isHapticEnabled
                val updatedSettings = currentSettings.copy(
                    isSoundEnabled = isSoundEnabled,
                    isHapticEnabled = isHapticEnabled
                )
                settingsRepository.saveSettings(updatedSettings)
                _settings.value = updatedSettings // Update LiveData jika berhasil
            } catch (e: Exception) {
                // Log error atau tampilkan pesan Toast jika gagal menyimpan
                e.printStackTrace()
            }
        }
    }
}

class BreathingSettingsViewModelFactory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BreathingSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BreathingSettingsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}