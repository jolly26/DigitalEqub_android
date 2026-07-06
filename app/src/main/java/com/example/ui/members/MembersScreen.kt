package com.example.ui.members

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.EqubViewModel
import com.example.data.*
import com.example.ui.components.DoubleTapOutlinedTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: EqubViewModel,
    equb: EqubGroup,
    members: List<Member>,
    installments: List<Installment>,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(value = false) }
    var showPaymentDialogForMember by remember { mutableStateOf<Member?>(null) }

    val currentRole by viewModel.currentRole.collectAsState()
    val isWriteAuthorized = currentRole != "MEMBER"

    val chairmanId by viewModel.chairmanMemberId.collectAsState()
    val coChairId by viewModel.coChairMemberId.collectAsState()
    
    val filteredMembers = members.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.phone.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("members_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!isWriteAuthorized) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFEF3C7)).border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚠️", fontSize = 16.sp)
                Column {
                    Text(text = "View-Only Audit Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Text(text = "Adding or deleting members, recording payments, and triggering payouts are restricted to the Chairman.", fontSize = 11.sp, color = Color(0xFFB45309), lineHeight = 15.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DoubleTapOutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search members...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.weight(1f).testTag("member_search_input"), shape = RoundedCornerShape(16.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF1E293B), unfocusedTextColor = Color(0xFF1E293B), focusedBorderColor = Color(0xFF4F46E5), unfocusedBorderColor = Color(0xFFE2E8F0), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
            Button(onClick = { showAddDialog = true }, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), disabledContainerColor = Color(0xFFCBD5E1)), shape = RoundedCornerShape(16.dp), modifier = Modifier.height(56.dp).testTag("add_member_button")) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add", fontWeight = FontWeight.Bold) }
            }
        }

        Text(
            text = "Equb Directory (${filteredMembers.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569),
        )

        if (filteredMembers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text(text = if (searchQuery.isEmpty()) "No members added to this Equb yet." else "No matches found.", fontSize = 14.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium) }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).testTag("members_list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredMembers) { member ->
                    MemberRowCard(
                        member = member,
                        equb = equb,
                        installments = installments.asSequence().filter { (it.memberId == member.id) && (it.round == equb.currentRound) && (it.cycleIndex == equb.currentCycleIndex) }.toList(),
                        isChairman = member.id == chairmanId,
                        isCoChair = member.id == coChairId,
                        isWriteAuthorized = isWriteAuthorized,
                        onToggleActive = { viewModel.toggleMemberActive(member) },
                        onDelete = { viewModel.deleteMember(member) },
                        onRecordPayment = { showPaymentDialogForMember = member },
                        onSendReminder = { 
                            val paySum = installments.asSequence().filter { (it.memberId == member.id) && (it.round == equb.currentRound) && (it.cycleIndex == equb.currentCycleIndex) }.sumOf { it.amount }
                            val text = viewModel.generateReminderText(member, equb.contribution, paySum)
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text) }
                            val shareIntent = Intent.createChooser(intent, "Send Reminder via")
                            coroutineScope.launch { viewModel.feedbackMessage.emit("Prepared reminder for ${member.name}") }
                            context.startActivity(shareIntent)
                        }
                    ) {
                        viewModel.setManualWinner(member.id)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var isTeam by remember { mutableStateOf(value = false) }
        var participants by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Add New Equb Member", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(4.dp))
                        DoubleTapOutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isTeam) "Team/Group Name" else "Member Full Name") }, modifier = Modifier.fillMaxWidth().testTag("new_member_name_input"), shape = RoundedCornerShape(12.dp), singleLine = true)
                        DoubleTapOutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.all { char -> char.isDigit() || (char == '+') }) phone = it },
                            label = { Text("Phone Number (e.g. 0911223344)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("new_member_phone_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp).clickable { isTeam = !isTeam }) {
                            Checkbox(checked = isTeam, onCheckedChange = { isTeam = it })
                            Text("This is a Team/Group ticket", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        if (isTeam) {
                            DoubleTapOutlinedTextField(
                                value = participants,
                                onValueChange = { participants = it },
                                label = { Text("Participants (Comma-separated)") },
                                placeholder = { Text("e.g. Abebe, Kebede, Chala") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Text("The contribution will be split equally between participants.", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                    Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Color(0xFF64748B)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (name.isNotBlank()) {
                                    viewModel.addMember(
                                        name,
                                        phone,
                                        isTeam,
                                        if (isTeam) participants.ifBlank { null } else null,
                                    )
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.testTag("save_member_button"),
                        ) {
                            Text("Save Member")
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialogForMember != null) {
        val member = showPaymentDialogForMember!!
        var amountStr by remember { mutableStateOf(equb.contribution.toString()) }
        var method by remember { mutableStateOf("CBE") }
        var reference by remember { mutableStateOf("") }
        var remarks by remember { mutableStateOf("") }
        var senderName by remember { mutableStateOf("") }
        var verifyImmediately by remember(currentRole) { mutableStateOf(currentRole == "CHAIRMAN") }
        Dialog(onDismissRequest = { showPaymentDialogForMember = null }) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp)) {
                        Text(text = "Record Contribution payment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                        Text("Adding payment for ${member.name}", fontSize = 13.sp, color = Color(0xFF64748B))
                        DoubleTapOutlinedTextField(value = amountStr, onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it }, label = { Text("Amount (ETB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"), shape = RoundedCornerShape(12.dp), singleLine = true)
                        
                        Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val methods = listOf("CBE", "Telebirr", "Dashen", "Awash", "Cash", "Other")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { methods.take(3).forEach { mthd -> Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (method == mthd) Color(0xFF4F46E5) else Color(0xFFF1F5F9)).clickable { method = mthd }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(text = mthd, color = if (method == mthd) Color.White else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { methods.drop(3).forEach { mthd -> Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (method == mthd) Color(0xFF4F46E5) else Color(0xFFF1F5F9)).clickable { method = mthd }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(text = mthd, color = if (method == mthd) Color.White else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
                        
                        if (member.isTeam && (member.teamParticipants != null)) {
                            val participants = member.teamParticipants.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                            Text("Select Participant Paying", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                participants.forEach { p ->
                                val pPaid = installments.asSequence().filter { it.senderName?.trim().equals(p, ignoreCase = true) }.sumOf { it.amount }
                                val share = equb.contribution / participants.size
                                    val isPFull = pPaid >= share
                                    
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (senderName == p) Color(0xFF4F46E5) else if (isPFull) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)).clickable { senderName = p }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(text = if (isPFull) "$p (Paid)" else p, color = if (senderName == p) Color.White else if (isPFull) Color(0xFF16A34A) else Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            DoubleTapOutlinedTextField(value = senderName, onValueChange = { senderName = it }, label = { Text("Sender Name (if not ${member.name})") }, placeholder = { Text("e.g. Spouse name, or 'Cash'") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        }

                        DoubleTapOutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text("Reference Number (Optional)") }, modifier = Modifier.fillMaxWidth().testTag("payment_reference_input"), shape = RoundedCornerShape(12.dp), singleLine = true)
                        DoubleTapOutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks (e.g. Late fine paid)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                        if (currentRole == "CHAIRMAN") { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) { Checkbox(checked = verifyImmediately, onCheckedChange = { verifyImmediately = it })
                                Text("Mark payment as verified immediately", fontSize = 12.sp, fontWeight = FontWeight.Medium) } }
                    }
                    Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showPaymentDialogForMember = null }) { Text("Cancel", color = Color(0xFF64748B)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                val amount = amountStr.toLongOrNull() ?: 0
                                if (amount > 0) { 
                                    viewModel.addInstallment(
                                        memberId = member.id, 
                                        amount = amount, 
                                        paymentMethod = method, 
                                        referenceNumber = reference, 
                                        remarks = remarks, 
                                        isVerified = verifyImmediately,
                                        senderName = senderName.ifBlank { null }
                                    )
                                    showPaymentDialogForMember = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.testTag("payment_submit_button")
                        ) {
                            Text("Confirm Payment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberRowCard(
    member: Member,
    equb: EqubGroup,
    installments: List<Installment>,
    isChairman: Boolean = false,
    isCoChair: Boolean = false,
    isWriteAuthorized: Boolean = true,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onRecordPayment: () -> Unit,
    onSendReminder: () -> Unit,
    onPayoutClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(value = false) }
    val totalPaid = installments.sumOf { it.amount }
    
    val isFullyPaid = if (member.isTeam && (member.teamParticipants != null)) {
        val participants = member.teamParticipants.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (participants.isEmpty()) {
            totalPaid >= equb.contribution
        } else {
            val share = equb.contribution / participants.size
            participants.all { p ->
                installments.asSequence().filter { it.senderName?.trim().equals(p, ignoreCase = true) }.sumOf { it.amount } >= share
            }
        }
    } else {
        totalPaid >= equb.contribution
    }
    
    val isPartial = (totalPaid > 0) && !isFullyPaid
    val statusText = when {
        isFullyPaid -> "Paid (ሙሉ የከፈሉ)"
        isPartial -> "Partial (ከፊል)"
        else -> "Unpaid (ምንም ያልከፈሉ)"
    }
    val statusColor = when {
        isFullyPaid -> Color(0xFF16A34A)
        isPartial -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
    val statusBg = when {
        isFullyPaid -> Color(0xFFDCFCE7)
        isPartial -> Color(0xFFFEF3C7)
        else -> Color(0xFFFEE2E2)
    }
    val initials = if (member.name.isNotBlank()) {
        member.name.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
    } else "UN"

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, if (expanded) Color(0xFF4F46E5) else Color(0xFFF1F5F9)), modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.testTag("member_card_${member.id}")) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(if (member.isActive) Color(0xFFEEF2F6) else Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) { Text(text = initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (member.isActive) Color(0xFF4F46E5) else Color(0xFF94A3B8)) }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = member.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (member.isActive) Color(0xFF1E293B) else Color(0xFF94A3B8))
                            if (isChairman) { Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(Color(0xFFE0E7FF)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("CHAIRMAN", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF4F46E5)) } } else if (isCoChair) { Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(Color(0xFFE0F2FE)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("CO-CHAIR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7)) } }
                            if (!member.isActive) { Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(Color(0xFFE2E8F0)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("INACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B)) } }
                        }
                        Text(text = member.phone.ifBlank { "No Phone" }, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(statusBg).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(text = "$statusText ($totalPaid/${equb.contribution})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = statusColor) }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    if (member.isTeam && (member.teamParticipants != null)) {
                        val participants = member.teamParticipants.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                        if (participants.isNotEmpty()) {
                            val share = equb.contribution / participants.size
                            Text("Team Progress (Share: $share ETB each)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                            participants.forEach { p ->
                                val pPaid = installments.asSequence().filter { it.senderName?.trim().equals(p, ignoreCase = true) }.sumOf { it.amount }
                                val isPFull = pPaid >= share
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• $p", fontSize = 12.sp, color = if (isPFull) Color(0xFF16A34A) else Color(0xFF334155))
                                    Text(if (isPFull) "Paid" else "Due: ${share - pPaid}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isPFull) Color(0xFF16A34A) else Color(0xFFDC2626))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }

                    if (installments.isNotEmpty()) {
                        Text("Cycle Installments list:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        installments.forEach { inst ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "• ${inst.amount} ETB (${inst.paymentMethod})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                                Text(text = if (inst.isVerified) "Verified" else "Awaiting Verification", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (inst.isVerified) Color(0xFF16A34A) else Color(0xFF3B82F6))
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRecordPayment, enabled = isWriteAuthorized && !isFullyPaid, colors = ButtonDefaults.buttonColors(containerColor = if (isFullyPaid) Color(0xFF10B981) else Color(0xFF4F46E5), disabledContainerColor = Color(0xFFCBD5E1)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(38.dp).testTag("record_pay_btn_${member.id}")) { Text(text = if (isFullyPaid) "Paid Full" else "Record Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        if (!isFullyPaid) { OutlinedButton(onClick = onSendReminder, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFF64748B)), modifier = Modifier.weight(1f).height(38.dp).testTag("remind_btn_${member.id}")) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
                        if (member.payoutCycleIndex == null) { Button(onClick = onPayoutClick, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669), disabledContainerColor = Color(0xFFCBD5E1)), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1.5f).height(38.dp).testTag("payout_btn_${member.id}")) { Text("Set Winner (ዕጣ)", fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onToggleActive, enabled = isWriteAuthorized, modifier = Modifier.testTag("toggle_active_btn_${member.id}")) { Text(text = if (member.isActive) "Deactivate Member" else "Activate Member", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isWriteAuthorized) Color(0xFF475569) else Color(0xFF94A3B8)) }
                        IconButton(onClick = onDelete, enabled = isWriteAuthorized, modifier = Modifier.testTag("delete_member_btn_${member.id}")) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete member", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }
}
