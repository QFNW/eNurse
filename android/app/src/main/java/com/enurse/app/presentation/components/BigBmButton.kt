package com.enurse.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.presentation.theme.Blue700

@Composable
fun BigBmButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue700),
        enabled = !isLoading
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isLoading) "..." else "💩",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "大便打卡",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "点击记录",
                fontSize = 14.sp
            )
        }
    }
}
