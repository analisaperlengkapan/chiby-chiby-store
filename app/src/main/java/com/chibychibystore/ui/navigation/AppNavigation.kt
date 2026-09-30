package com.chibychibystore.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    val currentUser by authService.observeCurrentUser().collectAsState(initial = null)

    // Top-level destinations share one drawer, so a drawer tap pops back to the
    // dashboard hub (saving each screen's state) instead of stacking screens.
    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    AppShell(
        isLoggedIn = currentUser != null,
        currentRoute = currentRoute,
        drawerState = drawerState,
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

/**
 * Mounts the app-wide [AppDrawer] around [content] only for an authenticated user
 * who is on a real module. Two separate cases must not expose the menu:
 *
 *  - no session yet: the drawer used to wrap the whole NavHost, so it could be
 *    swiped open over the login screen to reach a protected module;
 *  - a session that is *sitting on* the login destination (logout, or a restored
 *    back stack): gating on user state alone would still show the modules over
 *    the login form.
 *
 * So the drawer requires both a user and a non-login route.
 */
@Composable
fun AppShell(
    isLoggedIn: Boolean,
    currentRoute: String,
    drawerState: DrawerState,
    onNavigateToRoute: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    content: @Composable () -> Unit
) {
    val showDrawer = isLoggedIn && currentRoute != Screen.Login.route
    if (showDrawer) {
        AppDrawer(
            drawerState = drawerState,
            currentRoute = currentRoute,
            onNavigateToRoute = onNavigateToRoute,
            onCloseDrawer = onCloseDrawer,
            content = content
        )
    } else {
        content()
    }
}
