package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {
    @Test
    fun parse_standard_sms_with_ref_and_amount() {
        val sms = "Account credited with ETB 5,000.00 Ref: FT261858X3N from John Doe on 2026-07-05"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(5000L, parsed!!.amount)
        assertEquals("FT261858X3N", parsed.reference)
        assertEquals("John Doe", parsed.senderName)
        assertEquals("2026-07-05", parsed.date)
    }

    @Test
    fun parse_sms_without_currency_but_digits() {
        val sms = "You have received 2500 Birr from Alice. Ref: TXABC123456 on 05/07/2026"
        val parsed = SmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(2500L, parsed!!.amount)
        assertEquals("TXABC123456", parsed.reference)
    }
}
