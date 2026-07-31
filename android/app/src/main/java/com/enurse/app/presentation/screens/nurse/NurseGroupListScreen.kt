package com.enurse.app.presentation.screens.nurse

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.NurseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseGroupListScreen(
    onGoGroup: (String) -> Unit,
    onGoBack: () -> Unit,
    viewModel: NurseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showJoinStaff by remember { mutableStateOf(false) }
    var staffInviteCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    var groupName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadGroups() }

    // Watch join-as-staff result: close dialog on success, show error on failure
    LaunchedEffect(state.error) {
        val err = state.error
        if (err != null && showJoinStaff) {
            if (err.startsWith("✓")) {
                showJoinStaff = false
                staffInviteCode = ""
                joinError = null
            } else {
                joinError = err
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("我的小组", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                actions = { TextButton(onClick = { showJoinStaff = true }) { Text("加入小组", color = Primary, fontSize = 14.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = Primary,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (state.groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(if (state.error != null) "加载失败" else "还没有小组", fontSize = 16.sp, color = TextSecondary)
                    Text(if (state.error != null) (state.error ?: "") else "点击 + 创建一个", fontSize = 14.sp, color = TextHint)
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.loadGroups() }, shape = RoundedCornerShape(28.dp)) {
                            Text("重试", color = Primary, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                items(state.groups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        onClick = { onGoGroup(group.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                Text("邀请码: ${group.inviteCode}", fontSize = 13.sp, color = TextHint)
                            }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryBg)
                            ) {
                                Text(
                                    "${group._count?.members ?: 0}人",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (!state.error.isNullOrBlank()) {
        Text(state.error!!, color = AlertRed, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    }

    // Create dialog
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("创建小组", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("小组名称") },
                    shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createGroup(groupName)
                    showCreate = false; groupName = ""
                }, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } }
        )
    }

    // Join as staff dialog
    if (showJoinStaff) {
        AlertDialog(
            onDismissRequest = { showJoinStaff = false; joinError = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("加入小组为管理员") },
            text = {
                Column {
                    OutlinedTextField(value = staffInviteCode, onValueChange = { staffInviteCode = it; joinError = null },
                        label = { Text("邀请码") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
                    if (joinError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(joinError!!, color = AlertRed, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (staffInviteCode.isNotBlank()) {
                        joinError = null
                        viewModel.joinAsStaff(staffInviteCode)
                    }
                }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("加入") }
            },
            dismissButton = { TextButton(onClick = { showJoinStaff = false; joinError = null }) { Text("取消") } }
        )
    }
}
