package com.enurse.app.presentation.reminder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enurse.app.reminder.ReminderScheduler
import com.enurse.app.presentation.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全屏亮屏提醒页（闹钟效果）：息屏/锁屏时由系统自动点亮（showWhenLocked + turnScreenOn），
 * 全屏显示提醒文字；铃声/振动由 AlarmReceiver 发的通知负责（受系统 DND 管理）。
 * 数据全部来自 Intent extras（Receiver 预拼好），本页不联网不查库。
 */
class ReminderActivity : ComponentActivity() {

    private var titleText by mutableStateOf("提醒")
    private var bodyText by mutableStateOf("")
    private var type by mutableStateOf("MEDICATION")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeScreen()

        intent?.extras?.let { readExtras(it) }

        setContent {
            eNurseTheme(darkTheme = false) {
                ReminderContent(
                    title = titleText,
                    body = bodyText,
                    type = type,
                    onDismiss = { finish() },
                    onSnooze = { snooze() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask：多个闹钟同刻触发时后到者覆盖内容
        intent?.extras?.let { readExtras(it) }
    }

    private fun readExtras(b: Bundle) {
        titleText = b.getString(ReminderScheduler.EXTRA_TITLE) ?: titleText
        bodyText = b.getString(ReminderScheduler.EXTRA_BODY) ?: ""
        type = b.getString(ReminderScheduler.EXTRA_TYPE) ?: type
    }

    /** 息屏/锁屏时点亮并显示（XML 属性 API 27+ 已配，此处代码兜底兼容 API 26）。 */
    private fun wakeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** 稍后提醒：以当前快照重排 +5 分钟（仍会复查打卡，已打卡则不响）。 */
    private fun snooze() {
        val extras = intent?.extras ?: run { finish(); return }
        val fired = Intent(this, com.enurse.app.reminder.AlarmReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_ALARM_FIRE
            putExtras(extras)
            putExtra(ReminderScheduler.EXTRA_SNOOZE, true)
        }
        ReminderScheduler.scheduleNextFromExtras(this, fired)
        finish()
    }
}

@Composable
private fun ReminderContent(
    title: String,
    body: String,
    type: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = PrimaryBg) {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 圆形图标
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(SurfaceWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (type == "BM") "💩" else "💊", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(14.dp))
            Text(body, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(18.dp))
            var time by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                while (true) {
                    time = fmt.format(Date())
                    delay(1000)
                }
            }
            Text(time, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

            Spacer(modifier = Modifier.weight(2f))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("知道了", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onSnooze) {
                Text("稍后提醒（5 分钟）", color = Primary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
