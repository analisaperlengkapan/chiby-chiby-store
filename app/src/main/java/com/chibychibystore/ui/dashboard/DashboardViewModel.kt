package com.chibychibystore.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.DataTren
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.ReportingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI State untuk Dashboard Screen
 */
data class DashboardUiState(
    val todaySales: Double = 0.0,
    val todayTransactionCount: Int = 0,
    val lowStockItems: List<Produk> = emptyList(),
    val recentTransactions: List<Penjualan> = emptyList(),
    val salesTrend: List<DataTren> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel untuk Dashboard Screen
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val saleService: SaleService,
    private val reportingService: ReportingService,
    private val produkRepository: ProdukRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboardData()
    }

    /**
     * Memuat semua data dashboard secara asynchronous
     */
    private fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val todayDate = LocalDate.now()
                val today = todayDate.toString() // yyyy-MM-dd
                val sevenDaysAgo = todayDate.minusDays(6)

                // Parallel execution for dashboard metrics
                // Note: Using a properly formatted date string for services expected "yyyy-MM-dd"
                // Assuming services handle "yyyy-MM-dd" correctly.
                
                val todaySalesDeferred = async { saleService.getTotalPenjualanByRentangTanggal(today, today) }
                val transactionCountDeferred = async { saleService.getPenjualanCountByRentangTanggal(today, today) }
                val lowStockDeferred = async { produkRepository.getLowStockProduk().first() }
                val recentSalesDeferred = async { saleService.getRecentPenjualan(10) }
                val salesTrendDeferred = async { reportingService.getSalesTrend(sevenDaysAgo, todayDate) }

                // Await results
                val todaySalesRes = todaySalesDeferred.await()
                val transactionCountRes = transactionCountDeferred.await()
                val lowStock = lowStockDeferred.await()
                val recentSalesRes = recentSalesDeferred.await()
                val salesTrendRes = salesTrendDeferred.await()

                _uiState.value = _uiState.value.copy(
                    todaySales = todaySalesRes.getOrNull() ?: 0.0,
                    todayTransactionCount = transactionCountRes.getOrNull() ?: 0,
                    lowStockItems = lowStock,
                    recentTransactions = recentSalesRes.getOrNull() ?: emptyList(),
                    salesTrend = salesTrendRes.getOrNull() ?: emptyList(),
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat data dashboard: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}