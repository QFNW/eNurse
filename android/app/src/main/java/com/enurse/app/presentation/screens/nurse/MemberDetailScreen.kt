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
                    OutlinedButton(onClick = { viewModel.proxyBm(groupId, userId, proxyNotes.ifBlank { null }); proxyNotes = "" },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("💩 代大便打卡") }
                    Spacer(Modifier.height(6.dp))
                    proxyMeds.forEach { med ->
                        val m = med as? Map<*, *> ?: return@forEach
                        OutlinedButton(onClick = { viewModel.proxyMedication(groupId, userId, m["id"]?.toString() ?: "", proxyNotes.ifBlank { null }); proxyNotes = "" },
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
                    onCreateMedication = { name, dosage -> viewModel.createMedicationForPatient(groupId, userId, name, dosage) },
                    onUpdateBmSettings = { intervalDays, reminderTime ->
                        val p = mutableMapOf<String, String>()
                        intervalDays?.let { p["intervalDays"] = it }
                        reminderTime?.let { p["reminderTime"] = it }
                        if (p.isNotEmpty()) viewModel.updateMemberStrategies(groupId, userId, p)
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
    onCreateMedication: (String, String?) -> Unit,
    onUpdateBmSettings: (String?, String?) -> Unit
) {
    var showCreateMed by remember { mutableStateOf(false) }
    var newMedName by remember { mutableStateOf("") }
    var newMedDosage by remember { mutableStateOf("") }
    var editInterval by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var showBmEdit by remember { mutableStateOf(false) }

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
                        OutlinedButton(onClick = { editInterval = intervalDays; editTime = reminderTime; showBmEdit = true }, shape = RoundedCornerShape(28.dp), modifier = Modifier.height(36.dp)) { Text("编辑", fontSize = 13.sp, color = Primary) }
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
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateMed) CreateMedDialog(showCreateMed, { showCreateMed = false }, newMedName, { newMedName = it }, newMedDosage, { newMedDosage = it }, onCreateMedication)
    if (showBmEdit) BmEditDialog(showBmEdit, { showBmEdit = false }, editInterval, { editInterval = it }, editTime, { editTime = it }, onUpdateBmSettings)
}

@Composable
private fun CreateMedDialog(
    show: Boolean, onDismiss: () -> Unit,
    name: String, onNameChange: (String) -> Unit,
    dosage: String, onDosageChange: (String) -> Unit,
    onCreate: (String, String?) -> Unit
) {
    if (!show) return
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("创建药品") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("药品名") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = dosage, onValueChange = onDosageChange, label = { Text("剂量（可选）") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) { onCreate(name, dosage.ifBlank { null }); onDismiss() } }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("创建") }
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
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("编辑大便策略") },
        text = {
            Column {
                OutlinedTextField(value = interval, onValueChange = onIntervalChange, label = { Text("间隔天数") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = time, onValueChange = onTimeChange, label = { Text("提醒时间 (HH:mm)") }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(interval.ifBlank { null }, time.ifBlank { null }); onDismiss() }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
