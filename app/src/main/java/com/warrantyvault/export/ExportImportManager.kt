package com.warrantyvault.export

import android.content.Context
import android.net.Uri
import com.warrantyvault.data.WarrantyDao
import com.warrantyvault.data.AppDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.warrantyvault.data.WarrantyItemEntity
import com.warrantyvault.data.WarrantyItemWithAttachments
import com.warrantyvault.reminders.ReminderScheduler
import com.warrantyvault.security.BackupEncryption
import java.io.File

@Singleton
class ExportImportManager @Inject constructor(
	@ApplicationContext private val context: Context,
	private val dao: WarrantyDao,
	private val database: AppDatabase,
	private val reminders: ReminderScheduler
) {

	suspend fun exportJson(target: Uri, password: String? = null) = withContext(Dispatchers.IO) {
		val items: List<WarrantyItemWithAttachments> = dao.getAllItemsWithAttachments()
		val arr = JSONArray()
		val tempDir = File(context.cacheDir, "export_${System.currentTimeMillis()}").apply { mkdirs() }
		try {
			for (it in items) {
				val o = JSONObject()
				val item = it.item
				o.put("id", item.id)
				o.put("name", item.name)
				o.put("category", item.category)
				o.put("storeOrBrand", item.storeOrBrand)
				o.put("purchaseDate", item.purchaseDate)
				o.put("warrantyEndDate", item.warrantyEndDate)
				o.put("returnDeadline", item.returnDeadline)
				o.put("serialNumber", item.serialNumber)
				o.put("notes", item.notes)
				o.put("reminderDaysBefore", item.reminderDaysBefore)
				val at = JSONArray()
				for (a in it.attachments) {
					val ao = JSONObject()
					ao.put("id", a.id)
					ao.put("displayName", a.displayName)
					ao.put("mimeType", a.mimeType)
					ao.put("type", a.type.name)
					// Copy attachment file into temp dir and record relative path
					val src = runCatching { java.io.File(java.net.URI(a.uri)) }.getOrNull() ?: java.io.File(a.uri.removePrefix("file://"))
					if (src.exists()) {
						val rel = "attachments/${item.id}/${src.name}"
						val dest = File(tempDir, rel).apply { parentFile?.mkdirs() }
						src.copyTo(dest, overwrite = true)
						ao.put("uri", rel)
					} else {
						ao.put("uri", a.uri)
					}
					at.put(ao)
				}
				o.put("attachments", at)
				arr.put(o)
			}

			// write data.json in temp dir
			val jsonData = arr.toString()
			val finalData = if (password != null && password.isNotEmpty()) {
				// Add metadata and encrypt
				val metadata = JSONObject().apply {
					put("version", "2.0")
					put("encrypted", true)
					put("exportDate", System.currentTimeMillis())
				}
				val combined = JSONObject().apply {
					put("metadata", metadata)
					put("data", jsonData)
				}
				BackupEncryption.encryptString(combined.toString(), password)
			} else {
				// Unencrypted export with metadata
				val metadata = JSONObject().apply {
					put("version", "2.0")
					put("encrypted", false)
					put("exportDate", System.currentTimeMillis())
				}
				val combined = JSONObject().apply {
					put("metadata", metadata)
					put("data", arr)
				}
				combined.toString()
			}
			
			File(tempDir, "data.json").writeText(finalData)
			// zip tempDir into target
			context.contentResolver.openOutputStream(target)?.use { outStream ->
				java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(outStream)).use { zos ->
					fun addFile(file: File, basePath: String) {
						if (file.isDirectory) {
							file.listFiles()?.forEach { addFile(it, basePath) }
						} else {
							val entryName = file.relativeTo(File(tempDir.path)).path.replace('\\', '/')
							zos.putNextEntry(java.util.zip.ZipEntry(entryName))
							file.inputStream().use { it.copyTo(zos) }
							zos.closeEntry()
						}
					}
					addFile(tempDir, tempDir.path)
				}
			}
		} finally {
			try { tempDir.deleteRecursively() } catch (_: Exception) {}
		}
	}

	suspend fun exportCsv(target: Uri) = withContext(Dispatchers.IO) {
		val items = dao.getAllItems()
		val sb = StringBuilder()
		sb.append("id,name,category,storeOrBrand,purchaseDate,warrantyEndDate,returnDeadline,serialNumber,notes\n")
		for (i in items) {
			sb.append(i.id).append(',')
			sb.append(csvEscape(i.name)).append(',')
			sb.append(csvEscape(i.category)).append(',')
			sb.append(csvEscape(i.storeOrBrand)).append(',')
			sb.append(i.purchaseDate ?: "").append(',')
			sb.append(i.warrantyEndDate ?: "").append(',')
			sb.append(i.returnDeadline ?: "").append(',')
			sb.append(csvEscape(i.serialNumber)).append(',')
			sb.append(csvEscape(i.notes)).append('\n')
		}
		context.contentResolver.openOutputStream(target)?.use { out ->
			out.write(sb.toString().toByteArray())
		}
	}

	suspend fun restoreJson(source: Uri, password: String? = null) = withContext(Dispatchers.IO) {
		val isZip = source.toString().lowercase().endsWith(".zip") || (runCatching {
			context.contentResolver.openInputStream(source)?.use { input ->
				val header = ByteArray(4)
				val read = input.read(header)
				read >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
			} ?: false
		}.getOrNull() == true)

		val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
		try {
			val jsonText = if (isZip) {
				context.contentResolver.openInputStream(source)?.use { input ->
					java.util.zip.ZipInputStream(java.io.BufferedInputStream(input)).use { zis ->
						var entry = zis.nextEntry
						while (entry != null) {
							val outFile = File(tempDir, entry.name)
							if (entry.isDirectory) outFile.mkdirs() else {
								outFile.parentFile?.mkdirs()
								java.io.FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
							}
							zis.closeEntry()
							entry = zis.nextEntry
						}
					}
				}
				File(tempDir, "data.json").readText()
			} else {
				context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }.orEmpty()
			}

			// Handle encrypted or new format with metadata
			val finalJsonText = if (BackupEncryption.isEncryptedBackup(jsonText)) {
				if (password != null && password.isNotEmpty()) {
					BackupEncryption.decryptString(jsonText, password) ?: throw SecurityException("Invalid password or corrupted backup")
				} else {
					throw SecurityException("Backup is encrypted. Please provide password.")
				}
			} else {
				jsonText
			}

			// Parse the JSON - handle both old format (array) and new format (object with metadata)
			val rows = if (finalJsonText.trim().startsWith("[")) {
				// Old format - direct array
				JSONArray(finalJsonText)
			} else {
				// New format - object with metadata
				val obj = JSONObject(finalJsonText)
				val metadata = obj.optJSONObject("metadata")
				val isEncrypted = metadata?.optBoolean("encrypted", false) ?: false
				
				if (isEncrypted && (password == null || password.isEmpty())) {
					throw SecurityException("Backup is encrypted. Please provide password.")
				}
				
				val data = obj.opt("data")
				when (data) {
					is String -> JSONArray(data) // Encrypted data is stored as string
					is JSONArray -> data // Unencrypted data is JSONArray
					else -> JSONArray() // Fallback
				}
			}
			
			val now = System.currentTimeMillis()

			database.withTransaction {
				try { dao.clearItems() } catch (_: Throwable) {}

				for (i in 0 until rows.length()) {
					val obj = rows.getJSONObject(i)
					val item = WarrantyItemEntity(
						name = obj.optString("name"),
						category = obj.optString("category"),
						storeOrBrand = obj.optString("storeOrBrand"),
						purchaseDate = optNullableLong(obj, "purchaseDate"),
						warrantyEndDate = optNullableLong(obj, "warrantyEndDate"),
						returnDeadline = optNullableLong(obj, "returnDeadline"),
						serialNumber = obj.optString("serialNumber"),
						notes = obj.optString("notes"),
						reminderDaysBefore = obj.optInt("reminderDaysBefore", 14),
						createdAt = now,
						updatedAt = now
					)
					val id = dao.insertItem(item)

					val attachments = obj.optJSONArray("attachments")
					if (attachments != null) {
						for (ai in 0 until attachments.length()) {
							val aobj = attachments.getJSONObject(ai)
							val uriStr = aobj.optString("uri")
							if (uriStr.isNotBlank()) {
								val possible = File(tempDir, uriStr)
								if (possible.exists()) {
									val dir = File(context.filesDir, "attachments/$id").apply { mkdirs() }
									val target = File(dir, possible.name)
									possible.copyTo(target, overwrite = true)
									dao.insertAttachment(com.warrantyvault.data.AttachmentEntity(
										itemId = id,
										type = com.warrantyvault.data.AttachmentType.valueOf(aobj.optString("type")),
										displayName = aobj.optString("displayName"),
										mimeType = aobj.optString("mimeType"),
										uri = android.net.Uri.fromFile(target).toString(),
										createdAt = now
									))
								}
							}
						}
					}
					reminders.scheduleFor(item.copy(id = id))
				}
			}

		} finally {
			try { tempDir.deleteRecursively() } catch (_: Exception) {}
		}
	}

	private fun csvEscape(s: String?): String {
		if (s == null) return ""
		return if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"${s.replace("\"", "\"\"")}\"" else s
	}

	private fun optNullableLong(obj: JSONObject, key: String): Long? = if (obj.has(key) && !obj.isNull(key)) obj.getLong(key) else null

}
