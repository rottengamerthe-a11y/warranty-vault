package com.warrantyvault.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttachmentType { Receipt, Manual, Photo, File }

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = WarrantyItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val type: AttachmentType,
    val displayName: String,
    val mimeType: String?,
    val uri: String,
    val createdAt: Long
)
