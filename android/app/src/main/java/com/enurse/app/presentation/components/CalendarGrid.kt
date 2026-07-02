package com.enurse.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.presentation.theme.AlertRed
import com.enurse.app.presentation.theme.Blue50

@Composable
fun CalendarGrid(
    year: Int,
    month: Int,
    days: List<CalendarDayData>,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(days.size) { index ->
                val day = days[index]
                DayCell(
                    date = day.date,
                    dayNumber = day.dayNumber,
                    hasRecord = day.hasRecord,
                    onClick = { onDayClick(day.date) }
                )
            }
        }
    }
}

data class CalendarDayData(
    val date: String,
    val dayNumber: String,
    val hasRecord: Boolean
)

@Composable
private fun DayCell(
    date: String,
    dayNumber: String,
    hasRecord: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (hasRecord) AlertRed.copy(alpha = 0.2f) else Blue50.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber,
            fontSize = 14.sp,
            fontWeight = if (hasRecord) FontWeight.Bold else FontWeight.Normal,
            color = if (hasRecord) AlertRed else MaterialTheme.colorScheme.onSurface
        )
    }
}
