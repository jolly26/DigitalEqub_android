package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EqubViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EqubRepository = EqubRepository(AppDatabase.getDatabase(application).equbDao())
    private val prefs = application.getSharedPreferences("equb_security_prefs", android.content.Context.MODE_PRIVATE)

    // Security & Role Mapping States
    val chairmanMemberId = MutableStateFlow(prefs.getLong("chairman_member_id", -1L))
    val coChairMemberId = MutableStateFlow(prefs.getLong("co_chair_member_id", -1L))
    val chairmanPin = MutableStateFlow(prefs.getString("chairman_pin", "1234") ?: "1234")
    val coChairPin = MutableStateFlow(prefs.getString("co_chair_pin", "5678") ?: "5678")

    // Database Flows
    val equbGroup = repository.equbGroup.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val members = repository.members.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val installments = repository.installments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    val activeTab = MutableStateFlow("home") // "home", "members", "sms", "reports", "backups"
    val selectedMemberForDetail = MutableStateFlow<Member?>(null)
    
    // SMS Parsing State
    val smsInputText = MutableStateFlow("")
    val parsedPaymentProposal = MutableStateFlow<ParsedPayment?>(null)
    val smsSearchQuery = MutableStateFlow("")

    // Sync / Backup State
    val importedJsonString = MutableStateFlow("")
    val pendingBackupData = MutableStateFlow<BackupData?>(null)
    val importDiffList = MutableStateFlow<List<DiffItem>>(emptyList())
    val currentRole = MutableStateFlow("CHAIRMAN") // "CHAIRMAN" or "CO_CHAIR"

    // Toast/Feedback state
    val feedbackMessage = MutableSharedFlow<String>()

    init {
        // Keep currentRole in sync with EqubGroup's roleSetting
        viewModelScope.launch {
            equbGroup.collect { eq ->
                eq?.let { currentRole.value = it.roleSetting }
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun toOrdinal(n: Int): String {
        if (n <= 0) return n.toString()
        val suffixes = arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
        return when (n % 100) {
            11, 12, 13 -> "${n}th"
            else -> "$n${suffixes[n % 10]}"
        }
    }

    private fun logAction(action: String, details: String, amount: Long = 0) {
        viewModelScope.launch {
            val role = currentRole.value
            repository.insertAuditLog(
                AuditLog(
                    timestamp = System.currentTimeMillis(),
                    action = action,
                    details = details,
                    amount = amount,
                    performedBy = role,
                ),
            )
        }
    }

    fun verifyAndSetRole(role: String, pin: String): Boolean {
        if (role == "MEMBER") {
            viewModelScope.launch {
                repository.updateRole(role)
                currentRole.value = role
                logAction("SET_ROLE", "User set role to Regular MEMBER")
                feedbackMessage.emit("Role switched to Regular Member (View-Only)")
            }
            return true
        }

        val requiredPin = if (role == "CHAIRMAN") chairmanPin.value else coChairPin.value
        if (pin == requiredPin) {
            viewModelScope.launch {
                repository.updateRole(role)
                currentRole.value = role
                logAction("SET_ROLE", "User authenticated and set role to $role")
                feedbackMessage.emit("Welcome! Authenticated successfully as $role")
            }
            return true
        }
        return false
    }

    fun updateSecuritySettings(chairId: Long, coChairId: Long, chairPinStr: String, coChairPinStr: String) {
        viewModelScope.launch {
            if ((chairPinStr.length < 4) || (coChairPinStr.length < 4)) {
                feedbackMessage.emit("PINs must be at least 4 digits")
                return@launch
            }
            prefs.edit().apply {
                putLong("chairman_member_id", chairId)
                putLong("co_chair_member_id", coChairId)
                putString("chairman_pin", chairPinStr)
                putString("co_chair_pin", coChairPinStr)
                apply()
            }
            chairmanMemberId.value = chairId
            coChairMemberId.value = coChairId
            chairmanPin.value = chairPinStr
            coChairPin.value = coChairPinStr
            logAction("SECURITY_UPDATE", "Updated role mappings and security PINs")
            feedbackMessage.emit("Role mappings and security PINs updated!")
        }
    }

    // Create / Setup Equb Group
    fun setupEqub(name: String, contribution: Long, cycleType: String, startDate: String, autoDraw: Boolean = true) {
        viewModelScope.launch {
            val existing = repository.getEqubGroup()
            val newGroup = EqubGroup(
                id = 1,
                name = name,
                contribution = contribution,
                cycleType = cycleType,
                startDate = startDate,
                currentRound = existing?.currentRound ?: 1,
                currentCycleIndex = existing?.currentCycleIndex ?: 1,
                roleSetting = existing?.roleSetting ?: "CHAIRMAN",
                autoDrawEnabled = autoDraw,
            )
            repository.insertOrUpdateEqubGroup(newGroup)
            logAction("CREATE_EQUB", "Equb setup/updated: $name ($cycleType, contribution: $contribution ETB, auto-draw: $autoDraw)", contribution)
            feedbackMessage.emit("Equb '$name' setup successfully")
        }
    }

    // Member Management
    fun addMember(name: String, phone: String, isTeam: Boolean = false, participants: String? = null) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedPhone = phone.trim()

            if (trimmedName.isBlank()) {
                feedbackMessage.emit("Validation Error: Member name cannot be empty")
                return@launch
            }

            // Ethiopian Phone Validation: Should be 10 digits (e.g. 0911223344)
            if (!trimmedPhone.all { it.isDigit() } || trimmedPhone.length != 10) {
                feedbackMessage.emit("Validation Error: Phone number must be exactly 10 digits (e.g. 0911223344)")
                return@launch
            }

            // Check for duplicate phone or name
            val allMembers = repository.getAllMembers()
            if (allMembers.any { it.phone == trimmedPhone }) {
                feedbackMessage.emit("Validation Error: A member with phone number $trimmedPhone already exists")
                return@launch
            }
            if (allMembers.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                feedbackMessage.emit("Validation Error: A member named '$trimmedName' already exists")
                return@launch
            }

            val member = Member(
                name = trimmedName,
                phone = trimmedPhone,
                isTeam = isTeam,
                teamParticipants = participants?.trim(),
            )
            val id = repository.insertMember(member)
            val logDetails = if (isTeam) {
                "Added Team: ${member.name} (Participants: $participants)"
            } else {
                "Added member: ${member.name} (Phone: ${member.phone}), ID: $id"
            }
            logAction("ADD_MEMBER", logDetails)
            feedbackMessage.emit("Added ${if (isTeam) "Team" else "Member"}: ${member.name}")
        }
    }

    fun toggleMemberActive(member: Member) {
        viewModelScope.launch {
            val updated = member.copy(isActive = !member.isActive)
            repository.updateMember(updated)
            val status = if (updated.isActive) "Active" else "Inactive"
            logAction("MEMBER_TOGGLE_ACTIVE", "Toggled member '${member.name}' to $status")
            feedbackMessage.emit("Member '${member.name}' is now $status")
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member.id)
            logAction("DELETE_MEMBER", "Deleted member: ${member.name}")
            feedbackMessage.emit("Deleted member '${member.name}'")
            if (selectedMemberForDetail.value?.id == member.id) {
                selectedMemberForDetail.value = null
            }
        }
    }

    // Installments / Payments
    fun addInstallment(
        memberId: Long,
        amount: Long,
        paymentMethod: String,
        referenceNumber: String,
        remarks: String,
        isVerified: Boolean = false,
        penaltyAmount: Long = 0,
        senderName: String? = null,
    ) {
        viewModelScope.launch {
            val member = repository.getMemberById(memberId)
            val memberName = member?.name ?: "Unknown"
            val eq = repository.getEqubGroup() ?: return@launch
            
            // Prevent double payment if already fully paid
            val currentInsts = repository.getInstallmentsForMember(memberId, eq.currentRound, eq.currentCycleIndex)
            
            if (isMemberFullyPaid(member!!, currentInsts, eq.contribution)) {
                 feedbackMessage.emit("Payment Rejected: $memberName has already fully paid for this cycle.")
                 return@launch
            }

            val contributionCount = currentInsts.size + 1
            val installment = Installment(
                memberId = memberId,
                round = eq.currentRound,
                cycleIndex = eq.currentCycleIndex,
                amount = amount,
                paymentDate = getCurrentTimestamp(),
                paymentMethod = paymentMethod,
                referenceNumber = referenceNumber.trim(),
                remarks = remarks.trim(),
                isVerified = isVerified,
                penaltyAmount = penaltyAmount,
                senderName = senderName?.trim(),
            )
            
            repository.insertInstallment(installment)
            val stateStr = if (isVerified) "Verified" else "Unverified (Paid Belief)"
            val logDetails = if (senderName.isNullOrBlank()) {
                "Recorded payment of $amount ETB for $memberName in Rd ${eq.currentRound} Month ${eq.currentCycleIndex} ($stateStr, Ref: $referenceNumber)"
            } else {
                "Recorded payment of $amount ETB for $memberName (via $senderName) in Rd ${eq.currentRound} Month ${eq.currentCycleIndex} ($stateStr, Ref: $referenceNumber)"
            }
            logAction(
                "ADD_INSTALLMENT", 
                logDetails,
                amount,
            )
            val ordinalStr = toOrdinal(contributionCount)
            feedbackMessage.emit("Selam $memberName, you've made your $ordinalStr contribution, thank you!")
        }
    }

    fun isMemberFullyPaid(member: Member, installments: List<Installment>, contribution: Long): Boolean {
        val memberInsts = installments.filter { it.memberId == member.id }
        val totalPaid = memberInsts.sumOf { it.amount }
        
        return if (member.isTeam && (member.teamParticipants != null)) {
            val participants = member.teamParticipants.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            if (participants.isEmpty()) {
                totalPaid >= contribution
            } else {
                val share = contribution / participants.size
                participants.all { p ->
                    memberInsts.asSequence().filter { it.senderName?.trim().equals(p, ignoreCase = true) }.sumOf { it.amount } >= share
                }
            }
        } else {
            totalPaid >= contribution
        }
    }

    fun verifyInstallment(installmentId: Long, isVerified: Boolean) {
        viewModelScope.launch {
            repository.updateInstallmentVerification(installmentId, isVerified)
            // Query installment to log member name
            val instList = repository.getAllInstallments()
            val inst = instList.find { it.id == installmentId }
            if (inst != null) {
                val memberName = repository.getMemberById(inst.memberId)?.name ?: "Unknown"
                val state = if (isVerified) "Verified" else "Unverified"
                logAction("VERIFY_PAYMENT", "Set installment ID $installmentId ($memberName, ${inst.amount} ETB) to $state", inst.amount)
                feedbackMessage.emit("Payment marked as $state")
                
                if (isVerified) {
                    checkAndTransitionCycle()
                }
            }
        }
    }

    private suspend fun checkAndTransitionCycle() {
        val eq = repository.getEqubGroup() ?: return
        val allMembers = repository.getAllMembers()
        val activeMembers = allMembers.filter { it.isActive }
        if (activeMembers.isEmpty()) return

        val installments = repository.getAllInstallments()
        val currentRound = eq.currentRound
        val currentCycle = eq.currentCycleIndex

        // Check if everyone has paid fully and all their installments for this cycle are verified
        var allMembersPaidAndVerified = true
        for (m in activeMembers) {
            val memberInsts = installments.filter { 
                (it.memberId == m.id) && (it.round == currentRound) && (it.cycleIndex == currentCycle)
            }
            val fullyPaid = isMemberFullyPaid(m, memberInsts, eq.contribution)
            val allVerified = memberInsts.all { it.isVerified }
            
            if (!fullyPaid || !allVerified || memberInsts.isEmpty()) {
                allMembersPaidAndVerified = false
                break
            }
        }

        if (allMembersPaidAndVerified) {
            val currentWinner = allMembers.find { (it.payoutRound == currentRound) && (it.payoutCycleIndex == currentCycle) }
            
            if (eq.autoDrawEnabled) {
                var winnerName = currentWinner?.name
                if (currentWinner == null) {
                    val eligible = activeMembers.filter { (it.payoutRound != currentRound) || (it.payoutCycleIndex == null) }
                    if (eligible.isNotEmpty()) {
                        val newWinner = eligible.random()
                        winnerName = newWinner.name
                        repository.setMemberPayout(newWinner.id, currentRound, currentCycle, getCurrentTimestamp())
                        logAction("AUTO_DRAW_WINNER", "Cycle complete! Automated Lottery Drawn (ዕጣ): $winnerName", eq.contribution)
                    }
                }
                
                feedbackMessage.emit("Cycle $currentCycle Complete! All payments verified. Winner: ${winnerName ?: "Unknown"}")
                proceedToNextCycle(eq, allMembers, activeMembers, currentRound, currentCycle)
            } else {
                // Manual Draw mode
                if (currentWinner == null) {
                    feedbackMessage.emit("Cycle $currentCycle: All payments verified! Please set a winner manually to advance to the next cycle.")
                } else {
                    feedbackMessage.emit("Cycle $currentCycle complete! Winner ${currentWinner.name} recorded. Advancing...")
                    proceedToNextCycle(eq, allMembers, activeMembers, currentRound, currentCycle)
                }
            }
        }
    }

    private suspend fun proceedToNextCycle(
        eq: EqubGroup,
        allMembers: List<Member>,
        activeMembers: List<Member>,
        currentRound: Int,
        currentCycle: Int,
    ) {
        val stillEligible = activeMembers.count { (it.payoutRound != currentRound) || (it.payoutCycleIndex == null) }
        if (stillEligible == 0) {
            // Round complete
            val nextRound = currentRound + 1
            repository.updateRoundAndCycle(nextRound, 1)
            for (m in allMembers) {
                repository.setMemberPayout(m.id, null, null, null)
            }
            logAction("AUTO_ARCHIVE_ROUND", "Round $currentRound complete! Automatically started Round $nextRound")
            feedbackMessage.emit("Round $currentRound finished! Archive generated. Starting Round $nextRound.")
        } else {
            val nextCycle = currentCycle + 1
            repository.updateCurrentCycle(nextCycle)
            val cycleLabel = when (eq.cycleType) {
                "Weekly" -> "Week"
                "Bi-weekly" -> "Bi-week"
                else -> "Month"
            }
            logAction("AUTO_ADVANCE_CYCLE", "Cycle complete! Automatically advanced to $cycleLabel $nextCycle")
            feedbackMessage.emit("Moving to $cycleLabel $nextCycle...")
        }
    }

    fun deleteInstallment(installmentId: Long) {
        viewModelScope.launch {
            val instList = repository.getAllInstallments()
            val inst = instList.find { it.id == installmentId }
            if (inst != null) {
                val memberName = repository.getMemberById(inst.memberId)?.name ?: "Unknown"
                repository.deleteInstallment(installmentId)
                logAction("DELETE_INSTALLMENT", "Deleted payment of ${inst.amount} ETB for $memberName", inst.amount)
                feedbackMessage.emit("Deleted payment of ${inst.amount} ETB")
            }
        }
    }

    // Lottery Payout Draw (ዕጣ)
    fun drawLotteryWinner() {
        viewModelScope.launch {
            val eq = repository.getEqubGroup()
            if (eq == null) {
                feedbackMessage.emit("Please setup the Equb first")
                return@launch
            }

            val allMembers = repository.getAllMembers()
            val activeMembers = allMembers.filter { it.isActive }
            if (activeMembers.isEmpty()) {
                feedbackMessage.emit("No active members in the Equb")
                return@launch
            }

            // Find members who have not yet received payout in this current round
            val eligible = activeMembers.filter { (it.payoutRound != eq.currentRound) || (it.payoutCycleIndex == null) }
            if (eligible.isEmpty()) {
                feedbackMessage.emit("All members have received payout for Round ${eq.currentRound}!")
                return@launch
            }

            // Draw a random winner!
            val winner = eligible.random()
            val payoutDate = getCurrentTimestamp()
            
            repository.setMemberPayout(winner.id, eq.currentRound, eq.currentCycleIndex, payoutDate)
            logAction("DRAW_WINNER", "Lottery Drawn (ዕጣ): ${winner.name} selected as payout winner for Rd ${eq.currentRound} Month ${eq.currentCycleIndex}", eq.contribution)
            feedbackMessage.emit("Winner: ${winner.name}! Pot of ${(activeMembers.size * eq.contribution)} ETB goes to them!")
        }
    }

    fun setManualWinner(memberId: Long) {
        viewModelScope.launch {
            val eq = repository.getEqubGroup()
            if (eq == null) {
                feedbackMessage.emit("Please setup the Equb first")
                return@launch
            }

            val winner = repository.getMemberById(memberId)
            if (winner == null) {
                feedbackMessage.emit("Member not found")
                return@launch
            }

            if (winner.payoutRound == eq.currentRound && winner.payoutCycleIndex != null) {
                feedbackMessage.emit("Rejected: ${winner.name} has already received a payout in Round ${eq.currentRound}.")
                return@launch
            }

            val payoutDate = getCurrentTimestamp()
            repository.setMemberPayout(winner.id, eq.currentRound, eq.currentCycleIndex, payoutDate)
            logAction("DRAW_WINNER", "Winner manually set: ${winner.name} for Rd ${eq.currentRound} Month ${eq.currentCycleIndex}", eq.contribution)
            feedbackMessage.emit("Winner set: ${winner.name}")
            
            checkAndTransitionCycle()
        }
    }

    fun clearWinnerForCurrentCycle() {
        viewModelScope.launch {
            val eq = repository.getEqubGroup() ?: return@launch
            val allMembers = repository.getAllMembers()
            val currentWinner = allMembers.find { (it.payoutRound == eq.currentRound) && (it.payoutCycleIndex == eq.currentCycleIndex) }
            if (currentWinner != null) {
                repository.setMemberPayout(currentWinner.id, null, null, null)
                logAction("CLEAR_WINNER", "Cleared winner marker for ${currentWinner.name} in Rd ${eq.currentRound} Month ${eq.currentCycleIndex}")
                feedbackMessage.emit("Cleared winner for current month")
            }
        }
    }

    // Cycle Advancement
    fun advanceCycle() {
        viewModelScope.launch {
            val eq = repository.getEqubGroup() ?: return@launch
            val nextCycle = eq.currentCycleIndex + 1
            repository.updateCurrentCycle(nextCycle)
            logAction("ADVANCE_CYCLE", "Advanced Equb cycle index to Month $nextCycle")
            feedbackMessage.emit("Advanced to Month $nextCycle")
        }
    }

    fun decreaseCycle() {
        viewModelScope.launch {
            val eq = repository.getEqubGroup() ?: return@launch
            if (eq.currentCycleIndex > 1) {
                val prevCycle = eq.currentCycleIndex - 1
                repository.updateCurrentCycle(prevCycle)
                logAction("DECREASE_CYCLE", "Moved back Equb cycle index to Month $prevCycle")
                feedbackMessage.emit("Returned to Month $prevCycle")
            }
        }
    }

    // Archiving & Round Reset
    fun archiveAndStartNextRound() {
        viewModelScope.launch {
            val eq = repository.getEqubGroup() ?: return@launch
            val allMembers = repository.getAllMembers()
            val activeMembers = allMembers.filter { it.isActive }
            
            // Check if everyone has won in this round
            val winnersCount = activeMembers.count { (it.payoutRound == eq.currentRound) && (it.payoutCycleIndex != null) }
            if (winnersCount < activeMembers.size) {
                feedbackMessage.emit("Cannot complete Round: Only $winnersCount of ${activeMembers.size} members have received payouts.")
                return@launch
            }

            val nextRound = eq.currentRound + 1
            repository.updateRoundAndCycle(nextRound, 1)
            
            for (m in allMembers) {
                repository.setMemberPayout(m.id, null, null, null)
            }

            logAction("ARCHIVE_ROUND", "Completed Round ${eq.currentRound}! Archived and started Round $nextRound, Month 1")
            feedbackMessage.emit("Round ${eq.currentRound} Completed! Starting Round $nextRound, Month 1!")
        }
    }

    // SMS Parsing logic
    fun parseSms(text: String) {
        smsInputText.value = text
        val result = SmsParser.parse(text)
        parsedPaymentProposal.value = result
        if ((result == null) && text.isNotBlank()) {
            viewModelScope.launch {
                feedbackMessage.emit("Could not parse. Please try a different SMS format or enter manually.")
            }
        }
    }

    fun applySmsProposal(memberId: Long) {
        val proposal = parsedPaymentProposal.value ?: return
        addInstallment(
            memberId = memberId,
            amount = proposal.amount,
            paymentMethod = proposal.bank,
            referenceNumber = proposal.reference,
            remarks = "SMS Autoparse (${proposal.date})",
            isVerified = false, // Keep unverified until final checked (always safe)
            senderName = proposal.senderName.ifBlank { null },
        )
        parsedPaymentProposal.value = null
        smsInputText.value = ""
    }

    fun discardSmsProposal() {
        parsedPaymentProposal.value = null
        smsInputText.value = ""
    }

    // Import Co-Organizer Backup
    fun analyzeBackup(jsonStr: String) {
        importedJsonString.value = jsonStr
        viewModelScope.launch {
            val incoming = JsonBackup.parseBackup(jsonStr)
            if (incoming == null) {
                feedbackMessage.emit("Invalid JSON backup file.")
                return@launch
            }

            // Get local state
            val localEq = repository.getEqubGroup()
            val localMembers = repository.getAllMembers()
            val localInstallments = repository.getAllInstallments()
            val localLogs = repository.getAllAuditLogs()

            val localBackup = BackupData(
                equbGroup = localEq,
                members = localMembers,
                installments = localInstallments,
                auditLogs = localLogs,
                roleSetting = currentRole.value,
            )

            val diff = JsonBackup.calculateDiff(localBackup, incoming)
            importDiffList.value = diff
            pendingBackupData.value = incoming

            if (diff.isEmpty()) {
                feedbackMessage.emit("Co-organizer's backup matches your local data exactly. No changes to import!")
            } else {
                feedbackMessage.emit("Detected ${diff.size} changes! Please review them below.")
            }
        }
    }

    fun confirmSyncImport() {
        val incoming = pendingBackupData.value ?: return
        viewModelScope.launch {
            repository.syncDatabase(incoming, currentRole.value)

            val changeCount = importDiffList.value.size
            logAction("SYNC_IMPORT", "Imported co-organizer sync backup with $changeCount detected differences", 0)

            pendingBackupData.value = null
            importDiffList.value = emptyList()
            importedJsonString.value = ""

            feedbackMessage.emit("Database synchronized successfully!")
        }
    }

    fun discardSyncProposal() {
        pendingBackupData.value = null
        importDiffList.value = emptyList()
        importedJsonString.value = ""
    }

    // Export JSON string
    suspend fun getExportString(): String {
        val eq = repository.getEqubGroup()
        val mList = repository.getAllMembers()
        val iList = repository.getAllInstallments()
        val lList = repository.getAllAuditLogs()
        return JsonBackup.exportToString(eq, mList, iList, lList)
    }

    // Generate Shareable SMS/WhatsApp text for reminders
    fun generateReminderText(member: Member, expectedAmount: Long, paidAmount: Long): String {
        val equbName = equbGroup.value?.name ?: "Equb"
        val pending = expectedAmount - paidAmount
        return "Selam ${member.name}, this is a friendly reminder from $equbName organizer. Your contribution for this month is $expectedAmount ETB, you have paid $paidAmount ETB so far. Pending balance of $pending ETB is due. Please deposit to our bank account. Thank you!"
    }

    // Generate Monthly Summary text for sharing on Telegram / WhatsApp
    fun generateSummaryReportText(
        totalExpected: Long,
        totalCollected: Long,
        totalPending: Long,
        paidCount: Int,
        totalCount: Int,
        winnerName: String?,
    ): String {
        val eq = equbGroup.value ?: return "No Equb Active"
        val progressPercent = if (totalExpected > 0) ((totalCollected * 100) / totalExpected) else 0
        return """
            --- 🇪🇹 ${eq.name} ---
            Round: ${eq.currentRound} | Cycle Month: ${eq.currentCycleIndex}
            ------------------------------------
            • Total Members: $totalCount
            • Contribution: ${eq.contribution} ETB
            • Progress: $paidCount/$totalCount Paid ($progressPercent%)
            ------------------------------------
            • Expected Total: $totalExpected ETB
            • Collected Total: $totalCollected ETB
            • Pending Total: $totalPending ETB
            ------------------------------------
            • Monthly Winner (ዕጣ): ${winnerName ?: "Not Drawn Yet"}
            ------------------------------------
            Generated offline using Equb Manager.
        """.trimIndent()
    }
}
