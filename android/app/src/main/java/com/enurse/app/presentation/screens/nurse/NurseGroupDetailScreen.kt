package com.enurse.app.presentation.screens.nurse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.viewmodel.NurseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseGroupDetailScreen(
    groupId: String,
    onGoMember: (String) -> Unit,
    onGoBack: () -> Unit,
    viewModel: NurseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetail(groupId)
        viewModel.loadMembers(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedGroup?.name ?: "小组详情") },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.selectedGroup != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("邀请码: ${state.selectedGroup!!.inviteCode}", style = MaterialTheme.typography.titleMedium)
                        Text("成员数: ${state.members.size}")
                    }
                }
            }

            Text("成员列表", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(state.members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onGoMember(member.userId) }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(member.user?.nickname ?: member.user?.email ?: member.userId.take(8), style = MaterialTheme.typography.titleSmall)
                                Text(member.roleInGroup)
                            }
                            if ((member.alertCount ?: 0) > 0) {
                                Text("⚠ ${member.alertCount}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
