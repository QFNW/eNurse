package com.enurse.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.enurse.app.presentation.navigation.AppNavGraph
import com.enurse.app.presentation.screens.splash.BrandSplashScreen
import com.enurse.app.presentation.theme.Primary
import com.enurse.app.presentation.theme.TextSecondary
import com.enurse.app.presentation.theme.eNurseTheme
import com.enurse.app.presentation.viewmodel.SplashUiState
import com.enurse.app.presentation.viewmodel.SplashViewModel
import com.enurse.app.update.UpdateChecker
import com.enurse.app.update.VersionInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, polling still runs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash on screen until the start destination is decided,
        // but never for more than ~3s (last-resort guard against a stuck splash).
        val startedAt = SystemClock.elapsedRealtime()
        installSplashScreen().setKeepOnScreenCondition {
            // Keep while not ready, but at most 3s total
            splashViewModel.state.value !is SplashUiState.Ready &&
                SystemClock.elapsedRealtime() - startedAt < 3000
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            eNurseTheme {
                val splashState by splashViewModel.state.collectAsState()
                var showBrand by remember { mutableStateOf(true) }

                when (val s = splashState) {
                    // System splash still covers the window — render nothing
                    is SplashUiState.NotReady -> Unit
                    is SplashUiState.Ready ->
                        if (showBrand) {
                            BrandSplashScreen(onFinished = { showBrand = false })
                        } else {
                            AppNavGraph(startDestination = s.startDest)
                            // 每次启动静默检查更新，发现新版本弹提示（不打断启动流程）
                            CheckUpdateOnStart()
                        }
                }
            }
        }
    }
}

/**
 * 启动时静默检查版本：发现新版本弹出更新框（下载进度 + 自动安装），
 * 无更新或检查失败时静默不打扰。与「我的 → 版本更新」手动检查并存。
 */
@Composable
private fun CheckUpdateOnStart() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val latest = UpdateChecker.checkVersion(context.applicationContext)
        if (latest != null && latest.versionCode > BuildConfig.VERSION_CODE) {
            updateInfo = latest
        }
    }

    val info = updateInfo
    if (info != null) {
        AlertDialog(
            onDismissRequest = { if (!downloading) updateInfo = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("发现新版本 v${info.versionName}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    if (downloading) {
                        Text("正在下载更新...", fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${(downloadProgress * 100).toInt()}%", fontSize = 13.sp, color = TextSecondary)
                    } else {
                        Text("当前版本 ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = TextSecondary)
                        info.changelog?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (!downloading) {
                    Button(
                        onClick = {
                            scope.launch {
                                downloading = true
                                val ok = UpdateChecker.downloadAndInstall(
                                    context.applicationContext, info
                                ) { p -> downloadProgress = p }
                                downloading = false
                                updateInfo = null
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("立即更新") }
                }
            },
            dismissButton = {
                if (!downloading) TextButton(onClick = { updateInfo = null }) { Text("稍后") }
            }
        )
    }
}
