package com.enurse.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.data.remote.dto.response.AlertResponse
import com.enurse.app.presentation.theme.AlertRed
import com.enurse.app.presentation.theme.AlertRedBg

@Composable
fun AlertBanner(
    alerts: List<AlertResponse>,
    onResolve: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "⚠ 警报提醒",
            color = AlertRed,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRedBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = alert.message,
                            color = AlertRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { onResolve(alert.id) }) {
                            Text("已处理", color = AlertRed)
                        }
                    }
                }
            }
        }
    }
}
