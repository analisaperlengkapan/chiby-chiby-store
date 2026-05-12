package com.chibychibystore.ui.screens
import org.robolectric.annotation.Config
import com.chibychibystore.ui.dashboard.DashboardViewModel
import com.chibychibystore.ui.dashboard.DashboardUiState

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.chibychibystore.ui.viewmodel.DashboardUiState
import com.chibychibystore.ui.viewmodel.DashboardViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dashboard screen displays title and refresh button`() {
        val viewModel = mock<DashboardViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(DashboardUiState())
            )
        }
        val onNavigateToRoute = mock<(String) -> Unit>()

        composeTestRule.setContent {
            DashboardScreen(
                currentRoute = "dashboard",
                onNavigateToRoute = onNavigateToRoute,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Chiby Chiby Store").assertIsDisplayed()
    }

    @Test
    fun `dashboard screen shows loading indicator when loading`() {
        val viewModel = mock<DashboardViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(
                    DashboardUiState(isLoading = true)
                )
            )
        }
        val onNavigateToRoute = mock<(String) -> Unit>()

        composeTestRule.setContent {
            DashboardScreen(
                currentRoute = "dashboard",
                onNavigateToRoute = onNavigateToRoute,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Memuat data dashboard...").assertIsDisplayed()
    }

    @Test
    fun `dashboard screen displays sales summary`() {
        val viewModel = mock<DashboardViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(
                    DashboardUiState(
                        todaySales = 2500000.0,
                        todayTransactionCount = 25
                    )
                )
            )
        }
        val onNavigateToRoute = mock<(String) -> Unit>()

        composeTestRule.setContent {
            DashboardScreen(
                currentRoute = "dashboard",
                onNavigateToRoute = onNavigateToRoute,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Penjualan Hari Ini").assertIsDisplayed()
        composeTestRule.onNodeWithText("25 transaksi").assertIsDisplayed()
    }

    @Test
    fun `dashboard screen shows low stock alert when items exist`() {
        val mockLowStockItems = listOf(
            com.chibychibystore.data.model.Produk(
                id = 1,
                nama = "Test Product",
                barcode = "123456789",
                hargaJual = 10000.0,
                hargaBeli = 8000.0,
                stok = 5,
                kategoriId = 1,
                gudangId = 1,
                pemasokId = 1
            )
        )

        val viewModel = mock<DashboardViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(
                    DashboardUiState(lowStockItems = mockLowStockItems)
                )
            )
        }
        val onNavigateToRoute = mock<(String) -> Unit>()

        composeTestRule.setContent {
            DashboardScreen(
                currentRoute = "dashboard",
                onNavigateToRoute = onNavigateToRoute,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Peringatan Stok Rendah").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 produk stok rendah").assertIsDisplayed()
    }

    @Test
    fun `dashboard screen displays recent transactions`() {
        val mockTransactions = listOf(
            com.chibychibystore.data.model.Penjualan(
                id = 1,
                tanggal = System.currentTimeMillis(),
                total = 15000.0,
                bayar = 15000.0,
                kembalian = 0.0,
                penggunaId = 1,
                items = emptyList()
            )
        )

        val viewModel = mock<DashboardViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(
                    DashboardUiState(recentTransactions = mockTransactions)
                )
            )
        }
        val onNavigateToRoute = mock<(String) -> Unit>()

        composeTestRule.setContent {
            DashboardScreen(
                currentRoute = "dashboard",
                onNavigateToRoute = onNavigateToRoute,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Transaksi Terbaru").assertIsDisplayed()
    }

    @Test
    fun `dashboard screen displays sales chart`() {
        val viewModel = mock<DashboardViewModel> {
            on { uiState }.thenReturn(
                androidx.compose.runtime.mutableStateOf(DashboardUiState())
            )
        }
        val onNavigateToRoute = mock<(String) -> Unit>()

        composeTestRule.setContent {
            DashboardScreen(
                currentRoute = "dashboard",
                onNavigateToRoute = onNavigateToRoute,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Trend Penjualan 7 Hari Terakhir").assertIsDisplayed()
    }
}