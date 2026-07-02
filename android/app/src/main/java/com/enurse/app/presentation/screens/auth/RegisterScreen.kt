package com.enurse.app.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("PATIENT") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success) onRegisterSuccess() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("注册账号") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("邮箱") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码（至少8位）") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Role selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = role == "PATIENT", onClick = { role = "PATIENT" }, label = { Text("患者") })
                FilterChip(selected = role == "NURSE", onClick = { role = "NURSE" }, label = { Text("护理人员") })
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.register(email, password, role) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !state.isLoading && email.isNotBlank() && password.length >= 8
            ) { Text(if (state.isLoading) "注册中..." else "注册") }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onGoBack) { Text("已有账号？返回登录") }
        }
    }
}
