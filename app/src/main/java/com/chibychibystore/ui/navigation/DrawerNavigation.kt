package com.chibychibystore.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * The navigation drawer is hosted once in [AppNavigation] and shared by every
 * top-level destination. Screens open it from their own top bar via
 * [rememberDrawerNavigation] instead of owning a drawer themselves, so a screen
 * can be previewed or tested on its own with a plain [rememberDrawerState].
 */
@Composable
fun rememberDrawerNavigation(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
): () -> Unit {
    val scope = rememberCoroutineScope()
    return { scope.launch { drawerState.open() } }
}
