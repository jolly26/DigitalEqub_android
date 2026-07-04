package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EqubViewModel
import com.example.data.*
import com.example.utils.DiffItem
import com.example.utils.ParsedPayment
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

private data class RoleBadgeStyle(
    val bg: Color,
    val dot: Color,
    val label: String,
    val text: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqubAppContent(viewModel: EqubViewModel) {
    val context = LocalContext.current
    val equb by viewModel.equbGroup.collectAsState()
    val memberList by viewModel.members.collectAsState()
    val installmentList by viewModel.installments.collectAsState()
    val logsList by viewModel.auditLogs.collectAsState()
    
    val activeTab by viewModel.activeTab.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val feedbackMessage = viewModel.feedbackMessage

    var showRoleSecurityDialog by remember { mutableStateOf(false) }

    // Collect toasts
    LaunchedEffect(key1 = true) {
        feedbackMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    if (showRoleSecurityDialog) {
        RoleSecurityDialog(
            viewModel = viewModel,
            onDismiss = { showRoleSecurityDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FF),
        topBar = {
            HeaderBar(
                equbName = equb?.name ?: "No Active Equb",
                round = equb?.currentRound ?: 1,
                cycleIndex = equb?.currentCycleIndex ?: 1,
                role = currentRole,
                onRoleClick = {
                    showRoleSecurityDialog = true
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                activeTab = activeTab,
                onTabSelect = { viewModel.activeTab.value = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (equb == null) {
                // Show initial setup screen
                InitialSetupScreen(onSetup = { name, contribution, cycle, start ->
                    viewModel.setupEqub(name, contribution, cycle, start)
                })
            } else {
                when (activeTab) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        equb = equb!!,
                        members = memberList,
                        installments = installmentList
                    )
                    "members" -> MembersScreen(
                        viewModel = viewModel,
                        equb = equb!!,
                        members = memberList,
                        installments = installmentList
                    )
                    "sms" -> SmsScreen(
                        viewModel = viewModel,
                        members = memberList,
                        equb = equb!!,
                        installments = installmentList
                    )
                    "reports" -> ReportsScreen(
                        viewModel = viewModel,
                        equb = equb!!,
                        members = memberList,
                        installments = installmentList,
                        logs = logsList
                    )
                    "backups" -> BackupsScreen(
                        viewModel = viewModel,
                        equb = equb!!,
                        members = memberList
                    )
                }
            }
        }
    }
}

// APP BAR HEADER
@Composable
fun HeaderBar(
    equbName: String,
    round: Int,
    cycleIndex: Int,
    role: String,
    onRoleClick: () -> Unit
) {
    Surface(
        color = Color.White,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = equbName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "ROUND $round • CYCLE MONTH $cycleIndex",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
            }
            
            // Interactive Role toggle
            val style = when (role) {
                "CHAIRMAN" -> RoleBadgeStyle(Color(0xFFE0E7FF), Color(0xFF4F46E5), "Chairman (A)", Color(0xFF312E81))
                "CO_CHAIR" -> RoleBadgeStyle(Color(0xFFE0F2FE), Color(0xFF0284C7), "Co-Chair (B)", Color(0xFF0369A1))
                else -> RoleBadgeStyle(Color(0xFFF1F5F9), Color(0xFF64748B), "Regular Member", Color(0xFF334155))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(style.bg)
                    .clickable { onRoleClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("role_badge_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(style.dot)
                    )
                    Text(
                        text = style.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = style.text
                    )
                }
            }
        }
    }
}

// INITIAL SETUP
@Composable
fun InitialSetupScreen(
    onSetup: (name: String, contribution: Long, cycle: String, startDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contributionStr by remember { mutableStateOf("") }
    var cycleType by remember { mutableStateOf("Monthly") }
    var startDate by remember { mutableStateOf("") }

    // Auto set date
    LaunchedEffect(key1 = true) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        startDate = sdf.format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Equb Savings Logo",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(56.dp)
                )
                
                Text(
                    text = "Setup New Equb Group",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                
                Text(
                    text = "Ethiopian Rotating Savings Association (ዕቁብ) management completely offline & secure.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DoubleTapOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Equb Name") },
                    placeholder = { Text("e.g., Abyssinia Traders Equb") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_equb_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                DoubleTapOutlinedTextField(
                    value = contributionStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) contributionStr = it },
                    label = { Text("Monthly Contribution (ETB)") },
                    placeholder = { Text("e.g., 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("setup_equb_contribution"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Simple Cycle selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Contribution Frequency", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Weekly", "Bi-weekly", "Monthly").forEach { freq ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (cycleType == freq) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                    .clickable { cycleType = freq }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freq,
                                    color = if (cycleType == freq) Color.White else Color(0xFF475569),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                DoubleTapOutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_equb_start_date"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val contrib = contributionStr.toLongOrNull() ?: 0
                        if (name.isNotBlank() && contrib > 0 && startDate.isNotBlank()) {
                            onSetup(name, contrib, cycleType, startDate)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("setup_submit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Initialize Equb", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 1. HOME / DASHBOARD TAB
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

    // Calculate progress statistics
    val expectedTotal = activeMembers.size * equb.contribution
    
    // Sum verified + paid (unverified) installments for current round and cycle
    val currentInstallments = installments.filter { it.round == currentRound && it.cycleIndex == currentCycle }
    
    // Group payments by member
    val memberPaymentsMap = currentInstallments.groupBy { it.memberId }
    val paidSumMap = memberPaymentsMap.mapValues { entry -> entry.value.sumOf { it.amount } }
    
    val collectedTotal = currentInstallments.sumOf { it.amount }
    val pendingTotal = maxOf(0L, expectedTotal - collectedTotal)
    
    // Classification of member payment status
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

    // Find if we have a lottery winner drawn for this cycle
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
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isWriteAuthorized) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Column {
                        Text(
                            text = "View-Only Audit Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "All write actions (adding payments, lottery drawing, etc.) are restricted to the Chairman.",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // SUMMARY PROGRESS CARD
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collection_progress_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Collection Progress",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE0E7FF)
                            )
                            Text(
                                text = "$collectedTotal ETB",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$progressPercent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent / 100f)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardSummaryBox(label = "Expected", value = "$expectedTotal ETB", modifier = Modifier.weight(1f))
                        CardSummaryBox(label = "Pending Balance", value = "$pendingTotal ETB", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // STATS CLASSIFICATION ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBadgeBox(
                    label = "Paid",
                    count = paidCount,
                    textColor = Color(0xFF16A34A),
                    bgColor = Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedFilter = if (selectedFilter == "Paid") null else "Paid"
                    }
                )
                StatBadgeBox(
                    label = "Partial",
                    count = partialCount,
                    textColor = Color(0xFFD97706),
                    bgColor = Color(0xFFFEF3C7),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedFilter = if (selectedFilter == "Partial") null else "Partial"
                    }
                )
                StatBadgeBox(
                    label = "Unpaid",
                    count = unpaidCount,
                    textColor = Color(0xFFDC2626),
                    bgColor = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedFilter = if (selectedFilter == "Unpaid") null else "Unpaid"
                    }
                )
            }
        }

        if (selectedFilter != null) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedFilter Members",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = member.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "$paid ETB",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // INTERACTIVE LOTTERY WINNER (ዕጣ) DRAWER SECTION
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lottery_winner_section")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Payout Rotational Winner (ዕጣ)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    if (cycleWinner != null) {
                        // Display winner details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEEF2F6))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF08A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏆", fontSize = 20.sp)
                                }
                                Column {
                                    Text(
                                        text = cycleWinner.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Receives Pot of ${activeMembers.size * equb.contribution} ETB",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { viewModel.clearWinnerForCurrentCycle() },
                                enabled = isWriteAuthorized,
                                modifier = Modifier.testTag("clear_winner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Winner",
                                    tint = if (isWriteAuthorized) Color(0xFFEF4444) else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    } else {
                        // Draw layout
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Current Month's Winner not chosen.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = { viewModel.drawLotteryWinner() },
                                enabled = isWriteAuthorized,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4F46E5),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("run_lottery_draw_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Lottery Draw Winner (ዕጣ) 🎲", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // QUICK RECENT TRANSACTIONS HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Cycle Payments",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Text(
                    text = "Total: ${currentInstallments.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5)
                )
            }
        }

        // RECENT PAYMENTS LIST
        if (currentInstallments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No payments recorded for this cycle yet.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFC7D2FE),
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun StatBadgeBox(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(bgColor)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun PaymentItemCard(
    memberName: String,
    installment: Installment,
    isWriteAuthorized: Boolean = true,
    onVerifyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val initials = if (memberName.isNotBlank()) {
        memberName.split(" ").asSequence().take(2).map { it.take(1) }.joinToString("").uppercase()
    } else "UN"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF8FAFC)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }

                Column {
                    Text(
                        text = memberName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${installment.amount} ETB via ${installment.paymentMethod} • ${installment.paymentDate}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    if (installment.referenceNumber.isNotBlank()) {
                        Text(
                            text = "Ref: ${installment.referenceNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4F46E5)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Verification toggle click
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (!isWriteAuthorized) Color(0xFFF1F5F9) else if (installment.isVerified) Color(0xFFDCFCE7) else Color(0xFFEFF6FF))
                        .border(
                            1.dp, 
                            if (!isWriteAuthorized) Color(0xFFE2E8F0) else if (installment.isVerified) Color(0xFFBBF7D0) else Color(0xFFDBEAFE), 
                            RoundedCornerShape(100.dp)
                        )
                        .then(
                            if (isWriteAuthorized) {
                                Modifier.clickable { onVerifyClick() }
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (installment.isVerified) "Verified" else "Paid",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (!isWriteAuthorized) Color(0xFF94A3B8) else if (installment.isVerified) Color(0xFF15803D) else Color(0xFF1D4ED8)
                    )
                }

                if (isWriteAuthorized) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Payment",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// 2. MEMBER MANAGEMENT TAB
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: EqubViewModel,
    equb: EqubGroup,
    members: List<Member>,
    installments: List<Installment>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPaymentDialogForMember by remember { mutableStateOf<Member?>(null) }

    val currentRole by viewModel.currentRole.collectAsState()
    val isWriteAuthorized = currentRole != "MEMBER"

    val chairmanId by viewModel.chairmanMemberId.collectAsState()
    val coChairId by viewModel.coChairMemberId.collectAsState()
    
    val filteredMembers = members.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.phone.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("members_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isWriteAuthorized) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFEF3C7))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("⚠️", fontSize = 16.sp)
                Column {
                    Text(
                        text = "View-Only Audit Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                    Text(
                        text = "Adding or deleting members, recording payments, and triggering payouts are restricted to the Chairman.",
                        fontSize = 11.sp,
                        color = Color(0xFFB45309),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Search & Add row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DoubleTapOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search members...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("member_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1E293B),
                    unfocusedTextColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Button(
                onClick = { showAddDialog = true },
                enabled = isWriteAuthorized,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("add_member_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Members listing
        Text(
            text = "Equb Directory (${filteredMembers.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )

        if (filteredMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "No members added to this Equb yet." else "No matches found.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("members_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMembers) { member ->
                    MemberRowCard(
                        member = member,
                        equb = equb,
                        installments = installments.filter { it.memberId == member.id && it.round == equb.currentRound && it.cycleIndex == equb.currentCycleIndex },
                        isChairman = member.id == chairmanId,
                        isCoChair = member.id == coChairId,
                        isWriteAuthorized = isWriteAuthorized,
                        onToggleActive = { viewModel.toggleMemberActive(member) },
                        onDelete = { viewModel.deleteMember(member) },
                        onRecordPayment = { showPaymentDialogForMember = member },
                        onSendReminder = { 
                            // Manual share intent
                            val paySum = installments.filter { it.memberId == member.id && it.round == equb.currentRound && it.cycleIndex == equb.currentCycleIndex }.sumOf { it.amount }
                            val text = viewModel.generateReminderText(member, equb.contribution, paySum)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            val shareIntent = Intent.createChooser(intent, "Send Reminder via")
                            coroutineScope.launch {
                                viewModel.feedbackMessage.emit("Prepared reminder for ${member.name}")
                            }
                            context.startActivity(shareIntent)
                        },
                        onPayoutClick = { viewModel.setManualWinner(member.id) }
                    )
                }
            }
        }
    }

    // ADD MEMBER DIALOG
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .imePadding()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Add New Equb Member",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        DoubleTapOutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Member Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("new_member_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        DoubleTapOutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '+' }) phone = it },
                            label = { Text("Phone Number (e.g. 0911223344)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("new_member_phone_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Wrapping container around the Save/Cancel buttons with position: fixed and bottom: 0
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.addMember(name, phone)
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.testTag("save_member_button")
                        ) {
                            Text("Save Member")
                        }
                    }
                }
            }
        }
    }

    // RECORD PAYMENT DIALOG
    if (showPaymentDialogForMember != null) {
        val member = showPaymentDialogForMember!!
        var amountStr by remember { mutableStateOf(equb.contribution.toString()) }
        var method by remember { mutableStateOf("CBE") }
        var reference by remember { mutableStateOf("") }
        var remarks by remember { mutableStateOf("") }
        var verifyImmediately by remember(currentRole) { mutableStateOf(currentRole == "CHAIRMAN") }

        Dialog(onDismissRequest = { showPaymentDialogForMember = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .imePadding()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp)
                    ) {
                        Text(
                            text = "Record Contribution payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )

                        Text("Adding payment for ${member.name}", fontSize = 13.sp, color = Color(0xFF64748B))

                        DoubleTapOutlinedTextField(
                            value = amountStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                            label = { Text("Amount (ETB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Method Selector Grid
                        Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val methods = listOf("CBE", "Telebirr", "Dashen", "Awash", "Cash", "Other")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            methods.take(3).forEach { mthd ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (method == mthd) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                        .clickable { method = mthd }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mthd,
                                        color = if (method == mthd) Color.White else Color(0xFF475569),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            methods.drop(3).forEach { mthd ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (method == mthd) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                        .clickable { method = mthd }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mthd,
                                        color = if (method == mthd) Color.White else Color(0xFF475569),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        DoubleTapOutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it },
                            label = { Text("Reference Number (Optional)") },
                            modifier = Modifier.fillMaxWidth().testTag("payment_reference_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        DoubleTapOutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks (e.g. Late fine paid)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Immediate Verification checkbox (only allowed/useful for chairman)
                        if (currentRole == "CHAIRMAN") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = verifyImmediately,
                                    onCheckedChange = { verifyImmediately = it }
                                )
                                Text("Mark payment as verified immediately", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Wrapping container around the Save/Cancel buttons with position: fixed and bottom: 0
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showPaymentDialogForMember = null }) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
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
                                        isVerified = verifyImmediately
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
    var expanded by remember { mutableStateOf(false) }
    
    val totalPaid = installments.sumOf { it.amount }
    val isFullyPaid = totalPaid >= equb.contribution
    val isPartial = totalPaid > 0 && totalPaid < equb.contribution

    val statusText = when {
        isFullyPaid -> "Paid"
        isPartial -> "Partial"
        else -> "Unpaid"
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
        member.name.split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
    } else "UN"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (expanded) Color(0xFF4F46E5) else Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("member_card_${member.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (member.isActive) Color(0xFFEEF2F6) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (member.isActive) Color(0xFF4F46E5) else Color(0xFF94A3B8)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = member.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (member.isActive) Color(0xFF1E293B) else Color(0xFF94A3B8)
                            )
                            if (isChairman) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFE0E7FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CHAIRMAN", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF4F46E5))
                                }
                            } else if (isCoChair) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFE0F2FE))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CO-CHAIR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                                }
                            }
                            if (!member.isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("INACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                }
                            }
                        }
                        Text(
                            text = member.phone.ifBlank { "No Phone" },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$statusText ($totalPaid/${equb.contribution})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            // Expanded view with interactive operational buttons
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Installments History sub-section
                    if (installments.isNotEmpty()) {
                        Text("Cycle Installments list:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        installments.forEach { inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${inst.amount} ETB (${inst.paymentMethod})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = if (inst.isVerified) "Verified" else "Awaiting Verification",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inst.isVerified) Color(0xFF16A34A) else Color(0xFF3B82F6)
                                )
                            }
                        }
                    }

                    // Action Button Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRecordPayment,
                            enabled = isWriteAuthorized && !isFullyPaid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFullyPaid) Color(0xFF10B981) else Color(0xFF4F46E5),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("record_pay_btn_${member.id}")
                        ) {
                            Text(
                                text = if (isFullyPaid) "Paid Full" else "Record Pay",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Reminder button if partial or unpaid
                        if (!isFullyPaid) {
                            OutlinedButton(
                                onClick = onSendReminder,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF64748B)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("remind_btn_${member.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        // Rotational payout click if they haven't won yet
                        if (member.payoutCycleIndex == null) {
                            Button(
                                onClick = onPayoutClick,
                                enabled = isWriteAuthorized,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(38.dp)
                                    .testTag("payout_btn_${member.id}")
                            ) {
                                Text("Set Winner (ዕጣ)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Administrative Toggles (Deactivate / Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onToggleActive,
                            enabled = isWriteAuthorized,
                            modifier = Modifier.testTag("toggle_active_btn_${member.id}")
                        ) {
                            Text(
                                text = if (member.isActive) "Deactivate Member" else "Activate Member",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWriteAuthorized) Color(0xFF475569) else Color(0xFF94A3B8)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            enabled = isWriteAuthorized,
                            modifier = Modifier.testTag("delete_member_btn_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete member",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. AUTOMATIC SMS PARSER & DISPATCHER TAB
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
    
    // Filter out members who have already fully paid for the current cycle
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("sms_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isWriteAuthorized) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠️", fontSize = 14.sp)
                    Text(
                        text = "View-Only Mode: SMS parsing and payment linking is restricted to the Chairman and Co-Chair.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paste Banking SMS Notification 📩",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "Avoid manual entry errors. Paste transaction text from CBE, Telebirr, Dashen, or Awash bank below, and the app will parse the references, amount, and dates automatically.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )

                    DoubleTapOutlinedTextField(
                        value = smsText,
                        onValueChange = { viewModel.parseSms(it) },
                        placeholder = { Text("Paste bank transaction SMS here...") },
                        enabled = isWriteAuthorized,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("sms_text_paste_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Display parsed proposal if success
        val proposalVal = proposal
        if (proposalVal != null) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2F6)),
                    border = BorderStroke(1.dp, Color(0xFF6366F1)),
                    modifier = Modifier.testTag("parsed_proposal_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Bank Details Successfully!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                            IconButton(onClick = { viewModel.discardSmsProposal() }) {
                                Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color(0xFFEF4444))
                            }
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

                        if (proposalVal.senderName.isNotBlank()) {
                            ParsedFieldBox(label = "Discovered Sender", value = proposalVal.senderName, modifier = Modifier.fillMaxWidth())
                        }

                        HorizontalDivider(color = Color(0xFFCBD5E1))

                        // Match Member section
                        Text(
                            text = "Step 2: Connect this Payment to a Member",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        DoubleTapOutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.smsSearchQuery.value = it },
                            placeholder = { Text("Search member to link...") },
                            enabled = isWriteAuthorized,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("sms_member_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Member link matching
                        if (filteredMembers.isEmpty()) {
                            Text("No matching active members.", fontSize = 11.sp, color = Color(0xFFEF4444))
                        } else {
                            Text("Select member to deposit funds to:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredMembers.forEach { member ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isWriteAuthorized) Color.White else Color(0xFFF1F5F9))
                                            .clickable(enabled = isWriteAuthorized) { viewModel.applySmsProposal(member.id) }
                                            .padding(10.dp)
                                            .testTag("sms_link_member_${member.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(member.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            if (isWriteAuthorized) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
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
}

@Composable
fun ParsedFieldBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(10.dp)
    ) {
        Column {
            Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

// 4. REPORTS TAB (Visualizations, Audits, Sharing summaries)
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

    // Pre-calculate sums for current round & cycle
    val currentRoundInstallments = installments.filter { it.round == currentRound && it.cycleIndex == currentCycle }
    val memberPaymentsMap = currentRoundInstallments.groupBy { it.memberId }
    val paidSumMap = memberPaymentsMap.mapValues { entry -> entry.value.sumOf { it.amount } }

    val expectedTotal = activeMembers.size * equb.contribution
    val collectedTotal = currentRoundInstallments.sumOf { it.amount }
    val pendingTotal = maxOf(0L, expectedTotal - collectedTotal)
    val winner = activeMembers.find { it.payoutRound == currentRound && it.payoutCycleIndex == currentCycle }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("grid" to "Monthly Ledger", "audits" to "Audit Trail", "unpaid" to "Outstanding").forEach { (tabId, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedReportSubTab == tabId) Color(0xFF4F46E5) else Color(0xFFEEF2F6))
                        .clickable { selectedReportSubTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedReportSubTab == tabId) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SHARE TEXT SUMMARY ACTION
        Button(
            onClick = {
                val reportSummary = viewModel.generateSummaryReportText(
                    totalExpected = expectedTotal,
                    totalCollected = collectedTotal,
                    totalPending = pendingTotal,
                    paidCount = activeMembers.count { (paidSumMap[it.id] ?: 0L) >= equb.contribution },
                    totalCount = activeMembers.size,
                    winnerName = winner?.name
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, reportSummary)
                }
                context.startActivity(Intent.createChooser(intent, "Share Report Summary"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("share_summary_report_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Share Monthly Summary to Telegram/WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        // VIEW RENDERING BASED ON SUB TAB
        when (selectedReportSubTab) {
            "grid" -> {
                // Monthly Collection Grid list
                Text("Members Contribution Audit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("reports_monthly_ledger"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeMembers) { m ->
                        val totalPaid = paidSumMap[m.id] ?: 0L
                        val ratio = "$totalPaid/${equb.contribution} ETB"
                        val isFullyPaid = totalPaid >= equb.contribution
                        val statusText = if (isFullyPaid) "Fully Paid" else if (totalPaid > 0) "Partial" else "No payment"
                        
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(statusText, fontSize = 11.sp, color = if (isFullyPaid) Color(0xFF16A34A) else Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                }
                                Text(ratio, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }
            }
            
            "audits" -> {
                // Search field for logs
                DoubleTapOutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search logs (e.g. Winner, Sync)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val filteredLogs = logs.filter { 
                    it.action.contains(searchQuery, ignoreCase = true) || 
                    it.details.contains(searchQuery, ignoreCase = true) ||
                    it.performedBy.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("reports_audit_trail"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLogs) { log ->
                        val date = SimpleDateFormat("HH:mm - yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestamp))
                        
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEEF2F6))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(log.action, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF4F46E5))
                                    }
                                    Text(date, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(log.details, fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "By: ${log.performedBy}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                    if (log.amount > 0) {
                                        Text("${log.amount} ETB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "unpaid" -> {
                // Unpaid lists
                val outstandingMembers = activeMembers.filter { (paidSumMap[it.id] ?: 0L) < equb.contribution }

                Text("Outstanding Balances (${outstandingMembers.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                if (outstandingMembers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Everyone is paid in full for this cycle! 🎉", fontSize = 13.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).testTag("reports_outstanding"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(outstandingMembers) { m ->
                            val totalPaid = paidSumMap[m.id] ?: 0L
                            val remaining = equb.contribution - totalPaid
                            
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(m.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Paid: $totalPaid ETB", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text(
                                        text = "Due: $remaining ETB", 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.ExtraBold, 
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. BACKUPS & SYNC CONSOLE
@Composable
fun BackupsScreen(
    viewModel: EqubViewModel,
    equb: EqubGroup,
    members: List<Member>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentRole by viewModel.currentRole.collectAsState()
    val isWriteAuthorized = currentRole != "MEMBER"
    val incomingDiffs by viewModel.importDiffList.collectAsState()
    val pendingData by viewModel.pendingBackupData.collectAsState()
    val textToImport by viewModel.importedJsonString.collectAsState()

    val initialChairId by viewModel.chairmanMemberId.collectAsState()
    val initialCoChairId by viewModel.coChairMemberId.collectAsState()
    val initialChairPin by viewModel.chairmanPin.collectAsState()
    val initialCoChairPin by viewModel.coChairPin.collectAsState()

    var selectedChairId by remember(initialChairId) { mutableLongStateOf(initialChairId) }
    var selectedCoChairId by remember(initialCoChairId) { mutableLongStateOf(initialCoChairId) }
    var inputChairPin by remember(initialChairPin) { mutableStateOf(initialChairPin) }
    var inputCoChairPin by remember(initialCoChairPin) { mutableStateOf(initialCoChairPin) }

    var showSwitchDialog by remember { mutableStateOf(false) }

    if (showSwitchDialog) {
        RoleSecurityDialog(
            viewModel = viewModel,
            onDismiss = { showSwitchDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("backups_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ROLE PICKER & MAPPING CONSOLE
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Role Mapping & Security Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "To guarantee consensus and valid audit trails, map roles to specific registered members, customize security PINs, or switch your currently authenticated active role.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Active Role Display Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Current Active Role",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F9FF))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (currentRole) {
                                                "CHAIRMAN" -> Color(0xFF4F46E5)
                                                "CO_CHAIR" -> Color(0xFF0284C7)
                                                else -> Color(0xFF64748B)
                                            }
                                        )
                                )
                                Text(
                                    text = when (currentRole) {
                                        "CHAIRMAN" -> "Chairman (Authorized)"
                                        "CO_CHAIR" -> "Co-Chair / Auditor"
                                        else -> "Regular Member (Read Only)"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Button(
                                onClick = { showSwitchDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Switch Role", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    val isAdmin = currentRole == "CHAIRMAN"
                    if (!isAdmin) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️", fontSize = 14.sp)
                            Text(
                                text = "Only the active Chairman can modify roles or security PIN configurations.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Dropdown Mapping section
                    MemberDropdownSelector(
                        label = "Assign Chairman Role (Admin Permissions)",
                        members = members,
                        selectedMemberId = selectedChairId,
                        enabled = isAdmin,
                        onSelect = { selectedChairId = it }
                    )

                    MemberDropdownSelector(
                        label = "Assign Co-Chair Role (Auditor Permissions)",
                        members = members,
                        selectedMemberId = selectedCoChairId,
                        enabled = isAdmin,
                        onSelect = { selectedCoChairId = it }
                    )

                    // Pin input fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Chairman PIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) Color(0xFF475569) else Color(0xFF94A3B8)
                            )
                            DoubleTapOutlinedTextField(
                                value = inputChairPin,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputChairPin = it },
                                placeholder = { Text("e.g. 1234") },
                                enabled = isAdmin,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("chairman_pin_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2196F3),
                                    unfocusedBorderColor = Color(0xFF757575)
                                )
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Co-Chair PIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) Color(0xFF475569) else Color(0xFF94A3B8)
                            )
                            DoubleTapOutlinedTextField(
                                value = inputCoChairPin,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputCoChairPin = it },
                                placeholder = { Text("e.g. 5678") },
                                enabled = isAdmin,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("co_chair_pin_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2196F3),
                                    unfocusedBorderColor = Color(0xFF757575)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (inputChairPin.length < 4 || inputCoChairPin.length < 4) {
                                coroutineScope.launch {
                                    viewModel.feedbackMessage.emit("Error: Security PINs must be at least 4 digits long.")
                                }
                            } else {
                                viewModel.updateSecuritySettings(
                                    chairId = selectedChairId,
                                    coChairId = selectedCoChairId,
                                    chairPinStr = inputChairPin,
                                    coChairPinStr = inputCoChairPin
                                )
                                coroutineScope.launch {
                                    viewModel.feedbackMessage.emit("Security settings updated successfully!")
                                }
                            }
                        },
                        enabled = isAdmin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_security_settings_button")
                    ) {
                        Text("Save Security Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ARCHIVE CURRENT ROUND / CYCLE CARD
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cycle & Round Administrations",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.advanceCycle() },
                            enabled = isWriteAuthorized,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f).testTag("advance_cycle_button")
                        ) {
                            Text("Next Month ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.decreaseCycle() },
                            enabled = isWriteAuthorized,
                            border = BorderStroke(1.dp, if (isWriteAuthorized) Color(0xFF64748B) else Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f).testTag("decrease_cycle_button")
                        ) {
                            Text("⬅ Prev Month", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Text(
                        text = "Archive Current Round",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWriteAuthorized) Color(0xFF334155) else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "When all ${members.count { it.isActive }} members have received their rotational payout, complete Round ${equb.currentRound} to archive audit history and start 'Round ${equb.currentRound + 1}' without losing old logs.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Button(
                        onClick = { viewModel.archiveAndStartNextRound() },
                        enabled = isWriteAuthorized,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("archive_round_button")
                    ) {
                        Text("Archive & Start Round ${equb.currentRound + 1}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // EXPORT EXCEL/JSON BACKUP
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Share Database Backup File (JSON)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Export the entire local database containing current members, payment registers, and audit trails to copy or share with your co-organizer via Telegram/Bluetooth.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val backupText = viewModel.getExportString()
                                clipboardManager.setText(AnnotatedString(backupText))
                                
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, backupText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share JSON Backup"))
                                viewModel.feedbackMessage.emit("Database JSON copied to clipboard and shared!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Export & Share Backup 📤", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // IMPORT / SYNC RECONCILIATION FILE
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Import Co-Organizer Sync State",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Paste the JSON database backup text received from your co-organizer here to run automatic conflict checking and preview changes before writing them to disk.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    DoubleTapOutlinedTextField(
                        value = textToImport,
                        onValueChange = { viewModel.importedJsonString.value = it },
                        placeholder = { Text("Paste JSON backup file text here...") },
                        enabled = isWriteAuthorized,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("import_json_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (textToImport.isNotBlank()) {
                                viewModel.analyzeBackup(textToImport)
                            }
                        },
                        enabled = isWriteAuthorized,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("analyze_sync_button")
                    ) {
                        Text("Analyze Sync Differences 🔍", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CO-ORGANIZER CONFLICT RESOLUTION VISUALIZER
        if (pendingData != null && incomingDiffs.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(2.dp, Color(0xFFD97706)),
                    modifier = Modifier.testTag("sync_conflict_visualizer_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reviewing Co-Organizer's Differences",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            IconButton(onClick = { viewModel.discardSyncProposal() }) {
                                Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color(0xFFEF4444))
                            }
                        }

                        Text(
                            text = "A total of ${incomingDiffs.size} records are different. Review and confirm below to merge data into your offline ledger.",
                            fontSize = 12.sp,
                            color = Color(0xFF78350F)
                        )

                        HorizontalDivider(color = Color(0xFFFDE68A))

                        // Render diff items nicely
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            incomingDiffs.forEach { diff ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (diff.changeType == "Added") "➕" else "✏️",
                                        fontSize = 12.sp
                                    )
                                    Column {
                                        Text(
                                            text = "[${diff.category}] ${diff.changeType}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB45309)
                                        )
                                        Text(
                                            text = diff.description,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1E293B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFFDE68A))

                        Button(
                            onClick = { viewModel.confirmSyncImport() },
                            enabled = isWriteAuthorized,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_sync_import_btn")
                        ) {
                            Text("Confirm & Sync All Changes ✅", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// BOTTOM NAVIGATION BAR
@Composable
fun BottomNavBar(
    activeTab: String,
    onTabSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        val tabs = listOf(
            Triple("home", "Home", Icons.Default.Home),
            Triple("members", "Directory", Icons.Default.Group),
            Triple("sms", "Sms Parse", Icons.Default.Mail),
            Triple("reports", "Reports", Icons.Default.Analytics),
            Triple("backups", "Backups", Icons.Default.Backup)
        )

        tabs.forEach { (tabId, label, icon) ->
            NavigationBarItem(
                selected = activeTab == tabId,
                onClick = { onTabSelect(tabId) },
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4F46E5),
                    selectedTextColor = Color(0xFF4F46E5),
                    indicatorColor = Color(0xFFEEF2F6),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.testTag("nav_tab_$tabId")
            )
        }
    }
}

@Composable
fun DoubleTapOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF333333),
        unfocusedTextColor = Color(0xFF333333),
        disabledTextColor = Color(0xFF64748B),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF2196F3),
        unfocusedBorderColor = Color(0xFF757575),
        focusedLabelColor = Color(0xFF2196F3),
        unfocusedLabelColor = Color(0xFF64748B),
        focusedPlaceholderColor = Color(0xFF94A3B8),
        unfocusedPlaceholderColor = Color(0xFF94A3B8)
    )
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val finalKeyboardActions = KeyboardActions(
        onDone = {
            focusManager.clearFocus()
            keyboardController?.hide()
            keyboardActions.onDone?.invoke(this)
        },
        onNext = {
            focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next)
            keyboardActions.onNext?.invoke(this)
        },
        onSearch = {
            focusManager.clearFocus()
            keyboardController?.hide()
            keyboardActions.onSearch?.invoke(this)
        },
        onGo = {
            focusManager.clearFocus()
            keyboardController?.hide()
            keyboardActions.onGo?.invoke(this)
        }
    )

    val finalKeyboardOptions = if (singleLine) {
        keyboardOptions.copy(
            imeAction = if (keyboardOptions.imeAction == ImeAction.Default) {
                ImeAction.Done
            } else {
                keyboardOptions.imeAction
            }
        )
    } else {
        keyboardOptions
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = finalKeyboardOptions,
        keyboardActions = finalKeyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = shape,
        colors = colors
    )
}

@Composable
fun MemberDropdownSelector(
    label: String,
    members: List<Member>,
    selectedMemberId: Long,
    enabled: Boolean = true,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.find { it.id == selectedMemberId }
    val displayText = selectedMember?.name ?: "No Member Assigned"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (enabled) Color(0xFF475569) else Color(0xFF94A3B8))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) Color(0xFFF8F9FF) else Color(0xFFF1F5F9))
                .border(1.dp, if (enabled) Color(0xFFCBD5E1) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = displayText, fontSize = 14.sp, color = if (!enabled) Color(0xFF94A3B8) else if (selectedMember != null) Color(0xFF1E293B) else Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF64748B) else Color(0xFFCBD5E1)
                )
            }
            if (enabled) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Unassigned / None", fontSize = 14.sp) },
                        onClick = {
                            onSelect(-1L)
                            expanded = false
                        }
                    )
                    members.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.name, fontSize = 14.sp) },
                            onClick = {
                                onSelect(m.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSecurityDialog(
    viewModel: EqubViewModel,
    onDismiss: () -> Unit
) {
    var selectedRole by remember { mutableStateOf("MEMBER") }
    var enteredPin by remember { mutableStateOf("") }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var recoveryMasterPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val currentRole by viewModel.currentRole.collectAsState()
    val members by viewModel.members.collectAsState()
    val chairmanId by viewModel.chairmanMemberId.collectAsState()
    val coChairId by viewModel.coChairMemberId.collectAsState()

    val chairmanName = if (chairmanId != -1L) {
        members.find { it.id == chairmanId }?.name ?: "Assigned Member"
    } else "Not Mapped"
    val coChairName = if (coChairId != -1L) {
        members.find { it.id == coChairId }?.name ?: "Assigned Member"
    } else "Not Mapped"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isRecoveryMode) {
                    Text(
                        text = "Authenticate & Select Role",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "To guarantee consensus and local audit trails, configure or unlock your active role.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Role List selection buttons
                    listOf(
                        Triple("CHAIRMAN", "Chairman (Admin)", "Full read & write master. Mapped: $chairmanName"),
                        Triple("CO_CHAIR", "Co-Chair / Auditor", "Auditor & backup author. Mapped: $coChairName"),
                        Triple("MEMBER", "Regular Member", "View-only read access to audit reports.")
                    ).forEach { (role, label, desc) ->
                        val isSelected = selectedRole == role
                        val isCurrent = currentRole == role
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(if (isSelected) Color(0xFFEEF2F6) else Color.White)
                                .clickable {
                                    selectedRole = role
                                    errorMessage = ""
                                    enteredPin = ""
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                                            }
                                        }
                                    }
                                    Text(text = desc, fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 14.sp)
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedRole = role
                                        errorMessage = ""
                                        enteredPin = ""
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4F46E5))
                                )
                            }
                        }
                    }

                    if (selectedRole != "MEMBER") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Enter 4-Digit Security PIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            DoubleTapOutlinedTextField(
                                value = enteredPin,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) enteredPin = it },
                                placeholder = { Text("••••") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("role_pin_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2196F3),
                                    unfocusedBorderColor = Color(0xFF757575)
                                )
                            )
                            if (errorMessage.isNotEmpty()) {
                                Text(text = errorMessage, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedRole != "MEMBER") {
                            TextButton(onClick = { isRecoveryMode = true }) {
                                Text("Forgot PIN? Unlock with Support Key", color = Color(0xFF4F46E5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel", color = Color(0xFF64748B))
                            }
                            Button(
                                onClick = {
                                    val success = viewModel.verifyAndSetRole(selectedRole, enteredPin)
                                    if (success) {
                                        onDismiss()
                                    } else {
                                        errorMessage = "Invalid PIN. Please try again."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Apply Role")
                            }
                        }
                    }
                } else {
                    // Developer Recovery Mode Panel
                    Text(
                        text = "Emergency Recovery (Best Practice)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "Since Equb is fully local-first and offline-secure, security PINs are stored only on your device. " +
                               "If you forgot your PIN, entering the Developer Master Recovery Key will immediately reset your PINs to default (Chairman: 1234, Co-Chair: 5678) without losing your local member balances or payment ledgers.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Enter Developer Master Bypass Key",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        DoubleTapOutlinedTextField(
                            value = recoveryMasterPin,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) recoveryMasterPin = it },
                            placeholder = { Text("e.g. 258012") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_bypass_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFF757575)
                            )
                        )
                        if (errorMessage.isNotEmpty()) {
                            Text(text = errorMessage, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Tip: Developer Support Key is 258012",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isRecoveryMode = false; errorMessage = "" }) {
                            Text("Back", color = Color(0xFF64748B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (recoveryMasterPin == "258012") {
                                    val success = viewModel.verifyAndSetRole("CHAIRMAN", "258012")
                                    if (success) {
                                        onDismiss()
                                    }
                                } else {
                                    errorMessage = "Invalid Master Bypass Key. Contact your developer."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Unlock & Reset PINs", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
