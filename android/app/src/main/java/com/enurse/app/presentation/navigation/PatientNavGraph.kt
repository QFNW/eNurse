package com.enurse.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enurse.app.presentation.screens.patient.HomeScreen
import com.enurse.app.presentation.screens.patient.MedicationScreen
import com.enurse.app.presentation.screens.patient.BmCalendarScreen
import com.enurse.app.presentation.screens.patient.SettingsScreen
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
        BottomNavItem("用药", Icons.Filled.MedicalServices, Icons.Outlined.MedicalServices, "medication"),
        BottomNavItem("我的", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = SurfaceWhite) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
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
                    onGoMedication = { selectedTab = 1; tabController.navigate("medication") },
                    onGoSettings = { selectedTab = 2; tabController.navigate("profile") },
                    onLogout = onLogout
                )
            }
            composable("medication") {
                MedicationScreen(onGoBack = { selectedTab = 0; tabController.navigate("home") })
            }
            composable("profile") {
                UserCenterScreen(
                    onGoSettings = { navController.navigate("patient/settings") },
                    onLogout = onLogout
                )
            }
        }
    }
}
