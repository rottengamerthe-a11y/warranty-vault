package com.warrantyvault.ocr

import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object OcrDateParser {
    private val monthNames = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12
    )

    /**
     * Parse the first plausible date found in text and return ISO date string `yyyy-MM-dd`, or null.
     */
    fun parseFirstIsoDate(text: String): String? {
        val normalized = text.replace(',', ' ').trim()

        // 1) yyyy-MM-dd or yyyy/MM/dd
        Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").find(normalized)?.let { m ->
            val y = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            val d = m.groupValues[3].toInt()
            return tryFormat(y, mo, d)
        }

        // 2) dd[-/]MM[-/]yyyy (assume day-first for three-part numeric where last part is 4-digit)
        Regex("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})").find(normalized)?.let { m ->
            val d = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            val y = m.groupValues[3].toInt()
            return tryFormat(y, mo, d)
        }

        // 3) Month name patterns like "5 Nov 2024" or "Nov 5 2024" or "November 5 2024"
        Regex("(\\d{1,2})\\s+([A-Za-z]{3,9})\\s+(\\d{4})").find(normalized)?.let { m ->
            val d = m.groupValues[1].toInt()
            val mo = monthNames[m.groupValues[2].lowercase(Locale.US)] ?: return null
            val y = m.groupValues[3].toInt()
            return tryFormat(y, mo, d)
        }

        Regex("([A-Za-z]{3,9})\\s+(\\d{1,2})\\s+(\\d{4})").find(normalized)?.let { m ->
            val mo = monthNames[m.groupValues[1].lowercase(Locale.US)] ?: return null
            val d = m.groupValues[2].toInt()
            val y = m.groupValues[3].toInt()
            return tryFormat(y, mo, d)
        }

        return null
    }

    private fun tryFormat(y: Int, mo: Int, d: Int): String? {
        return try {
            val ld = LocalDate.of(y, mo, d)
            ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeException) {
            null
        }
    }
}
