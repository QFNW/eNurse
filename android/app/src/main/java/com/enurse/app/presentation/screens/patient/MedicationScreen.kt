package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.domain.repository.MedicationRepository
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(onGoBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val repo = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MedicationRepoEntryPoint::class.java
        )
        entryPoint.medicationRepository()
    }

    var medications by remember { mutableStateOf<List<MedicationResponse>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDosage by remember { mutableStateOf("") }

    suspend fun refreshMeds() {
        repo.getMedications()
            .onSuccess { medications = it }
            .onFailure { snackbarHostState.showSnackbar("加载失败: ${it.message}") }
    }

    LaunchedEffect(Unit) { refreshMeds() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用药管理") },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("返回") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(medications) { med ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(med.name, style = MaterialTheme.typography.titleMedium)
                            med.dosage?.let { Text("剂量: $it") }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    scope.launch {
                                        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(java.util.Date())
                                        repo.record(med.id, now)
                                            .onSuccess {
                                                snackbarHostState.showSnackbar("打卡成功")
                                                refreshMeds()
                                            }
                                            .onFailure { snackbarHostState.showSnackbar("打卡失败: ${it.message}") }
                                    }
                                }) { Text("打卡") }
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            repo.deleteMedication(med.id)
                                            snackbarHostState.showSnackbar("已删除")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("删除失败: ${e.message}")
                                            return@launch
                                        }
                                        // Refresh after successful delete (best-effort)
                                        try { refreshMeds() } catch (_: Exception) {}
                                    }
                                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加药品") },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("药品名") })
                    OutlinedTextField(value = newDosage, onValueChange = { newDosage = it }, label = { Text("剂量（可选）") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.createMedication(newName, newDosage.ifBlank { null })
                            .onSuccess {
                                snackbarHostState.showSnackbar("已添加")
                                refreshMeds()
                                showAdd = false; newName = ""; newDosage = ""
                            }
                            .onFailure { snackbarHostState.showSnackbar("添加失败: ${it.message}") }
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }
}

@EntryPoint @InstallIn(SingletonComponent::class)
interface MedicationRepoEntryPoint {
    fun medicationRepository(): MedicationRepository
}
