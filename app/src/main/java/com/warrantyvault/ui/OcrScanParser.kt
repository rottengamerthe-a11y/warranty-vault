package com.warrantyvault.ui

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class OcrScanResult(
    val text: String,
    val barcodes: List<String>
)

fun OcrScanResult.toItemDraft(): ItemDraft {
    val lines = text.lines()
        .map { it.trim() }
        .filter { it.length >= 2 }
    val store = inferStore(lines)
    val purchaseDate = inferDate(lines)
    val serial = inferSerial(lines, barcodes)
    val itemName = inferItemName(lines, store, purchaseDate, serial)
    val returnDays = if (store.contains("amazon", ignoreCase = true)) 30L else 14L
    val returnDeadline = purchaseDate.toEpochMillis()?.let {
        LocalDate.parse(purchaseDate).plusDays(returnDays)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
            .formatInputDate()
    }.orEmpty()

    return ItemDraft(
        name = itemName,
        storeOrBrand = store,
        purchaseDate = purchaseDate,
        returnDeadline = returnDeadline,
        serialNumber = serial
    )
}

private fun inferStore(lines: List<String>): String {
    val knownStores = listOf(
        "Amazon",
        "Best Buy",
        "Target",
        "Walmart",
        "Costco",
        "Apple",
        "Home Depot",
        "Lowe's",
        "Staples",
        "B&H"
    )
    val known = knownStores.firstOrNull { store ->
        lines.any { it.contains(store, ignoreCase = true) }
    }
    if (known != null) return known

    return lines.firstOrNull { line ->
        line.any(Char::isLetter) &&
            !line.contains("receipt", ignoreCase = true) &&
            !line.contains("invoice", ignoreCase = true) &&
            !line.contains("order", ignoreCase = true)
    }.orEmpty()
}

private fun inferDate(lines: List<String>): String {
    val compactDate = Regex("""\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b""")
    val localDate = Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b""")
    val namedFormatters = listOf(
        DateTimeFormatter.ofPattern("MMM d yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
    )

    lines.forEach { line ->
        compactDate.find(line)?.let { match ->
            return safeDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
        }
        localDate.find(line)?.let { match ->
            val year = match.groupValues[3].toInt().let { if (it < 100) 2000 + it else it }
            return safeDate(year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
        }
        namedFormatters.forEach { formatter ->
            try {
                return LocalDate.parse(line.replace(Regex("""\s+"""), " "), formatter).toString()
            } catch (_: DateTimeParseException) {
            }
        }
    }
    return ""
}

private fun safeDate(year: Int, month: Int, day: Int): String {
    return try {
        LocalDate.of(year, month, day).toString()
    } catch (_: RuntimeException) {
        ""
    }
}

private fun inferSerial(lines: List<String>, barcodes: List<String>): String {
    val labeledSerial = Regex("""(?i)\b(?:s/n|sn|serial|serial no|serial number)[:#\s-]*([A-Z0-9-]{6,32})\b""")
    lines.forEach { line ->
        labeledSerial.find(line)?.groupValues?.getOrNull(1)?.let { return it.trim('-') }
    }
    return barcodes.firstOrNull { it.length in 6..32 }.orEmpty()
}

private fun inferItemName(lines: List<String>, store: String, purchaseDate: String, serial: String): String {
    val reject = listOf("receipt", "invoice", "subtotal", "total", "tax", "visa", "mastercard", "change", "cashier")
    return lines.firstOrNull { line ->
        line.length in 5..80 &&
            line.any(Char::isLetter) &&
            !line.equals(store, ignoreCase = true) &&
            !line.contains(purchaseDate) &&
            !line.contains(serial) &&
            reject.none { line.contains(it, ignoreCase = true) }
    }.orEmpty()
}

private fun String.toEpochMillis(): Long? = parseInputDate(this)
