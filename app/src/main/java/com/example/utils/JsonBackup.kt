package com.example.utils

import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val equbGroup: EqubGroup?,
    val members: List<Member>,
    val installments: List<Installment>,
    val auditLogs: List<AuditLog>,
    val roleSetting: String // "CHAIRMAN" or "CO_CHAIR" from the exported file
)

data class DiffItem(
    val category: String, // "Member", "Payment", "Settings", "Audit Log"
    val changeType: String, // "Added", "Updated", "Deactivated"
    val description: String,
    val amount: Long = 0
)

object JsonBackup {

    fun exportToString(
        equbGroup: EqubGroup?,
        members: List<Member>,
        installments: List<Installment>,
        auditLogs: List<AuditLog>
    ): String {
        val root = JSONObject()
        
        // Export Equb
        if (equbGroup != null) {
            val eqObj = JSONObject()
            eqObj.put("id", equbGroup.id)
            eqObj.put("name", equbGroup.name)
            eqObj.put("contribution", equbGroup.contribution)
            eqObj.put("cycleType", equbGroup.cycleType)
            eqObj.put("startDate", equbGroup.startDate)
            eqObj.put("currentRound", equbGroup.currentRound)
            eqObj.put("currentCycleIndex", equbGroup.currentCycleIndex)
            eqObj.put("roleSetting", equbGroup.roleSetting)
            root.put("equb", eqObj)
        }

        // Export Members
        val membersArr = JSONArray()
        for (m in members) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("name", m.name)
            mObj.put("phone", m.phone)
            mObj.put("isActive", m.isActive)
            mObj.put("payoutRound", m.payoutRound ?: JSONObject.NULL)
            mObj.put("payoutCycleIndex", m.payoutCycleIndex ?: JSONObject.NULL)
            mObj.put("payoutDate", m.payoutDate ?: JSONObject.NULL)
            membersArr.put(mObj)
        }
        root.put("members", membersArr)

        // Export Installments
        val installmentsArr = JSONArray()
        for (i in installments) {
            val iObj = JSONObject()
            iObj.put("id", i.id)
            iObj.put("memberId", i.memberId)
            iObj.put("round", i.round)
            iObj.put("cycleIndex", i.cycleIndex)
            iObj.put("amount", i.amount)
            iObj.put("paymentDate", i.paymentDate)
            iObj.put("paymentMethod", i.paymentMethod)
            iObj.put("referenceNumber", i.referenceNumber)
            iObj.put("remarks", i.remarks)
            iObj.put("isVerified", i.isVerified)
            iObj.put("penaltyAmount", i.penaltyAmount)
            installmentsArr.put(iObj)
        }
        root.put("installments", installmentsArr)

        // Export Audit Logs
        val logsArr = JSONArray()
        for (l in auditLogs) {
            val lObj = JSONObject()
            lObj.put("id", l.id)
            lObj.put("timestamp", l.timestamp)
            lObj.put("action", l.action)
            lObj.put("details", l.details)
            lObj.put("amount", l.amount)
            lObj.put("performedBy", l.performedBy)
            logsArr.put(lObj)
        }
        root.put("auditLogs", logsArr)

        return root.toString(2)
    }

    fun parseBackup(jsonStr: String): BackupData? {
        return try {
            val root = JSONObject(jsonStr)

            // Parse Equb
            var equbGroup: EqubGroup? = null
            if (root.has("equb")) {
                val eqObj = root.getJSONObject("equb")
                equbGroup = EqubGroup(
                    id = eqObj.optInt("id", 1),
                    name = eqObj.optString("name", "Unnamed Equb"),
                    contribution = eqObj.optLong("contribution", 1000),
                    cycleType = eqObj.optString("cycleType", "Monthly"),
                    startDate = eqObj.optString("startDate", "2026-07-04"),
                    currentRound = eqObj.optInt("currentRound", 1),
                    currentCycleIndex = eqObj.optInt("currentCycleIndex", 1),
                    roleSetting = eqObj.optString("roleSetting", "CHAIRMAN")
                )
            }

            // Parse Members
            val membersList = mutableListOf<Member>()
            if (root.has("members")) {
                val arr = root.getJSONArray("members")
                for (idx in 0 until arr.length()) {
                    val mObj = arr.getJSONObject(idx)
                    val pRound = if (mObj.isNull("payoutRound")) null else mObj.getInt("payoutRound")
                    val pCycle = if (mObj.isNull("payoutCycleIndex")) null else mObj.getInt("payoutCycleIndex")
                    val pDate = if (mObj.isNull("payoutDate")) null else mObj.getString("payoutDate")
                    membersList.add(
                        Member(
                            id = mObj.optLong("id"),
                            name = mObj.optString("name", "Unknown"),
                            phone = mObj.optString("phone", ""),
                            isActive = mObj.optBoolean("isActive", true),
                            payoutRound = pRound,
                            payoutCycleIndex = pCycle,
                            payoutDate = pDate
                        )
                    )
                }
            }

            // Parse Installments
            val installmentsList = mutableListOf<Installment>()
            if (root.has("installments")) {
                val arr = root.getJSONArray("installments")
                for (idx in 0 until arr.length()) {
                    val iObj = arr.getJSONObject(idx)
                    installmentsList.add(
                        Installment(
                            id = iObj.optLong("id"),
                            memberId = iObj.optLong("memberId"),
                            round = iObj.optInt("round", 1),
                            cycleIndex = iObj.optInt("cycleIndex", 1),
                            amount = iObj.optLong("amount"),
                            paymentDate = iObj.optString("paymentDate", "Today"),
                            paymentMethod = iObj.optString("paymentMethod", "Cash"),
                            referenceNumber = iObj.optString("referenceNumber", ""),
                            remarks = iObj.optString("remarks", ""),
                            isVerified = iObj.optBoolean("isVerified", false),
                            penaltyAmount = iObj.optLong("penaltyAmount", 0)
                        )
                    )
                }
            }

            // Parse Audit Logs
            val logsList = mutableListOf<AuditLog>()
            if (root.has("auditLogs")) {
                val arr = root.getJSONArray("auditLogs")
                for (idx in 0 until arr.length()) {
                    val lObj = arr.getJSONObject(idx)
                    logsList.add(
                        AuditLog(
                            id = lObj.optLong("id"),
                            timestamp = lObj.optLong("timestamp"),
                            action = lObj.optString("action"),
                            details = lObj.optString("details"),
                            amount = lObj.optLong("amount"),
                            performedBy = lObj.optString("performedBy")
                        )
                    )
                }
            }

            BackupData(
                equbGroup = equbGroup,
                members = membersList,
                installments = installmentsList,
                auditLogs = logsList,
                roleSetting = equbGroup?.roleSetting ?: "CO_CHAIR"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateDiff(local: BackupData, incoming: BackupData): List<DiffItem> {
        val diffList = mutableListOf<DiffItem>()

        // 1. Settings / Equb Info Diff
        val localEq = local.equbGroup
        val incomingEq = incoming.equbGroup
        if (localEq != null && incomingEq != null) {
            if (localEq.name != incomingEq.name) {
                diffList.add(DiffItem("Settings", "Updated", "Equb Name: '${localEq.name}' ➔ '${incomingEq.name}'"))
            }
            if (localEq.contribution != incomingEq.contribution) {
                diffList.add(DiffItem("Settings", "Updated", "Base Contribution: ${localEq.contribution} ETB ➔ ${incomingEq.contribution} ETB"))
            }
            if (localEq.currentRound != incomingEq.currentRound || localEq.currentCycleIndex != incomingEq.currentCycleIndex) {
                diffList.add(DiffItem("Settings", "Updated", "Round/Cycle index: Rd ${localEq.currentRound} Month ${localEq.currentCycleIndex} ➔ Rd ${incomingEq.currentRound} Month ${incomingEq.currentCycleIndex}"))
            }
        }

        // 2. Members Diff
        val localMembersMap = local.members.associateBy { it.id }
        for (incMember in incoming.members) {
            val locMember = localMembersMap[incMember.id]
            if (locMember == null) {
                diffList.add(DiffItem("Member", "Added", "New Member: ${incMember.name} (${incMember.phone})"))
            } else {
                if (locMember.name != incMember.name || locMember.phone != incMember.phone) {
                    diffList.add(DiffItem("Member", "Updated", "Member Info: ${locMember.name} ➔ ${incMember.name}"))
                }
                if (locMember.isActive != incMember.isActive) {
                    val status = if (incMember.isActive) "Active" else "Inactive"
                    diffList.add(DiffItem("Member", "Updated", "Member '${incMember.name}' set to $status"))
                }
                if (locMember.payoutCycleIndex != incMember.payoutCycleIndex || locMember.payoutRound != incMember.payoutRound) {
                    if (incMember.payoutCycleIndex != null) {
                        diffList.add(DiffItem("Member", "Updated", "'${incMember.name}' marked as Winner for Rd ${incMember.payoutRound} Month ${incMember.payoutCycleIndex}"))
                    } else {
                        diffList.add(DiffItem("Member", "Updated", "Cleared winner marker for '${incMember.name}'"))
                    }
                }
            }
        }

        // 3. Installments / Payments Diff
        // Match installments by combination of memberId, round, cycleIndex, paymentDate, referenceNumber to be highly accurate
        val localInstallmentsSet = local.installments.map { 
            "${it.memberId}_${it.round}_${it.cycleIndex}_${it.amount}_${it.referenceNumber}"
        }.toSet()

        for (incIns in incoming.installments) {
            val key = "${incIns.memberId}_${incIns.round}_${incIns.cycleIndex}_${incIns.amount}_${incIns.referenceNumber}"
            if (!localInstallmentsSet.contains(key)) {
                val memberName = incoming.members.find { it.id == incIns.memberId }?.name ?: "Unknown Member"
                val verifiedStr = if (incIns.isVerified) "Verified" else "Unverified (Paid Belief)"
                diffList.add(
                    DiffItem(
                        "Payment", 
                        "Added", 
                        "Payment installment for $memberName: ${incIns.amount} ETB via ${incIns.paymentMethod} ($verifiedStr, Ref: ${incIns.referenceNumber})",
                        incIns.amount
                    )
                )
            } else {
                // Check if verification status changed
                val matchingLocal = local.installments.find { 
                    it.memberId == incIns.memberId && it.round == incIns.round && it.cycleIndex == incIns.cycleIndex && it.amount == incIns.amount && it.referenceNumber == incIns.referenceNumber 
                }
                if (matchingLocal != null && matchingLocal.isVerified != incIns.isVerified) {
                    val memberName = incoming.members.find { it.id == incIns.memberId }?.name ?: "Unknown Member"
                    val state = if (incIns.isVerified) "Verified" else "Unverified (Paid Belief)"
                    diffList.add(DiffItem("Payment", "Updated", "Verification changed for $memberName's ${incIns.amount} ETB to: $state"))
                }
            }
        }

        // 4. Audit Logs Diff
        val localLogCount = local.auditLogs.size
        val incomingLogCount = incoming.auditLogs.size
        if (incomingLogCount > localLogCount) {
            val diffCount = incomingLogCount - localLogCount
            diffList.add(DiffItem("Audit Log", "Added", "Co-organizer performed $diffCount new transaction/auditable actions", 0))
        }

        return diffList
    }
}
