package com.chibychibystore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.screens.auth.LoginScreen
import com.chibychibystore.ui.viewmodel.AuthViewModel
import javax.inject.Inject

/**
 * Navigation guard that requires authentication
 * Redirects to login screen if user is not authenticated
 */
@Composable
fun AuthGuard(
    authService: AuthService,
    onLoginSuccess: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentUser by authService.observeCurrentUser().collectAsState(initial = null)

    if (currentUser == null) {
        // User not authenticated, show login screen
        LoginScreen(onLoginSuccess = onLoginSuccess)
    } else {
        // User authenticated, show protected content
        content()
    }
}

/**
 * Check if current route requires authentication
 */
fun requiresAuth(route: String): Boolean {
    val publicRoutes = listOf(
        Routes.LOGIN
    )
    return route !in publicRoutes
}