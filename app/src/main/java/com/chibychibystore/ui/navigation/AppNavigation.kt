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
import com.chibychibystore.ui.inventory.*
import com.chibychibystore.ui.pos.PosScreen
import com.chibychibystore.ui.barcode.BarcodeScannerScreen
import com.chibychibystore.ui.barcode.BarcodePrintScreen
import com.chibychibystore.ui.backup.BackupScreen
import com.chibychibystore.ui.dashboard.DashboardScreen
import com.chibychibystore.ui.settings.SettingsScreen
import com.chibychibystore.ui.reports.ReportsScreen
import com.chibychibystore.ui.sales.SalesHistoryScreen
import com.chibychibystore.ui.auth.LoginScreen
import com.chibychibystore.ui.expense.ExpenseListScreen
import com.chibychibystore.ui.expense.ExpenseDetailScreen
import com.chibychibystore.ui.expense.ExpenseAddScreen
import com.chibychibystore.ui.user.UserDetailScreen
import com.chibychibystore.ui.supplier.SupplierListScreen
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
            AuthGuard(authService = authService) {
                DashboardScreen(
                    navController = navController,
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
            AuthGuard(authService = authService) {
                InventoryScreen(navController = navController)
            }
        }

        composable(Screen.ProductDetail.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            if (productId != null) {
                AuthGuard(authService = authService) {
                    ProductDetailScreen(navController = navController, productId = productId)
                }
            }
        }

        composable(Screen.ProductAdd.route) {
            AuthGuard(authService = authService) {
                AddProductScreen(navController = navController)
            }
        }

        composable(Screen.WarehouseList.route) {
            AuthGuard(authService = authService) {
                WarehouseListScreen(navController = navController)
            }
        }

        composable(Screen.WarehouseDetail.route) { backStackEntry ->
            val warehouseId = backStackEntry.arguments?.getString("warehouseId")
            if (warehouseId != null) {
                AuthGuard(authService = authService) {
                    WarehouseDetailScreen(navController = navController, warehouseId = warehouseId)
                }
            }
        }

        composable(Screen.WarehouseAdd.route) {
            AuthGuard(authService = authService) {
                AddWarehouseScreen(navController = navController)
            }
        }

        composable(Screen.WarehouseEdit.route) { backStackEntry ->
            val warehouseId = backStackEntry.arguments?.getString("warehouseId")
            if (warehouseId != null) {
                AuthGuard(authService = authService) {
                    EditWarehouseScreen(navController = navController, warehouseId = warehouseId)
                }
            }
        }

        composable(Screen.Pos.route) {
            AuthGuard(authService = authService) {
                PosScreen(navController = navController)
            }
        }

        composable(Screen.SalesHistory.route) {
            AuthGuard(authService = authService) {
                SalesHistoryScreen(navController = navController)
            }
        }

        composable(Screen.BarcodeScanner.route) {
            AuthGuard(authService = authService) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("scanned_barcode", barcode)
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.BarcodePrint.route) {
            AuthGuard(authService = authService) {
                BarcodePrintScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Reports.route) {
            AuthGuard(authService = authService) {
                ReportsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.ExpenseList.route) {
            AuthGuard(authService = authService) {
                ExpenseListScreen(navController = navController)
            }
        }

        composable(Screen.ExpenseAdd.route) {
            AuthGuard(authService = authService) {
                ExpenseAddScreen(navController = navController)
            }
        }

        composable(Screen.ExpenseDetail.route) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
            if (expenseId != null) {
                AuthGuard(authService = authService) {
                    ExpenseDetailScreen(navController = navController, expenseId = expenseId)
                }
            }
        }

        composable(Screen.Backup.route) {
            AuthGuard(authService = authService) {
                BackupScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Settings.route) {
            AuthGuard(authService = authService) {
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
            AuthGuard(authService = authService) {
                UserManagementScreen(
                    drawerState = drawerState,
                    currentRoute = Screen.UserList.route,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.UserList.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.UserDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) {
            val userId = it.arguments?.getLong("userId") ?: 0L
            AuthGuard(authService = authService) {
                UserDetailScreen(navController = navController, userId = userId)
            }
        }

        composable(Screen.SupplierList.route) {
            AuthGuard(authService = authService) {
                SupplierListScreen(navController = navController)
            }
        }
    }
}