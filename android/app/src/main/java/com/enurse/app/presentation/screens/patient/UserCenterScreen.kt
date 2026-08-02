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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun UserCenterScreen(
    isPatient: Boolean = false,
    onGoBmReminder: () -> Unit = {},
    onGoJoinGroup: () -> Unit = {},
    onGoMyGroups: () -> Unit = {},
    onLogout: () -> Unit
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val authState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var renameValue by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    // Watch for delete account success
    LaunchedEffect(authState.success) {
        if (authState.success && authState.userRole == null) {
            onLogout()
        }
    }

    // Show snackbar on profile saved
    LaunchedEffect(authState.profileSaved) {
        if (authState.profileSaved) {
            viewModel.clearProfileSaved()
            snackbarHostState.showSnackbar("已保存")
        }
    }

    // Show snackbar on feedback sent — state is already updated in ViewModel before this
    LaunchedEffect(authState.feedbackSent) {
        if (authState.feedbackSent) {
            viewModel.clearFeedbackSent()
            snackbarHostState.showSnackbar("✓ 反馈已提交，感谢您的建议")
        }
    }

    // Show snackbar on password changed, then force re-login
    LaunchedEffect(authState.passwordChanged) {
        if (authState.passwordChanged) {
            snackbarHostState.showSnackbar("密码已修改，请重新登录")
            onLogout()
        }
    }

    // Show error snackbar (profile/update/password/feedback errors)
    LaunchedEffect(authState.error) {
        val err = authState.error
        if (err != null) {
            viewModel.clearError()
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
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
            Text(
                authState.profile?.displayName ?: authState.profile?.nickname ?: "用户",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Text(authState.profile?.email ?: "", fontSize = 13.sp, color = TextHint)
            Spacer(modifier = Modifier.height(24.dp))

            // Menu cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    MenuRow("✏️", "修改姓名", onClick = { renameValue = authState.profile?.displayName ?: ""; showRenameDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                    MenuRow("🔑", "修改密码", onClick = { oldPassword = ""; newPassword = ""; showPasswordDialog = true })
                    if (isPatient) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                        MenuRow("👥", "我的小组", onClick = onGoMyGroups)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                        MenuRow("💩", "大便提醒", onClick = onGoBmReminder)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = CardBorder)
                        MenuRow("➕", "加入小组", onClick = onGoJoinGroup)
                    }
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
                        onClick = {
                            if (feedbackText.isNotBlank()) {
                                viewModel.submitFeedback(feedbackText)
                                feedbackText = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !authState.feedbackSubmitting
                    ) { Text(if (authState.feedbackSubmitting) "提交中..." else "提交反馈") }
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("修改姓名") },
            text = {
                OutlinedTextField(
                    value = renameValue, onValueChange = { renameValue = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameValue.isNotBlank()) {
                            viewModel.updateProfile(renameValue)
                            showRenameDialog = false
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("取消") } }
        )
    }

    // Password dialog — two fields always rendered together (no if/else switching)
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("修改密码") },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPassword, onValueChange = { oldPassword = it },
                        label = { Text("原密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword, onValueChange = { newPassword = it },
                        label = { Text("新密码（至少 6 位）") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPassword.isNotBlank() && newPassword.length >= 6) {
                            viewModel.changePassword(oldPassword, newPassword)
                            showPasswordDialog = false
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = !authState.isLoading
                ) { Text(if (authState.isLoading) "修改中..." else "确认修改") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("取消") } }
        )
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
