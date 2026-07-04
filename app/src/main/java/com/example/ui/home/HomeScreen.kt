package com.example.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EqubViewModel
import com.example.data.EqubGroup
import com.example.data.Installment
import com.example.data.Member

@Composable
fun HomeScreen(
    viewModel: EqubViewModel,
    equb: EqubGroup,
    members: List<Member>,
    installments: List<Installment>
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val isWriteAuthorized = currentRole != "MEMBER"
    val activeMembers = members.filter { it.isActive }
    val currentCycle = equb.currentCycleIndex
    val currentRound = equb.currentRound

    val expectedTotal = activeMembers.size * equb.contribution
    val currentInstallments = installments.filter { it.round == currentRound && it.cycleIndex == currentCycle }
    val memberPaymentsMap = currentInstallments.groupBy { it.memberId }
    val paidSumMap = memberPaymentsMap.mapValues { entry -> entry.value.sumOf { it.amount } }
    
    val collectedTotal = currentInstallments.sumOf { it.amount }
    val pendingTotal = maxOf(0L, expectedTotal - collectedTotal)
    
    var paidCount = 0
    var partialCount = 0
    var unpaidCount = 0
    
    for (m in activeMembers) {
        val totalPaid = paidSumMap[m.id] ?: 0L
        if (totalPaid >= equb.contribution) {
            paidCount++
        } else if (totalPaid > 0) {
            partialCount++
        } else {
            unpaidCount++
        }
    }

    val progressPercent = if (expectedTotal > 0) (collectedTotal * 100 / expectedTotal).toInt() else 0
    val cycleWinner = activeMembers.find { it.payoutRound == currentRound && it.payoutCycleIndex == currentCycle }

    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val filteredMembersForStats = remember(selectedFilter, activeMembers, paidSumMap, equb.contribution) {
        when (selectedFilter) {
            "Paid" -> activeMembers.filter { (paidSumMap[it.id] ?: 0L) >= equb.contribution }
            "Partial" -> activeMembers.filter { (paidSumMap[it.id] ?: 0L) in 1 until equb.contribution }
            "Unpaid" -> activeMembers.filter { (paidSumMap[it.id] ?: 0L) == 0L }
            else -> emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isWriteAuthorized) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFEF3C7)).border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Column {
                        Text(text = "View-Only Audit Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Text(text = "All write actions (adding payments, lottery drawing, etc.) are restricted to the Chairman.", fontSize = 11.sp, color = Color(0xFFB45309), lineHeight = 15.sp)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().testTag("collection_progress_card")
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text(text = "Collection Progress", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE0E7FF))
                            Text(text = "$collectedTotal ETB", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(text = "$progressPercent%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progressPercent / 100f).clip(CircleShape).background(Color.White))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CardSummaryBox(label = "Expected", value = "$expectedTotal ETB", modifier = Modifier.weight(1f))
                        CardSummaryBox(label = "Pending Balance", value = "$pendingTotal ETB", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBadgeBox(label = "Paid (ሙሉ የከፈሉ)", count = paidCount, textColor = Color(0xFF16A34A), bgColor = Color(0xFFDCFCE7), modifier = Modifier.weight(1f), onClick = { selectedFilter = if (selectedFilter == "Paid") null else "Paid" })
                StatBadgeBox(label = "Partial (ከፊል)", count = partialCount, textColor = Color(0xFFD97706), bgColor = Color(0xFFFEF3C7), modifier = Modifier.weight(1f), onClick = { selectedFilter = if (selectedFilter == "Partial") null else "Partial" })
                StatBadgeBox(label = "Unpaid (ምንም ያልከፈሉ)", count = unpaidCount, textColor = Color(0xFFDC2626), bgColor = Color(0xFFFEE2E2), modifier = Modifier.weight(1f), onClick = { selectedFilter = if (selectedFilter == "Unpaid") null else "Unpaid" })
            }
        }

        if (selectedFilter != null) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$selectedFilter Members", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            IconButton(onClick = { selectedFilter = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                        if (filteredMembersForStats.isEmpty()) {
                            Text("No members in this category.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        } else {
                            filteredMembersForStats.forEach { member ->
                                val paid = paidSumMap[member.id] ?: 0L
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = member.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                    Text(text = "$paid ETB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), modifier = Modifier.fillMaxWidth().testTag("lottery_winner_section")) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Payout Rotational Winner (ዕጣ)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    if (cycleWinner != null) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFEEF2F6)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFEF08A)), contentAlignment = Alignment.Center) { Text("🏆", fontSize = 20.sp) }
                                Column {
                                    Text(text = cycleWinner.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Text(text = "Receives Pot of ${activeMembers.size * equb.contribution} ETB", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                                }
                            }
                            IconButton(onClick = { viewModel.clearWinnerForCurrentCycle() }, enabled = isWriteAuthorized, modifier = Modifier.testTag("clear_winner_button")) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Winner", tint = if (isWriteAuthorized) Color(0xFFEF4444) else Color(0xFFCBD5E1))
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Current Month's Winner not chosen.", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Button(onClick = { viewModel.drawLotteryWinner() }, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), disabledContainerColor = Color(0xFFCBD5E1)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(44.dp).testTag("run_lottery_draw_button")) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Lottery Draw Winner (ዕጣ) 🎲", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Recent Cycle Payments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Text(text = "Total: ${currentInstallments.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
            }
        }

        if (currentInstallments.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No payments recorded for this cycle yet.", fontSize = 13.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                }
            }
        } else {
            items(currentInstallments.take(5)) { inst ->
                val m = members.find { it.id == inst.memberId }
                PaymentItemCard(
                    memberName = m?.name ?: "Unknown",
                    installment = inst,
                    isWriteAuthorized = isWriteAuthorized,
                    onVerifyClick = { viewModel.verifyInstallment(inst.id, !inst.isVerified) },
                    onDeleteClick = { viewModel.deleteInstallment(inst.id) }
                )
            }
        }
    }
}

@Composable
fun CardSummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f)).padding(12.dp)) {
        Column {
            Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFC7D2FE), letterSpacing = 0.5.sp)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun StatBadgeBox(label: String, count: Int, textColor: Color, bgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), modifier = modifier.clickable { onClick() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), textAlign = TextAlign.Center, lineHeight = 12.sp, maxLines = 2)
            Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(bgColor).padding(horizontal = 14.dp, vertical = 4.dp)) {
                Text(text = count.toString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            }
        }
    }
}

@Composable
fun PaymentItemCard(memberName: String, installment: Installment, isWriteAuthorized: Boolean = true, onVerifyClick: () -> Unit, onDeleteClick: () -> Unit) {
    val initials = if (memberName.isNotBlank()) memberName.split(" ").asSequence().take(2).map { it.take(1) }.joinToString("").uppercase() else "UN"
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF8FAFC)), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) { Text(text = initials, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B)) }
                Column {
                    Text(text = memberName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = "${installment.amount} ETB via ${installment.paymentMethod} • ${installment.paymentDate}", fontSize = 11.sp, color = Color(0xFF64748B))
                    if (installment.referenceNumber.isNotBlank()) Text(text = "Ref: ${installment.referenceNumber}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4F46E5))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(if (!isWriteAuthorized) Color(0xFFF1F5F9) else if (installment.isVerified) Color(0xFFDCFCE7) else Color(0xFFEFF6FF)).border(1.dp, if (!isWriteAuthorized) Color(0xFFE2E8F0) else if (installment.isVerified) Color(0xFFBBF7D0) else Color(0xFFDBEAFE), RoundedCornerShape(100.dp)).then(if (isWriteAuthorized) Modifier.clickable { onVerifyClick() } else Modifier).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(text = if (installment.isVerified) "Verified" else "Paid", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (!isWriteAuthorized) Color(0xFF94A3B8) else if (installment.isVerified) Color(0xFF15803D) else Color(0xFF1D4ED8))
                }
                if (isWriteAuthorized) {
                    IconButton(onClick = onDeleteClick) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Payment", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}
