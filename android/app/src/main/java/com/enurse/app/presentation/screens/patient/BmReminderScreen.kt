package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.BmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmReminderScreen(
    onGoBack: () -> Unit,
    bmViewModel: BmViewModel = hiltViewModel()
) {
    val bmState by bmViewModel.state.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("大便提醒", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            // BM Settings Card
            SectionCard("💩 大便提醒") {
                val settings = bmState.settings
                if (settings != null) {
                    var intervalDays by remember(settings) { mutableStateOf(settings.intervalDays.toString()) }
                    var reminderTime by remember(settings) { mutableStateOf(settings.reminderTime) }

                    OutlinedTextField(
                        value = intervalDays, onValueChange = { intervalDays = it },
                        label = { Text("提醒间隔（天）") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reminderTime, onValueChange = { reminderTime = it },
                        label = { Text("提醒时间（HH:mm）") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { bmViewModel.updateSettings(intervalDays = intervalDays.toIntOrNull(), reminderTime = reminderTime, isEnabled = null) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("保存设置", fontWeight = FontWeight.SemiBold) }

                    if (settings.isLocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("策略已由护理人员锁定", color = AlertRed, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
