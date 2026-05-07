package com.chibychibystore.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class PurchaseUiState(
    val purchases: List<Pembelian> = emptyList(),
    val suppliers: List<Pemasok> = emptyList(),
    val products: List<Produk> = emptyList(),
    val warehouses: List<Gudang> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class PurchaseItemInput(
    val product: Produk,
    var quantity: Int = 1,
    var costPrice: Double = product.costPrice
)

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val purchaseService: PurchaseService,
    private val supplierService: SupplierService,
    private val productService: ProductService,
    private val warehouseService: WarehouseService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    private val _purchaseItems = MutableStateFlow<List<PurchaseItemInput>>(emptyList())
    val purchaseItems: StateFlow<List<PurchaseItemInput>> = _purchaseItems.asStateFlow()

    init {
        loadPurchases()
        loadSuppliers()
        loadProducts()
        loadWarehouses()
    }

    private fun loadPurchases() {
        viewModelScope.launch {
            purchaseService.observeAllPurchases()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, purchases = list) }
                }
        }
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            supplierService.ambilSemuaPemasok()
                .collect { list ->
                    _uiState.update { it.copy(suppliers = list) }
                }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productService.observeProduks()
                .collect { list ->
                    _uiState.update { it.copy(products = list) }
                }
        }
    }

    private fun loadWarehouses() {
        viewModelScope.launch {
            warehouseService.observeGudangs()
                .collect { list ->
                    _uiState.update { it.copy(warehouses = list) }
                }
        }
    }

    fun addItem(product: Produk) {
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = current[index].quantity + 1)
        } else {
            current.add(PurchaseItemInput(product))
        }
        _purchaseItems.value = current
    }

    fun removeItem(productId: Long) {
        _purchaseItems.value = _purchaseItems.value.filter { it.product.id != productId }
    }

    fun updateItemQuantity(productId: Long, quantity: Int) {
        val current = _purchaseItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = quantity)
            _purchaseItems.value = current
        }
    }

    fun savePurchase(pemasokId: Long, warehouseId: Long, invoiceNumber: String, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val buyer = authService.getCurrentUser()
            if (buyer == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            val totalAmount = _purchaseItems.value.sumOf { it.quantity * it.costPrice }
            val pembelian = Pembelian(
                supplierId = pemasokId,
                warehouseId = warehouseId,
                receivedBy = buyer.id,
                invoiceNumber = invoiceNumber,
                purchaseDate = Date(),
                totalAmount = totalAmount,
                notes = notes
            )

            val items = _purchaseItems.value.map { input ->
                ItemPembelian(
                    purchaseId = 0,
                    productId = input.product.id,
                    quantity = input.quantity,
                    unitPrice = input.costPrice,
                    totalPrice = input.quantity * input.costPrice
                )
            }

            val result = purchaseService.createPembelian(pembelian, items)
            if (result is Result.Success) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _purchaseItems.value = emptyList()
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
