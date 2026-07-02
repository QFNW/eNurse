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
fun NurseGroupListScreen(
    onGoDetail: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: NurseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadGroups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的小组") },
                actions = { TextButton(onClick = onLogout) { Text("退出") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Text("+") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) CircularProgressIndicator()
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(state.groups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onGoDetail(group.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                            Text("邀请码: ${group.inviteCode}")
                            Text("成员: ${group._count?.members ?: 0}")
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("创建小组") },
            text = {
                OutlinedTextField(
                    value = newGroupName, onValueChange = { newGroupName = it },
                    label = { Text("小组名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createGroup(newGroupName)
                    showCreate = false
                    newGroupName = ""
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } }
        )
    }
}
