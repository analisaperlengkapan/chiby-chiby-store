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
import kotlinx.coroutines.supervisorScope
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
        observeMetrics()
    }

    private fun observeMetrics() {
        viewModelScope.launch {
            reportingService.observeSalesMetrics().collect { metrics ->
                _uiState.value = _uiState.value.copy(
                    todaySales = metrics.penjualanHariIni,
                    todayTransactionCount = metrics.transaksiHariIni
                )
            }
        }
    }

    /**
     * Memuat semua data dashboard secara asynchronous
     */
    private fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val todayDate = LocalDate.now()
                val sevenDaysAgo = todayDate.minusDays(6)

                // Parallel execution for dashboard metrics
                supervisorScope {
                    val lowStockDeferred = async { produkRepository.getLowStockProduk().first() }
                    val recentSalesDeferred = async { saleService.getRecentPenjualan(10) }
                    val salesTrendDeferred = async { reportingService.getSalesTrend(sevenDaysAgo, todayDate) }
                    // Also fetch the first metrics value alongside the initial
                    // load so todaySales/todayTransactionCount are populated by
                    // the time isLoading flips to false. Without this, there's
                    // a brief window where the dashboard shows isLoading=false
                    // with metrics still at 0 until observeMetrics() emits its
                    // first value.
                    val initialMetricsDeferred = async {
                        runCatching { reportingService.observeSalesMetrics().first() }.getOrNull()
                    }

                    // Await results
                    val lowStock = lowStockDeferred.await()
                    val recentSalesRes = recentSalesDeferred.await()
                    val salesTrendRes = salesTrendDeferred.await()
                    val initialMetrics = initialMetricsDeferred.await()

                    _uiState.value = _uiState.value.copy(
                        lowStockItems = lowStock,
                        recentTransactions = recentSalesRes.getOrNull() ?: emptyList(),
                        salesTrend = salesTrendRes.getOrNull() ?: emptyList(),
                        todaySales = initialMetrics?.penjualanHariIni ?: _uiState.value.todaySales,
                        todayTransactionCount = initialMetrics?.transaksiHariIni ?: _uiState.value.todayTransactionCount,
                        isLoading = false,
                        errorMessage = null
                    )
                }

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