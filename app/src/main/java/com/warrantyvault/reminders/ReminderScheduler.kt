package com.warrantyvault.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.warrantyvault.data.WarrantyItemEntity
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ReminderScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun scheduleFor(item: WarrantyItemEntity) {
        // Respect global settings: if reminders are disabled, cancel any existing schedules.
        val remindersEnabled = prefs.getBoolean("reminders_enabled", true)
        if (!remindersEnabled) {
            cancelFor(item.id)
            return
        }

        // Use default reminder days when item doesn't have a positive value
        val defaultDays = prefs.getInt("default_reminder_days", 14)
        val effective = if (item.reminderDaysBefore > 0) item.reminderDaysBefore else defaultDays
        val itemWithEffective = item.copy(reminderDaysBefore = effective)

        cancelFor(item.id)
        schedule(itemWithEffective, "warranty", item.warrantyEndDate)
        schedule(itemWithEffective, "return", item.returnDeadline)
    }

    fun cancelFor(itemId: Long) {
        workManager.cancelUniqueWork(workName(itemId, "warranty"))
        workManager.cancelUniqueWork(workName(itemId, "return"))
    }

    private fun schedule(item: WarrantyItemEntity, kind: String, deadline: Long?) {
        if (deadline == null || item.id == 0L) return
        val reminderAt = deadline - TimeUnit.DAYS.toMillis(item.reminderDaysBefore.toLong())
        if (reminderAt <= System.currentTimeMillis()) return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(max(0, reminderAt - System.currentTimeMillis()), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_ITEM_ID to item.id,
                    ReminderWorker.KEY_KIND to kind,
                    ReminderWorker.KEY_ITEM_NAME to item.name,
                    ReminderWorker.KEY_DEADLINE to deadline
                )
            )
            .build()
        workManager.enqueueUniqueWork(workName(item.id, kind), ExistingWorkPolicy.REPLACE, request)
    }

    private fun workName(itemId: Long, kind: String) = "item-$itemId-$kind-reminder"
}
