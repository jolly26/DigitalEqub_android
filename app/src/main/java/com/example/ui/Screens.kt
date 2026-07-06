package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.EqubViewModel
import com.example.ui.backups.BackupsScreen
import com.example.ui.components.BottomNavBar
import com.example.ui.components.HeaderBar
import com.example.ui.components.InitialSetupScreen
import com.example.ui.components.RoleSecurityDialog
import com.example.ui.home.HomeScreen
import com.example.ui.members.MembersScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.sms.SmsScreen

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

    var showRoleSecurityDialog by remember { mutableStateOf(value = false) }

    // Collect toasts
    LaunchedEffect(key1 = true) {
        feedbackMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    if (showRoleSecurityDialog) {
        RoleSecurityDialog(
            viewModel = viewModel,
        ) { showRoleSecurityDialog = false }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FF),
        topBar = {
            HeaderBar(
                equbName = equb?.name ?: "No Active Equb",
                round = equb?.currentRound ?: 1,
                cycleIndex = equb?.currentCycleIndex ?: 1,
                role = currentRole
            ) {
                showRoleSecurityDialog = true
            }
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
                InitialSetupScreen { name, contribution, cycle, start, autoDraw ->
                    viewModel.setupEqub(name, contribution, cycle, start, autoDraw)
                }
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
