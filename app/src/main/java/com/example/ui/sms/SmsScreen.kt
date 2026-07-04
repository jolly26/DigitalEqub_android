package com.example.ui.sms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EqubViewModel
import com.example.data.*
import com.example.ui.components.DoubleTapOutlinedTextField

@Composable
fun SmsScreen(
    viewModel: EqubViewModel,
    members: List<Member>,
    equb: EqubGroup,
    installments: List<Installment>
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val isWriteAuthorized = currentRole != "MEMBER"

    val smsText by viewModel.smsInputText.collectAsState()
    val proposal by viewModel.parsedPaymentProposal.collectAsState()
    val searchQuery by viewModel.smsSearchQuery.collectAsState()

    val currentRound = equb.currentRound
    val currentCycle = equb.currentCycleIndex

    val activeMembers = members.filter { it.isActive }
    
    val filteredMembers = activeMembers.filter { member ->
        val memberInstallments = installments.filter { 
            it.memberId == member.id && it.round == currentRound && it.cycleIndex == currentCycle 
        }
        val totalPaid = memberInstallments.sumOf { it.amount }
        val isNotFullyPaid = totalPaid < equb.contribution

        isNotFullyPaid && (
            member.name.contains(searchQuery, ignoreCase = true) || 
            member.phone.contains(searchQuery, ignoreCase = true)
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("sms_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!isWriteAuthorized) {
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFEF3C7)).border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚠️", fontSize = 14.sp)
                    Text(text = "View-Only Mode: SMS parsing and payment linking is restricted to the Chairman and Co-Chair.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309), lineHeight = 16.sp)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Paste Banking SMS Notification 📩", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = "Avoid manual entry errors. Paste transaction text from CBE, Telebirr, Dashen, or Awash bank below, and the app will parse the references, amount, and dates automatically.", fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 16.sp)
                    DoubleTapOutlinedTextField(value = smsText, onValueChange = { viewModel.parseSms(it) }, placeholder = { Text("Paste bank transaction SMS here...") }, enabled = isWriteAuthorized, modifier = Modifier.fillMaxWidth().height(110.dp).testTag("sms_text_paste_input"), shape = RoundedCornerShape(12.dp))
                }
            }
        }

        val proposalVal = proposal
        if (proposalVal != null) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2F6)), border = BorderStroke(1.dp, Color(0xFF6366F1)), modifier = Modifier.testTag("parsed_proposal_card")) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Parsed Bank Details Successfully!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                            IconButton(onClick = { viewModel.discardSmsProposal() }) { Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color(0xFFEF4444)) }
                        }
                        HorizontalDivider(color = Color(0xFFCBD5E1))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ParsedFieldBox(label = "Bank Source", value = proposalVal.bank, modifier = Modifier.weight(1f))
                            ParsedFieldBox(label = "Amount Detected", value = "${proposalVal.amount} ETB", modifier = Modifier.weight(1.2f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ParsedFieldBox(label = "Reference Key", value = proposalVal.reference.ifBlank { "N/A" }, modifier = Modifier.weight(1.5f))
                            ParsedFieldBox(label = "SMS Date", value = proposalVal.date, modifier = Modifier.weight(1f))
                        }
                        if (proposalVal.senderName.isNotBlank()) { ParsedFieldBox(label = "Discovered Sender", value = proposalVal.senderName, modifier = Modifier.fillMaxWidth()) }
                        HorizontalDivider(color = Color(0xFFCBD5E1))
                        Text(text = "Step 2: Connect this Payment to a Member", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        DoubleTapOutlinedTextField(value = searchQuery, onValueChange = { viewModel.smsSearchQuery.value = it }, placeholder = { Text("Search member to link...") }, enabled = isWriteAuthorized, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth().testTag("sms_member_search_input"), shape = RoundedCornerShape(12.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF1E293B), unfocusedTextColor = Color(0xFF1E293B), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
                        if (filteredMembers.isEmpty()) { Text("No matching active members.", fontSize = 11.sp, color = Color(0xFFEF4444)) } else {
                            Text("Select member to deposit funds to:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredMembers.forEach { member ->
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isWriteAuthorized) Color.White else Color(0xFFF1F5F9)).clickable(enabled = isWriteAuthorized) { viewModel.applySmsProposal(member.id) }.padding(10.dp).testTag("sms_link_member_${member.id}")) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(member.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            if (isWriteAuthorized) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParsedFieldBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(10.dp)) {
        Column {
            Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}
