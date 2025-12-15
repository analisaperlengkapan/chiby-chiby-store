package com.chibychibystore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.WarehouseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
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
 * Mengelola gudang dan transfer stok antar gudang
 */
@HiltViewModel
class WarehouseViewModel @Inject constructor(
    private val warehouseService: WarehouseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehouseUiState())
    val uiState: StateFlow<WarehouseUiState> = _uiState

    init {
        loadWarehouses()
    }

    /**
     * Load semua gudang
     */
    fun loadWarehouses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = warehouseService.getWarehouses()
                result.onSuccess { warehouses ->
                    _uiState.value = _uiState.value.copy(
                        warehouses = warehouses,
                        isLoading = false
                    )
                    // Setup reactive updates
                    observeWarehouses()

                    // Load stock untuk semua gudang
                    loadAllWarehouseStock()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Gagal memuat gudang"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Observe perubahan gudang secara real-time
     */
    private fun observeWarehouses() {
        viewModelScope.launch {
            warehouseService.observeWarehouses()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Gagal mengamati perubahan gudang: ${e.message}"
                    )
                }
                .collectLatest { warehouses ->
                    _uiState.value = _uiState.value.copy(warehouses = warehouses)
                }
        }
    }

    /**
     * Pilih gudang untuk melihat stok
     */
    fun selectWarehouse(warehouse: Gudang) {
        _uiState.value = _uiState.value.copy(
            selectedWarehouse = warehouse,
            error = null,
            successMessage = null
        )
        loadWarehouseStock(warehouse.id)
    }

    /**
     * Load stok untuk gudang tertentu
     */
    private fun loadWarehouseStock(warehouseId: Long) {
        viewModelScope.launch {
            try {
                val result = warehouseService.getWarehouseStock(warehouseId)
                result.onSuccess { products ->
                    _uiState.value = _uiState.value.copy(products = products)
                    // Setup reactive updates untuk gudang ini
                    observeWarehouseStock(warehouseId)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Gagal memuat stok gudang"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Observe perubahan stok gudang secara real-time
     */
    private fun observeWarehouseStock(warehouseId: Long) {
        viewModelScope.launch {
            warehouseService.observeWarehouseStock(warehouseId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Gagal mengamati perubahan stockQuantity: ${e.message}"
                    )
                }
                .collectLatest { products ->
                    _uiState.value = _uiState.value.copy(products = products)
                }
        }
    }

    /**
     * Load stok untuk semua gudang
     */
    private fun loadAllWarehouseStock() {
        viewModelScope.launch {
            try {
                val result = warehouseService.getAllWarehouseStock()
                result.onSuccess { warehouseStock ->
                    _uiState.value = _uiState.value.copy(allWarehouseStock = warehouseStock)
                }.onFailure { /* Ignore error untuk all warehouse stock */ }
            } catch (e: Exception) {
                // Ignore error untuk all warehouse stock
            }
        }
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
            _uiState.value = _uiState.value.copy(isTransferring = true, error = null, successMessage = null)

            try {
                val result = warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isTransferring = false,
                        successMessage = "Transfer stok berhasil"
                    )
                    // Refresh data
                    loadAllWarehouseStock()
                    _uiState.value.selectedWarehouse?.let { selected ->
                        loadWarehouseStock(selected.id)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isTransferring = false,
                        error = exception.message ?: "Gagal transfer stok"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTransferring = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Assign produk ke gudang
     */
    fun assignProductToWarehouse(productId: Long, warehouseId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            try {
                val result = warehouseService.assignProductToWarehouse(productId, warehouseId)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Produk berhasil dipindahkan ke gudang"
                    )
                    // Refresh data
                    loadAllWarehouseStock()
                    _uiState.value.selectedWarehouse?.let { selected ->
                        loadWarehouseStock(selected.id)
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Gagal memindahkan produk"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Create new warehouse
     */
    fun createWarehouse(name: String, location: String, capacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            try {
                val warehouse = Gudang(
                    id = 0, // Will be generated by service
                    name = name,
                    location = location,
                    capacity = capacity
                )

                val result = warehouseService.createWarehouse(warehouse)
                result.onSuccess { createdWarehouse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Gudang '${createdWarehouse.name}' berhasil dibuat"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Gagal membuat gudang"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Update existing warehouse
     */
    fun updateWarehouse(warehouseId: Long, name: String, location: String, capacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            try {
                val warehouse = Gudang(
                    id = warehouseId,
                    name = name,
                    location = location,
                    capacity = capacity
                )

                val result = warehouseService.updateWarehouse(warehouse)
                result.onSuccess { updatedWarehouse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Gudang '${updatedWarehouse.name}' berhasil diperbarui"
                    )
                    // Refresh warehouse list
                    loadWarehouses()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Gagal memperbarui gudang"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadWarehouses()
    }

    /**
     * Get gudang berdasarkan ID
     */
    fun getWarehouseById(warehouseId: Long): Gudang? {
        return _uiState.value.warehouses.find { it.id == warehouseId }
    }

    /**
     * Get produk berdasarkan ID dari gudang yang dipilih
     */
    fun getProductById(productId: Long): Produk? {
        return _uiState.value.products.find { it.id == productId }
    }
}