package com.example.utils

import android.util.Log
import java.util.regex.Pattern

data class ParsedPayment(
    val bank: String,
    val amount: Long,
    val reference: String,
    val senderName: String,
    val date: String
)

object SmsParser {
    fun parse(text: String): ParsedPayment? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        var bank = "Other"
        var amount: Long = 0
        var reference = ""
        var senderName = ""
        var date = ""

        // Detect Bank
        val lowerText = trimmed.lowercase()
        if (lowerText.contains("cbe") || lowerText.contains("commercial bank")) {
            bank = "CBE"
        } else if (lowerText.contains("telebirr")) {
            bank = "Telebirr"
        } else if (lowerText.contains("dashen") || lowerText.contains("amole")) {
            bank = "Dashen"
        } else if (lowerText.contains("awash")) {
            bank = "Awash"
        }

        // 1. Amount Extraction (matches ETB 5,000.00, 5,000.00 Birr, 5000 Birr, ETB 1000)
        val textWithoutCommas = trimmed.replace(",", "")

        // Prefer amounts that are annotated with currency words
        val amountPattern = Pattern.compile("(?i)(?:etb|birr|credited with|amount)[:\s]*?(\d+(?:\.\d{1,2})?)|(?i)(\d+(?:\\.\\d{1,2})?)\\s*(?:etb|birr|ebirr)")
        val amountMatcher = amountPattern.matcher(textWithoutCommas)
        if (amountMatcher.find()) {
            val a1 = try { amountMatcher.group(1) } catch (t: Throwable) { null }
            val a2 = try { amountMatcher.group(2) } catch (t: Throwable) { null }
            val amountStr = a1 ?: a2
            if (!amountStr.isNullOrBlank()) {
                val parsed = amountStr.toDoubleOrNull()
                if (parsed != null) {
                    amount = Math.round(parsed)
                }
            }
        } else {
            // Backup simpler digits match but avoid matching phone numbers (9+ digits)
            val digitsPattern = Pattern.compile("\\b(\\d{3,7})\\b")
            val digitsMatcher = digitsPattern.matcher(textWithoutCommas)
            while (digitsMatcher.find()) {
                val candidateStr = digitsMatcher.group(1) ?: continue
                if (candidateStr.length >= 9) continue // likely a phone number
                val candidate = candidateStr.toLongOrNull() ?: 0
                if (candidate in 100..500000) { // realistic contribution limits
                    amount = candidate
                    break
                }
            }
        }

        if (amount == 0L) {
            Log.w("SmsParser", "Could not confidently parse amount from SMS: $trimmed")
        }

        // 2. Reference Number (Matches Ref: FT261858X3N, Ref: CBE928102, Ref MPT2839210)
        val refPattern = Pattern.compile("(?i)\\bref(?:erence)?:?\\s*([a-zA-Z0-9-]+)\\b")
        val refMatcher = refPattern.matcher(trimmed)
        if (refMatcher.find()) {
            reference = refMatcher.group(1) ?: ""
        } else {
            // Try matching general alphanumeric codes typical for transactions (e.g. TX12345678, FT98321...)
            val altRefPattern = Pattern.compile("\\b(FT\\d+[A-Z0-9]+|TX[A-Z0-9]{6,12}|CBE[A-Z0-9]{6,12}|MPT\\d{6,12})\\b")
            val altRefMatcher = altRefPattern.matcher(trimmed)
            if (altRefMatcher.find()) {
                reference = altRefMatcher.group(1) ?: ""
            }
        }

        // 3. Sender Name
        val senderPattern = Pattern.compile("(?i)(?:from|transfer of|deposited by)\\s+([a-zA-Z\\s]{3,30})(?:\\s+to|\\s+on|\\.|\\s+at)")
        val senderMatcher = senderPattern.matcher(trimmed)
        if (senderMatcher.find()) {
            senderName = senderMatcher.group(1)?.trim() ?: ""
        }

        // 4. Date
        val datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-\\w{3}-\\d{4})")
        val dateMatcher = datePattern.matcher(trimmed)
        if (dateMatcher.find()) {
            date = dateMatcher.group(1) ?: ""
        } else {
            date = "Today"
        }

        return ParsedPayment(
            bank = bank,
            amount = amount,
            reference = reference,
            senderName = senderName,
            date = date
        )
    }
}
