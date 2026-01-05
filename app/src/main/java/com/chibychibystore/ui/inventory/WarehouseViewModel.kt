package com.chibychibystore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.WarehouseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Warehouse Screen
 */
data class WarehouseUiState(
    val warehouses: List<Gudang> = emptyList(),
    val selectedWarehouse: Gudang? = null,
    val products: List<Produk> = emptyList(),
    val isLoading: Boolean = false,
    val isTransferring: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val allWarehouseStock: Map<Gudang, List<Produk>> = emptyMap()
)

/**
 * ViewModel untuk Warehouse Screen
 * Mengelola gudang dan transfer stok antar gudang dengan reactive architecture.
 */
@HiltViewModel
class WarehouseViewModel @Inject constructor(
    private val warehouseService: WarehouseService
) : ViewModel() {

    // Triggers and Local State
    private val _selectedWarehouseId = MutableStateFlow<Long?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isTransferring = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    // Warehouses Stream
    private val _warehousesFlow = warehouseService.observeGudangs()
        .catch { e ->
            _error.update { "Gagal memuat gudang: ${e.message}" }
            emit(emptyList())
        }

    // Warehouse Stock Stream (Reactive based on selection)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _warehouseStockFlow = _selectedWarehouseId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            warehouseService.observeStokGudang(id)
        }
    }.catch { e ->
        _error.update { "Gagal memuat stok gudang: ${e.message}" }
        emit(emptyList())
    }

    // Combined UI State
    val uiState: StateFlow<WarehouseUiState> = combine(
        _warehousesFlow,
        _warehouseStockFlow,
        _selectedWarehouseId,
        _isLoading,
        _isTransferring,
        _error,
        _successMessage
    ) { values ->
        val warehouses = values[0] as List<Gudang>
        val products = values[1] as List<Produk>
        val selectedId = values[2] as Long?
        val isLoading = values[3] as Boolean
        val isTransferring = values[4] as Boolean
        val error = values[5] as String?
        val success = values[6] as String?
        
        WarehouseUiState(
            warehouses = warehouses,
            selectedWarehouse = warehouses.find { it.id == selectedId },
            products = products,
            isLoading = isLoading,
            isTransferring = isTransferring,
            error = error,
            successMessage = success
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WarehouseUiState(isLoading = true)
    )

    /**
     * Pilih gudang untuk melihat stok
     */
    fun selectWarehouse(gudang: Gudang) {
        _selectedWarehouseId.update { gudang.id }
        clearMessages()
    }

    /**
     * Transfer stok antar gudang
     */
    fun transferStock(
        productId: Long,
        fromWarehouseId: Long,
        toWarehouseId: Long,
        quantity: Int
    ) {
        viewModelScope.launch {
            _isTransferring.update { true }
            clearMessages()
            try {
                val result = warehouseService.transferStok(productId, fromWarehouseId, toWarehouseId, quantity)
                result.onSuccess {
                    _successMessage.update { "Transfer stok berhasil" }
                }.onFailure { e ->
                    _error.update { e.message ?: "Gagal transfer stok" }
                }
            } catch (e: Exception) {
                _error.update { "Terjadi kesalahan: ${e.message}" }
            } finally {
                _isTransferring.update { false }
            }
        }
    }

    /**
     * Assign product ke gudang
     */
    fun assignProductToWarehouse(productId: Long, warehouseId: Long) {
        viewModelScope.launch {
            _isLoading.update { true }
            clearMessages()
            try {
                val result = warehouseService.tugaskanProdukKeGudang(productId, warehouseId)
                result.onSuccess {
                    _successMessage.update { "Produk berhasil dipindahkan ke gudang" }
                }.onFailure { e ->
                    _error.update { e.message ?: "Gagal memindahkan produk" }
                }
            } catch (e: Exception) {
                _error.update { "Terjadi kesalahan: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Create new warehouse
     */
    fun createWarehouse(name: String, location: String, capacity: Int) {
        viewModelScope.launch {
            _isLoading.update { true }
            clearMessages()
            try {
                val gudang = Gudang(
                    id = 0,
                    name = name,
                    location = location,
                    createdAt = java.util.Date()
                )
                val result = warehouseService.createGudang(gudang)
                result.onSuccess { created ->
                    _successMessage.update { "Gudang '${created.name}' berhasil dibuat" }
                }.onFailure { e ->
                    _error.update { e.message ?: "Gagal membuat gudang" }
                }
            } catch (e: Exception) {
                _error.update { "Terjadi kesalahan: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Update existing warehouse
     */
    fun updateWarehouse(warehouseId: Long, name: String, location: String, capacity: Int) {
        viewModelScope.launch {
            _isLoading.update { true }
            clearMessages()
            try {
                val gudang = Gudang(
                    id = warehouseId,
                    name = name,
                    location = location,
                    createdAt = java.util.Date()
                )
                val result = warehouseService.updateGudang(gudang)
                result.onSuccess { updated ->
                    _successMessage.update { "Gudang '${updated.name}' berhasil diperbarui" }
                }.onFailure { e ->
                    _error.update { e.message ?: "Gagal memperbarui gudang" }
                }
            } catch (e: Exception) {
                _error.update { "Terjadi kesalahan: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearError() {
        _error.update { null }
    }

    fun clearSuccessMessage() {
        _successMessage.update { null }
    }

    private fun clearMessages() {
        _error.update { null }
        _successMessage.update { null }
    }

    /**
     * Refresh data - mainly for error clearing or if service pulled from network
     */
    fun refresh() {
        clearMessages()
    }

    // Legacy accessors
    fun getWarehouseById(warehouseId: Long): Gudang? {
        return uiState.value.warehouses.find { it.id == warehouseId }
    }

    fun getProductById(productId: Long): Produk? {
        return uiState.value.products.find { it.id == productId }
    }
}
