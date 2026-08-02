package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.components.AlertBanner
import com.enurse.app.presentation.components.BigBmButton
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoCalendar: () -> Unit,
    onGoMedication: () -> Unit,
    onGoSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var bmNotes by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("易护", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = onGoSettings) { Text("设置", color = TextSecondary) }
                    TextButton(onClick = onLogout) { Text("退出", color = TextSecondary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Alert banner — fixed at top, always visible
            AlertBanner(alerts = state.alerts, onResolve = { viewModel.resolveAlert(it) })

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            Spacer(modifier = Modifier.height(32.dp))

            // Notes input
            OutlinedTextField(
                value = bmNotes, onValueChange = { bmNotes = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Big BM Button
            BigBmButton(onClick = { viewModel.recordBm(bmNotes.ifBlank { null }); bmNotes = "" }, isLoading = state.isRecording)

            // BM feedback
            if (state.recordSuccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("✓ 打卡成功！", color = SuccessGreen, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (!state.error.isNullOrBlank()) {
                    Text(state.error!!, color = AlertRed, fontSize = 13.sp)
                }
                LaunchedEffect(state.recordSuccess) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearRecordSuccess()
                }
            } else if (!state.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.error!!, color = AlertRed, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Quick links
            Text("快捷入口", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextHint, modifier = Modifier.padding(start = 20.dp).fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuickLinkCard("📅", "大便日历", onClick = onGoCalendar)
                QuickLinkCard("💊", "用药记录", onClick = onGoMedication)
            }
            } // inner scrolling Column
        } // outer Column
    }
}

@Composable
private fun RowScope.QuickLinkCard(icon: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}
