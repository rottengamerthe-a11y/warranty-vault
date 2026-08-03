package com.warrantyvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warrantyvault.backup.GoogleDriveBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    application: Application,
    private val driveManager: GoogleDriveBackupManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GoogleDriveUiState>(GoogleDriveUiState.Loading)
    val uiState: StateFlow<GoogleDriveUiState> = _uiState.asStateFlow()

    init {
        checkSignInStatus()
    }

    fun checkSignInStatus() {
        val isSignedIn = driveManager.isSignedIn()
        val account = driveManager.getSignedInAccount()
        _uiState.value = if (isSignedIn && account != null) {
            GoogleDriveUiState.SignedIn(
                accountEmail = account.email ?: "Unknown",
                accountName = account.displayName ?: "User"
            )
        } else {
            GoogleDriveUiState.SignedOut
        }
    }

    fun signOut() {
        driveManager.signOut()
        checkSignInStatus()
    }

    fun uploadBackup(filename: String? = null, password: String? = null) {
        viewModelScope.launch {
            _uiState.value = GoogleDriveUiState.Uploading()
            val result = driveManager.uploadBackup(filename, password)
            result.onSuccess { backupInfo ->
                _uiState.value = GoogleDriveUiState.BackupSuccess(
                    message = "Backup uploaded: ${backupInfo.name}",
                    backupInfo = backupInfo
                )
            }.onFailure { error ->
                _uiState.value = GoogleDriveUiState.Error(
                    message = "Upload failed: ${error.message}"
                )
            }
        }
    }

    fun listBackups() {
        viewModelScope.launch {
            _uiState.value = GoogleDriveUiState.Loading
            val result = driveManager.listBackups()
            result.onSuccess { backups ->
                _uiState.value = GoogleDriveUiState.BackupList(backups)
            }.onFailure { error ->
                _uiState.value = GoogleDriveUiState.Error(
                    message = "Failed to list backups: ${error.message}"
                )
            }
        }
    }

    fun restoreBackup(backupId: String, password: String? = null) {
        viewModelScope.launch {
            _uiState.value = GoogleDriveUiState.Loading
            val result = driveManager.restoreBackup(backupId, password)
            result.onSuccess { message ->
                _uiState.value = GoogleDriveUiState.Success(message)
                listBackups() // Refresh the list
            }.onFailure { error ->
                _uiState.value = GoogleDriveUiState.Error(
                    message = "Restore failed: ${error.message}"
                )
            }
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            _uiState.value = GoogleDriveUiState.Loading
            val result = driveManager.deleteBackup(backupId)
            result.onSuccess {
                _uiState.value = GoogleDriveUiState.Success("Backup deleted")
                listBackups() // Refresh the list
            }.onFailure { error ->
                _uiState.value = GoogleDriveUiState.Error(
                    message = "Delete failed: ${error.message}"
                )
            }
        }
    }

    fun getStorageUsage() {
        viewModelScope.launch {
            val result = driveManager.getStorageUsage()
            result.onSuccess { usage ->
                val currentStatus = _uiState.value
                if (currentStatus is GoogleDriveUiState.SignedIn) {
                    _uiState.value = currentStatus.copy(
                        storageUsage = "${usage.formattedSize()} (${usage.fileCount} files)"
                    )
                }
            }
        }
    }

    fun resetToNormal() {
        val currentStatus = _uiState.value
        if (currentStatus is GoogleDriveUiState.SignedIn) {
            _uiState.value = currentStatus.copy(
                isLoading = false,
                errorMessage = null,
                successMessage = null
            )
        } else if (currentStatus is GoogleDriveUiState.SignedOut) {
            _uiState.value = GoogleDriveUiState.SignedOut
        }
    }

    fun getSignInClient() = driveManager.getSignInClient()
}

sealed class GoogleDriveUiState {
    object Loading : GoogleDriveUiState()
    
    data class SignedIn(
        val accountEmail: String,
        val accountName: String,
        val isLoading: Boolean = false,
        val storageUsage: String? = null,
        val errorMessage: String? = null,
        val successMessage: String? = null
    ) : GoogleDriveUiState()
    
    object SignedOut : GoogleDriveUiState()
    
    data class Uploading(val progress: Int = 0) : GoogleDriveUiState()
    
    data class BackupSuccess(
        val message: String,
        val backupInfo: GoogleDriveBackupManager.DriveBackupInfo
    ) : GoogleDriveUiState()
    
    data class BackupList(
        val backups: List<GoogleDriveBackupManager.DriveBackupInfo>
    ) : GoogleDriveUiState()
    
    data class Success(val message: String) : GoogleDriveUiState()
    
    data class Error(val message: String) : GoogleDriveUiState()
}