package com.warrantyvault.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.warrantyvault.MainActivity
import com.warrantyvault.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
class ReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val itemId = inputData.getLong(KEY_ITEM_ID, 0L)
        val kind = inputData.getString(KEY_KIND).orEmpty()
        val itemName = inputData.getString(KEY_ITEM_NAME).orEmpty()
        val deadline = inputData.getLong(KEY_DEADLINE, 0L)
        val label = if (kind == "return") "Return deadline" else "Warranty"
        val date = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(deadline))

        val intent = Intent(appContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ITEM_ID, itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            itemId.toInt() + kind.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$label coming up")
            .setContentText("$itemName: $date")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify((itemId.toInt() * 31) + kind.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_KIND = "kind"
        const val KEY_ITEM_NAME = "itemName"
        const val KEY_DEADLINE = "deadline"
    }
}
