package com.warrantyvault.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warrantyvault.data.WarrantyRepository
import com.warrantyvault.export.ExportImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImport: ExportImportManager,
    private val repository: WarrantyRepository
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            runCatching { exportImport.exportJson(uri) }
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

    fun restoreJson(uri: Uri) {
        viewModelScope.launch {
            runCatching { exportImport.restoreJson(uri) }
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
}

private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "please try again"
