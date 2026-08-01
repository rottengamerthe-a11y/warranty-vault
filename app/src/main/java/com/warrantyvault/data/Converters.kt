package com.warrantyvault.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name
}
