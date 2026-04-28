package com.chibychibystore.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.PurchaseService
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.SupplierService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class PurchaseUiState(
    val purchases: List<Pembelian> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val suppliers: List<Pemasok> = emptyList(),
    val products: List<Produk> = emptyList()
)

data class CartItemPurchase(
    val product: Produk,
    var quantity: Int,
    var unitPrice: Double
)

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val purchaseService: PurchaseService,
    private val productService: ProductService,
    private val supplierService: SupplierService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItemPurchase>>(emptyList())
    val cart: StateFlow<List<CartItemPurchase>> = _cart.asStateFlow()

    init {
        loadPurchases()
        loadSuppliers()
        loadProducts()
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
            productService.observeProducts()
                .collect { list ->
                    _uiState.update { it.copy(products = list) }
                }
        }
    }

    fun addToCart(product: Produk, quantity: Int, unitPrice: Double) {
        val currentCart = _cart.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val item = currentCart[existingIndex]
            currentCart[existingIndex] = item.copy(quantity = item.quantity + quantity)
        } else {
            currentCart.add(CartItemPurchase(product, quantity, unitPrice))
        }
        _cart.value = currentCart
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun createPurchase(supplierId: Long, invoiceNumber: String, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val totalAmount = _cart.value.sumOf { it.quantity * it.unitPrice }
            val pembelian = Pembelian(
                supplierId = supplierId,
                invoiceNumber = invoiceNumber,
                totalAmount = totalAmount,
                notes = notes,
                purchaseDate = Date()
            )
            val items = _cart.value.map {
                ItemPembelian(
                    purchaseId = 0, // Will be set in service
                    productId = it.product.id,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    totalPrice = it.quantity * it.unitPrice
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
