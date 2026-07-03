package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.components.AlertBanner
import com.enurse.app.presentation.components.BigBmButton
import com.enurse.app.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoCalendar: () -> Unit,
    onGoMedication: () -> Unit,
    onGoSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("易护") },
                actions = {
                    TextButton(onClick = onGoSettings) { Text("设置") }
                    TextButton(onClick = onLogout) { Text("退出") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Alert Banner
            AlertBanner(
                alerts = state.alerts,
                onResolve = { viewModel.resolveAlert(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Big BM Button
            BigBmButton(
                onClick = { viewModel.recordBm() },
                isLoading = state.isRecording
            )

            if (state.recordSuccess) {
                Text("打卡成功！", color = MaterialTheme.colorScheme.primary)
                if (!state.error.isNullOrBlank()) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                LaunchedEffect(state.recordSuccess) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearRecordSuccess()
                }
            } else if (!state.error.isNullOrBlank()) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick access buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onGoCalendar) { Text("大便日历") }
                OutlinedButton(onClick = onGoMedication) { Text("用药记录") }
            }
        }
    }
}
