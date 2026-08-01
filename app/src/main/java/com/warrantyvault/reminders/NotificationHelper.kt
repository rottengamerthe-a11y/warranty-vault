package com.warrantyvault.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_ID = "deadline_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Deadline reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Warranty and return deadline reminders"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
