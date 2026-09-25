package com.chibychibystore.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.navigation.NavController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.service.ArusKas
import com.chibychibystore.service.LaporanLabaBersih
import com.chibychibystore.service.LaporanLabaRugi
import com.chibychibystore.service.LaporanMarginLaba
import com.chibychibystore.service.LaporanPengeluaran
import com.chibychibystore.service.LaporanPenjualanKotor
import com.chibychibystore.service.NeracaSaldo
import com.chibychibystore.service.PeriodicPerformance
import com.chibychibystore.service.PenjualanKategori
import com.chibychibystore.service.PenjualanProduk
import com.chibychibystore.service.StockMovement
import com.chibychibystore.service.UserStats
import com.chibychibystore.ui.audit.AuditAddScreen
import com.chibychibystore.ui.audit.AuditItemInput
import com.chibychibystore.ui.audit.AuditListScreen
import com.chibychibystore.ui.audit.AuditUiState
import com.chibychibystore.ui.audit.AuditViewModel
import com.chibychibystore.ui.auth.LoginScreen
import com.chibychibystore.ui.auth.LoginUiState
import com.chibychibystore.ui.auth.LoginViewModel
import com.chibychibystore.ui.backup.BackupScreen
import com.chibychibystore.ui.components.shared.AppDrawer
import com.chibychibystore.ui.backup.BackupUiState
import com.chibychibystore.ui.backup.BackupViewModel
import com.chibychibystore.ui.barcode.BarcodePrintScreen
import com.chibychibystore.ui.barcode.BarcodePrintUiState
import com.chibychibystore.ui.barcode.BarcodePrintViewModel
import com.chibychibystore.ui.barcode.BarcodeScannerScreen
import com.chibychibystore.ui.barcode.BarcodeScannerUiState
import com.chibychibystore.ui.barcode.BarcodeScannerViewModel
import com.chibychibystore.ui.cash.ShiftHistoryScreen
import com.chibychibystore.ui.cash.ShiftScreen
import com.chibychibystore.ui.cash.ShiftUiState
import com.chibychibystore.ui.cash.ShiftViewModel
import com.chibychibystore.ui.dashboard.DashboardScreen
import com.chibychibystore.ui.dashboard.DashboardUiState
import com.chibychibystore.ui.dashboard.DashboardViewModel
import com.chibychibystore.ui.expense.ExpenseAddScreen
import com.chibychibystore.ui.expense.ExpenseAddUiState
import com.chibychibystore.ui.expense.ExpenseAddViewModel
import com.chibychibystore.ui.expense.ExpenseDetailScreen
import com.chibychibystore.ui.expense.ExpenseDetailUiState
import com.chibychibystore.ui.expense.ExpenseDetailViewModel
import com.chibychibystore.ui.expense.ExpenseListScreen
import com.chibychibystore.ui.expense.ExpenseUiState
import com.chibychibystore.ui.expense.ExpenseViewModel
import com.chibychibystore.ui.inventory.AddProductScreen
import com.chibychibystore.ui.inventory.InventoryScreen
import com.chibychibystore.ui.inventory.InventoryUiState
import com.chibychibystore.ui.inventory.InventoryViewModel
import com.chibychibystore.ui.inventory.ProductDetailScreen
import com.chibychibystore.ui.inventory.ProductDetailUiState
import com.chibychibystore.ui.inventory.ProductDetailViewModel
import com.chibychibystore.ui.inventory.WarehouseDetailScreen
import com.chibychibystore.ui.inventory.WarehouseListScreen
import com.chibychibystore.ui.inventory.WarehouseUiState
import com.chibychibystore.ui.inventory.WarehouseViewModel
import com.chibychibystore.ui.pelanggan.PelangganAddEditScreen
import com.chibychibystore.ui.pelanggan.PelangganListScreen
import com.chibychibystore.ui.pelanggan.PelangganUiState
import com.chibychibystore.ui.pelanggan.PelangganViewModel
import com.chibychibystore.ui.pos.PosScreen
import com.chibychibystore.ui.pos.PosUiState
import com.chibychibystore.ui.pos.PosViewModel
import com.chibychibystore.ui.promotion.PromotionAddEditScreen
import com.chibychibystore.ui.promotion.PromotionListScreen
import com.chibychibystore.ui.promotion.PromotionUiState
import com.chibychibystore.ui.promotion.PromotionViewModel
import com.chibychibystore.ui.purchase.CartItemPurchase
import com.chibychibystore.ui.purchase.PurchaseAddScreen
import com.chibychibystore.ui.purchase.PurchaseListScreen
import com.chibychibystore.ui.purchase.PurchaseUiState
import com.chibychibystore.ui.purchase.PurchaseViewModel
import com.chibychibystore.ui.reports.ReportType
import com.chibychibystore.ui.reports.ReportsScreen
import com.chibychibystore.ui.reports.ReportsUiState
import com.chibychibystore.ui.reports.ReportsViewModel
import com.chibychibystore.ui.sales.SalesHistoryScreen
import com.chibychibystore.ui.sales.SalesHistoryUiState
import com.chibychibystore.ui.sales.SalesHistoryViewModel
import com.chibychibystore.ui.settings.SettingsScreen
import com.chibychibystore.ui.settings.SettingsUiState
import com.chibychibystore.ui.settings.SettingsViewModel
import com.chibychibystore.ui.supplier.SupplierListScreen
import com.chibychibystore.ui.supplier.SupplierUiState
import com.chibychibystore.ui.supplier.SupplierViewModel
import com.chibychibystore.ui.user.CreateUserFormState
import com.chibychibystore.ui.user.EditUserFormState
import com.chibychibystore.ui.user.ResetPasswordFormState
import com.chibychibystore.ui.user.UserAddScreen
import com.chibychibystore.ui.user.UserDetailScreen
import com.chibychibystore.ui.user.UserDetailUiState
import com.chibychibystore.ui.user.UserDetailViewModel
import com.chibychibystore.ui.user.UserListScreen
import com.chibychibystore.ui.user.UserManagementUiState
import com.chibychibystore.ui.user.UserManagementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

/**
 * One capture per user-visible page/view. ViewModels are mocked with populated
 * state so every screen renders its full layout instead of an empty shell.
 */
class ScreenshotCatalog(
    private val rule: AndroidComposeTestRule<*, ComponentActivity>
) {

    fun capture(name: String, content: @Composable () -> Unit) =
        ScreenshotHarness.capture(rule, name, content)

    private fun <T> state(value: T): StateFlow<T> = MutableStateFlow(value)

    private inline fun <reified VM : Any> mockVm(configure: VM.() -> Unit = {}): VM =
        mock<VM>().also { it.configure() }

    @Composable
    private fun nav(): TestNavHostController {
        return remember {
            TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
        }
    }

    fun captureLogin() = capture("01-login") {
        val vm = mockVm<LoginViewModel>() {
            whenever(uiState).thenReturn(state(LoginUiState(username = "", password = "")))
        }
        LoginScreen(onLoginSuccess = {}, viewModel = vm)
    }

    fun captureDashboard() = capture("02-dashboard") {
        val vm = mockVm<DashboardViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    DashboardUiState(
                        todaySales = 280_500.0,
                        todayTransactionCount = 2,
                        lowStockItems = SampleData.lowStock,
                        recentTransactions = SampleData.sales.take(3),
                        salesTrend = listOf(
                            com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 26), 1_250_000.0, 18),
                            com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 27), 1_680_000.0, 24),
                            com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 28), 2_120_000.0, 31),
                            com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 29), 1_540_000.0, 22),
                            com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 30), 2_480_000.0, 35),
                            com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 31), 2_010_000.0, 29),
                            com.chibychibystore.service.DataTren(LocalDate.of(2026, 1, 1), 280_500.0, 2)
                        ),
                        isLoading = false
                    )
                )
            )
        }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        DashboardScreen(
            navController = nav(),
            drawerState = drawerState,
            currentRoute = "dashboard",
            onNavigateToRoute = {},
            viewModel = vm
        )
    }

    fun captureDashboardDrawer() = capture("02b-navigation-drawer") {
        val vm = mockVm<DashboardViewModel>() {
            whenever(uiState).thenReturn(
                state(DashboardUiState(todaySales = 280_500.0, todayTransactionCount = 2, isLoading = false))
            )
        }
        AppDrawer(
            drawerState = rememberDrawerState(DrawerValue.Open),
            currentRoute = "dashboard",
            onNavigateToRoute = {},
            onCloseDrawer = {}
        ) {
            DashboardScreen(
                navController = nav(),
                drawerState = rememberDrawerState(DrawerValue.Closed),
                currentRoute = "dashboard",
                onNavigateToRoute = {},
                viewModel = vm
            )
        }
    }

    fun captureInventory() = capture("03-inventory") {
        val vm = mockVm<InventoryViewModel>() {
            whenever(uiState).thenReturn(
                state(InventoryUiState(products = SampleData.products, lowStockProducts = SampleData.lowStock))
            )
        }
        InventoryScreen(navController = nav(), viewModel = vm)
    }

    fun captureAddProduct() = capture("04-inventory-add-product") {
        val vm = mockVm<ProductDetailViewModel>() {
            whenever(uiState).thenReturn(state(ProductDetailUiState(isEditing = true)))
        }
        AddProductScreen(navController = nav(), viewModel = vm)
    }

    fun captureProductDetail() = capture("05-inventory-product-detail") {
        val vm = mockVm<ProductDetailViewModel>() {
            whenever(uiState).thenReturn(
                state(ProductDetailUiState(product = SampleData.products.first(), categories = SampleData.categories))
            )
        }
        ProductDetailScreen(navController = nav(), productId = "1", viewModel = vm)
    }

    fun captureWarehouseList() = capture("06-warehouse-list") {
        val vm = mockVm<WarehouseViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    WarehouseUiState(
                        warehouses = SampleData.warehouses,
                        allWarehouseStock = SampleData.warehouses.associateWith { SampleData.products }
                    )
                )
            )
        }
        WarehouseListScreen(navController = nav(), viewModel = vm)
    }

    fun captureWarehouseDetail() = capture("07-warehouse-detail") {
        val vm = mockVm<WarehouseViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    WarehouseUiState(
                        warehouses = SampleData.warehouses,
                        selectedWarehouse = SampleData.warehouses.first(),
                        products = SampleData.products
                    )
                )
            )
        }
        WarehouseDetailScreen(navController = nav(), warehouseId = "1", viewModel = vm)
    }

    fun captureWarehouseEdit() = capture("08-warehouse-edit") {
        val vm = mockVm<WarehouseViewModel>() {
            whenever(uiState).thenReturn(
                state(WarehouseUiState(warehouses = SampleData.warehouses, selectedWarehouse = SampleData.warehouses.first()))
            )
        }
        com.chibychibystore.ui.inventory.EditWarehouseScreen(navController = nav(), warehouseId = "1", viewModel = vm)
    }

    fun captureWarehouseAdd() = capture("09-warehouse-add") {
        val vm = mockVm<WarehouseViewModel>() {
            whenever(uiState).thenReturn(state(WarehouseUiState(warehouses = SampleData.warehouses)))
        }
        com.chibychibystore.ui.inventory.AddWarehouseScreen(navController = nav(), viewModel = vm)
    }

    fun capturePos() = capture("10-pos") {
        val vm = mockVm<PosViewModel>() {
            val products = SampleData.products.take(4)
            whenever(uiState).thenReturn(
                state(
                    PosUiState(
                        cartItems = listOf(
                            com.chibychibystore.ui.pos.CartItem(products[0], 2),
                            com.chibychibystore.ui.pos.CartItem(products[3], 1)
                        ),
                        searchQuery = "",
                        searchResults = products,
                        subtotal = 17_500.0,
                        tax = 1_925.0,
                        discount = 0.0,
                        total = 19_425.0,
                        pelangganList = SampleData.customers,
                        selectedPelanggan = SampleData.customers.first(),
                        warehouses = SampleData.warehouses,
                        selectedWarehouseId = 1
                    )
                )
            )
        }
        PosScreen(navController = nav(), viewModel = vm)
    }

    fun captureSalesHistory() = capture("11-sales-history") {
        val vm = mockVm<SalesHistoryViewModel>() {
            whenever(uiState).thenReturn(state(SalesHistoryUiState(sales = SampleData.sales)))
        }
        SalesHistoryScreen(navController = nav(), viewModel = vm)
    }

    fun captureSalesReceipt() = capture("12-sales-receipt-dialog") {
        val vm = mockVm<SalesHistoryViewModel>() {
            whenever(uiState).thenReturn(
                state(SalesHistoryUiState(sales = SampleData.sales, selectedSale = SampleData.saleWithItems, showReceiptDialog = true))
            )
        }
        SalesHistoryScreen(navController = nav(), viewModel = vm)
    }

    fun capturePurchaseList() = capture("13-purchase-list") {
        val vm = mockVm<PurchaseViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    PurchaseUiState(
                        purchases = SampleData.purchases,
                        suppliers = SampleData.suppliers,
                        products = SampleData.products,
                        warehouses = SampleData.warehouses
                    )
                )
            )
        }
        PurchaseListScreen(navController = nav(), viewModel = vm)
    }

    fun capturePurchaseAdd() = capture("14-purchase-add") {
        val vm = mockVm<PurchaseViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    PurchaseUiState(
                        suppliers = SampleData.suppliers,
                        products = SampleData.products,
                        warehouses = SampleData.warehouses
                    )
                )
            )
            whenever(cart).thenReturn(
                state(
                    listOf(
                        CartItemPurchase(SampleData.products[0], 48, 2_500.0),
                        CartItemPurchase(SampleData.products[5], 40, 58_000.0)
                    )
                )
            )
        }
        PurchaseAddScreen(navController = nav(), viewModel = vm)
    }

    fun capturePromotionList() = capture("15-promotion-list") {
        val vm = mockVm<PromotionViewModel>() {
            whenever(uiState).thenReturn(state(PromotionUiState(promotions = SampleData.promotions)))
        }
        PromotionListScreen(navController = nav(), viewModel = vm)
    }

    fun capturePromotionAdd() = capture("16-promotion-add") {
        val vm = mockVm<PromotionViewModel>() {
            whenever(uiState).thenReturn(state(PromotionUiState(promotions = SampleData.promotions)))
        }
        PromotionAddEditScreen(navController = nav(), viewModel = vm)
    }

    fun captureShift() = capture("17-cash-shift") {
        val vm = mockVm<ShiftViewModel>() {
            whenever(uiState).thenReturn(
                state(ShiftUiState(currentShift = SampleData.shifts.first(), shifts = SampleData.shifts))
            )
        }
        ShiftScreen(navController = nav(), viewModel = vm)
    }

    fun captureShiftHistory() = capture("18-cash-shift-history") {
        val vm = mockVm<ShiftViewModel>() {
            whenever(uiState).thenReturn(state(ShiftUiState(shifts = SampleData.shifts)))
        }
        ShiftHistoryScreen(navController = nav(), viewModel = vm)
    }

    fun captureAuditList() = capture("19-audit-list") {
        val vm = mockVm<AuditViewModel>() {
            whenever(uiState).thenReturn(
                state(AuditUiState(audits = SampleData.audits, warehouses = SampleData.warehouses, products = SampleData.products))
            )
        }
        AuditListScreen(navController = nav(), viewModel = vm)
    }

    fun captureAuditAdd() = capture("20-audit-add") {
        val vm = mockVm<AuditViewModel>() {
            whenever(uiState).thenReturn(
                state(AuditUiState(warehouses = SampleData.warehouses, products = SampleData.products))
            )
            whenever(auditItems).thenReturn(
                state(SampleData.products.map { AuditItemInput(it, it.stockQuantity, it.stockQuantity - 1) })
            )
        }
        AuditAddScreen(navController = nav(), viewModel = vm)
    }

    fun capturePelangganList() = capture("21-customer-list") {
        val vm = mockVm<PelangganViewModel>() {
            whenever(uiState).thenReturn(state(PelangganUiState(pelangganList = SampleData.customers)))
            whenever(searchQuery).thenReturn(state(""))
        }
        PelangganListScreen(navController = nav(), viewModel = vm)
    }

    fun capturePelangganAdd() = capture("22-customer-add") {
        val vm = mockVm<PelangganViewModel>() {
            whenever(uiState).thenReturn(state(PelangganUiState(pelangganList = SampleData.customers)))
            whenever(searchQuery).thenReturn(state(""))
        }
        PelangganAddEditScreen(navController = nav(), viewModel = vm)
    }

    fun capturePelangganEdit() = capture("22b-customer-edit") {
        val vm = mockVm<PelangganViewModel>() {
            whenever(uiState).thenReturn(state(PelangganUiState(pelangganList = SampleData.customers)))
            whenever(searchQuery).thenReturn(state(""))
        }
        PelangganAddEditScreen(navController = nav(), pelangganId = 1L, viewModel = vm)
    }

    fun capturePromotionEdit() = capture("16b-promotion-edit") {
        val vm = mockVm<PromotionViewModel>() {
            whenever(uiState).thenReturn(state(PromotionUiState(promotions = SampleData.promotions)))
        }
        PromotionAddEditScreen(navController = nav(), promotionId = 1L, viewModel = vm)
    }

    fun captureBarcodeScanner() = capture("23-barcode-scanner") {
        val vm = mockVm<BarcodeScannerViewModel>() {
            whenever(uiState).thenReturn(
                state(BarcodeScannerUiState(hasCameraPermission = false, isScanning = false))
            )
        }
        BarcodeScannerScreen(onBarcodeScanned = {}, onDismiss = {}, viewModel = vm)
    }

    fun captureBarcodePrint() = capture("24-barcode-print") {
        val vm = mockVm<BarcodePrintViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    BarcodePrintUiState(
                        products = SampleData.products,
                        selectedProduct = SampleData.products.first(),
                        quantity = 3
                    )
                )
            )
        }
        BarcodePrintScreen(onNavigateBack = {}, viewModel = vm)
    }

    fun captureExpenseList() = capture("25-expense-list") {
        val vm = mockVm<ExpenseViewModel>() {
            whenever(uiState).thenReturn(
                state(ExpenseUiState(expenses = SampleData.expenses, totalExpenses = SampleData.expenses.sumOf { it.amount }))
            )
        }
        ExpenseListScreen(navController = nav(), viewModel = vm)
    }

    fun captureExpenseAdd() = capture("26-expense-add") {
        val vm = mockVm<ExpenseAddViewModel>() {
            whenever(uiState).thenReturn(state(ExpenseAddUiState()))
        }
        ExpenseAddScreen(navController = nav(), viewModel = vm)
    }

    fun captureExpenseDetail() = capture("27-expense-detail") {
        val vm = mockVm<ExpenseDetailViewModel>() {
            whenever(uiState).thenReturn(state(ExpenseDetailUiState(expense = SampleData.expenses.first())))
        }
        ExpenseDetailScreen(navController = nav(), expenseId = 1, viewModel = vm)
    }

    fun captureBackup() = capture("28-backup-restore") {
        val vm = mockVm<BackupViewModel>() {
            whenever(uiState).thenReturn(state(BackupUiState(backupHistory = emptyList())))
            whenever(backupProgress).thenReturn(state(null))
            whenever(restoreProgress).thenReturn(state(null))
        }
        BackupScreen(onNavigateBack = {}, viewModel = vm)
    }

    fun captureUserList() = capture("29-user-list") {
        val vm = mockVm<UserManagementViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    UserManagementUiState(
                        users = SampleData.users,
                        filteredUsers = SampleData.users,
                        userStats = UserStats(totalUsers = 5, activeUsers = 4, owners = 1, managers = 1, cashiers = 2, warehouseStaff = 1)
                    )
                )
            )
            whenever(createUserFormState).thenReturn(state(CreateUserFormState()))
            whenever(editUserFormState).thenReturn(state(EditUserFormState()))
            whenever(resetPasswordFormState).thenReturn(state(ResetPasswordFormState()))
        }
        UserListScreen(navController = nav(), viewModel = vm)
    }

    fun captureUserAdd() = capture("30-user-add") {
        val vm = mockVm<UserManagementViewModel>() {
            whenever(uiState).thenReturn(state(UserManagementUiState()))
            whenever(createUserFormState).thenReturn(state(CreateUserFormState()))
        }
        UserAddScreen(navController = nav(), viewModel = vm)
    }

    fun captureUserDetail() = capture("31-user-detail") {
        val vm = mockVm<UserDetailViewModel>() {
            whenever(uiState).thenReturn(state(UserDetailUiState(user = SampleData.users[1])))
        }
        UserDetailScreen(navController = nav(), userId = 2, viewModel = vm)
    }

    fun captureSupplierList() = capture("32-supplier-list") {
        val vm = mockVm<SupplierViewModel>() {
            whenever(uiState).thenReturn(state(SupplierUiState(suppliers = SampleData.suppliers)))
            whenever(searchQuery).thenReturn(state(""))
        }
        SupplierListScreen(navController = nav(), viewModel = vm)
    }

    fun captureSettings() = capture("33-settings") {
        val vm = mockVm<SettingsViewModel>() {
            whenever(uiState).thenReturn(state(SettingsUiState(currentUser = SampleData.owner, isLoading = false)))
        }
        SettingsScreen(currentRoute = "settings", onNavigateToRoute = {}, onLogout = {}, viewModel = vm)
    }

    fun captureReport(name: String, type: ReportType, data: Any) = capture("34-report-$name") {
        val vm = mockVm<ReportsViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    ReportsUiState(
                        selectedReportType = type,
                        startDate = LocalDate.of(2025, 12, 1),
                        endDate = LocalDate.of(2025, 12, 31),
                        reportData = data
                    )
                )
            )
        }
        ReportsScreen(onNavigateBack = {}, viewModel = vm)
    }

    fun captureUserDeleteDialog() = capture("37-dialog-user-delete") {
        val vm = mockVm<UserManagementViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    UserManagementUiState(
                        users = SampleData.users,
                        filteredUsers = SampleData.users,
                        showDeleteUserDialog = true,
                        selectedUser = SampleData.users[2],
                        userStats = UserStats(totalUsers = 5, activeUsers = 4, owners = 1, managers = 1, cashiers = 2, warehouseStaff = 1)
                    )
                )
            )
            whenever(createUserFormState).thenReturn(state(CreateUserFormState()))
            whenever(editUserFormState).thenReturn(state(EditUserFormState()))
            whenever(resetPasswordFormState).thenReturn(state(ResetPasswordFormState()))
        }
        UserListScreen(navController = nav(), viewModel = vm)
    }

    fun capturePosReceiptDialog() = capture("39-dialog-pos-receipt") {
        val vm = mockVm<PosViewModel>() {
            whenever(uiState).thenReturn(
                state(
                    PosUiState(
                        cartItems = emptyList(),
                        showReceiptDialog = true,
                        completedSaleId = 12L,
                        pelangganList = SampleData.customers,
                        warehouses = SampleData.warehouses,
                        selectedWarehouseId = 1
                    )
                )
            )
        }
        PosScreen(navController = nav(), viewModel = vm)
    }

    fun captureAllReports() {
        captureReport(
            "gross-sales", ReportType.GROSS_SALES,
            LaporanPenjualanKotor(12_480_000.0, 148, 84_324.32)
        )
        captureReport(
            "profit-margin", ReportType.PROFIT_MARGIN,
            LaporanMarginLaba(12_480_000.0, 8_920_000.0, 3_560_000.0, 28.5)
        )
        captureReport(
            "net-profit", ReportType.NET_PROFIT,
            LaporanLabaBersih(3_560_000.0, 1_850_000.0, 1_710_000.0, 13.7)
        )
        captureReport(
            "sales-by-product", ReportType.SALES_BY_PRODUCT,
            listOf(
                PenjualanProduk(1, "Air Mineral 600ml", 480, 1_920_000.0, 1_200_000.0, 720_000.0),
                PenjualanProduk(6, "Beras Premium 5kg", 120, 8_160_000.0, 6_960_000.0, 1_200_000.0)
            )
        )
        captureReport(
            "sales-by-category", ReportType.SALES_BY_CATEGORY,
            listOf(
                PenjualanKategori(1, "Minuman", 620, 2_480_000.0, 1_550_000.0, 930_000.0),
                PenjualanKategori(3, "Sembako", 210, 8_900_000.0, 7_560_000.0, 1_340_000.0)
            )
        )
        captureReport(
            "sales-trend", ReportType.SALES_TREND,
            listOf(
                com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 30), 2_480_000.0, 35),
                com.chibychibystore.service.DataTren(LocalDate.of(2025, 12, 31), 2_010_000.0, 29)
            )
        )
        captureReport(
            "income-statement", ReportType.INCOME_STATEMENT,
            LaporanLabaRugi(12_480_000.0, 8_920_000.0, 3_560_000.0, 1_850_000.0, 1_710_000.0)
        )
        captureReport(
            "cash-flow", ReportType.CASH_FLOW,
            ArusKas(1_710_000.0, -850_000.0, 500_000.0, 1_360_000.0, 4_200_000.0, 5_560_000.0, "Desember 2025")
        )
        captureReport(
            "expense", ReportType.EXPENSE_REPORT,
            LaporanPengeluaran(1_850_000.0, mapOf("Utilitas" to 1_250_000.0, "Pemasaran" to 600_000.0))
        )
        captureReport(
            "balance-sheet", ReportType.BALANCE_SHEET,
            NeracaSaldo(42_500_000.0, 12_300_000.0, 30_200_000.0, 18_750_000.0)
        )
        captureReport(
            "stock-movement", ReportType.STOCK_MOVEMENT,
            listOf(
                StockMovement(LocalDate.of(2025, 12, 30), 1, "Air Mineral 600ml", "PURCHASE", 240, "Gudang Utama", "INV-2026-0001"),
                StockMovement(LocalDate.of(2025, 12, 31), 1, "Air Mineral 600ml", "SALE", -12, "Gudang Utama", "TRX-0007")
            )
        )
        captureReport(
            "periodic-summary", ReportType.PERIODIC_SUMMARY,
            listOf(
                PeriodicPerformance("2025-12-30", 2_480_000.0, 410_000.0, 35),
                PeriodicPerformance("2025-12-31", 2_010_000.0, 328_000.0, 29)
            )
        )
    }

    fun captureAll() {
        captureLogin()
        captureDashboard()
        captureDashboardDrawer()
        captureInventory()
        captureAddProduct()
        captureProductDetail()
        captureWarehouseList()
        captureWarehouseDetail()
        captureWarehouseEdit()
        captureWarehouseAdd()
        capturePos()
        captureSalesHistory()
        captureSalesReceipt()
        capturePurchaseList()
        capturePurchaseAdd()
        capturePromotionList()
        capturePromotionAdd()
        captureShift()
        captureShiftHistory()
        captureAuditList()
        captureAuditAdd()
        capturePelangganList()
        capturePelangganAdd()
        capturePelangganEdit()
        capturePromotionEdit()
        captureBarcodeScanner()
        captureBarcodePrint()
        captureExpenseList()
        captureExpenseAdd()
        captureExpenseDetail()
        captureBackup()
        captureUserList()
        captureUserAdd()
        captureUserDetail()
        captureSupplierList()
        captureSettings()
        captureUserDeleteDialog()
        capturePosReceiptDialog()
        captureAllReports()
    }
}
