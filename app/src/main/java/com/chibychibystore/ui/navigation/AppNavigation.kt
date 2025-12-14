package com.chibychibystore.ui.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.inventory.AddProductScreen
import com.chibychibystore.ui.inventory.InventoryScreen
import com.chibychibystore.ui.inventory.ProductDetailScreen
import com.chibychibystore.ui.inventory.WarehouseDetailScreen
import com.chibychibystore.ui.inventory.WarehouseListScreen
import com.chibychibystore.ui.pos.PosScreen
import com.chibychibystore.ui.barcode.BarcodeScannerScreen
import com.chibychibystore.ui.barcode.BarcodePrintScreen
import com.chibychibystore.ui.screens.BackupScreen
import com.chibychibystore.ui.screens.DashboardScreen
import com.chibychibystore.ui.screens.UserManagementScreen
import com.chibychibystore.ui.screens.SettingsScreen
import com.chibychibystore.ui.reports.ReportsScreen
import com.chibychibystore.ui.sales.SalesHistoryScreen
import com.chibychibystore.ui.screens.auth.LoginScreen
import com.chibychibystore.ui.expense.ExpenseListScreen
import com.chibychibystore.ui.expense.ExpenseDetailScreen
import com.chibychibystore.ui.expense.ExpenseAddScreen
import com.chibychibystore.ui.user.UserListScreen
import com.chibychibystore.ui.user.UserAddScreen
import com.chibychibystore.ui.user.UserDetailScreen
import javax.inject.Inject

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    authService: AuthService
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                DashboardScreen(
                    drawerState = drawerState,
                    currentRoute = Screen.Dashboard.route,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        composable(Screen.Inventory.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.Inventory.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                InventoryScreen(navController = navController)
            }
        }

        composable(Screen.ProductDetail.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")?.toLongOrNull()
            if (productId != null) {
                AuthGuard(
                    authService = authService,
                    onLoginSuccess = {
                        navController.navigate(Screen.ProductDetail.createRoute(productId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                ) {
                    ProductDetailScreen(
                        navController = navController,
                        productId = productId
                    )
                }
            }
        }

        composable(Screen.ProductAdd.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.ProductAdd.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                AddProductScreen(navController = navController)
            }
        }

        composable(Screen.WarehouseList.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.WarehouseList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                WarehouseListScreen(navController = navController)
            }
        }

        composable(Screen.WarehouseDetail.route) { backStackEntry ->
            val warehouseId = backStackEntry.arguments?.getString("warehouseId")
            if (warehouseId != null) {
                AuthGuard(
                    authService = authService,
                    onLoginSuccess = {
                        navController.navigate(Screen.WarehouseDetail.createRoute(warehouseId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                ) {
                    WarehouseDetailScreen(
                        navController = navController,
                        warehouseId = warehouseId
                    )
                }
            }
        }

        composable(Screen.WarehouseAdd.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.WarehouseAdd.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                com.chibychibystore.ui.inventory.AddWarehouseScreen(navController = navController)
            }
        }

        composable(Screen.WarehouseEdit.route) { backStackEntry ->
            val warehouseId = backStackEntry.arguments?.getString("warehouseId")
            if (warehouseId != null) {
                AuthGuard(
                    authService = authService,
                    onLoginSuccess = {
                        navController.navigate(Screen.WarehouseEdit.createRoute(warehouseId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                ) {
                    com.chibychibystore.ui.inventory.EditWarehouseScreen(
                        navController = navController,
                        warehouseId = warehouseId
                    )
                }
            }
        }

        composable(Screen.Pos.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.Pos.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                PosScreen(navController = navController)
            }
        }

        composable(Screen.SalesHistory.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.SalesHistory.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                SalesHistoryScreen(navController = navController)
            }
        }

        composable(Screen.BarcodeScanner.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.BarcodeScanner.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        // Navigate back with result
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_barcode", barcode)
                        navController.popBackStack()
                    },
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.BarcodePrint.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.BarcodePrint.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                BarcodePrintScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Reports.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.Reports.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                ReportsScreen(navController = navController)
            }
        }

        composable(Screen.ExpenseList.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.ExpenseList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                ExpenseListScreen(navController = navController)
            }
        }

        composable(Screen.ExpenseAdd.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.ExpenseAdd.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                ExpenseAddScreen(navController = navController)
            }
        }

        composable(Screen.ExpenseDetail.route) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
            if (expenseId != null) {
                AuthGuard(
                    authService = authService,
                    onLoginSuccess = {
                        navController.navigate(Screen.ExpenseDetail.createRoute(expenseId)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                ) {
                    ExpenseDetailScreen(navController = navController, expenseId = expenseId)
                }
            }
        }

        composable(Screen.Backup.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.Backup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                BackupScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.Settings.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                SettingsScreen(
                    currentRoute = Screen.Settings.route,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Settings.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.UserList.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.UserList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                UserListScreen(navController = navController)
            }
        }

        composable(Screen.UserAdd.route) {
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.UserAdd.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                UserAddScreen(navController = navController)
            }
        }

        composable(
            route = Screen.UserDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) {
            val userId = it.arguments?.getLong("userId") ?: 0L
            AuthGuard(
                authService = authService,
                onLoginSuccess = {
                    navController.navigate(Screen.UserDetail.createRoute(userId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            ) {
                UserDetailScreen(navController = navController, userId = userId)
            }
        }
    }
}