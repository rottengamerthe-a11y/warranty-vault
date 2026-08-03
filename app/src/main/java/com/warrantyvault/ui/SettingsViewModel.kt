package com.warrantyvault.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantyvault.data.WarrantyRepository
import com.warrantyvault.export.ExportImportManager
// Firebase imports - commented out until Firebase is properly configured
// import com.warrantyvault.cloud.FirebaseAuthManager
// import com.warrantyvault.cloud.CloudBackupManager
// import com.warrantyvault.cloud.FirestoreSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImport: ExportImportManager,
    private val repository: WarrantyRepository
    // Firebase dependencies - commented out until Firebase is properly configured
    // private val authManager: FirebaseAuthManager,
    // private val cloudBackupManager: CloudBackupManager,
    // private val syncManager: FirestoreSyncManager
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun exportJson(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            runCatching { exportImport.exportJson(uri, password) }
                .onSuccess { _messages.emit("JSON backup saved") }
                .onFailure { _messages.emit("Could not export JSON: ${it.userMessage()}") }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching { exportImport.exportCsv(uri) }
                .onSuccess { _messages.emit("CSV exported") }
                .onFailure { _messages.emit("Could not export CSV: ${it.userMessage()}") }
        }
    }

    fun restoreJson(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            runCatching { exportImport.restoreJson(uri, password) }
                .onSuccess { _messages.emit("Backup restored") }
                .onFailure { _messages.emit("Could not restore backup: ${it.userMessage()}") }
        }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            runCatching { repository.clearAll() }
                .onSuccess { _messages.emit("All items deleted") }
                .onFailure { _messages.emit("Could not clear vault: ${it.userMessage()}") }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        if (!enabled) {
            viewModelScope.launch {
                runCatching { repository.cancelAllReminders() }
                    .onSuccess { _messages.emit("Reminders disabled") }
                    .onFailure { _messages.emit("Could not cancel reminders: ${it.userMessage()}") }
            }
        }
    }
    
    // Firebase cloud features - commented out until Firebase is properly configured
    /*
    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { syncManager.enableSync(enabled) }
                .onSuccess { 
                    if (enabled) {
                        _messages.emit("Cloud sync enabled")
                    } else {
                        _messages.emit("Cloud sync disabled")
                    }
                }
                .onFailure { _messages.emit("Could not ${if (enabled) "enable" else "disable"} cloud sync: ${it.userMessage()}") }
        }
    }
    
    fun setCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                runCatching { cloudBackupManager.listBackups() }
                    .onSuccess { _messages.emit("Cloud backup enabled") }
                    .onFailure { _messages.emit("Cloud backup not available: ${it.userMessage()}") }
            } else {
                _messages.emit("Cloud backup disabled")
            }
        }
    }
    
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            runCatching { authManager.sendPasswordResetEmail(email) }
                .onSuccess { _messages.emit("Password reset email sent") }
                .onFailure { _messages.emit("Could not send reset email: ${it.userMessage()}") }
        }
    }
    */
}

private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "please try again"
