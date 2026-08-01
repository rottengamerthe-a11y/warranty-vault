package com.warrantyvault.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.warrantyvault.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WarrantyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: WarrantyDao,
    private val reminders: ReminderScheduler
) {
    fun observeItems(): Flow<List<WarrantyItemWithAttachments>> = dao.observeItems()
    fun observeItem(id: Long): Flow<WarrantyItemWithAttachments?> = dao.observeItem(id)
    fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    suspend fun saveItem(item: WarrantyItemEntity): Long {
        val now = System.currentTimeMillis()
        val savedId = if (item.id == 0L) {
            dao.insertItem(item.copy(createdAt = now, updatedAt = now))
        } else {
            dao.updateItem(item.copy(updatedAt = now))
            item.id
        }
        val saved = item.copy(id = savedId, updatedAt = now)
        reminders.scheduleFor(saved)
        return savedId
    }

    suspend fun deleteItem(item: WarrantyItemEntity) {
        reminders.cancelFor(item.id)
        dao.getAttachmentsForItem(item.id).forEach { deleteStoredAttachmentFile(it.uri) }
        dao.deleteItem(item)
    }

    suspend fun addAttachment(itemId: Long, sourceUri: Uri, type: AttachmentType): Long = withContext(Dispatchers.IO) {
        val meta = readMeta(sourceUri)
        val dir = File(context.filesDir, "attachments/$itemId").apply { mkdirs() }
        val extension = meta.name.substringAfterLast('.', "")
        val fileName = buildString {
            append(System.currentTimeMillis())
            if (extension.isNotBlank()) append(".").append(extension)
        }
        val target = File(dir, fileName)
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Cannot open attachment" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        dao.insertAttachment(
            AttachmentEntity(
                itemId = itemId,
                type = type,
                displayName = meta.name,
                mimeType = meta.mimeType,
                uri = Uri.fromFile(target).toString(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteAttachment(attachmentId: Long) {
        dao.getAttachment(attachmentId)?.let { deleteStoredAttachmentFile(it.uri) }
        dao.deleteAttachment(attachmentId)
    }

    suspend fun clearAll() {
        dao.observeItems().first().forEach { row ->
            reminders.cancelFor(row.item.id)
            row.attachments.forEach { deleteStoredAttachmentFile(it.uri) }
        }
        dao.clearItems()
    }

    suspend fun cancelAllReminders() {
        dao.observeItems().first().forEach { row ->
            reminders.cancelFor(row.item.id)
        }
    }

    private fun readMeta(uri: Uri): AttachmentMeta {
        var name = "attachment"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index) ?: name
        }
        return AttachmentMeta(name, context.contentResolver.getType(uri))
    }

    private fun deleteStoredAttachmentFile(uri: String) {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "file") return
        val path = parsed.path ?: return
        val file = File(path)
        val attachmentsRoot = File(context.filesDir, "attachments").canonicalFile
        val target = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (target.path.startsWith(attachmentsRoot.path)) target.delete()
    }
}

private data class AttachmentMeta(val name: String, val mimeType: String?)
