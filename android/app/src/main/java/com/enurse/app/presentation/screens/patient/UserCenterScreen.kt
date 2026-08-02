package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.AuthViewModel

@Composable
fun UserCenterScreen(
    onGoSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val authState by viewModel.state.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var feedbackSent by remember { mutableStateOf(false) }

    // Watch for delete account success
    LaunchedEffect(authState.success) {
        if (authState.success && authState.userRole == null) {
            onLogout()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar placeholder
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = 40.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("用户中心", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        // Menu cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                MenuRow("⚙️", "设置", onClick = onGoSettings)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                MenuRow("🚪", "退出登录", onClick = { showLogoutConfirm = true })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                MenuRow("🗑️", "注销账号", onClick = { showDeleteConfirm = true })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feedback
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💬 问题反馈", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = feedbackText, onValueChange = { feedbackText = it },
                    label = { Text("请描述您遇到的问题") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { feedbackText = ""; feedbackSent = true },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text(if (feedbackSent) "✓ 已提交" else "提交反馈") }
            }
        }
    }

    // Logout confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("确认退出") },
            text = { Text("退出登录后需要重新输入账号密码") },
            confirmButton = {
                Button(onClick = { showLogoutConfirm = false; onLogout() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") } }
        )
    }

    // Delete account confirm
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("注销账号", fontWeight = FontWeight.Bold) },
            text = { Text("此操作不可撤销。\n\n注销后，您的所有数据将被永久删除，包括打卡记录、用药记录和个人设置。") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAccount() },
                    shape = RoundedCornerShape(20.dp),
                    enabled = !authState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) { Text(if (authState.isLoading) "注销中..." else "确认注销") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun MenuRow(icon: String, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("→", fontSize = 15.sp, color = TextHint)
        }
    }
}
