package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.data.remote.dto.response.GroupMemberResponse
import com.enurse.app.data.remote.dto.response.GroupResponse
import com.enurse.app.domain.repository.GroupRepository
import com.enurse.app.presentation.theme.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(onGoBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val groupRepo = remember {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, GroupRepoEntryPoint::class.java)
        ep.groupRepository()
    }

    var groups by remember { mutableStateOf<List<GroupResponse>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedGroup by remember { mutableStateOf<GroupResponse?>(null) }
    var members by remember { mutableStateOf<List<GroupMemberResponse>>(emptyList()) }
    var membersLoading by remember { mutableStateOf(false) }
    var leaveTarget by remember { mutableStateOf<GroupResponse?>(null) }
    var leaving by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loadError = null
            groupRepo.getMyGroups()
                .onSuccess { groups = it }
                .onFailure { loadError = it.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (selectedGroup == null) "我的小组" else "小组成员", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = {
                        if (selectedGroup != null) selectedGroup = null else onGoBack()
                    }) { Text("← 返回", color = Primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        when {
            // Group roster view
            selectedGroup != null -> {
                val group = selectedGroup!!
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                    Text("邀请码: ${group.inviteCode}", fontSize = 13.sp, color = TextHint)
                                }
                                Text("${members.size} 名成员", fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    if (membersLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                    }
                    items(members) { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.user?.displayName ?: member.user?.nickname ?: "成员", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    Text(roleLabel(member.roleInGroup), fontSize = 12.sp, color = TextHint)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { leaveTarget = group },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            enabled = !leaving
                        ) { Text(if (leaving) "退出中..." else "退出小组", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            // Group list view
            groups == null && loadError == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            loadError != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", fontSize = 16.sp, color = TextSecondary)
                        Text(loadError!!, fontSize = 13.sp, color = TextHint)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { refresh() }, shape = RoundedCornerShape(28.dp)) {
                            Text("重试", color = Primary, fontSize = 14.sp)
                        }
                    }
                }
            }
            groups?.isEmpty() == true -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("还没有加入小组", fontSize = 16.sp, color = TextSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    items(groups ?: emptyList()) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            onClick = {
                                selectedGroup = group
                                members = emptyList()
                                membersLoading = true
                                scope.launch {
                                    groupRepo.getMembers(group.id)
                                        .onSuccess { members = it }
                                        .onFailure {
                                            snackbarHostState.showSnackbar(it.message ?: "加载成员失败")
                                        }
                                    membersLoading = false
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                    Text("邀请码: ${group.inviteCode}", fontSize = 13.sp, color = TextHint)
                                }
                                Text("查看成员 →", fontSize = 13.sp, color = Primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Leave group confirmation
    if (leaveTarget != null) {
        AlertDialog(
            onDismissRequest = { leaveTarget = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("退出小组") },
            text = { Text("确定退出「${leaveTarget!!.name}」吗？退出后将无法查看该小组数据。") },
            confirmButton = {
                Button(
                    onClick = {
                        leaving = true
                        scope.launch {
                            groupRepo.leaveGroup(leaveTarget!!.id)
                                .onSuccess {
                                    leaving = false
                                    leaveTarget = null
                                    selectedGroup = null
                                    members = emptyList()
                                    refresh()
                                    // Refresh state first, then show snackbar (suspend)
                                    snackbarHostState.showSnackbar("已退出小组")
                                }
                                .onFailure {
                                    leaving = false
                                    leaveTarget = null
                                    snackbarHostState.showSnackbar(it.message ?: "退出失败")
                                }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    enabled = !leaving
                ) { Text(if (leaving) "退出中..." else "确认退出") }
            },
            dismissButton = { TextButton(onClick = { leaveTarget = null }) { Text("取消") } }
        )
    }
}

private fun roleLabel(role: String): String = when (role) {
    "OWNER" -> "组长"
    "ADMIN" -> "管理员"
    else -> "成员"
}
