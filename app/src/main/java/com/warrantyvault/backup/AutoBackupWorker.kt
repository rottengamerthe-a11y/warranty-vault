package com.warrantyvault.backup

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.warrantyvault.export.ExportImportManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val exportImport: ExportImportManager
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_INTERVAL_DAYS = "interval_days"
        const val KEY_MAX_VERSIONS = "max_versions"
    }

    override suspend fun doWork(): Result {
        return try {
            val maxVersions = inputData.getInt(KEY_MAX_VERSIONS, AutoBackupScheduler.MAX_BACKUP_VERSIONS)
            val scheduler = AutoBackupScheduler(applicationContext)
            
            // Create a real backup using ExportImportManager
            val backupDir = File(applicationContext.filesDir, "auto_backups").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "auto_backup_$timestamp.zip")
            
            // Use the stored backup password if set, otherwise create an unencrypted backup
            val backupPassword = scheduler.getBackupPassword()
            exportImport.exportJson(Uri.fromFile(backupFile), backupPassword)
            
            // Rotate old backups
            rotateBackups(backupDir, maxVersions)
            
            // Update last backup time
            scheduler.setLastBackupTime(timestamp)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun rotateBackups(backupDir: File, maxVersions: Int) {
        val backups = backupDir.listFiles()
            ?.filter { it.name.startsWith("auto_backup_") && it.name.endsWith(".zip") }
            ?.sortedByDescending { it.name.substringAfter("auto_backup_").substringBefore(".zip").toLong() }
            ?: return

        if (backups.size > maxVersions) {
            backups.drop(maxVersions).forEach { it.delete() }
        }
    }
}