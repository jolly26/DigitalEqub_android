package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.EqubViewModel

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
                                Text("Forgot PIN?", color = Color(0xFF4F46E5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    // Recovery Panel
                    Text(
                        text = "Emergency Recovery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "Since Equb is fully local-first and offline-secure, security PINs are stored only on your device. Contact your admin for recovery support.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isRecoveryMode = false; errorMessage = "" }) {
                            Text("Back", color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }
    }
}
