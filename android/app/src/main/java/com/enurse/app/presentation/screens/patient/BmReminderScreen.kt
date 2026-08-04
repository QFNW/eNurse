package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.BmViewModel
import com.enurse.app.reminder.ReminderScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmReminderScreen(
    onGoBack: () -> Unit,
    bmViewModel: BmViewModel = hiltViewModel()
) {
    val bmState by bmViewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    // Save feedback: success → snackbar (state update happens before the snackbar
    // suspends, per project rule); failure → red text + snackbar.
    LaunchedEffect(bmState.lastSaveAt) {
        if (bmState.lastSaveAt > 0L) {
            ReminderScheduler.refresh(context.applicationContext)
            if (!ReminderScheduler.canScheduleExact(context)) showExactAlarmDialog = true
            snackbarHostState.showSnackbar("保存成功")
        }
    }
    LaunchedEffect(bmState.error) {
        bmState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

    // 精确闹钟权限引导（Android 12+ 默认可能未授权，提醒会延迟）
    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("开启精确提醒", fontWeight = FontWeight.SemiBold) },
            text = { Text("系统限制了闹钟的精确触发（提醒可能延迟几分钟）。建议在系统设置中允许「闹钟和提醒」，确保按时提醒。", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showExactAlarmDialog = false
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("去开启") }
            },
            dismissButton = { TextButton(onClick = { showExactAlarmDialog = false }) { Text("取消") } }
        )
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
