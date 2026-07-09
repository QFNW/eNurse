package com.enurse.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.presentation.theme.Primary
import com.enurse.app.presentation.theme.PrimaryBg
import com.enurse.app.presentation.theme.PrimaryLight
import com.enurse.app.presentation.theme.TextHint
import java.util.*

@Composable
fun CalendarGrid(
    year: Int,
    month: Int,
    days: List<CalendarDayData>,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Calendar.getInstance() }
    val todayStr = remember {
        val t = Calendar.getInstance()
        "${t.get(Calendar.YEAR)}-${String.format("%02d", t.get(Calendar.MONTH) + 1)}-${String.format("%02d", t.get(Calendar.DAY_OF_MONTH))}"
    }

    Column(modifier = modifier) {
        // Weekday headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextHint
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

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
                    isToday = day.date == todayStr,
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
    isToday: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        hasRecord -> Primary
        isToday -> PrimaryLight.copy(alpha = 0.1f)
        else -> PrimaryBg.copy(alpha = 0.3f)
    }
    val borderMod = if (isToday && !hasRecord)
        Modifier.border(1.5.dp, Primary, CircleShape)
    else Modifier

    Box(
        modifier = Modifier
            .padding(3.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(borderMod)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber,
            fontSize = 14.sp,
            fontWeight = if (hasRecord || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                hasRecord -> androidx.compose.ui.graphics.Color.White
                isToday -> Primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
