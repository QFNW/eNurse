package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.domain.repository.GroupRepository
import com.enurse.app.presentation.theme.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(onGoBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groupRepo = remember {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, GroupRepoEntryPoint::class.java)
        ep.groupRepository()
    }
    var inviteCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("加入小组", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            // Join Group Card
            SectionCard("👥 加入小组") {
                OutlinedTextField(
                    value = inviteCode, onValueChange = { inviteCode = it },
                    label = { Text("邀请码") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                if (joinError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(joinError!!, fontSize = 13.sp, color = if (joinError!!.contains("成功")) SuccessGreen else AlertRed)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        scope.launch {
                            groupRepo.joinByInvite(inviteCode)
                                .onSuccess { joinError = "加入成功！" }
                                .onFailure { joinError = it.message }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("加入", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@EntryPoint @InstallIn(SingletonComponent::class)
interface GroupRepoEntryPoint {
    fun groupRepository(): GroupRepository
}
