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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.enurse.app.presentation.navigation.AppNavGraph
import com.enurse.app.presentation.screens.splash.BrandSplashScreen
import com.enurse.app.presentation.theme.eNurseTheme
import com.enurse.app.presentation.viewmodel.SplashUiState
import com.enurse.app.presentation.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

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
                        }
                }
            }
        }
    }
}
