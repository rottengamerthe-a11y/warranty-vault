package com.warrantyvault.data

import androidx.room.Embedded
import androidx.room.Relation

data class WarrantyItemWithAttachments(
    @Embedded val item: WarrantyItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val attachments: List<AttachmentEntity>
)
