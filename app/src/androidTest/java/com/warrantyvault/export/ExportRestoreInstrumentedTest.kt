package com.warrantyvault.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.warrantyvault.data.getDatabase
import com.warrantyvault.data.WarrantyItemEntity
import com.warrantyvault.data.AttachmentEntity
import com.warrantyvault.data.AttachmentType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.net.Uri
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportRestoreInstrumentedTest {
    @Test
    fun exportAndRestore_withAttachments_restoresData() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = getDatabase(ctx)
        val dao = db.warrantyDao()

        // ensure clean start
        dao.clearItems()

        val now = System.currentTimeMillis()
        val item = WarrantyItemEntity(
            name = "Test Item",
            category = "",
            storeOrBrand = "",
            purchaseDate = null,
            warrantyEndDate = now + 1000L * 60 * 60 * 24 * 30,
            returnDeadline = null,
            serialNumber = "",
            notes = "test",
            reminderDaysBefore = 7,
            createdAt = now,
            updatedAt = now
        )
        val id = dao.insertItem(item)

        // create attachment file
        val attachDir = File(ctx.filesDir, "attachments/$id").apply { mkdirs() }
        val attachFile = File(attachDir, "demo.txt").apply { writeText("hello") }
        dao.insertAttachment(
            AttachmentEntity(
                itemId = id,
                type = AttachmentType.File,
                displayName = "demo.txt",
                mimeType = "text/plain",
                uri = Uri.fromFile(attachFile).toString(),
                createdAt = now
            )
        )

        val exportManager = ExportImportManager(ctx, dao, db, com.warrantyvault.reminders.ReminderScheduler(ctx))

        // export to a temp zip
        val outFile = File(ctx.cacheDir, "test_export_${System.currentTimeMillis()}.zip")
        val outUri = Uri.fromFile(outFile)
        exportManager.exportJson(outUri)

        // clear DB
        dao.clearItems()
        assertEquals(0, dao.getAllItems().size)

        // restore
        exportManager.restoreJson(outUri)

        val items = dao.getAllItems()
        assertEquals(1, items.size)
        val restoredId = items[0].id
        val restoredAttachments = dao.getAttachmentsForItem(restoredId)
        assertTrue(restoredAttachments.isNotEmpty())
        val restoredFile = File(ctx.filesDir, "attachments/$restoredId/${restoredAttachments[0].uri.substringAfterLast('/')}")
        assertTrue(restoredFile.exists())
    }
}
