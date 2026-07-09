package com.enurse.app.presentation.screens.patient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.domain.repository.MedicationRepository
import com.enurse.app.presentation.theme.*
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
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, MedicationRepoEntryPoint::class.java)
        ep.medicationRepository()
    }
    val userId = remember {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, MedicationRepoEntryPoint::class.java)
        ep.tokenDataStore()
    }

    var medications by remember { mutableStateOf<List<MedicationResponse>>(emptyList()) }
    var allRecords by remember { mutableStateOf<List<com.enurse.app.data.remote.dto.response.MedicationRecordResponse>>(emptyList()) }
    var expandedRecords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDosage by remember { mutableStateOf("") }
    var showSchedule by remember { mutableStateOf<String?>(null) }
    var scheduleType by remember { mutableStateOf("FIXED_TIME") }
    var scheduleTimes by remember { mutableStateOf("08:00,20:00") }
    var scheduleInterval by remember { mutableStateOf("8") }
    var showRecord by remember { mutableStateOf<MedicationResponse?>(null) }
    var recordNotes by remember { mutableStateOf("") }
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US) }

    suspend fun refreshMeds() {
        val result = repo.getMedications()
        if (result.isSuccess) medications = result.getOrThrow()
        else snackbarHostState.showSnackbar("加载失败: ${result.exceptionOrNull()?.message}")
        repo.getRecords()
            .onSuccess { allRecords = it }
            .onFailure { snackbarHostState.showSnackbar("记录加载失败: ${it.message}") }
    }

    LaunchedEffect(Unit) { refreshMeds() }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("用药管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = Primary,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(medications) { med ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(med.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (med.isActive) SuccessGreen else TextHint)
                            ) {
                                Text(
                                    if (med.isActive) "服用中" else "已停用",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        med.dosage?.let { Text("剂量: $it", fontSize = 14.sp, color = TextSecondary) }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Schedule info
                        if (med.schedules != null && med.schedules.isNotEmpty()) {
                            med.schedules!!.forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔔", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        when (s.scheduleType) {
                                            "FIXED_TIME" -> "每日 ${(s.fixedTimes ?: emptyList<String>()).joinToString("、")}"
                                            "INTERVAL" -> {
                                                val h = s.intervalHours
                                                val d = if (h != null && h == h.toLong().toDouble()) h.toLong().toString() else (h?.toString() ?: "?")
                                                "每${d}小时"
                                            }
                                            else -> "已设提醒"
                                        },
                                        fontSize = 13.sp, color = Primary
                                    )
                                }
                            }
                        }
                        // Records section
                        val medRecords = allRecords.filter { it.medicationId == med.id }
                        if (medRecords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val expanded = expandedRecords.contains(med.id)
                            TextButton(
                                onClick = {
                                    expandedRecords = if (expanded) expandedRecords - med.id else expandedRecords + med.id
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "📋 ${if (expanded) "收起" else "打卡记录"} (${medRecords.size})",
                                    fontSize = 13.sp, color = Primary
                                )
                            }
                            if (expanded) {
                                medRecords.forEach { r ->
                                    Text(
                                        "${r.takenAt.take(16).replace("T", " ")} ${r.notes?.let { "— $it" } ?: ""}",
                                        fontSize = 12.sp, color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showRecord = med; recordNotes = "" },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                modifier = Modifier.height(40.dp)
                            ) { Text("打卡", fontSize = 14.sp) }
                            OutlinedButton(
                                onClick = { showSchedule = med.id },
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    if (med.schedules != null && med.schedules!!.any { it.isEnabled }) "修改提醒" else "设置提醒",
                                    fontSize = 13.sp, color = Primary
                                )
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    try { repo.deleteMedication(med.id); refreshMeds(); snackbarHostState.showSnackbar("已删除") }
                                    catch (e: Exception) { snackbarHostState.showSnackbar("删除失败: ${e.message}") }
                                }
                            }) { Text("删除", color = AlertRed, fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("添加药品", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("药品名") },
                        shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = newDosage, onValueChange = { newDosage = it }, label = { Text("剂量（可选）") },
                        shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val result = repo.createMedication(newName, newDosage.ifBlank { null })
                        if (result.isSuccess) {
                            showAdd = false; newName = ""; newDosage = ""
                            try { refreshMeds() } catch (_: Exception) {}
                            snackbarHostState.showSnackbar("✓ 已添加")
                        } else snackbarHostState.showSnackbar("添加失败: ${result.exceptionOrNull()?.message}")
                    }
                }, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }

    // Record dialog with notes
    if (showRecord != null) {
        AlertDialog(
            onDismissRequest = { showRecord = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("💊 ${showRecord!!.name}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("确认服药打卡", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = recordNotes, onValueChange = { recordNotes = it },
                        label = { Text("备注（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val med = showRecord!!
                    showRecord = null
                    scope.launch {
                        val uid = userId.userId.first() ?: ""
                        val now = dateFormat.format(java.util.Date())
                        val result = repo.record(uid, med.id, now, recordNotes.ifBlank { null })
                        if (result.isSuccess) { refreshMeds(); snackbarHostState.showSnackbar("✓ 打卡成功") }
                        else {
                            val e = result.exceptionOrNull()
                            val isNetErr = e is java.net.ConnectException || e is java.net.UnknownHostException || e is java.net.SocketTimeoutException || e is java.net.SocketException
                            if (isNetErr) { repo.recordOffline(uid, med.id, med.name, now); snackbarHostState.showSnackbar("网络异常，已本地保存") }
                            else snackbarHostState.showSnackbar("打卡失败: ${e?.message}")
                        }
                    }
                }, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("确认打卡") }
            },
            dismissButton = { TextButton(onClick = { showRecord = null }) { Text("取消") } }
        )
    }

    // Schedule dialog
    if (showSchedule != null) {
        AlertDialog(
            onDismissRequest = { showSchedule = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("设置用药提醒", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = scheduleType == "FIXED_TIME", onClick = { scheduleType = "FIXED_TIME" },
                            label = { Text("固定时间") }, shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBg, selectedLabelColor = Primary))
                        FilterChip(selected = scheduleType == "INTERVAL", onClick = { scheduleType = "INTERVAL" },
                            label = { Text("间隔") }, shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBg, selectedLabelColor = Primary))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    AnimatedVisibility(visible = scheduleType == "FIXED_TIME") {
                        OutlinedTextField(value = scheduleTimes, onValueChange = { scheduleTimes = it },
                            label = { Text("时间 (逗号分隔, 如 08:00,20:00)") }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                    }
                    AnimatedVisibility(visible = scheduleType == "INTERVAL") {
                        OutlinedTextField(value = scheduleInterval, onValueChange = { scheduleInterval = it },
                            label = { Text("每 N 小时") }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val medId = showSchedule!!
                    // 解析输入
                    val times = if (scheduleType == "FIXED_TIME") scheduleTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                    val interval = if (scheduleType == "INTERVAL") scheduleInterval.toDoubleOrNull() else null
                    // 校验
                    if (scheduleType == "FIXED_TIME" && (times.isEmpty() || !times.all { it.matches(Regex("^\\d{2}:\\d{2}$")) })) {
                        scope.launch { snackbarHostState.showSnackbar("时间格式须为 HH:mm（如 08:00,20:00）") }
                        return@Button
                    }
                    if (scheduleType == "INTERVAL" && (interval == null || interval <= 0)) {
                        scope.launch { snackbarHostState.showSnackbar("小时数须为正数（如 6 或 0.5）") }
                        return@Button
                    }
                    // 校验通过
                    showSchedule = null
                    scope.launch {
                        val med = medications.find { it.id == medId }
                        med?.schedules?.filter { !it.isLocked }?.forEach { s -> try { repo.deleteSchedule(s.id) } catch (_: Exception) {} }
                        repo.createSchedule(medId, scheduleType, times, interval)
                            .onSuccess { refreshMeds(); snackbarHostState.showSnackbar("✓ 提醒已设置") }
                            .onFailure { snackbarHostState.showSnackbar("设置失败: ${it.message}") }
                    }
                }, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSchedule = null }) { Text("取消") } }
        )
    }
}

@EntryPoint @InstallIn(SingletonComponent::class)
interface MedicationRepoEntryPoint {
    fun medicationRepository(): MedicationRepository
    fun tokenDataStore(): com.enurse.app.data.local.datastore.TokenDataStore
}
