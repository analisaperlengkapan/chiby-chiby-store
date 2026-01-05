package com.chibychibystore.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.SaleWithItems
import com.chibychibystore.service.SaleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI State untuk Sales History Screen
 */
data class SalesHistoryUiState(
    val sales: List<Sale> = emptyList(),
    val selectedSale: SaleWithItems? = null,
    val isLoading: Boolean = false,
    val isLoadingReceipt: Boolean = false,
    val isPrintingReceipt: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val showReceiptDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val datePickerType: DatePickerType = DatePickerType.START
)

enum class DatePickerType {
    START, END
}

/**
 * ViewModel untuk Sales History Screen
 */
@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val saleService: SaleService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
    val uiState: StateFlow<SalesHistoryUiState> = _uiState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadSales()
    }

    /**
     * Load semua penjualan dengan filter tanggal dan pencarian
     */
    fun loadSales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentState = _uiState.value
                val result = saleService.getSales(
                    startDate = currentState.startDate?.let { dateFormat.format(it) },
                    endDate = currentState.endDate?.let { dateFormat.format(it) },
                    query = currentState.searchQuery.takeIf { it.isNotBlank() }
                )

                result.onSuccess { sales ->
                    _uiState.update {
                        it.copy(
                            sales = sales,
                            isLoading = false
                        )
                    }
                    // Setup reactive updates
                    observeSales()
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Gagal memuat data penjualan"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Observe perubahan penjualan secara real-time dalam date range
     */
    private fun observeSales() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val startDateStr = currentState.startDate?.let { dateFormat.format(it) } ?: "1900-01-01"
            val endDateStr = currentState.endDate?.let { dateFormat.format(it) } ?: "2100-12-31"
            val query = currentState.searchQuery.takeIf { it.isNotBlank() }

            saleService.observeSalesFiltered(startDateStr, endDateStr, query)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = "Gagal mengamati perubahan penjualan: ${e.message}"
                        )
                    }
                }
                .collectLatest { sales ->
                    _uiState.update { it.copy(sales = sales) }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadSales()
    }

    fun setStartDate(date: Date?) {
        _uiState.update { it.copy(startDate = date) }
        loadSales()
    }

    fun setEndDate(date: Date?) {
        _uiState.update { it.copy(endDate = date) }
        loadSales()
    }

    fun showDatePicker(type: DatePickerType) {
        _uiState.update {
            it.copy(
                showDatePicker = true,
                datePickerType = type
            )
        }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun loadReceipt(saleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReceipt = true, error = null) }

            try {
                val result = saleService.getSale(saleId)
                result.onSuccess { saleWithItems ->
                    _uiState.update {
                        it.copy(
                            selectedSale = saleWithItems,
                            showReceiptDialog = true,
                            isLoadingReceipt = false
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoadingReceipt = false,
                            error = exception.message ?: "Gagal memuat struk"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingReceipt = false,
                        error = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    fun hideReceiptDialog() {
        _uiState.update {
            it.copy(
                showReceiptDialog = false,
                selectedSale = null
            )
        }
    }

    fun printReceipt() {
        val saleId = _uiState.value.selectedSale?.sale?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingReceipt = true, error = null) }

            try {
                val result = saleService.printReceipt(saleId)
                result.onSuccess {
                    _uiState.update { it.copy(isPrintingReceipt = false) }
                    // Could show success message here
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isPrintingReceipt = false,
                            error = exception.message ?: "Gagal mencetak struk"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPrintingReceipt = false,
                        error = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearFilters() {
        _uiState.update { SalesHistoryUiState() }
        loadSales()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
