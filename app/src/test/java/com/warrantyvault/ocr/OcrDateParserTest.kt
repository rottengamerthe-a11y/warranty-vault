package com.warrantyvault.ocr

import org.junit.Test
import kotlin.test.assertEquals

class OcrDateParserTest {
    @Test
    fun parseSimpleDate() {
        val input = "2024-11-05"
        val out = OcrDateParser.parseFirstIsoDate(input)
        assertEquals("2024-11-05", out)
    }

    @Test
    fun parseSlashedDate() {
        val input = "05/11/2024"
        val out = OcrDateParser.parseFirstIsoDate(input)
        assertEquals("2024-11-05", out)
    }

    @Test
    fun parseMonthName() {
        val input = "Nov 5 2024"
        val out = OcrDateParser.parseFirstIsoDate(input)
        assertEquals("2024-11-05", out)
    }
}
