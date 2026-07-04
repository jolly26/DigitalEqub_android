package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Member

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
