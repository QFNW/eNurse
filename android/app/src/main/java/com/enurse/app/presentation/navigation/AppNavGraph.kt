package com.enurse.app.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.presentation.screens.auth.LoginScreen
import com.enurse.app.presentation.screens.auth.RegisterScreen
import com.enurse.app.presentation.screens.patient.BmCalendarScreen
import com.enurse.app.presentation.screens.patient.MedicationScreen
import com.enurse.app.presentation.screens.patient.BmReminderScreen
import com.enurse.app.presentation.screens.patient.JoinGroupScreen
import com.enurse.app.presentation.screens.patient.MyGroupsScreen
import com.enurse.app.presentation.screens.nurse.NurseGroupDetailScreen
import com.enurse.app.presentation.screens.nurse.MemberDetailScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val appContext = context.applicationContext
    val tokenDataStore = run {
        val entryPoint = EntryPointAccessors.fromApplication(appContext, TokenDataStoreEntryPoint::class.java)
        entryPoint.tokenDataStore()
    }

    var startDest by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenDataStore.accessToken.first()
        val role = tokenDataStore.userRole.first()
        startDest = if (token != null) {
            when (role) { "NURSE" -> "nurse/main" else -> "patient/main" }
        } else {
            "auth/login"
        }
    }

    // Watch for session expiry → redirect to login
    LaunchedEffect(Unit) {
        tokenDataStore.accessToken.collect { token ->
            if (token == null && startDest != null && startDest != "auth/login") {
                navController.navigate("auth/login") { popUpTo(0) { inclusive = true } }
            }
        }
    }

    if (startDest == null) return

    NavHost(navController = navController, startDestination = startDest!!) {
        composable("auth/login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = if (role == "NURSE") "nurse/main" else "patient/main"
                    navController.navigate(dest) { popUpTo("auth/login") { inclusive = true } }
                },
                onGoRegister = { navController.navigate("auth/register") }
            )
        }
        composable("auth/register") {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    val dest = if (role == "NURSE") "nurse/main" else "patient/main"
                    navController.navigate(dest) { popUpTo("auth/login") { inclusive = true } }
                },
                onGoBack = { navController.popBackStack() }
            )
        }
        // Patient Tab Navigation
        composable("patient/main") {
            PatientNavGraph(
                navController = navController,
                onLogout = {
                    kotlinx.coroutines.runBlocking { tokenDataStore.clear() }
                    navController.navigate("auth/login") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable("patient/calendar") {
            BmCalendarScreen(onGoBack = { navController.popBackStack() })
        }
        composable("patient/medication") {
            MedicationScreen(onGoBack = { navController.popBackStack() })
        }
        composable("patient/bm-reminder") {
            BmReminderScreen(onGoBack = { navController.popBackStack() })
        }
        composable("patient/join-group") {
            JoinGroupScreen(onGoBack = { navController.popBackStack() })
        }
        composable("patient/my-groups") {
            MyGroupsScreen(onGoBack = { navController.popBackStack() })
        }
        // Nurse Tab Navigation
        composable("nurse/main") {
            NurseNavGraph(
                navController = navController,
                onLogout = {
                    kotlinx.coroutines.runBlocking { tokenDataStore.clear() }
                    navController.navigate("auth/login") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable("nurse/groups/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            NurseGroupDetailScreen(
                groupId = groupId,
                onGoMember = { gid, uid -> navController.navigate("nurse/groups/$gid/members/$uid") },
                onGoBack = { navController.popBackStack() }
            )
        }
        composable("nurse/groups/{groupId}/members/{memberId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val memberId = backStackEntry.arguments?.getString("memberId") ?: return@composable
            MemberDetailScreen(
                groupId = groupId,
                userId = memberId,
                onGoBack = { navController.popBackStack() }
            )
        }
    }
}
