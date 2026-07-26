package com.enurse.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enurse.app.presentation.screens.nurse.NurseGroupListScreen
import com.enurse.app.presentation.screens.nurse.NurseGroupDetailScreen
import com.enurse.app.presentation.screens.nurse.MemberDetailScreen
import com.enurse.app.presentation.screens.patient.UserCenterScreen
import com.enurse.app.presentation.theme.Primary
import com.enurse.app.presentation.theme.SurfaceWhite
import com.enurse.app.presentation.theme.TextHint
import com.enurse.app.presentation.viewmodel.NurseViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NurseNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    val tabController = rememberNavController()
    val tabs = listOf(
        BottomNavItem("小组", Icons.Filled.Groups, Icons.Outlined.Groups, "groups"),
        BottomNavItem("成员", Icons.Filled.People, Icons.Outlined.People, "members"),
        BottomNavItem("我的", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val nurseViewModel: NurseViewModel = hiltViewModel()

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
            startDestination = "groups",
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable("groups") {
                NurseGroupListScreen(
                    onGoGroup = { id -> navController.navigate("nurse/groups/$id") },
                    onGoBack = onLogout
                )
            }
            composable("members") {
                val state by nurseViewModel.state.collectAsState()
                if (state.selectedGroup != null) {
                    NurseGroupDetailScreen(
                        groupId = state.selectedGroup!!.id,
                        onGoMember = { gid, uid -> navController.navigate("nurse/groups/$gid/members/$uid") },
                        onGoBack = { selectedTab = 0; tabController.navigate("groups") }
                    )
                } else {
                    Box(modifier = Modifier.padding(32.dp)) { Text("请先在小组页面选择一个小组", color = TextHint) }
                }
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
