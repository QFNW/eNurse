package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.domain.repository.GroupRepository
import com.enurse.app.presentation.viewmodel.BmViewModel
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onGoBack: () -> Unit,
    bmViewModel: BmViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groupRepo = remember {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, GroupRepoEntryPoint::class.java)
        ep.groupRepository()
    }

    val bmState by bmViewModel.state.collectAsState()
    var inviteCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }, navigationIcon = { TextButton(onClick = onGoBack) { Text("返回") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("大便提醒设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val settings = bmState.settings
            if (settings != null) {
                var intervalDays by remember(settings) { mutableStateOf(settings.intervalDays.toString()) }
                var reminderTime by remember(settings) { mutableStateOf(settings.reminderTime) }

                OutlinedTextField(
                    value = intervalDays, onValueChange = { intervalDays = it },
                    label = { Text("提醒间隔（天）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reminderTime, onValueChange = { reminderTime = it },
                    label = { Text("提醒时间（HH:mm）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    bmViewModel.updateSettings(
                        intervalDays = intervalDays.toIntOrNull(),
                        reminderTime = reminderTime,
                        isEnabled = null
                    )
                }) { Text("保存") }

                if (settings.isLocked) {
                    Text("策略已由护理人员锁定，无法自行编辑", color = MaterialTheme.colorScheme.error)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("加入小组", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inviteCode, onValueChange = { inviteCode = it },
                label = { Text("邀请码") },
                modifier = Modifier.fillMaxWidth()
            )
            if (joinError != null) Text(joinError!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    groupRepo.joinByInvite(inviteCode)
                        .onSuccess { joinError = "加入成功！" }
                        .onFailure { joinError = it.message }
                }
            }) { Text("加入") }
        }
    }
}

@EntryPoint @InstallIn(SingletonComponent::class)
interface GroupRepoEntryPoint {
    fun groupRepository(): GroupRepository
}
