package com.warrantyvault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Transaction
    @Query("SELECT * FROM warranty_items ORDER BY COALESCE(warrantyEndDate, returnDeadline, 9223372036854775807), name")
    fun observeItems(): Flow<List<WarrantyItemWithAttachments>>

    @Transaction
    @Query("SELECT * FROM warranty_items WHERE id = :id")
    fun observeItem(id: Long): Flow<WarrantyItemWithAttachments?>

    @Query("SELECT * FROM warranty_items")
    suspend fun getAllItems(): List<WarrantyItemEntity>

    @Transaction
    @Query("SELECT * FROM warranty_items")
    suspend fun getAllItemsWithAttachments(): List<WarrantyItemWithAttachments>

    @Query("SELECT DISTINCT category FROM warranty_items WHERE TRIM(category) != '' ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WarrantyItemEntity): Long

    @Update
    suspend fun updateItem(item: WarrantyItemEntity)

    @Delete
    suspend fun deleteItem(item: WarrantyItemEntity)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getAttachment(attachmentId: Long): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE itemId = :itemId")
    suspend fun getAttachmentsForItem(itemId: Long): List<AttachmentEntity>

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: Long)

    @Query("DELETE FROM warranty_items")
    suspend fun clearItems()
}
