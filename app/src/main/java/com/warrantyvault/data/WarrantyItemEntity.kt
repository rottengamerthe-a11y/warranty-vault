package com.warrantyvault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warranty_items")
data class WarrantyItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val storeOrBrand: String,
    val purchaseDate: Long?,
    val warrantyEndDate: Long?,
    val returnDeadline: Long?,
    val serialNumber: String,
    val notes: String,
    val reminderDaysBefore: Int,
    val location: String = "",
    val createdAt: Long,
    val updatedAt: Long
)
