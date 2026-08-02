package com.enurse.app.presentation.screens.nurse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.NurseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseGroupDetailScreen(
    groupId: String,
    onGoMember: (String, String) -> Unit,
    onGoBack: () -> Unit,
    viewModel: NurseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var removeTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAddVirtual by remember { mutableStateOf(false) }
    var virtualName by remember { mutableStateOf("") }
    LaunchedEffect(groupId) { viewModel.loadGroupDetail(groupId); viewModel.loadAdminRequests(groupId) }

    val adminReqs = (state.memberEvents?.get("adminRequests") as? List<*>)?.filter {
        (it as? Map<*,*>)?.get("status") == "PENDING"
    } ?: emptyList<Any>()

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.selectedGroup?.name ?: "小组详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("邀请码", fontSize = 13.sp, color = TextHint)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(state.selectedGroup?.inviteCode ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Primary)
                                TextButton(
                                    onClick = {
                                        val code = state.selectedGroup?.inviteCode ?: return@TextButton
                                        clipboard.setText(AnnotatedString(code))
                                        scope.launch { snackbarHostState.showSnackbar("邀请码已复制") }
                                    },
                                    contentPadding = PaddingValues(start = 8.dp, end = 4.dp)
                                ) { Text("复制", fontSize = 13.sp, color = Primary) }
                            }
                        }
                        Text("${state.selectedGroup?.members?.size ?: 0} 名成员", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }
            if (adminReqs.isNotEmpty()) {
                item {
                    Text("📋 待审批管理员", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AlertRed)
                    Spacer(Modifier.height(8.dp))
                }
                adminReqs.forEach { req ->
                    val r = req as? Map<*, *> ?: return@forEach
                    val reqId = r["id"]?.toString() ?: ""
                    val reqUser = r["user"] as? Map<*, *>
                    val reqName = reqUser?.get("displayName")?.toString() ?: reqUser?.get("username")?.toString() ?: "-"
                    item {
                        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = ErrorBg)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(reqName, Modifier.weight(1f), fontSize = 14.sp)
                                TextButton(onClick = { viewModel.approveRequest(groupId, reqId) }) { Text("✓ 通过", color = SuccessGreen, fontSize = 13.sp) }
                                TextButton(onClick = { viewModel.rejectRequest(groupId, reqId) }) { Text("✗ 拒绝", color = AlertRed, fontSize = 13.sp) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showAddVirtual = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("＋ 添加虚拟患者", color = Primary, fontSize = 14.sp) }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item { Text("成员", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) }
            item { Spacer(modifier = Modifier.height(10.dp)) }

            state.selectedGroup?.members?.let { members ->
                items(members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        onClick = { onGoMember(groupId, member.userId) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.user?.displayName ?: member.user?.nickname ?: member.user?.email ?: "成员", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(member.roleInGroup, fontSize = 12.sp, color = TextHint)
                            }
                            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = PrimaryBg)) {
                                Text("${member.alertCount ?: 0} 警报", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Medium)
                            }
                            if (member.roleInGroup != "OWNER") {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { removeTarget = Pair(groupId, member.userId) }, contentPadding = PaddingValues(0.dp)) {
                                    Text("移除", fontSize = 12.sp, color = AlertRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add virtual patient dialog
    if (showAddVirtual) {
        AlertDialog(
            onDismissRequest = { showAddVirtual = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("添加虚拟患者") },
            text = {
                OutlinedTextField(value = virtualName, onValueChange = { virtualName = it },
                    label = { Text("患者姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary))
            },
            confirmButton = {
                Button(onClick = {
                    if (virtualName.isNotBlank()) {
                        viewModel.createVirtualPatient(groupId, virtualName)
                        showAddVirtual = false; virtualName = ""
                    }
                }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("创建")
                }
            },
            dismissButton = { TextButton(onClick = { showAddVirtual = false }) { Text("取消") } }
        )
    }

    // Remove member confirmation
    if (removeTarget != null) {
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("确认移除") },
            text = { Text("将该患者移出小组后，护理端将无法再查看其数据。") },
            confirmButton = {
                Button(onClick = {
                    val (gid, uid) = removeTarget!!
                    viewModel.removeMember(gid, uid)
                    removeTarget = null
                }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = AlertRed)) {
                    Text("移除")
                }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("取消") } }
        )
    }
}
