package com.chibychibystore.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.components.shared.AppDrawer
import com.chibychibystore.ui.components.shared.LoadingIndicator

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    authService: AuthService
) {
    val scope = rememberCoroutineScope()

    // A persisted session is restored before the graph is shown, so a returning
    // user lands on the dashboard instead of having to log in again. The NavHost
    // is not composed until the restore settles: with a live NavHost the Login
    // destination would render (and could be navigated away from) before the
    // session appears, and the drawer's "is the user on the login screen" gate
    // would briefly be wrong. Restoring is a couple of Room reads, so the splash
    // is short; it also avoids a login-form flash on every cold start.
    var sessionRestored by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        authService.initializeSession()
        sessionRestored = true
    }

    if (!sessionRestored) {
        SplashScreen()
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Dashboard.route
    val onOpenDrawer = rememberDrawerNavigation(drawerState)
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
 * Neutral placeholder shown while the persisted session is being restored. It
 * deliberately uses no drawer and no navigation, so it cannot leak a module
 * before authentication is known.
 */
@Composable
private fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingIndicator()
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
