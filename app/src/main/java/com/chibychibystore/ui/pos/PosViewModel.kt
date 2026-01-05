package com.chibychibystore.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.SaleService
import com.chibychibystore.error.ChibyChibyException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * Data class for item in shopping cart POS
 */
data class CartItem(
    val product: Produk,
    val quantity: Int,
    val unitPrice: Double = product.sellingPrice,
    val totalPrice: Double = product.sellingPrice * quantity
) {
    init {
        require(quantity > 0) { "Quantity harus lebih dari 0" }
    }

    fun updateQuantity(newQuantity: Int): CartItem {
        return copy(
            quantity = newQuantity,
            totalPrice = unitPrice * newQuantity
        )
    }
}

/**
 * UI State for POS Screen
 */
data class PosUiState(
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Produk> = emptyList(),
    val isSearching: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val paymentMethod: String = "CASH",
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val error: String? = null,
    val successMessage: String? = null,
    val completedSaleId: Long? = null,
    val showReceiptDialog: Boolean = false,
    val isPrintingReceipt: Boolean = false
)

/**
 * ViewModel for Point of Sale (POS) Screen
 */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productService: ProductService,
    private val saleService: SaleService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<PosUiState> = run {
        val searchResultsFlow = _searchQuery
            .debounce(300L)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    productService.observeSearchProduks(query)
                        .catch { emit(emptyList()) }
                }
            }

        combine(
            _uiState,
            searchResultsFlow,
            _searchQuery
        ) { currentState, searchResults, query ->
            currentState.copy(
                searchResults = searchResults,
                searchQuery = query,
                isSearching = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PosUiState()
        )
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(isSearching = query.isNotBlank()) }
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val result = productService.getProductByBarcode(barcode)
            result.onSuccess { produk ->
                if (produk != null) {
                    addProductToCart(produk)
                    _uiState.update { it.copy(isSearching = false, successMessage = "Produk ditambahkan: ${produk.name}") }
                } else {
                    _uiState.update { it.copy(isSearching = false, error = "Produk dengan barcode $barcode tidak ditemukan") }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isSearching = false, error = "Gagal memindai: ${e.message}") }
            }
        }
    }

    fun addProductToCart(product: Produk, quantity: Int = 1) {
        _uiState.update { currentState ->
            val existingItem = currentState.cartItems.find { it.product.id == product.id }
            
            // Basic stock validation
            val currentQtyInCart = existingItem?.quantity ?: 0
            if (product.stockQuantity < currentQtyInCart + quantity) {
                return@update currentState.copy(error = "Stok tidak mencukupi untuk ${product.name}")
            }

            val updatedCartItems = if (existingItem != null) {
                currentState.cartItems.map { item ->
                    if (item.product.id == product.id) {
                        item.updateQuantity(item.quantity + quantity)
                    } else {
                        item
                    }
                }
            } else {
                currentState.cartItems + CartItem(product, quantity)
            }

            calculateNewState(currentState, updatedCartItems)
        }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(productId)
            return
        }

        _uiState.update { currentState ->
            val updatedCartItems = currentState.cartItems.map { item ->
                if (item.product.id == productId) {
                    // Re-validate stock
                    if (item.product.stockQuantity < newQuantity) {
                         return@update currentState.copy(error = "Stok tidak mencukupi untuk ${item.product.name}")
                    }
                    item.updateQuantity(newQuantity)
                } else {
                    item
                }
            }
            calculateNewState(currentState, updatedCartItems)
        }
    }

    fun removeCartItem(productId: Long) {
        _uiState.update { currentState ->
            val updatedCartItems = currentState.cartItems.filter { it.product.id != productId }
            calculateNewState(currentState, updatedCartItems)
        }
    }

    fun clearCart() {
        _uiState.update { currentState ->
            currentState.copy(
                cartItems = emptyList(),
                subtotal = 0.0,
                tax = 0.0,
                discount = 0.0,
                total = 0.0,
                error = null,
                successMessage = null
            )
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun processPayment() {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) {
            _uiState.update { it.copy(error = "Keranjang kosong") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = true, error = null) }
            
            val currentUser = authService.getCurrentUser()
            val userId = currentUser?.id ?: 0L

            val sale = Penjualan(
                saleDate = Date(),
                totalAmount = currentState.total,
                tax = currentState.tax,
                discount = currentState.discount,
                paymentMethod = try { PaymentMethod.valueOf(currentState.paymentMethod) } catch(e: Exception) { PaymentMethod.CASH },
                cashierId = userId
            )

            val items = currentState.cartItems.map { cartItem ->
                ItemPenjualan(
                    saleId = 0, // Set by service
                    productId = cartItem.product.id,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitPrice,
                    totalPrice = cartItem.totalPrice
                )
            }

            val result = saleService.createPenjualan(sale, items)
            result.onSuccess { completedSale ->
                _uiState.update { 
                    it.copy(
                        isProcessingPayment = false,
                        successMessage = "Pembayaran berhasil",
                        completedSaleId = completedSale.sale.id,
                        showReceiptDialog = true,
                        cartItems = emptyList(),
                        subtotal = 0.0,
                        tax = 0.0,
                        discount = 0.0,
                        total = 0.0
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isProcessingPayment = false, error = "Gagal memproses pembayaran: ${e.message}") }
            }
        }
    }

    private fun calculateNewState(currentState: PosUiState, items: List<CartItem>): PosUiState {
        val subtotal = items.sumOf { it.totalPrice }
        val tax = subtotal * 0.1 // 10% tax
        val total = subtotal + tax - currentState.discount
        return currentState.copy(
            cartItems = items,
            subtotal = subtotal,
            tax = tax,
            total = maxOf(0.0, total)
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun printReceipt() {
        val saleId = _uiState.value.completedSaleId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingReceipt = true) }
            val result = saleService.cetakStruk(saleId)
            _uiState.update { 
                it.copy(
                    isPrintingReceipt = false,
                    successMessage = if (result.isSuccess) "Struk sedang dicetak" else "Gagal mencetak struk"
                )
            }
        }
    }

    fun startNewTransaction() {
        _uiState.update { 
            it.copy(
                completedSaleId = null,
                showReceiptDialog = false
            )
        }
        clearCart()
    }

    fun dismissReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false) }
    }
}
