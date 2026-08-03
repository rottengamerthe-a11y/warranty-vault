package com.warrantyvault.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun Long?.formatDate(): String {
    if (this == null) return ""
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(displayFormatter)
}

fun Long?.formatInputDate(): String {
    if (this == null) return ""
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}

fun parseInputDate(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        LocalDate.parse(value.trim())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}

fun daysUntil(epochMillis: Long?): Long? {
    if (epochMillis == null) return null
    val target = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), target)
}

fun Long.formatBackupDate(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(displayFormatter)
}
