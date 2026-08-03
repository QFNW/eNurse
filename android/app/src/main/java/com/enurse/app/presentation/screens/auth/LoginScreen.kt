package com.enurse.app.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.R
import com.enurse.app.presentation.theme.Primary
import com.enurse.app.presentation.theme.PrimaryDark
import com.enurse.app.presentation.theme.TextHint
import com.enurse.app.presentation.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onGoRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()

    // Sync the persisted remember-me choice on entry (user's own changes are
    // only written back to storage on a successful login)
    LaunchedEffect(state.rememberMe) { rememberMe = state.rememberMe }

    LaunchedEffect(state.success) {
        if (state.success && state.userRole != null) onLoginSuccess(state.userRole!!)
    }

    Scaffold(containerColor = androidx.compose.ui.graphics.Color.White) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo (same brand graphic as the splash screen, transparent bg)
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("易护 eNurse", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
            Text("患者生理体征追踪护理", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(40.dp))

            // Frequent accounts — tap to fill login (and password if remembered)
            if (state.savedLogins.isNotEmpty()) {
                Text("常用账号", fontSize = 13.sp, color = TextHint)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.savedLogins, key = { it.login }) { acc ->
                        InputChip(
                            selected = acc.login == login,
                            onClick = {
                                login = acc.login
                                password = acc.password ?: ""
                            },
                            label = { Text(acc.login, maxLines = 1) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除 ${acc.login}",
                                    modifier = Modifier.size(16.dp)
                                        .clickable { viewModel.removeSavedLogin(acc.login) }
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.12f),
                                selectedLabelColor = Primary
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = login, onValueChange = { login = it },
                label = { Text("用户名或邮箱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )
            // Remember password — plaintext on device, per product decision
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                )
                Text("记住密码", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { viewModel.login(login, password, rememberMe) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = !state.isLoading && login.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (state.isLoading) "登录中..." else "登录", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onGoRegister) {
                Text("没有账号？注册", color = Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
