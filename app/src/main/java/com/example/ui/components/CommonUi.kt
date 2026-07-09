package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Member
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HeaderBar(
    equbName: String,
    round: Int,
    cycleIndex: Int,
    cycleType: String,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = equbName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                val cycleLabel = when (cycleType) {
                    "Weekly" -> "ሳምንት"
                    "Bi-weekly" -> "ሁለት ሳምንት"
                    else -> "ወር"
                }
                Text(
                    text = "ዙር $round • $cycleLabel $cycleIndex",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            val style = when (role) {
                "CHAIRMAN" -> Triple(Color(0xFFE0E7FF), Color(0xFF4F46E5), "ሊቀመንበር (A)")
                "CO_CHAIR" -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "ምክትል (B)")
                else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "ተራ አባል")
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(style.first)
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
                            .background(style.second)
                    )
                    Text(
                        text = style.third,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

@Composable
fun InitialSetupScreen(
    onSetup: (name: String, contribution: Long, cycle: String, startDate: String, autoDraw: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contributionStr by remember { mutableStateOf("") }
    var cycleType by remember { mutableStateOf("ወርሃዊ") }
    var startDate by remember { mutableStateOf("") }
    var autoDraw by remember { mutableStateOf(true) }

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
                    text = "አዲስ ዕቁብ ያቋቁሙ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                
                Text(
                    text = "የኢትዮጵያ የዙር ቁጠባ ማህበር (ዕቁብ) አስተዳደር።",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DoubleTapOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("የዕቁብ ስም") },
                    placeholder = { Text("ምሳሌ፡ የአቢሲንያ ነጋዴዎች ዕቁብ") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_equb_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                DoubleTapOutlinedTextField(
                    value = contributionStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) contributionStr = it },
                    label = { Text("$cycleType መዋጮ (በብር)") },
                    placeholder = { Text("ምሳሌ፡ 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("setup_equb_contribution"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("የመዋጮ ድግግሞሽ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ሳምንታዊ", "ሁለት ሳምንት", "ወርሃዊ").forEach { freq ->
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
                    label = { Text("የመጀመሪያ ቀን") },
                    placeholder = { Text("ዓመተ-ምህረት-ወር-ቀን") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_equb_start_date"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { autoDraw = !autoDraw },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("አውቶማቲክ ዕጣ ማውጫ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("ሁሉም ሰው ሲከፍል ሲስተሙ በራሱ ዕጣ ያወጣል።", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = autoDraw, onCheckedChange = { autoDraw = it })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val contrib = contributionStr.toLongOrNull() ?: 0
                        val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                        
                        if (trimmedName.isBlank()) return@Button
                        if (contrib <= 0) return@Button
                        if (!startDate.matches(datePattern)) return@Button

                        val engCycle = when(cycleType) {
                            "ሳምንታዊ" -> "Weekly"
                            "ሁለት ሳምንት" -> "Bi-weekly"
                            else -> "Monthly"
                        }
                        onSetup(trimmedName, contrib, engCycle, startDate, autoDraw)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("setup_submit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("ዕቁቡን ጀምር", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    activeTab: String,
    onTabSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val tabs = listOf(
            Triple("home", "መነሻ", Icons.Default.Home),
            Triple("members", "አባላት", Icons.Default.Group),
            Triple("sms", "መልዕክት", Icons.Default.Mail),
            Triple("reports", "ሪፖርት", Icons.Default.Analytics),
            Triple("backups", "ምትኬ", Icons.Default.Backup)
        )

        tabs.forEach { (tabId, label, icon) ->
            NavigationBarItem(
                selected = activeTab == tabId,
                alwaysShowLabel = false,
                onClick = { onTabSelect(tabId) },
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
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
    var isEditingByDoubleTap by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val finalKeyboardActions = KeyboardActions(
        onDone = {
            focusManager.clearFocus()
            keyboardController?.hide()
            isEditingByDoubleTap = false
            keyboardActions.onDone?.invoke(this)
        },
        onNext = {
            focusManager.moveFocus(FocusDirection.Next)
            keyboardActions.onNext?.invoke(this)
        },
        onSearch = {
            focusManager.clearFocus()
            keyboardController?.hide()
            isEditingByDoubleTap = false
            keyboardActions.onSearch?.invoke(this)
        },
        onGo = {
            focusManager.clearFocus()
            keyboardController?.hide()
            isEditingByDoubleTap = false
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

    Box(
        modifier = modifier
            .pointerInput(enabled, isEditingByDoubleTap) {
                if (enabled && !isEditingByDoubleTap) {
                    detectTapGestures(
                        onDoubleTap = {
                            isEditingByDoubleTap = true
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    )
                }
            }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (!enabled || readOnly) return@OutlinedTextField
                if (isEditingByDoubleTap) {
                    onValueChange(it)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        isEditingByDoubleTap = false
                    }
                },
            enabled = enabled,
            readOnly = readOnly || !isEditingByDoubleTap,
            label = label ?: {
                if (!isEditingByDoubleTap && value.isEmpty()) {
                    Text("Double tap to edit")
                }
            },
            placeholder = placeholder ?: {
                if (!isEditingByDoubleTap && value.isEmpty()) {
                    Text("Double tap to edit")
                }
            },
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
