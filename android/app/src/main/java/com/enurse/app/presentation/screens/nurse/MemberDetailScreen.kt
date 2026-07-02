package com.enurse.app.presentation.screens.nurse

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.viewmodel.NurseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    groupId: String,
    memberId: String,
    onGoBack: () -> Unit,
    viewModel: NurseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(memberId) {
        viewModel.loadMemberEvents(groupId, memberId)
        viewModel.loadMemberStrategies(groupId, memberId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成员详情") },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("事件") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("策略") })
            }

            when (selectedTab) {
                0 -> {
                    // Events tab
                    val events = state.memberEvents
                    if (events != null) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("大便记录", style = MaterialTheme.typography.titleSmall)
                            val bmRecords = events["bmRecords"] as? List<*> ?: emptyList<Any>()
                            Text("共 ${bmRecords.size} 条")

                            Text("用药记录", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                            val medRecords = events["medRecords"] as? List<*> ?: emptyList<Any>()
                            Text("共 ${medRecords.size} 条")

                            Text("警报", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                            val alerts = events["alerts"] as? List<*> ?: emptyList<Any>()
                            Text("共 ${alerts.size} 条")
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
                1 -> {
                    // Strategies tab
                    val strategies = state.memberStrategies
                    if (strategies != null) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val bmSettings = strategies["bmSettings"] as? Map<*, *>
                            if (bmSettings != null) {
                                Text("大便策略", style = MaterialTheme.typography.titleMedium)
                                Text("间隔: ${bmSettings["intervalDays"]} 天")
                                Text("时间: ${bmSettings["reminderTime"]}")
                                val isLocked = bmSettings["isLocked"] as? Boolean ?: false
                                Row {
                                    Text("状态: ${if (isLocked) "已锁定" else "未锁定"}")
                                    TextButton(onClick = { viewModel.toggleLock(groupId, memberId, "bm") }) {
                                        Text(if (isLocked) "解锁" else "锁定")
                                    }
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
