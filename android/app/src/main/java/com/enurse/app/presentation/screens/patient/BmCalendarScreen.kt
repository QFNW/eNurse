package com.enurse.app.presentation.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enurse.app.presentation.components.CalendarDayData
import com.enurse.app.presentation.components.CalendarGrid
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
        topBar = {
            TopAppBar(
                title = { Text("大便日历") },
                navigationIcon = { TextButton(onClick = onGoBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Month navigation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    if (month == 1) { month = 12; year-- } else { month-- }
                    viewModel.loadCalendar(year, month)
                }) { Text("<") }
                Text("${year}年${month}月", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    if (month == 12) { month = 1; year++ } else { month++ }
                    viewModel.loadCalendar(year, month)
                }) { Text(">") }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            } else {
                val days = state.calendar?.days?.map {
                    val parts = it.date.split("-")
                    CalendarDayData(date = it.date, dayNumber = parts[2].toInt().toString(), hasRecord = it.hasRecord)
                } ?: emptyList()

                CalendarGrid(
                    year = year,
                    month = month,
                    days = days,
                    onDayClick = { date ->
                        viewModel.selectDay(date)
                        showDetail = true
                    }
                )
            }
        }
    }

    // Day detail bottom sheet
    if (showDetail && state.selectedDay != null) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = { Text(state.selectedDay!!) },
            text = {
                if (state.selectedDayRecords.isEmpty()) {
                    Text("当日无大便记录")
                } else {
                    Column {
                        state.selectedDayRecords.forEach { record ->
                            Text("时间: ${record.recordedAt.take(16).replace("T", " ")}")
                            record.notes?.let { Text("备注: $it") }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text("关闭") } }
        )
    }
}
