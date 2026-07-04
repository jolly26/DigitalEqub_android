package com.example.ui.reports

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EqubViewModel
import com.example.data.*
import com.example.ui.components.DoubleTapOutlinedTextField
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: EqubViewModel,
    equb: EqubGroup,
    members: List<Member>,
    installments: List<Installment>,
    logs: List<AuditLog>
) {
    val context = LocalContext.current
    var selectedReportSubTab by remember { mutableStateOf("grid") } // "grid", "audits", "unpaid"
    var searchQuery by remember { mutableStateOf("") }

    val activeMembers = members.filter { it.isActive }
    val currentRound = equb.currentRound
    val currentCycle = equb.currentCycleIndex

    val currentRoundInstallments = installments.filter { it.round == currentRound && it.cycleIndex == currentCycle }
    val memberPaymentsMap = currentRoundInstallments.groupBy { it.memberId }
    val paidSumMap = memberPaymentsMap.mapValues { entry -> entry.value.sumOf { it.amount } }

    val expectedTotal = activeMembers.size * equb.contribution
    val collectedTotal = currentRoundInstallments.sumOf { it.amount }
    val pendingTotal = maxOf(0L, expectedTotal - collectedTotal)
    val winner = activeMembers.find { it.payoutRound == currentRound && it.payoutCycleIndex == currentCycle }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("reports_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("grid" to "Monthly Ledger", "audits" to "Audit Trail", "unpaid" to "Outstanding").forEach { (tabId, label) -> Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selectedReportSubTab == tabId) Color(0xFF4F46E5) else Color(0xFFEEF2F6)).clickable { selectedReportSubTab = tabId }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(text = label, color = if (selectedReportSubTab == tabId) Color.White else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
        Button(onClick = { 
            val reportSummary = viewModel.generateSummaryReportText(totalExpected = expectedTotal, totalCollected = collectedTotal, totalPending = pendingTotal, paidCount = activeMembers.count { (paidSumMap[it.id] ?: 0L) >= equb.contribution }, totalCount = activeMembers.size, winnerName = winner?.name)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reportSummary) }
            context.startActivity(Intent.createChooser(intent, "Share Report Summary"))
        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(44.dp).testTag("share_summary_report_button")) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Share Monthly Summary to Telegram/WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        HorizontalDivider(color = Color(0xFFE2E8F0))
        when (selectedReportSubTab) {
            "grid" -> {
                Text("Members Contribution Audit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f).testTag("reports_monthly_ledger"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeMembers) { m ->
                        val totalPaid = paidSumMap[m.id] ?: 0L
                        val ratio = "$totalPaid/${equb.contribution} ETB"
                        val statusText = if (totalPaid >= equb.contribution) "Fully Paid (ሙሉ የከፈሉ)" else if (totalPaid > 0) "Partial (ከፊል)" else "No payment (ምንም ያልከፈሉ)"
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(text = statusText, fontSize = 11.sp, color = if (totalPaid >= equb.contribution) Color(0xFF16A34A) else Color(0xFFDC2626), fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)
                                }
                                Text(ratio, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            "audits" -> {
                DoubleTapOutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search logs (e.g. Winner, Sync)...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                val filteredLogs = logs.filter { it.action.contains(searchQuery, ignoreCase = true) || it.details.contains(searchQuery, ignoreCase = true) || it.performedBy.contains(searchQuery, ignoreCase = true) }
                LazyColumn(modifier = Modifier.weight(1f).testTag("reports_audit_trail"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredLogs) { log ->
                        val date = SimpleDateFormat("HH:mm - yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestamp))
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEF2F6)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(log.action, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF4F46E5)) }
                                    Text(date, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(log.details, fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(text = "By: ${log.performedBy}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    if (log.amount > 0) { Text("${log.amount} ETB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) }
                                }
                            }
                        }
                    }
                }
            }
            "unpaid" -> {
                val outstandingMembers = activeMembers.filter { (paidSumMap[it.id] ?: 0L) < equb.contribution }
                Text("Outstanding Balances (${outstandingMembers.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (outstandingMembers.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Everyone is paid in full for this cycle! 🎉", fontSize = 13.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold) } } else {
                    LazyColumn(modifier = Modifier.weight(1f).testTag("reports_outstanding"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(outstandingMembers) { m ->
                            val totalPaid = paidSumMap[m.id] ?: 0L
                            val remaining = equb.contribution - totalPaid
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Paid: $totalPaid ETB", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text(text = "Due: $remaining ETB", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
