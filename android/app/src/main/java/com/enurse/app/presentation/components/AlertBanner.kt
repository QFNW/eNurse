package com.enurse.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.data.remote.dto.response.AlertResponse
import com.enurse.app.presentation.theme.AlertRed
import com.enurse.app.presentation.theme.AlertRedBg
import com.enurse.app.presentation.theme.ErrorBorder

@Composable
fun AlertBanner(
    alerts: List<AlertResponse>,
    onResolve: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = "⚠ 警报提醒",
            color = AlertRed,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRedBg)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        // Left red indicator
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AlertRed)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.message,
                                color = AlertRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { onResolve(alert.id) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("标记已处理 →", color = AlertRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
