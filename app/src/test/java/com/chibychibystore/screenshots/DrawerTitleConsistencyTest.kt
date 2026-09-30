package com.chibychibystore.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.ui.audit.AuditListScreen
import com.chibychibystore.ui.audit.AuditUiState
import com.chibychibystore.ui.audit.AuditViewModel
import com.chibychibystore.ui.backup.BackupScreen
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
import com.chibychibystore.ui.components.shared.ScreenTitleTestTag
import com.chibychibystore.ui.components.shared.drawerNavItems
import com.chibychibystore.ui.dashboard.DashboardScreen
import com.chibychibystore.ui.dashboard.DashboardUiState
import com.chibychibystore.ui.dashboard.DashboardViewModel
import com.chibychibystore.ui.expense.ExpenseListScreen
import com.chibychibystore.ui.expense.ExpenseUiState
import com.chibychibystore.ui.expense.ExpenseViewModel
import com.chibychibystore.ui.inventory.InventoryScreen
import com.chibychibystore.ui.inventory.InventoryUiState
import com.chibychibystore.ui.inventory.InventoryViewModel
import com.chibychibystore.ui.inventory.WarehouseListScreen
import com.chibychibystore.ui.inventory.WarehouseUiState
import com.chibychibystore.ui.inventory.WarehouseViewModel
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.pelanggan.PelangganListScreen
import com.chibychibystore.ui.pelanggan.PelangganUiState
import com.chibychibystore.ui.pelanggan.PelangganViewModel
import com.chibychibystore.ui.pos.PosScreen
import com.chibychibystore.ui.pos.PosUiState
import com.chibychibystore.ui.pos.PosViewModel
import com.chibychibystore.ui.promotion.PromotionListScreen
import com.chibychibystore.ui.promotion.PromotionUiState
import com.chibychibystore.ui.promotion.PromotionViewModel
import com.chibychibystore.ui.purchase.PurchaseListScreen
import com.chibychibystore.ui.purchase.PurchaseUiState
import com.chibychibystore.ui.purchase.PurchaseViewModel
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
import com.chibychibystore.ui.theme.ChibyChibyStoreTheme
import com.chibychibystore.ui.user.CreateUserFormState
import com.chibychibystore.ui.user.EditUserFormState
import com.chibychibystore.ui.user.ResetPasswordFormState
import com.chibychibystore.ui.user.UserListScreen
import com.chibychibystore.ui.user.UserManagementUiState
import com.chibychibystore.ui.user.UserManagementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every drawer entry must open a screen whose top-bar title is the label the
 * user tapped. The drawer is the app-wide menu, so a mismatch means the user
 * taps "Pindai Barcode" and lands on a screen headed something else.
 *
 * The English-word test in [AppNavigationDrawerTest] cannot catch this on its
 * own: it only looks at the label strings, so a label can be perfectly
 * Indonesian while the screen it opens still shows the old wording (this is
 * exactly how `Pindai Barcode` came to open `Scan Barcode`).
 *
 * Each screen is rendered for real with mocked state and the tagged title is
 * compared against the drawer entry for its route. All mismatches are collected
 * so one run reports every offender instead of stopping at the first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DrawerTitleConsistencyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val slot = mutableStateOf<(@Composable () -> Unit)?>(null)

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ChibyChibyStoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        slot.value?.invoke()
                    }
                }
            }
        }
    }

    private fun nav(): TestNavHostController =
        TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

    private fun <T> state(value: T): StateFlow<T> = MutableStateFlow(value)

    private inline fun <reified VM : Any> mockVm(configure: VM.() -> Unit = {}): VM =
        mock<VM>().also { it.configure() }

    @Test
    fun `every drawer entry opens a screen titled with its own label`() {
        val labelByRoute = drawerNavItems.associate { it.route to it.label }

        val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
            Screen.Dashboard.route to {
                DashboardScreen(nav(), viewModel = mockVm<DashboardViewModel> {
                    whenever(uiState).thenReturn(state(DashboardUiState()))
                })
            },
            Screen.Pos.route to {
                PosScreen(nav(), viewModel = mockVm<PosViewModel> {
                    whenever(uiState).thenReturn(state(PosUiState()))
                })
            },
            Screen.Inventory.route to {
                InventoryScreen(nav(), viewModel = mockVm<InventoryViewModel> {
                    whenever(uiState).thenReturn(state(InventoryUiState()))
                })
            },
            Screen.SalesHistory.route to {
                SalesHistoryScreen(nav(), viewModel = mockVm<SalesHistoryViewModel> {
                    whenever(uiState).thenReturn(state(SalesHistoryUiState()))
                })
            },
            Screen.PurchaseList.route to {
                PurchaseListScreen(nav(), viewModel = mockVm<PurchaseViewModel> {
                    whenever(uiState).thenReturn(state(PurchaseUiState()))
                })
            },
            Screen.WarehouseList.route to {
                WarehouseListScreen(nav(), viewModel = mockVm<WarehouseViewModel> {
                    whenever(uiState).thenReturn(state(WarehouseUiState()))
                })
            },
            Screen.ExpenseList.route to {
                ExpenseListScreen(nav(), viewModel = mockVm<ExpenseViewModel> {
                    whenever(uiState).thenReturn(state(ExpenseUiState()))
                })
            },
            Screen.UserList.route to {
                UserListScreen(nav(), viewModel = mockVm<UserManagementViewModel> {
                    whenever(uiState).thenReturn(state(UserManagementUiState()))
                    whenever(createUserFormState).thenReturn(state(CreateUserFormState()))
                    whenever(editUserFormState).thenReturn(state(EditUserFormState()))
                    whenever(resetPasswordFormState).thenReturn(state(ResetPasswordFormState()))
                })
            },
            Screen.PromotionList.route to {
                PromotionListScreen(nav(), viewModel = mockVm<PromotionViewModel> {
                    whenever(uiState).thenReturn(state(PromotionUiState()))
                })
            },
            Screen.PelangganList.route to {
                PelangganListScreen(nav(), viewModel = mockVm<PelangganViewModel> {
                    whenever(uiState).thenReturn(state(PelangganUiState()))
                    whenever(searchQuery).thenReturn(state(""))
                })
            },
            Screen.CashShift.route to {
                ShiftScreen(nav(), viewModel = mockVm<ShiftViewModel> {
                    whenever(uiState).thenReturn(state(ShiftUiState()))
                })
            },
            Screen.CashHistory.route to {
                ShiftHistoryScreen(nav(), viewModel = mockVm<ShiftViewModel> {
                    whenever(uiState).thenReturn(state(ShiftUiState()))
                })
            },
            Screen.AuditList.route to {
                AuditListScreen(nav(), viewModel = mockVm<AuditViewModel> {
                    whenever(uiState).thenReturn(state(AuditUiState()))
                })
            },
            Screen.SupplierList.route to {
                SupplierListScreen(nav(), viewModel = mockVm<SupplierViewModel> {
                    whenever(uiState).thenReturn(state(SupplierUiState()))
                    whenever(searchQuery).thenReturn(state(""))
                })
            },
            Screen.Reports.route to {
                ReportsScreen(onNavigateBack = {}, viewModel = mockVm<ReportsViewModel> {
                    whenever(uiState).thenReturn(state(ReportsUiState()))
                })
            },
            Screen.BarcodeScanner.route to {
                BarcodeScannerScreen(onBarcodeScanned = {}, onDismiss = {}, viewModel = mockVm<BarcodeScannerViewModel> {
                    whenever(uiState).thenReturn(state(BarcodeScannerUiState()))
                })
            },
            Screen.BarcodePrint.route to {
                BarcodePrintScreen(onNavigateBack = {}, viewModel = mockVm<BarcodePrintViewModel> {
                    whenever(uiState).thenReturn(state(BarcodePrintUiState()))
                })
            },
            Screen.Backup.route to {
                BackupScreen(onNavigateBack = {}, viewModel = mockVm<BackupViewModel> {
                    whenever(uiState).thenReturn(state(BackupUiState()))
                    whenever(backupProgress).thenReturn(state(null))
                    whenever(restoreProgress).thenReturn(state(null))
                })
            },
            Screen.Settings.route to {
                SettingsScreen(onNavigateToRoute = {}, onLogout = {}, viewModel = mockVm<SettingsViewModel> {
                    whenever(uiState).thenReturn(state(SettingsUiState(currentUser = SampleData.owner, isLoading = false)))
                })
            }
        )

        val problems = mutableListOf<String>()
        screens.forEach { (route, content) ->
            val expected = labelByRoute[route]
            if (expected == null) {
                problems += "$route has a screen but no drawer entry"
                return@forEach
            }
            slot.value = content
            composeTestRule.waitForIdle()
            val actual = try {
                composeTestRule.onNodeWithTag(ScreenTitleTestTag, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .config.getOrNull(SemanticsProperties.Text)
                    ?.joinToString("") { it.text }
            } catch (t: Throwable) {
                problems += "$route: no title rendered (${t.message})"
                return@forEach
            }
            if (actual != expected) {
                problems += "$route: drawer label '$expected' but screen title '$actual'"
            }
        }

        assertTrue(
            "Drawer labels must match the title of the screen they open:\n" + problems.joinToString("\n"),
            problems.isEmpty()
        )
    }
}
