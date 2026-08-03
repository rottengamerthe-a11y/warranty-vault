package com.warrantyvault.backup

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.util.ExponentialBackOff
import com.warrantyvault.export.ExportImportManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportImport: ExportImportManager
) {
    private val prefs = context.getSharedPreferences("google_drive_settings", Context.MODE_PRIVATE)
    
    companion object {
        private const val APP_FOLDER_NAME = "WarrantyVault Backups"
        private val DRIVE_SCOPES = listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
    }

    data class DriveBackupInfo(
        val id: String,
        val name: String,
        val size: Long,
        val created: Long,
        val modified: Long,
        val isEncrypted: Boolean
    )

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .requestEmail()
            .build()
        
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInClient(): GoogleSignInClient = googleSignInClient

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            DRIVE_SCOPES
        ).setBackOff(ExponentialBackOff())
        
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("WarrantyVault")
            .build()
    }

    /**
     * Creates a real encrypted (or plain) backup of the vault using ExportImportManager
     * and uploads it to the user's private Drive app folder.
     */
    suspend fun uploadBackup(filename: String? = null, password: String? = null): Result<DriveBackupInfo> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: throw Exception("Not signed in to Google Drive")
            val driveService = getDriveService(account)
            
            // Create a real backup using ExportImportManager
            val tempFile = File(context.cacheDir, "drive_backup_${System.currentTimeMillis()}.zip")
            exportImport.exportJson(Uri.fromFile(tempFile), password)
            
            // Get or create app folder
            val appFolder = getOrCreateAppFolder(driveService)
            
            // Upload file
            val timestamp = System.currentTimeMillis()
            val isEncrypted = !password.isNullOrEmpty()
            val driveFilename = filename ?: "backup_${timestamp}${if (isEncrypted) "_encrypted" else ""}.zip"
            
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = driveFilename
                parents = listOf(appFolder.id)
                description = if (isEncrypted) "WarrantyVault encrypted backup" else "WarrantyVault backup"
            }
            
            val mediaContent = com.google.api.client.http.FileContent(
                "application/zip",
                tempFile
            )
            
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id,name,size,createdTime,modifiedTime")
                .execute()
            
            // Clean up temp file
            tempFile.delete()
            
            val backupInfo = DriveBackupInfo(
                id = uploadedFile.id,
                name = uploadedFile.name,
                size = (uploadedFile.size as? Long) ?: 0L,
                created = uploadedFile.createdTime.value,
                modified = uploadedFile.modifiedTime.value,
                isEncrypted = isEncrypted
            )
            
            Result.success(backupInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBackups(): Result<List<DriveBackupInfo>> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: throw Exception("Not signed in to Google Drive")
            val driveService = getDriveService(account)
            
            val appFolder = getOrCreateAppFolder(driveService)
            
            val files = driveService.files().list()
                .setQ("'${appFolder.id}' in parents and trashed=false")
                .setFields("files(id,name,size,createdTime,modifiedTime)")
                .execute()
            
            val backups = files.files.map { file ->
                DriveBackupInfo(
                    id = file.id,
                    name = file.name,
                    size = (file.size as? Long) ?: 0L,
                    created = file.createdTime.value,
                    modified = file.modifiedTime.value,
                    isEncrypted = file.name.contains("_encrypted")
                )
            }.sortedByDescending { it.created }
            
            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(backupId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: throw Exception("Not signed in to Google Drive")
            val driveService = getDriveService(account)
            
            val tempFile = File(context.cacheDir, "temp_drive_download_${System.currentTimeMillis()}.zip")
            
            val outputStream = FileOutputStream(tempFile)
            driveService.files().get(backupId)
                .executeMediaAndDownloadTo(outputStream)
            
            outputStream.close()
            
            Result.success(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a backup from Drive and restores it into the local vault.
     * If the backup is encrypted, the password must be provided.
     */
    suspend fun restoreBackup(backupId: String, password: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadedFile = downloadBackup(backupId).getOrThrow()
            
            exportImport.restoreJson(Uri.fromFile(downloadedFile), password)
            
            downloadedFile.delete() // Clean up temp file
            
            Result.success("Backup restored successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: throw Exception("Not signed in to Google Drive")
            val driveService = getDriveService(account)
            
            driveService.files().delete(backupId).execute()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStorageUsage(): Result<StorageUsage> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: throw Exception("Not signed in to Google Drive")
            val driveService = getDriveService(account)
            
            val appFolder = getOrCreateAppFolder(driveService)
            
            val files = driveService.files().list()
                .setQ("'${appFolder.id}' in parents and trashed=false")
                .setFields("files(size)")
                .execute()
            
            val totalBytes = files.files.sumOf { (it.size as? Long) ?: 0L }
            val fileCount = files.files.size
            
            Result.success(StorageUsage(totalBytes, fileCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class StorageUsage(
        val totalBytes: Long,
        val fileCount: Int
    ) {
        fun formattedSize(): String {
            val kb = totalBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            
            return when {
                gb >= 1 -> "%.2f GB".format(gb)
                mb >= 1 -> "%.2f MB".format(mb)
                kb >= 1 -> "%.2f KB".format(kb)
                else -> "$totalBytes bytes"
            }
        }
    }

    private suspend fun getOrCreateAppFolder(driveService: Drive): com.google.api.services.drive.model.File = withContext(Dispatchers.IO) {
        // Check if app folder exists
        val existingFolders = driveService.files().list()
            .setQ("name='$APP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setSpaces("appDataFolder")
            .setFields("files(id,name)")
            .execute()
        
        if (existingFolders.files.isNotEmpty()) {
            return@withContext existingFolders.files[0]
        }
        
        // Create new app folder
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = APP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf("appDataFolder")
        }
        
        driveService.files().create(folderMetadata)
            .setFields("id,name")
            .execute()
    }

    fun signOut() {
        googleSignInClient.signOut()
        prefs.edit().clear().apply()
    }
}