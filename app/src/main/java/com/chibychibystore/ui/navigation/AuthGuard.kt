package com.chibychibystore.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.service.AuthService

import com.chibychibystore.ui.auth.LoginScreen

import javax.inject.Inject

@Composable
fun AuthGuard(
    authService: AuthService,
    onLoginSuccess: () -> Unit = {},
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
