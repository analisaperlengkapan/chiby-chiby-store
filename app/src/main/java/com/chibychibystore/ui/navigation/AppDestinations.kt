package com.chibychibystore.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.inventory.*
import com.chibychibystore.ui.purchase.*
import com.chibychibystore.ui.promotion.*
import com.chibychibystore.ui.cash.*
import com.chibychibystore.ui.audit.*
import com.chibychibystore.ui.pelanggan.*
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
import com.chibychibystore.ui.user.UserListScreen
import com.chibychibystore.ui.user.UserDetailScreen
import com.chibychibystore.ui.supplier.SupplierListScreen

/**
 * Registers every destination of the app. Split out of `AppNavigation` so the
 * route graph can be built and asserted in tests without booting Hilt.
 */
fun NavGraphBuilder.appDestinations(
    navController: NavHostController,
    authService: AuthService,
    onOpenDrawer: () -> Unit,
    navigateToRoute: (String) -> Unit
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
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToRoute = navigateToRoute
                )
            }
        }

        composable(Screen.Inventory.route) {
            AuthGuard(authService = authService) {
                InventoryScreen(navController = navController, onOpenDrawer = onOpenDrawer)
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
                WarehouseListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
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
                PosScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.SalesHistory.route) {
            AuthGuard(authService = authService) {
                SalesHistoryScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.PurchaseList.route) {
            AuthGuard(authService = authService) {
                PurchaseListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.PurchaseAdd.route) {
            AuthGuard(authService = authService) {
                PurchaseAddScreen(navController = navController)
            }
        }

        composable(Screen.PromotionList.route) {
            AuthGuard(authService = authService) {
                PromotionListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.CashShift.route) {
            AuthGuard(authService = authService) {
                ShiftScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.CashHistory.route) {
            AuthGuard(authService = authService) {
                ShiftHistoryScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.AuditList.route) {
            AuthGuard(authService = authService) {
                AuditListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.AuditAdd.route) {
            AuthGuard(authService = authService) {
                AuditAddScreen(navController = navController)
            }
        }

        composable(Screen.PelangganList.route) {
            AuthGuard(authService = authService) {
                PelangganListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }

        composable(Screen.PelangganAdd.route) {
            AuthGuard(authService = authService) {
                PelangganAddEditScreen(navController = navController)
            }
        }

        composable(
            route = Screen.PelangganEdit.route,
            arguments = listOf(navArgument("pelangganId") { type = NavType.LongType })
        ) {
            val pelangganId = it.arguments?.getLong("pelangganId") ?: 0L
            AuthGuard(authService = authService) {
                PelangganAddEditScreen(navController = navController, pelangganId = pelangganId)
            }
        }

        composable(Screen.PromotionAdd.route) {
            AuthGuard(authService = authService) {
                PromotionAddEditScreen(navController = navController)
            }
        }

        composable(
            route = Screen.PromotionEdit.route,
            arguments = listOf(navArgument("promotionId") { type = NavType.LongType })
        ) {
            val promotionId = it.arguments?.getLong("promotionId") ?: 0L
            AuthGuard(authService = authService) {
                PromotionAddEditScreen(navController = navController, promotionId = promotionId)
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
                BarcodePrintScreen(onOpenDrawer = onOpenDrawer, onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Reports.route) {
            AuthGuard(authService = authService) {
                ReportsScreen(onOpenDrawer = onOpenDrawer, onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.ExpenseList.route) {
            AuthGuard(authService = authService) {
                ExpenseListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
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
                BackupScreen(onOpenDrawer = onOpenDrawer, onNavigateBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Settings.route) {
            AuthGuard(authService = authService) {
                SettingsScreen(
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToRoute = navigateToRoute,
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
                UserListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
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
                SupplierListScreen(navController = navController, onOpenDrawer = onOpenDrawer)
            }
        }
}
