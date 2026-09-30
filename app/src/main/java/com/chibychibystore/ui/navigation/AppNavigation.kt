package com.chibychibystore.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.components.shared.AppDrawer

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    authService: AuthService
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Dashboard.route
    val onOpenDrawer = rememberDrawerNavigation(drawerState)
    val scope = rememberCoroutineScope()

    // Top-level destinations share one drawer, so a drawer tap pops back to the
    // dashboard hub (saving each screen's state) instead of stacking screens.
    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    AppDrawer(
        drawerState = drawerState,
        currentRoute = currentRoute,
        onNavigateToRoute = navigateToRoute,
        onCloseDrawer = { scope.launch { drawerState.close() } }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route
        ) {
            appDestinations(
                navController = navController,
                authService = authService,
                onOpenDrawer = onOpenDrawer,
                navigateToRoute = navigateToRoute
            )
        }
    }
}