package com.example.utils

object Validation {
    private val PHONE_REGEX = Regex("^09\\d{8}$|^07\\d{8}$") // standard Ethiopian mobile formats
    private val NAME_REGEX = Regex("^[\\p{L}0-9 .,'-]{1,80}$")
    private const val MAX_AMOUNT = 1_000_000L

    fun isValidPhone(phone: String): Boolean {
        val p = phone.trim()
        return p.isNotEmpty() && PHONE_REGEX.matches(p)
    }

    fun isValidName(name: String): Boolean {
        val n = name.trim()
        return n.length in 1..80 && NAME_REGEX.matches(n)
    }

    fun isValidAmount(amount: Long): Boolean {
        return amount in 1..MAX_AMOUNT
    }

    fun sanitizeText(s: String): String = s.trim()
}
