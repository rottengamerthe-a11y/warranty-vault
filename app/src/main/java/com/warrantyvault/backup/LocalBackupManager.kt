package com.warrantyvault.backup

import android.content.Context
import android.net.Uri
import com.warrantyvault.export.ExportImportManager
import com.warrantyvault.security.EncryptedPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportImport: ExportImportManager
) {
    private val backupDir = File(context.filesDir, "auto_backups").apply { mkdirs() }
    private val prefs = EncryptedPrefs.create(context, "backup_settings")

    data class BackupInfo(
        val file: File,
        val name: String,
        val size: Long,
        val timestamp: Long,
        val isEncrypted: Boolean
    )

    /**
     * Creates a real backup of the vault into the local auto-backups directory.
     * If a password is provided, the backup is encrypted.
     */
    suspend fun createBackup(password: String? = null): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val isEncrypted = !password.isNullOrEmpty()
            val backupFile = File(backupDir, "auto_backup_${timestamp}${if (isEncrypted) "_encrypted" else ""}.zip")
            
            exportImport.exportJson(Uri.fromFile(backupFile), password)
            
            Result.success(
                BackupInfo(
                    file = backupFile,
                    name = backupFile.name,
                    size = backupFile.length(),
                    timestamp = timestamp,
                    isEncrypted = isEncrypted
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores a local backup file into the vault.
     * If the backup is encrypted, the password must be provided.
     */
    suspend fun restoreBackup(backupFile: File, password: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            exportImport.restoreJson(Uri.fromFile(backupFile), password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBackups(): Result<List<BackupInfo>> = withContext(Dispatchers.IO) {
        try {
            val backups = backupDir.listFiles()
                ?.filter { it.name.endsWith(".zip") }
                ?.map { file ->
                    val timestamp = extractTimestamp(file.name)
                    BackupInfo(
                        file = file,
                        name = file.name,
                        size = file.length(),
                        timestamp = timestamp,
                        isEncrypted = isFileEncrypted(file)
                    )
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
            
            Result.success(backups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (backupFile.exists()) {
                backupFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllBackups(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            backupDir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTotalBackupSize(): Long {
        return backupDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun getBackupCount(): Int {
        return backupDir.listFiles()?.size ?: 0
    }

    fun setBackupPassword(password: String?) {
        prefs.edit().putString("backup_password", password).apply()
    }

    fun getBackupPassword(): String? {
        return prefs.getString("backup_password", null)
    }

    fun isBackupPasswordSet(): Boolean {
        return !prefs.getString("backup_password", null).isNullOrEmpty()
    }

    private fun extractTimestamp(filename: String): Long {
        return try {
            val timestampStr = filename.substringAfter("_").substringBefore(".zip")
            timestampStr.toLong()
        } catch (e: Exception) {
            0L
        }
    }

    private fun isFileEncrypted(file: File): Boolean {
        return file.name.contains("_encrypted")
    }
}