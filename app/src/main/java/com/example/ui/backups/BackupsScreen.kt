package com.example.ui.backups

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EqubViewModel
import com.example.data.EqubGroup
import com.example.data.Member
import com.example.ui.components.DoubleTapOutlinedTextField
import com.example.ui.components.MemberDropdownSelector
import com.example.ui.components.RoleSecurityDialog
import kotlinx.coroutines.launch

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
        RoleSecurityDialog(viewModel = viewModel, onDismiss = { showSwitchDialog = false })
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("backups_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Role Mapping & Security Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = "To guarantee consensus and valid audit trails, map roles to specific registered members, customize security PINs, or switch your currently authenticated active role.", fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 15.sp)
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Current Active Role", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8F9FF)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(when (currentRole) { "CHAIRMAN" -> Color(0xFF4F46E5)
                                    "CO_CHAIR" -> Color(0xFF0284C7)
                                    else -> Color(0xFF64748B) }))
                                Text(text = when (currentRole) { "CHAIRMAN" -> "Chairman (Authorized)"
                                    "CO_CHAIR" -> "Co-Chair / Auditor"
                                    else -> "Regular Member (Read Only)" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                            Button(onClick = { showSwitchDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(32.dp)) { Text("Switch Role", fontSize = 11.sp, color = Color.White) }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    val isAdmin = currentRole == "CHAIRMAN"
                    if (!isAdmin) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFEF3C7)).border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠️", fontSize = 14.sp)
                            Text(text = "Only the active Chairman can modify roles or security PIN configurations.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309), lineHeight = 15.sp)
                        }
                    }
                    MemberDropdownSelector(label = "Assign Chairman Role (Admin Permissions)", members = members, selectedMemberId = selectedChairId, enabled = isAdmin, onSelect = { selectedChairId = it })
                    MemberDropdownSelector(label = "Assign Co-Chair Role (Auditor Permissions)", members = members, selectedMemberId = selectedCoChairId, enabled = isAdmin, onSelect = { selectedCoChairId = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Chairman PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isAdmin) Color(0xFF475569) else Color(0xFF94A3B8))
                            DoubleTapOutlinedTextField(value = inputChairPin, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputChairPin = it }, placeholder = { Text("e.g. 1234") }, enabled = isAdmin, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("chairman_pin_input"), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2196F3), unfocusedBorderColor = Color(0xFF757575)))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Co-Chair PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isAdmin) Color(0xFF475569) else Color(0xFF94A3B8))
                            DoubleTapOutlinedTextField(value = inputCoChairPin, onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputCoChairPin = it }, placeholder = { Text("e.g. 5678") }, enabled = isAdmin, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("co_chair_pin_input"), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2196F3), unfocusedBorderColor = Color(0xFF757575)))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = { if (inputChairPin.length < 4 || inputCoChairPin.length < 4) { coroutineScope.launch { viewModel.feedbackMessage.emit("Error: Security PINs must be at least 4 digits long.") } } else { viewModel.updateSecuritySettings(chairId = selectedChairId, coChairId = selectedCoChairId, chairPinStr = inputChairPin, coChairPinStr = inputCoChairPin)
                                coroutineScope.launch { viewModel.feedbackMessage.emit("Security settings updated successfully!") } } }, enabled = isAdmin, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), disabledContainerColor = Color(0xFFCBD5E1)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().testTag("save_security_settings_button")) { Text("Save Security Configuration", fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Cycle & Round Administrations", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { viewModel.advanceCycle() }, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), disabledContainerColor = Color(0xFFCBD5E1)), modifier = Modifier.weight(1f).testTag("advance_cycle_button")) { Text("Next Month ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = { viewModel.decreaseCycle() }, enabled = isWriteAuthorized, border = BorderStroke(1.dp, if (isWriteAuthorized) Color(0xFF64748B) else Color(0xFFCBD5E1)), modifier = Modifier.weight(1f).testTag("decrease_cycle_button")) { Text("⬅ Prev Month", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Text(text = "Archive Current Round", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isWriteAuthorized) Color(0xFF334155) else Color(0xFF94A3B8))
                    Text(text = "When all ${members.count { it.isActive }} members have received their rotational payout, complete Round ${equb.currentRound} to archive audit history and start 'Round ${equb.currentRound + 1}' without losing old logs.", fontSize = 11.sp, color = Color(0xFF64748B))
                    Button(onClick = { viewModel.archiveAndStartNextRound() }, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), disabledContainerColor = Color(0xFFCBD5E1)), modifier = Modifier.fillMaxWidth().testTag("archive_round_button")) { Text("Archive & Start Round ${equb.currentRound + 1}", fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Share Database Backup File (JSON)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = "Export the entire local database containing current members, payment registers, and audit trails to copy or share with your co-organizer via Telegram/Bluetooth.", fontSize = 11.sp, color = Color(0xFF64748B))
                    Button(onClick = { coroutineScope.launch { val backupText = viewModel.getExportString()
                                clipboardManager.setText(AnnotatedString(backupText))
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, backupText) }
                                context.startActivity(Intent.createChooser(intent, "Share JSON Backup"))
                                viewModel.feedbackMessage.emit("Database JSON copied to clipboard and shared!") } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)), modifier = Modifier.fillMaxWidth().testTag("export_backup_button")) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Export & Share Backup 📤", fontWeight = FontWeight.Bold) } }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Import Co-Organizer Sync State", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = "Paste the JSON database backup text received from your co-organizer here to run automatic conflict checking and preview changes before writing them to disk.", fontSize = 11.sp, color = Color(0xFF64748B))
                    DoubleTapOutlinedTextField(value = textToImport, onValueChange = { viewModel.importedJsonString.value = it }, placeholder = { Text("Paste JSON backup file text here...") }, enabled = isWriteAuthorized, modifier = Modifier.fillMaxWidth().height(110.dp).testTag("import_json_input"), shape = RoundedCornerShape(12.dp))
                    Button(onClick = { if (textToImport.isNotBlank()) { viewModel.analyzeBackup(textToImport) } }, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5), disabledContainerColor = Color(0xFFCBD5E1)), modifier = Modifier.fillMaxWidth().testTag("analyze_sync_button")) { Text("Analyze Sync Differences 🔍", fontWeight = FontWeight.Bold) }
                }
            }
        }

        if (pendingData != null && incomingDiffs.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(2.dp, Color(0xFFD97706)), modifier = Modifier.testTag("sync_conflict_visualizer_card")) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(text = "Reviewing Co-Organizer's Differences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            IconButton(onClick = { viewModel.discardSyncProposal() }) { Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color(0xFFEF4444)) } }
                        Text(text = "A total of ${incomingDiffs.size} records are different. Review and confirm below to merge data into your offline ledger.", fontSize = 12.sp, color = Color(0xFF78350F))
                        HorizontalDivider(color = Color(0xFFFDE68A))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                            incomingDiffs.forEach { diff ->
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = if (diff.changeType == "Added") "➕" else "✏️", fontSize = 12.sp)
                                    Column { Text(text = "[${diff.category}] ${diff.changeType}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309))
                                        Text(text = diff.description, fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium) }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFFDE68A))
                        Button(onClick = { viewModel.confirmSyncImport() }, enabled = isWriteAuthorized, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), disabledContainerColor = Color(0xFFCBD5E1)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().testTag("confirm_sync_import_btn")) { Text("Confirm & Sync All Changes ✅", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
