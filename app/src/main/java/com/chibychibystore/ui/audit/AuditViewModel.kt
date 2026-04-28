package com.chibychibystore.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.AuditStatus
import com.chibychibystore.data.local.entity.ItemStokOpname
import com.chibychibystore.data.local.entity.StokOpname
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.InventoryAuditService
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.WarehouseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AuditUiState(
    val audits: List<StokOpname> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val warehouses: List<Gudang> = emptyList(),
    val products: List<Produk> = emptyList()
)

data class AuditItemInput(
    val product: Produk,
    val expectedQuantity: Int,
    var actualQuantity: Int = expectedQuantity,
    var reason: String? = null
)

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val auditService: InventoryAuditService,
    private val productService: ProductService,
    private val warehouseService: WarehouseService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    private val _auditItems = MutableStateFlow<List<AuditItemInput>>(emptyList())
    val auditItems: StateFlow<List<AuditItemInput>> = _auditItems.asStateFlow()

    init {
        loadAudits()
        loadWarehouses()
        loadProducts()
    }

    private fun loadAudits() {
        viewModelScope.launch {
            auditService.observeAllAudits()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, audits = list) }
                }
        }
    }

    private fun loadWarehouses() {
        viewModelScope.launch {
            warehouseService.observeGudangs()
                .collect { list -> _uiState.update { it.copy(warehouses = list) } }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productService.observeProducts()
                .collect { list -> _uiState.update { it.copy(products = list) } }
        }
    }

    fun startNewAudit(warehouseId: Long) {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) }
             // In a real app, we'd only load products belonging to this warehouse
             val productsInWarehouse = _uiState.value.products // Simplified
             val items = productsInWarehouse.map { product ->
                 AuditItemInput(
                     product = product,
                     expectedQuantity = product.stockQuantity
                 )
             }
             _auditItems.value = items
             _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateActualQuantity(productId: Long, actual: Int) {
        val current = _auditItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(actualQuantity = actual)
            _auditItems.value = current
        }
    }

    fun saveAudit(warehouseId: Long, notes: String?, status: AuditStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val auditor = authService.getCurrentUser()
            if (auditor == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            val audit = StokOpname(
                warehouseId = warehouseId,
                auditorId = auditor.id,
                notes = notes,
                status = status,
                auditDate = Date()
            )

            val items = _auditItems.value.map { input ->
                ItemStokOpname(
                    auditId = 0,
                    productId = input.product.id,
                    expectedQuantity = input.expectedQuantity,
                    actualQuantity = input.actualQuantity,
                    difference = input.actualQuantity - input.expectedQuantity,
                    reason = input.reason
                )
            }

            val result = auditService.createAudit(audit, items)
            if (result is Result.Success) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = (result as Result.Failure).exception.message) }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
