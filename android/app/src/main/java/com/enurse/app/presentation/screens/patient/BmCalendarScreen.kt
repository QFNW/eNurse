package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.components.CalendarDayData
import com.enurse.app.presentation.components.CalendarGrid
import com.enurse.app.presentation.theme.*
import com.enurse.app.presentation.viewmodel.BmViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmCalendarScreen(
    onGoBack: () -> Unit,
    viewModel: BmViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cal = Calendar.getInstance()
    var year by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var showDetail by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("大便日历", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("← 返回", color = Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            // Month nav
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (month == 1) { month = 12; year-- } else { month-- }
                        viewModel.loadCalendar(year, month)
                    }) { Text("◀", fontSize = 16.sp, color = Primary) }
                    Text("${year}年${month}月", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        if (month == 12) { month = 1; year++ } else { month++ }
                        viewModel.loadCalendar(year, month)
                    }) { Text("▶", fontSize = 16.sp, color = Primary) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Primary)
            } else {
                val days = state.calendar?.days?.mapNotNull {
                    val parts = it.date.split("-")
                    if (parts.size < 3) null else
                        CalendarDayData(date = it.date, dayNumber = parts[2].toIntOrNull()?.toString() ?: "?", hasRecord = it.hasRecord)
                } ?: emptyList()
                CalendarGrid(year = year, month = month, days = days, onDayClick = { date ->
                    viewModel.selectDay(date)
                    showDetail = true
                })
            }
        }
    }

    if (showDetail && state.selectedDay != null) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(state.selectedDay!!, fontWeight = FontWeight.SemiBold) },
            text = {
                if (state.selectedDayRecords.isEmpty()) {
                    Text("当日无大便记录", color = TextSecondary)
                } else {
                    Column {
                        state.selectedDayRecords.forEach { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryBg)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🕐 ${record.recordedAt.take(16).replace("T", " ")}", fontWeight = FontWeight.Medium)
                                    record.notes?.let { Text("备注: $it", fontSize = 13.sp, color = TextSecondary) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDetail = false }, shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("关闭")
                }
            }
        )
    }
}
