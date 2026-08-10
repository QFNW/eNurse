package com.enurse.app.presentation.screens.nurse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.data.remote.dto.request.UpdateMemberStrategiesRequest
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.NurseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    groupId: String,
    userId: String,
    onGoBack: () -> Unit,
    viewModel: NurseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("事件", "策略")

    LaunchedEffect(groupId, userId) {
        viewModel.loadMemberEvents(groupId, userId)
        viewModel.loadMemberStrategies(groupId, userId)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("成员详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, label ->
                    FilterChip(selected = selectedTab == i, onClick = { selectedTab = i }, label = { Text(label) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary, selectedLabelColor = androidx.compose.ui.graphics.Color.White))
                }
            }

            // Proxy record section
            var showProxy by remember { mutableStateOf(false) }
            var proxyNotes by remember { mutableStateOf("") }
            val proxyMeds = state.memberStrategies?.get("medications") as? List<*> ?: emptyList<Any>()

            if (!state.error.isNullOrBlank()) {
                Text(state.error!!, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = if (state.error!!.startsWith("✓")) SuccessGreen else AlertRed)
            }

            Card(onClick = { showProxy = !showProxy }, shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBg), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("💉 代打卡 ${if (showProxy) "▲" else "▼"}", modifier = Modifier.padding(14.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Primary)
            }

            if (showProxy) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    OutlinedButton(onClick = { viewModel.proxyBm(groupId, userId, proxyNotes.ifBlank { null }) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("💩 代大便打卡") }
                    Spacer(Modifier.height(6.dp))
                    proxyMeds.forEach { med ->
                        val m = med as? Map<*, *> ?: return@forEach
                        OutlinedButton(onClick = { viewModel.proxyMedication(groupId, userId, m["id"]?.toString() ?: "", proxyNotes.ifBlank { null }) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("💊 代${m["name"]?.toString() ?: ""} 打卡") }
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedTextField(value = proxyNotes, onValueChange = { proxyNotes = it }, label = { Text("备注（可选）") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                    Spacer(Modifier.height(8.dp))
                }
            }

            when (selectedTab) {
                0 -> MemberEventsTab(events = state.memberEvents)
                1 -> MemberStrategiesTab(strategies = state.memberStrategies,
                    onToggleLock = { target, scheduleId -> viewModel.toggleLock(groupId, userId, target, scheduleId) },
                    onCreateMedication = { name, dosage, schedType, interval, times ->
                        viewModel.createMedicationForPatient(groupId, userId, name, dosage, schedType, interval, times)
                    },
                    onUpdateBmSettings = { intervalDays, reminderTime ->
                        val req = UpdateMemberStrategiesRequest(
                            intervalDays = intervalDays?.toIntOrNull(),
                            reminderTime = reminderTime
                        )
                        if (req.intervalDays != null || req.reminderTime != null) {
                            viewModel.updateMemberStrategies(groupId, userId, req)
                        }
                    },
                    onUpdateMedSchedule = { scheduleId, schedType, interval, times ->
                        viewModel.updateMemberStrategies(
                            groupId, userId,
                            UpdateMemberStrategiesRequest(
                                scheduleId = scheduleId,
                                scheduleType = schedType,
                                intervalHours = interval,
                                fixedTimes = times
                            )
                        )
                    })
            }
        }
    }
}

@Composable
private fun MemberEventsTab(events: Map<String, List<Any>>?) {
    if (events == null || events.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("暂无事件", color = TextHint, fontSize = 14.sp) }
        return
    }
    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        events["bmRecords"]?.let { records ->
            Text("💩 大便记录 — 共${records.size}条", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            records.forEach { record ->
                val map = record as? Map<*, *>
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("时间: ${map?.get("recordedAt")?.toString()?.take(16)?.replace("T", " ") ?: "-"}", fontSize = 14.sp)
                        map?.get("notes")?.toString()?.takeIf { it != "null" }?.let { Text("备注: $it", fontSize = 13.sp, color = TextSecondary) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        events["medRecords"]?.let { records ->
            Text("💊 用药记录 — 共${records.size}条", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            records.forEach { record ->
                val map = record as? Map<*, *>
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("药品: ${map?.get("medication")?.let { (it as? Map<*,*>)?.get("name") } ?: "-"}", fontSize = 14.sp)
                        Text("时间: ${map?.get("takenAt")?.toString()?.take(16)?.replace("T", " ") ?: "-"}", fontSize = 13.sp, color = TextSecondary)
                        map?.get("notes")?.toString()?.takeIf { it != "null" }?.let { Text("备注: $it", fontSize = 13.sp, color = TextSecondary) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        events["alerts"]?.let { alertList ->
            Text("⚠ 警报 — 共${alertList.size}条", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AlertRed)
            Spacer(Modifier.height(8.dp))
            alertList.forEach { alert ->
                val map = alert as? Map<*, *>
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = ErrorBg)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(map?.get("message")?.toString() ?: "-", fontSize = 14.sp, color = AlertRed)
                        Text(map?.get("createdAt")?.toString()?.take(16)?.replace("T", " ") ?: "-", fontSize = 12.sp, color = TextHint)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberStrategiesTab(
    strategies: Map<String, Any>?,
    onToggleLock: (String, String?) -> Unit,
    onCreateMedication: (String, String?, String?, Double?, List<String>?) -> Unit,
    onUpdateBmSettings: (String?, String?) -> Unit,
    onUpdateMedSchedule: (String, String, Double?, List<String>?) -> Unit
) {
    var showCreateMed by remember { mutableStateOf(false) }
    var newMedName by remember { mutableStateOf("") }
    var newMedDosage by remember { mutableStateOf("") }
    var editInterval by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var showBmEdit by remember { mutableStateOf(false) }
    // 排班编辑状态
    var showMedEdit by remember { mutableStateOf(false) }
    var editScheduleId by remember { mutableStateOf("") }
    var editSchedType by remember { mutableStateOf("INTERVAL") }
    var editSchedTimes by remember { mutableStateOf("") }
    var editSchedInterval by remember { mutableStateOf("8") }

    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        OutlinedButton(onClick = { showCreateMed = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("＋ 创建药品", color = Primary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))

        if (strategies == null || strategies.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("暂无策略数据", color = TextHint, fontSize = 14.sp) }
            // Still show dialogs if needed
            if (showCreateMed) CreateMedDialog(showCreateMed, { showCreateMed = false }, newMedName, { newMedName = it }, newMedDosage, { newMedDosage = it }, onCreateMedication)
            if (showBmEdit) BmEditDialog(showBmEdit, { showBmEdit = false }, editInterval, { editInterval = it }, editTime, { editTime = it }, onUpdateBmSettings)
            return@Column
        }

        val bmSettings = strategies["bmSettings"] as? Map<*, *>
        if (bmSettings != null) {
            val isLocked = bmSettings["isLocked"] as? Boolean ?: false
            val intervalDays = bmSettings["intervalDays"]?.toString() ?: "-"
            val reminderTime = bmSettings["reminderTime"]?.toString() ?: "-"
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Text("💩 大便提醒策略", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (isLocked) AlertRed else SuccessGreen)) {
                            Text(if (isLocked) "已锁定" else "未锁定", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("间隔: $intervalDays 天", fontSize = 14.sp, color = TextSecondary)
                    Text("提醒时间: $reminderTime", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onToggleLock("bm", null) }, shape = RoundedCornerShape(28.dp), modifier = Modifier.height(36.dp)) { Text(if (isLocked) "解锁" else "锁定", fontSize = 13.sp, color = Primary) }
                        // 锁定时不显示编辑按钮（与服务端 Forbidden 语义一致）
                        if (!isLocked) {
                            OutlinedButton(onClick = { editInterval = intervalDays; editTime = reminderTime; showBmEdit = true }, shape = RoundedCornerShape(28.dp), modifier = Modifier.height(36.dp)) { Text("编辑", fontSize = 13.sp, color = Primary) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        val medications = strategies["medications"] as? List<*>
        if (medications != null && medications.isNotEmpty()) {
            Text("💊 用药排班", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            medications.forEach { med ->
                val map = med as? Map<*, *> ?: return@forEach
                val name = map["name"]?.toString() ?: "-"
                val schedules = map["schedules"] as? List<*> ?: emptyList<Any>()
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        schedules.forEach { sch ->
                            val s = sch as? Map<*, *> ?: return@forEach
                            val sId = s["id"]?.toString() ?: ""
                            val sLocked = s["isLocked"] as? Boolean ?: false
                            val sType = s["scheduleType"]?.toString() ?: ""
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(when (sType) {
                                        "FIXED_TIME" -> "每日 ${(s["fixedTimes"] as? List<*>)?.joinToString("、") ?: "-"}"
                                        "INTERVAL" -> "每${s["intervalHours"]}小时"
                                        else -> sType
                                    }, fontSize = 14.sp, color = TextSecondary)
                                }
                                OutlinedButton(onClick = { onToggleLock("medication_schedule", sId) }, shape = RoundedCornerShape(28.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                    Text(if (sLocked) "解锁" else "锁定", fontSize = 12.sp, color = Primary)
                                }
                                if (!sLocked) {
                                    Spacer(Modifier.width(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            editScheduleId = sId
                                            editSchedType = sType.ifBlank { "INTERVAL" }
                                            editSchedTimes = (s["fixedTimes"] as? List<*>)?.joinToString(",") ?: ""
                                            editSchedInterval = s["intervalHours"]?.toString() ?: "8"
                                            showMedEdit = true
                                        },
                                        shape = RoundedCornerShape(28.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) { Text("编辑", fontSize = 12.sp, color = Primary) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateMed) CreateMedDialog(showCreateMed, { showCreateMed = false }, newMedName, { newMedName = it }, newMedDosage, { newMedDosage = it }, onCreateMedication)
    if (showBmEdit) BmEditDialog(showBmEdit, { showBmEdit = false }, editInterval, { editInterval = it }, editTime, { editTime = it }, onUpdateBmSettings)
    if (showMedEdit) MedScheduleEditDialog(
        scheduleId = editScheduleId,
        initialType = editSchedType,
        initialTimes = editSchedTimes,
        initialInterval = editSchedInterval,
        onDismiss = { showMedEdit = false },
        onSave = { sid, type, interval, times -> onUpdateMedSchedule(sid, type, interval, times); showMedEdit = false }
    )
}

@Composable
private fun CreateMedDialog(
    show: Boolean, onDismiss: () -> Unit,
    name: String, onNameChange: (String) -> Unit,
    dosage: String, onDosageChange: (String) -> Unit,
    onCreate: (String, String?, String?, Double?, List<String>?) -> Unit
) {
    if (!show) return
    var schedType by remember { mutableStateOf("FIXED_TIME") }
    var schedTimes by remember { mutableStateOf("08:00,20:00") }
    var schedInterval by remember { mutableStateOf("8") }
    var errorText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("创建药品") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("药品名") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = dosage, onValueChange = onDosageChange, label = { Text("剂量（可选）") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                Spacer(Modifier.height(12.dp))
                Text("提醒策略", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = schedType == "FIXED_TIME", onClick = { schedType = "FIXED_TIME" },
                        label = { Text("固定时间") }, shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBg, selectedLabelColor = Primary))
                    FilterChip(selected = schedType == "INTERVAL", onClick = { schedType = "INTERVAL" },
                        label = { Text("间隔") }, shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBg, selectedLabelColor = Primary))
                }
                Spacer(Modifier.height(8.dp))
                // 单一 TextField 绑定激活类型（项目规则：不切换 TextField，避免输入状态丢失）
                OutlinedTextField(
                    value = if (schedType == "FIXED_TIME") schedTimes else schedInterval,
                    onValueChange = { if (schedType == "FIXED_TIME") schedTimes = it else schedInterval = it },
                    label = { Text(if (schedType == "FIXED_TIME") "时间 (逗号分隔, 如 08:00,20:00)" else "每 N 小时") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                errorText.takeIf { it.isNotEmpty() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = AlertRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                // 全角标点归一（用户常输入中文标点）
                val normalized = schedTimes.replace('：', ':').replace('，', ',')
                val times = if (schedType == "FIXED_TIME") normalized.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                val interval = if (schedType == "INTERVAL") schedInterval.toDoubleOrNull() else null
                when {
                    schedType == "FIXED_TIME" && (times.isEmpty() || !times.all { it.matches(Regex("^\\d{2}:\\d{2}$")) }) ->
                        errorText = "时间格式须为 HH:mm（如 08:00,20:00）"
                    schedType == "INTERVAL" && (interval == null || interval <= 0) ->
                        errorText = "小时数须为正数（如 6 或 0.5）"
                    else -> {
                        onCreate(name, dosage.ifBlank { null }, schedType, interval, times.takeIf { schedType == "FIXED_TIME" })
                        onDismiss()
                    }
                }
            }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun MedScheduleEditDialog(
    scheduleId: String,
    initialType: String,
    initialTimes: String,
    initialInterval: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Double?, List<String>?) -> Unit
) {
    var schedType by remember { mutableStateOf(initialType) }
    var schedTimes by remember { mutableStateOf(initialTimes) }
    var schedInterval by remember { mutableStateOf(initialInterval) }
    var errorText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("编辑排班") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = schedType == "FIXED_TIME", onClick = { schedType = "FIXED_TIME" },
                        label = { Text("固定时间") }, shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBg, selectedLabelColor = Primary))
                    FilterChip(selected = schedType == "INTERVAL", onClick = { schedType = "INTERVAL" },
                        label = { Text("间隔") }, shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBg, selectedLabelColor = Primary))
                }
                Spacer(Modifier.height(12.dp))
                // 单一 TextField 绑定激活类型（项目规则）
                OutlinedTextField(
                    value = if (schedType == "FIXED_TIME") schedTimes else schedInterval,
                    onValueChange = { if (schedType == "FIXED_TIME") schedTimes = it else schedInterval = it },
                    label = { Text(if (schedType == "FIXED_TIME") "时间 (逗号分隔, 如 08:00,20:00)" else "每 N 小时") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                errorText.takeIf { it.isNotEmpty() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = AlertRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val normalized = schedTimes.replace('：', ':').replace('，', ',')
                val times = if (schedType == "FIXED_TIME") normalized.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                val interval = if (schedType == "INTERVAL") schedInterval.toDoubleOrNull() else null
                when {
                    schedType == "FIXED_TIME" && (times.isEmpty() || !times.all { it.matches(Regex("^\\d{2}:\\d{2}$")) }) ->
                        errorText = "时间格式须为 HH:mm（如 08:00,20:00）"
                    schedType == "INTERVAL" && (interval == null || interval <= 0) ->
                        errorText = "小时数须为正数（如 6 或 0.5）"
                    else -> {
                        onSave(scheduleId, schedType, interval, times.takeIf { schedType == "FIXED_TIME" })
                        onDismiss()
                    }
                }
            }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun BmEditDialog(
    show: Boolean, onDismiss: () -> Unit,
    interval: String, onIntervalChange: (String) -> Unit,
    time: String, onTimeChange: (String) -> Unit,
    onSave: (String?, String?) -> Unit
) {
    if (!show) return
    var errorText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("编辑大便策略") },
        text = {
            Column {
                OutlinedTextField(value = interval, onValueChange = onIntervalChange, label = { Text("间隔天数") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = time, onValueChange = onTimeChange, label = { Text("提醒时间 (HH:mm)") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                errorText.takeIf { it.isNotEmpty() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, color = AlertRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val days = interval.ifBlank { null }?.toIntOrNull()
                val t = time.ifBlank { null }
                when {
                    interval.isNotBlank() && (days == null || days < 1 || days > 30) ->
                        errorText = "间隔天数须为 1~30 的整数"
                    t != null && !t.matches(Regex("^\\d{2}:\\d{2}$")) ->
                        errorText = "提醒时间格式须为 HH:mm（如 08:00）"
                    else -> { onSave(interval.ifBlank { null }, t); onDismiss() }
                }
            }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
