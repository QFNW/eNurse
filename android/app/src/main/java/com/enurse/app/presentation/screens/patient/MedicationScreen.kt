package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.domain.repository.MedicationRepository
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(onGoBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repo = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MedicationRepoEntryPoint::class.java
        )
        entryPoint.medicationRepository()
    }
    val userId = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MedicationRepoEntryPoint::class.java
        )
        entryPoint.tokenDataStore()
    }

    var medications by remember { mutableStateOf<List<MedicationResponse>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDosage by remember { mutableStateOf("") }
    var showSchedule by remember { mutableStateOf<String?>(null) } // medication ID being edited
    var scheduleType by remember { mutableStateOf("FIXED_TIME") }
    var scheduleTimes by remember { mutableStateOf("08:00,20:00") }
    var scheduleInterval by remember { mutableStateOf("8") }

    suspend fun refreshMeds() {
        val result = repo.getMedications()
        if (result.isSuccess) {
            medications = result.getOrThrow()
        } else {
            snackbarHostState.showSnackbar("加载失败: ${result.exceptionOrNull()?.message}")
        }
    }

    LaunchedEffect(Unit) { refreshMeds() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用药管理") },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("返回") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(medications) { med ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(med.name, style = MaterialTheme.typography.titleMedium)
                            med.dosage?.let { Text("剂量: $it") }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    scope.launch {
                                        val uid = userId.userId.first() ?: ""
                                        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(java.util.Date())
                                        val result = repo.record(uid, med.id, now)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("打卡成功")
                                            refreshMeds()
                                        } else {
                                            val e = result.exceptionOrNull()
                                            val isNetworkError = e is java.net.ConnectException ||
                                                e is java.net.UnknownHostException ||
                                                e is java.net.SocketTimeoutException ||
                                                e is java.net.SocketException
                                            if (isNetworkError) {
                                                repo.recordOffline(uid, med.id, med.name, now)
                                                snackbarHostState.showSnackbar("网络异常，已本地保存")
                                            } else {
                                                snackbarHostState.showSnackbar("打卡失败: ${e?.message}")
                                            }
                                        }
                                    }
                                }) { Text("打卡") }
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            repo.deleteMedication(med.id)
                                            snackbarHostState.showSnackbar("已删除")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("删除失败: ${e.message}")
                                            return@launch
                                        }
                                        // Refresh after successful delete (best-effort)
                                        try { refreshMeds() } catch (_: Exception) {}
                                    }
                                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                            }
                            // Schedule indicator
                            if (med.schedules != null && med.schedules.isNotEmpty()) {
                                Text(
                                    med.schedules!!.joinToString(", ") { s ->
                                        when (s.scheduleType) {
                                            "FIXED_TIME" -> "每日 ${(s.fixedTimes?:emptyList<String>()).joinToString("、")}"
                                            "INTERVAL" -> "每${s.intervalHours}小时"
                                            else -> "已设提醒"
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedButton(
                                onClick = { showSchedule = med.id },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(if (med.schedules != null && med.schedules!!.any { it.isEnabled }) "修改提醒" else "设置提醒")
                            }
                        }
                    }
                }
            }
        }
    }

    // Schedule dialog
    if (showSchedule != null) {
        AlertDialog(
            onDismissRequest = { showSchedule = null },
            title = { Text("设置用药提醒") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = scheduleType == "FIXED_TIME", onClick = { scheduleType = "FIXED_TIME" }, label = { Text("固定时间") })
                        FilterChip(selected = scheduleType == "INTERVAL", onClick = { scheduleType = "INTERVAL" }, label = { Text("间隔") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (scheduleType == "FIXED_TIME") {
                        OutlinedTextField(
                            value = scheduleTimes, onValueChange = { scheduleTimes = it },
                            label = { Text("时间 (逗号分隔, 如 08:00,20:00)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = scheduleInterval, onValueChange = { scheduleInterval = it },
                            label = { Text("每 N 小时") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val medId = showSchedule!!
                        val times = if (scheduleType == "FIXED_TIME")
                            scheduleTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        else emptyList()
                        val interval = scheduleInterval.toIntOrNull()
                        repo.createSchedule(medId, scheduleType, times, interval)
                            .onSuccess {
                                snackbarHostState.showSnackbar("提醒已设置")
                                refreshMeds()
                                showSchedule = null
                            }
                            .onFailure { snackbarHostState.showSnackbar("设置失败: ${it.message}") }
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSchedule = null }) { Text("取消") } }
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加药品") },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("药品名") })
                    OutlinedTextField(value = newDosage, onValueChange = { newDosage = it }, label = { Text("剂量（可选）") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = repo.createMedication(newName, newDosage.ifBlank { null })
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("已添加")
                            refreshMeds()
                            showAdd = false; newName = ""; newDosage = ""
                        } else {
                            snackbarHostState.showSnackbar("添加失败: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }
}

@EntryPoint @InstallIn(SingletonComponent::class)
interface MedicationRepoEntryPoint {
    fun medicationRepository(): MedicationRepository
    fun tokenDataStore(): com.enurse.app.data.local.datastore.TokenDataStore
}
