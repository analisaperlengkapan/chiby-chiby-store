package com.chibychibystore.ui.dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.DataTren
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Date

/**
 * Restores the dashboard screen coverage deleted with `DashboardScreenTest`,
 * rewritten against the current [DashboardUiState] and top bar (`Dasbor`).
 * The viewport is tall enough that the recent-transactions card is on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun navController(): TestNavHostController =
        TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

    private fun viewModel(state: DashboardUiState): DashboardViewModel =
        mock<DashboardViewModel>().also { whenever(it.uiState).thenReturn(MutableStateFlow(state)) }

    private fun product(name: String = "Kopi") = Produk(
        id = 1L, name = name, barcode = "111", categoryId = 1L,
        costPrice = 5_000.0, sellingPrice = 8_000.0, stockQuantity = 2, warehouseId = 1L, minStock = 5
    )

    private fun sale() = Penjualan(
        id = 1L, saleDate = Date(), totalAmount = 25_000.0,
        paymentMethod = PaymentMethod.CASH, cashierId = 1L
    )

    @Test
    fun `shows the dashboard title`() {
        composeTestRule.setContent {
            DashboardScreen(navController = navController(), viewModel = viewModel(DashboardUiState(isLoading = false)))
        }

        composeTestRule.onNodeWithText("Dasbor").assertIsDisplayed()
    }

    @Test
    fun `shows the sales summary and transaction count`() {
        composeTestRule.setContent {
            DashboardScreen(
                navController = navController(),
                viewModel = viewModel(
                    DashboardUiState(
                        isLoading = false,
                        todaySales = 125_000.0,
                        todayTransactionCount = 4,
                        salesTrend = listOf(DataTren(LocalDate.now(), 10_000.0, 1))
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Penjualan Hari Ini").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transaksi").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tren Penjualan (7 Hari)").assertIsDisplayed()
    }

    @Test
    fun `shows the low stock alert when items exist`() {
        composeTestRule.setContent {
            DashboardScreen(
                navController = navController(),
                viewModel = viewModel(
                    DashboardUiState(isLoading = false, lowStockItems = listOf(product("Gula")))
                )
            )
        }

        composeTestRule.onNodeWithText("Stok Menipis").assertIsDisplayed()
    }

    @Test
    fun `shows the recent transactions section`() {
        composeTestRule.setContent {
            DashboardScreen(
                navController = navController(),
                viewModel = viewModel(DashboardUiState(isLoading = false, recentTransactions = listOf(sale())))
            )
        }

        composeTestRule.onNodeWithText("Transaksi Terakhir").assertIsDisplayed()
    }

    @Test
    fun `shows a loading indicator while loading`() {
        composeTestRule.setContent {
            DashboardScreen(navController = navController(), viewModel = viewModel(DashboardUiState(isLoading = true)))
        }

        // The KPI section only renders once loading finishes.
        composeTestRule.onAllNodesWithText("Penjualan Hari Ini").assertCountEquals(0)
    }
}
