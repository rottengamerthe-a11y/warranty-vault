package com.warrantyvault.backup

import android.content.Context
import androidx.work.*
import com.warrantyvault.security.EncryptedPrefs
import java.util.concurrent.TimeUnit

class AutoBackupScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val prefs = EncryptedPrefs.create(context, "backup_settings")

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup_work"
        const val DEFAULT_BACKUP_INTERVAL_DAYS = 7
        const val MAX_BACKUP_VERSIONS = 5
    }

    fun scheduleAutoBackup(intervalDays: Int = DEFAULT_BACKUP_INTERVAL_DAYS) {
        // Check if auto backup is enabled
        val autoBackupEnabled = prefs.getBoolean("auto_backup_enabled", true)
        if (!autoBackupEnabled) {
            cancelAutoBackup()
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(false)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalDays.toLong(), TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    AutoBackupWorker.KEY_INTERVAL_DAYS to intervalDays,
                    AutoBackupWorker.KEY_MAX_VERSIONS to prefs.getInt(
                        "max_backup_versions", 
                        MAX_BACKUP_VERSIONS
                    )
                )
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }

    fun cancelAutoBackup() {
        workManager.cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
    }

    fun isAutoBackupEnabled(): Boolean {
        return prefs.getBoolean("auto_backup_enabled", true)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
        if (enabled) {
            scheduleAutoBackup()
        } else {
            cancelAutoBackup()
        }
    }

    fun getBackupInterval(): Int {
        return prefs.getInt("backup_interval_days", DEFAULT_BACKUP_INTERVAL_DAYS)
    }

    fun setBackupInterval(days: Int) {
        prefs.edit().putInt("backup_interval_days", days).apply()
        scheduleAutoBackup(days)
    }

    fun getMaxBackupVersions(): Int {
        return prefs.getInt("max_backup_versions", MAX_BACKUP_VERSIONS)
    }

    fun setMaxBackupVersions(versions: Int) {
        prefs.edit().putInt("max_backup_versions", versions).apply()
    }

    fun getLastBackupTime(): Long {
        return prefs.getLong("last_backup_time", 0L)
    }

    fun setLastBackupTime(timestamp: Long) {
        prefs.edit().putLong("last_backup_time", timestamp).apply()
    }

    fun getBackupPassword(): String? {
        return prefs.getString("backup_password", null)
    }

    fun setBackupPassword(password: String?) {
        prefs.edit().putString("backup_password", password).apply()
    }
}
