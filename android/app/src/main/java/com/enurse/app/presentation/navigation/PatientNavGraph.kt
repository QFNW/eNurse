package com.enurse.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.enurse.app.presentation.screens.patient.HomeScreen
import com.enurse.app.presentation.screens.patient.UserCenterScreen
import com.enurse.app.presentation.theme.Primary
import com.enurse.app.presentation.theme.SurfaceWhite
import com.enurse.app.presentation.theme.TextHint

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun PatientNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    val tabController = rememberNavController()
    val tabs = listOf(
        BottomNavItem("首页", Icons.Filled.Home, Icons.Outlined.Home, "home"),
        BottomNavItem("我的", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    )
    // Highlight follows the actual tab route, so returning from a top-level
    // screen (e.g. patient/my-groups) never desyncs the selected tab
    val backStackEntry by tabController.currentBackStackEntryAsState()
    val selectedTab = tabs.indexOfFirst { it.route == backStackEntry?.destination?.route }.coerceAtLeast(0)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = SurfaceWhite) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            tabController.navigate(item.route) {
                                popUpTo(tabController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(if (selectedTab == index) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextHint,
                            unselectedTextColor = TextHint
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable("home") {
                HomeScreen(
                    onGoCalendar = { navController.navigate("patient/calendar") },
                    onGoMedication = { navController.navigate("patient/medication") }
                )
            }
            composable("profile") {
                UserCenterScreen(
                    isPatient = true,
                    onGoBmReminder = { navController.navigate("patient/bm-reminder") },
                    onGoJoinGroup = { navController.navigate("patient/join-group") },
                    onGoMyGroups = { navController.navigate("patient/my-groups") },
                    onLogout = onLogout
                )
            }
        }
    }
}
