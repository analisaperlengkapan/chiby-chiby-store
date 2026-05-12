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

data class CartItemPurchase(
    val product: Produk,
    val quantity: Int,
    val unitPrice: Double
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

    private val _cart = MutableStateFlow<List<CartItemPurchase>>(emptyList())
    val cart: StateFlow<List<CartItemPurchase>> = _cart.asStateFlow()

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

    fun addToCart(product: Produk, quantity: Int, unitPrice: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(
                quantity = current[index].quantity + quantity,
                unitPrice = unitPrice
            )
        } else {
            current.add(CartItemPurchase(product, quantity, unitPrice))
        }
        _cart.value = current
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun createPurchase(pemasokId: Long, warehouseId: Long, invoiceNumber: String, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val buyer = authService.getCurrentUser()
            if (buyer == null) {
                _uiState.update { it.copy(isLoading = false, error = "Login required") }
                return@launch
            }

            val totalAmount = _cart.value.sumOf { it.quantity * it.unitPrice }
            val pembelian = Pembelian(
                supplierId = pemasokId,
                warehouseId = warehouseId,
                receivedBy = buyer.id,
                invoiceNumber = invoiceNumber,
                purchaseDate = Date(),
                totalAmount = totalAmount,
                notes = notes
            )

            val items = _cart.value.map { input ->
                ItemPembelian(
                    purchaseId = 0,
                    productId = input.product.id,
                    quantity = input.quantity,
                    unitPrice = input.unitPrice,
                    totalPrice = input.quantity * input.unitPrice
                )
            }

            val result = purchaseService.createPembelian(pembelian, items)
            if (result is Result.Success) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _cart.value = emptyList()
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
