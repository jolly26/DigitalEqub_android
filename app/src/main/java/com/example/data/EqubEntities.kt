package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equb_groups")
data class EqubGroup(
    @PrimaryKey val id: Int = 1, // Only managing 1 Equb at a time as requested, but structured with ID
    val name: String,
    val contribution: Long, // in ETB (whole numbers, no decimals beyond cents)
    val cycleType: String, // "Weekly", "Bi-weekly", "Monthly"
    val startDate: String, // YYYY-MM-DD
    val currentRound: Int = 1,
    val currentCycleIndex: Int = 1,
    val roleSetting: String = "CHAIRMAN" // "CHAIRMAN" or "CO_CHAIR"
)

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val isActive: Boolean = true,
    // Payout fields for the current round
    val payoutRound: Int? = null,
    val payoutCycleIndex: Int? = null,
    val payoutDate: String? = null
)

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val round: Int,
    val cycleIndex: Int,
    val amount: Long,
    val paymentDate: String, // YYYY-MM-DD HH:MM
    val paymentMethod: String, // "CBE", "Telebirr", "Dashen", "Awash", "Cash", "Other"
    val referenceNumber: String,
    val remarks: String,
    val isVerified: Boolean = false, // false = paid (belief), true = verified (cross-checked)
    val penaltyAmount: Long = 0,
    val senderName: String? = null // For third-party payments (e.g., wife's name or cash source)
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val action: String, // "CREATE_EQUB", "ADD_MEMBER", "ADD_INSTALLMENT", "VERIFY_PAYMENT", "DRAW_WINNER", "SYNC_IMPORT", "MEMBER_TOGGLE_ACTIVE", "SET_ROLE"
    val details: String,
    val amount: Long = 0,
    val performedBy: String // "CHAIRMAN" or "CO_CHAIR"
)
