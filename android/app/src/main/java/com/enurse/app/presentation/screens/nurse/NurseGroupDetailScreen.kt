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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    LaunchedEffect(groupId) { viewModel.loadGroupDetail(groupId) }

    Scaffold(
        containerColor = Background,
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
                            Text(state.selectedGroup?.inviteCode ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Primary)
                        }
                        Text("${state.selectedGroup?.members?.size ?: 0} 名成员", fontSize = 14.sp, color = TextSecondary)
                    }
                }
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
                                Text(member.user?.nickname ?: member.user?.email ?: "成员", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(member.roleInGroup, fontSize = 12.sp, color = TextHint)
                            }
                            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = PrimaryBg)) {
                                Text("${member.alertCount ?: 0} 警报", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
