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
            // Loading
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            // Tab bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { i, label ->
                    FilterChip(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> MemberEventsTab(events = state.memberEvents)
                1 -> MemberStrategiesTab(
                    strategies = state.memberStrategies,
                    onToggleLock = { target, scheduleId ->
                        viewModel.toggleLock(groupId, userId, target, scheduleId)
                    }
                )
            }
        }
    }
}

@Composable
private fun MemberEventsTab(events: Map<String, List<Any>>?) {
    if (events == null || events.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("暂无事件", color = TextHint, fontSize = 14.sp)
        }
        return
    }
    Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        // bmRecords
        events["bmRecords"]?.let { records ->
            Text("💩 大便记录 — 共${records.size}条", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            records.forEach { record ->
                val map = record as? Map<*, *>
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("时间: ${map?.get("recordedAt")?.toString()?.take(16)?.replace("T", " ") ?: "-"}", fontSize = 14.sp)
                        map?.get("notes")?.toString()?.takeIf { it != "null" }?.let {
                            Text("备注: $it", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        // medRecords
        events["medRecords"]?.let { records ->
            Text("💊 用药记录 — 共${records.size}条", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            records.forEach { record ->
                val map = record as? Map<*, *>
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("药品: ${map?.get("medication")?.let { (it as? Map<*,*>)?.get("name") } ?: "-"}", fontSize = 14.sp)
                        Text("时间: ${map?.get("takenAt")?.toString()?.take(16)?.replace("T", " ") ?: "-"}", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        // alerts
        events["alerts"]?.let { alertList ->
            Text("⚠ 警报 — 共${alertList.size}条", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AlertRed)
            Spacer(modifier = Modifier.height(8.dp))
            alertList.forEach { alert ->
                val map = alert as? Map<*, *>
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
    onToggleLock: (String, String?) -> Unit
) {
    if (strategies == null || strategies.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("暂无策略数据", color = TextHint, fontSize = 14.sp)
        }
        return
    }
    Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        // BM settings
        val bmSettings = strategies["bmSettings"] as? Map<*, *>
        if (bmSettings != null) {
            val isLocked = bmSettings["isLocked"] as? Boolean ?: false
            val intervalDays = bmSettings["intervalDays"]?.toString() ?: "-"
            val reminderTime = bmSettings["reminderTime"]?.toString() ?: "-"
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text("💩 大便提醒策略", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isLocked) AlertRed else SuccessGreen)
                        ) {
                            Text(
                                if (isLocked) "已锁定" else "未锁定",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("间隔: $intervalDays 天", fontSize = 14.sp, color = TextSecondary)
                    Text("提醒时间: $reminderTime", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onToggleLock("bm", null) },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (isLocked) "解锁" else "锁定", fontSize = 13.sp, color = Primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        // Medication schedules
        val medications = strategies["medications"] as? List<*>
        if (medications != null && medications.isNotEmpty()) {
            Text("💊 用药排班", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            medications.forEach { med ->
                val map = med as? Map<*, *> ?: return@forEach
                val name = map["name"]?.toString() ?: "-"
                val schedules = map["schedules"] as? List<*> ?: emptyList<Any>()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        schedules.forEach { sch ->
                            val s = sch as? Map<*, *> ?: return@forEach
                            val sId = s["id"]?.toString() ?: ""
                            val sLocked = s["isLocked"] as? Boolean ?: false
                            val sType = s["scheduleType"]?.toString() ?: ""
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        when (sType) {
                                            "FIXED_TIME" -> {
                                                val times = (s["fixedTimes"] as? List<*>)?.joinToString("、") ?: "-"
                                                "每日 $times"
                                            }
                                            "INTERVAL" -> "每${s["intervalHours"]}小时"
                                            else -> sType
                                        },
                                        fontSize = 14.sp, color = TextSecondary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onToggleLock("medication_schedule", sId) },
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text(if (sLocked) "解锁" else "锁定", fontSize = 12.sp, color = Primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
