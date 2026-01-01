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
    val allWarehouseStock: Map<Gudang, List<Produk>> = emptyMap() // Keep for now if used by views, though redundant with reactive selection
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
    private val _warehousesFlow = warehouseService.observeWarehouses()
        .catch { e ->
            _error.value = "Gagal memuat gudang: ${e.message}"
            emit(emptyList())
        }

    // Warehouse Stock Stream (Reactive based on selection)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _warehouseStockFlow = _selectedWarehouseId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            warehouseService.observeWarehouseStock(id)
        }
    }.catch { e ->
        _error.value = "Gagal memuat stok gudang: ${e.message}"
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
    ) { warehouses, products, selectedId, isLoading, isTransferring, error, success ->
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
    fun selectWarehouse(warehouse: Gudang) {
        _selectedWarehouseId.value = warehouse.id
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
            _isTransferring.value = true
            clearMessages()
            try {
                val result = warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)
                result.onSuccess {
                    _successMessage.value = "Transfer stok berhasil"
                }.onFailure { e ->
                    _error.value = e.message ?: "Gagal transfer stok"
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isTransferring.value = false
            }
        }
    }

    /**
     * Assign produk ke gudang
     */
    fun assignProductToWarehouse(productId: Long, warehouseId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val result = warehouseService.assignProductToWarehouse(productId, warehouseId)
                result.onSuccess {
                    _successMessage.value = "Produk berhasil dipindahkan ke gudang"
                }.onFailure { e ->
                    _error.value = e.message ?: "Gagal memindahkan produk"
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create new warehouse
     */
    fun createWarehouse(name: String, location: String, capacity: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val warehouse = Gudang(
                    id = 0,
                    name = name,
                    location = location,
                    capacity = capacity,
                    createdAt = java.util.Date()
                )
                val result = warehouseService.createWarehouse(warehouse)
                result.onSuccess { created ->
                    _successMessage.value = "Gudang '${created.name}' berhasil dibuat"
                    // No manual reload needed, flow updates automatically
                }.onFailure { e ->
                    _error.value = e.message ?: "Gagal membuat gudang"
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update existing warehouse
     */
    fun updateWarehouse(warehouseId: Long, name: String, location: String, capacity: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val warehouse = Gudang(
                    id = warehouseId,
                    name = name,
                    location = location,
                    capacity = capacity,
                    createdAt = java.util.Date()
                )
                val result = warehouseService.updateWarehouse(warehouse)
                result.onSuccess { updated ->
                    _successMessage.value = "Gudang '${updated.name}' berhasil diperbarui"
                }.onFailure { e ->
                    _error.value = e.message ?: "Gagal memperbarui gudang"
                }
            } catch (e: Exception) {
                _error.value = "Terjadi kesalahan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    /**
     * Refresh data - mainly for error clearing or if service pulled from network
     */
    fun refresh() {
        clearMessages()
        // Flows restart automatically if needed due to sharing strategy,
        // or we could force a reload in a network scenario.
        // For local DB, this is mostly a no-op or error clear.
    }

    // Legacy accessors (can be removed if not used by view directly, but safe to keep)
    fun getWarehouseById(warehouseId: Long): Gudang? {
        return uiState.value.warehouses.find { it.id == warehouseId }
    }

    fun getProductById(productId: Long): Produk? {
        return uiState.value.products.find { it.id == productId }
    }
}
