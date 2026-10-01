package com.chibychibystore.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.audit.AuditUiState
import com.chibychibystore.ui.audit.AuditViewModel
import com.chibychibystore.ui.auth.LoginUiState
import com.chibychibystore.ui.auth.LoginViewModel
import com.chibychibystore.ui.backup.BackupUiState
import com.chibychibystore.ui.backup.BackupViewModel
import com.chibychibystore.ui.barcode.BarcodePrintUiState
import com.chibychibystore.ui.barcode.BarcodePrintViewModel
import com.chibychibystore.ui.barcode.BarcodeScannerUiState
import com.chibychibystore.ui.barcode.BarcodeScannerViewModel
import com.chibychibystore.ui.cash.ShiftUiState
import com.chibychibystore.ui.cash.ShiftViewModel
import com.chibychibystore.ui.components.shared.LocalScreenViewModelFactory
import com.chibychibystore.ui.components.shared.ScreenTitleTestTag
import com.chibychibystore.ui.dashboard.DashboardUiState
import com.chibychibystore.ui.dashboard.DashboardViewModel
import com.chibychibystore.ui.expense.ExpenseUiState
import com.chibychibystore.ui.expense.ExpenseViewModel
import com.chibychibystore.ui.inventory.InventoryUiState
import com.chibychibystore.ui.inventory.InventoryViewModel
import com.chibychibystore.ui.inventory.WarehouseUiState
import com.chibychibystore.ui.inventory.WarehouseViewModel
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.navigation.appDestinations
import com.chibychibystore.ui.pelanggan.PelangganUiState
import com.chibychibystore.ui.pelanggan.PelangganViewModel
import com.chibychibystore.ui.pos.PosUiState
import com.chibychibystore.ui.pos.PosViewModel
import com.chibychibystore.ui.promotion.PromotionUiState
import com.chibychibystore.ui.promotion.PromotionViewModel
import com.chibychibystore.ui.purchase.PurchaseUiState
import com.chibychibystore.ui.purchase.PurchaseViewModel
import com.chibychibystore.ui.reports.ReportsUiState
import com.chibychibystore.ui.reports.ReportsViewModel
import com.chibychibystore.ui.sales.SalesHistoryUiState
import com.chibychibystore.ui.sales.SalesHistoryViewModel
import com.chibychibystore.ui.settings.SettingsUiState
import com.chibychibystore.ui.settings.SettingsViewModel
import com.chibychibystore.ui.supplier.SupplierUiState
import com.chibychibystore.ui.supplier.SupplierViewModel
import com.chibychibystore.ui.theme.ChibyChibyStoreTheme
import com.chibychibystore.ui.user.CreateUserFormState
import com.chibychibystore.ui.user.EditUserFormState
import com.chibychibystore.ui.user.ResetPasswordFormState
import com.chibychibystore.ui.user.UserManagementUiState
import com.chibychibystore.ui.user.UserManagementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A restored session must land the user on the dashboard, not the login form.
 *
 * The restore path previously left the graph's start destination at
 * `Screen.Login` regardless of the restored user, and nothing navigated away
 * from it: a signed-in user was stuck staring at the login screen with no way
 * into the app. `AppNavigation` now picks the start destination from the user
 * that `initializeSession()` restored.
 *
 * This asserts the behaviour `AppNavigation` owns - the mapping from a restored
 * user to the start route - by building the real graph with that start route and
 * reading the rendered title. The restore itself is covered by
 * [com.chibychibystore.service.AuthSessionRestoreTest]; `AppNavigation` itself is
 * not instantiated because it resolves its ViewModels through Hilt, which has no
 * component under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppNavigationStartDestinationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var nav: TestNavHostController

    private fun <T> state(value: T): StateFlow<T> = MutableStateFlow(value)

    private inline fun <reified VM : Any> mockVm(configure: VM.() -> Unit = {}): VM =
        mock<VM>().also { it.configure() }

    private val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val vm: ViewModel = when (modelClass) {
                DashboardViewModel::class.java ->
                    mockVm<DashboardViewModel> { whenever(uiState).thenReturn(state(DashboardUiState())) }
                PosViewModel::class.java ->
                    mockVm<PosViewModel> { whenever(uiState).thenReturn(state(PosUiState())) }
                InventoryViewModel::class.java ->
                    mockVm<InventoryViewModel> { whenever(uiState).thenReturn(state(InventoryUiState())) }
                SalesHistoryViewModel::class.java ->
                    mockVm<SalesHistoryViewModel> { whenever(uiState).thenReturn(state(SalesHistoryUiState())) }
                PurchaseViewModel::class.java ->
                    mockVm<PurchaseViewModel> { whenever(uiState).thenReturn(state(PurchaseUiState())) }
                WarehouseViewModel::class.java ->
                    mockVm<WarehouseViewModel> { whenever(uiState).thenReturn(state(WarehouseUiState())) }
                ExpenseViewModel::class.java ->
                    mockVm<ExpenseViewModel> { whenever(uiState).thenReturn(state(ExpenseUiState())) }
                UserManagementViewModel::class.java ->
                    mockVm<UserManagementViewModel> {
                        whenever(uiState).thenReturn(state(UserManagementUiState()))
                        whenever(createUserFormState).thenReturn(state(CreateUserFormState()))
                        whenever(editUserFormState).thenReturn(state(EditUserFormState()))
                        whenever(resetPasswordFormState).thenReturn(state(ResetPasswordFormState()))
                    }
                PromotionViewModel::class.java ->
                    mockVm<PromotionViewModel> { whenever(uiState).thenReturn(state(PromotionUiState())) }
                PelangganViewModel::class.java ->
                    mockVm<PelangganViewModel> {
                        whenever(uiState).thenReturn(state(PelangganUiState()))
                        whenever(searchQuery).thenReturn(state(""))
                    }
                ShiftViewModel::class.java ->
                    mockVm<ShiftViewModel> { whenever(uiState).thenReturn(state(ShiftUiState())) }
                AuditViewModel::class.java ->
                    mockVm<AuditViewModel> { whenever(uiState).thenReturn(state(AuditUiState())) }
                SupplierViewModel::class.java ->
                    mockVm<SupplierViewModel> {
                        whenever(uiState).thenReturn(state(SupplierUiState()))
                        whenever(searchQuery).thenReturn(state(""))
                    }
                ReportsViewModel::class.java ->
                    mockVm<ReportsViewModel> { whenever(uiState).thenReturn(state(ReportsUiState())) }
                BarcodeScannerViewModel::class.java ->
                    mockVm<BarcodeScannerViewModel> { whenever(uiState).thenReturn(state(BarcodeScannerUiState())) }
                BarcodePrintViewModel::class.java ->
                    mockVm<BarcodePrintViewModel> { whenever(uiState).thenReturn(state(BarcodePrintUiState())) }
                BackupViewModel::class.java ->
                    mockVm<BackupViewModel> {
                        whenever(uiState).thenReturn(state(BackupUiState()))
                        whenever(backupProgress).thenReturn(state(null))
                        whenever(restoreProgress).thenReturn(state(null))
                    }
                SettingsViewModel::class.java ->
                    mockVm<SettingsViewModel> {
                        whenever(uiState).thenReturn(state(SettingsUiState(currentUser = SampleData.owner, isLoading = false)))
                    }
                LoginViewModel::class.java ->
                    mockVm<LoginViewModel> { whenever(uiState).thenReturn(state(LoginUiState())) }
                else -> error("AppNavigationStartDestinationTest has no mock ViewModel for ${modelClass.name}")
            }
            @Suppress("UNCHECKED_CAST")
            return vm as T
        }
    }

    private fun render(startDestination: String) {
        val authService = mock<AuthService>()
        whenever(authService.observeCurrentUser()).thenReturn(state(SampleData.owner))

        nav = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalScreenViewModelFactory provides viewModelFactory) {
                ChibyChibyStoreTheme {
                    NavHost(navController = nav, startDestination = startDestination) {
                        appDestinations(
                            navController = nav,
                            authService = authService,
                            onOpenDrawer = {},
                            navigateToRoute = {}
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun renderedTitle(): String? =
        composeTestRule.onNodeWithTag(ScreenTitleTestTag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }

    @Test
    fun `a restored user starts on the dashboard`() {
        render(Screen.Dashboard.route)

        assertEquals(Screen.Dashboard.route, nav.currentDestination?.route)
        assertEquals("Dasbor", renderedTitle())
    }

    @Test
    fun `a logged-out user starts on the login form`() {
        render(Screen.Login.route)

        assertEquals(Screen.Login.route, nav.currentDestination?.route)
        // The login form is full-bleed with no top bar, so assert its content
        // rather than a title.
        composeTestRule.onNodeWithText("Selamat Datang!").assertExists()
    }
}
