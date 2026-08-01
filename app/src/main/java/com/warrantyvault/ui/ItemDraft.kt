package com.warrantyvault.ui

import com.warrantyvault.data.WarrantyItemEntity
import com.warrantyvault.ui.formatInputDate
import com.warrantyvault.ui.parseInputDate

data class ItemDraft(
    val id: Long = 0,
    val name: String = "",
    val category: String = "",
    val storeOrBrand: String = "",
    val purchaseDate: String = "",
    val warrantyEndDate: String = "",
    val returnDeadline: String = "",
    val serialNumber: String = "",
    val notes: String = "",
    val reminderDaysBefore: String = "14",
    val createdAt: Long = 0
) {
    fun toEntity(): WarrantyItemEntity = WarrantyItemEntity(
        id = id,
        name = name.trim(),
        category = category.trim(),
        storeOrBrand = storeOrBrand.trim(),
        purchaseDate = parseInputDate(purchaseDate),
        warrantyEndDate = parseInputDate(warrantyEndDate),
        returnDeadline = parseInputDate(returnDeadline),
        serialNumber = serialNumber.trim(),
        notes = notes.trim(),
        reminderDaysBefore = reminderDaysBefore.toIntOrNull()?.coerceIn(0, 365) ?: 14,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )

    val isValid: Boolean get() = name.isNotBlank()
}

fun WarrantyItemEntity.toDraft(): ItemDraft = ItemDraft(
    id = id,
    name = name,
    category = category,
    storeOrBrand = storeOrBrand,
    purchaseDate = purchaseDate.formatInputDate(),
    warrantyEndDate = warrantyEndDate.formatInputDate(),
    returnDeadline = returnDeadline.formatInputDate(),
    serialNumber = serialNumber,
    notes = notes,
    reminderDaysBefore = reminderDaysBefore.toString(),
    createdAt = createdAt
)
