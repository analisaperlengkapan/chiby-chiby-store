package com.chibychibystore.ui.pos

import androidx.lifecycle.viewModelScope
<<<<<<< HEAD
import com.chibychibystore.constant.AppConstants
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.PaymentMethod
=======
import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.Product
>>>>>>> feat/ui-overhaul
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.PromoService
import com.chibychibystore.service.SaleService
<<<<<<< HEAD
import com.chibychibystore.ui.base.BaseViewModel
import com.chibychibystore.ui.base.UiState
=======
>>>>>>> feat/ui-overhaul
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
<<<<<<< HEAD
 * Data class for item in shopping cart POS
=======
 * Data class untuk item dalam shopping cart POS
 *
 * **Business Purpose:**
 * - Representasi product yang dipilih customer dalam transaksi
 * - Menyimpan quantity dan harga untuk perhitungan total
 * - Immutable data structure untuk thread safety
 * - Support untuk quantity updates tanpa side effects
 *
 * **Business Rules:**
 * - Quantity harus > 0 (tidak boleh 0 atau negatif)
 * - Unit price menggunakan product selling price sebagai default
 * - Total price = unitPrice * quantity (otomatis calculated)
 * - Product reference immutable untuk data consistency
 *
 * **Usage Context:**
 * - Created ketika customer menambah product ke cart
 * - Updated ketika quantity diubah via UI
 * - Used untuk calculate cart totals dan create sale items
 * - Passed ke SaleService untuk transaction processing
 *
 * **Thread Safety:**
 * - Immutable properties (val) untuk thread safety
 * - updateQuantity() returns new instance (functional update)
 * - No mutable state yang bisa cause race conditions
 *
 * **Integration Points:**
 * - UI: Display dalam cart list dengan quantity controls
 * - SaleService: Converted ke SaleItem untuk database storage
 * - Receipt printing: Used untuk generate receipt items
 *
 * @property product Product yang dipilih customer (immutable reference)
 * @property quantity Jumlah product dalam cart (harus > 0)
 * @property unitPrice Harga per unit (default: product.sellingPrice)
 * @property totalPrice Total harga untuk quantity ini (calculated: unitPrice * quantity)
 *
 * @constructor Create cart item dengan product dan quantity
 * @param product Product yang akan ditambahkan ke cart
 * @param quantity Jumlah product (default: 1)
 * @param unitPrice Harga per unit (default: product.sellingPrice)
 * @param totalPrice Total harga (auto-calculated jika tidak disediakan)
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see Product
 * @see SaleItem
 * @see PosViewModel
>>>>>>> feat/ui-overhaul
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
) : UiState

/**
 * ViewModel for Point of Sale (POS) Screen
 */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productService: ProductService,
    private val saleService: SaleService,
    private val authService: AuthService,
    private val promoService: PromoService
) : BaseViewModel<PosUiState>(PosUiState()) {

    private val _searchQuery = MutableStateFlow("")

    init {
        setupSearch()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun setupSearch() {
        val searchResultsFlow = _searchQuery
            .debounce(300L)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
<<<<<<< HEAD
                    productService.observeSearchProduks(query)
                        .catch { emit(emptyList()) }
=======
                    productService.observeSearchProducts(query)
                        .catch { e ->
                            emit(emptyList()) // Emit empty on error to keep stream alive
                        }
>>>>>>> feat/ui-overhaul
                }
            }

        // We launch a collection of search results to update the state
        viewModelScope.launch {
            searchResultsFlow.collect { results ->
                updateState {
                    it.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        updateState { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

<<<<<<< HEAD
    fun onBarcodeScanned(barcode: String) {
        launchWithState {
            updateState { it.copy(isSearching = true) }
            val result = productService.getProductByBarcode(barcode)
            result.onSuccess { produk ->
                if (produk != null) {
                    addProductToCart(produk)
                    updateState { it.copy(isSearching = false, successMessage = "Produk ditambahkan: ${produk.name}") }
                } else {
                    updateState { it.copy(isSearching = false, error = "Produk dengan barcode $barcode tidak ditemukan") }
=======
    /**
     * Tambah product ke shopping cart dengan quantity handling
     *
     * **Business Logic Flow:**
     * 1. **Duplicate Check**: Cek jika product sudah ada di cart
     * 2. **Quantity Update**: Jika sudah ada, tambah quantity
     * 3. **New Item Addition**: Jika belum ada, buat CartItem baru
     * 4. **Totals Recalculation**: Update subtotal, tax, discount, total
     * 5. **State Update**: Emit new state untuk UI refresh
     *
     * **Cart Management Rules:**
     * - Existing items: Quantity accumulation (1 + 1 = 2)
     * - New items: Added dengan specified quantity
     * - Stock validation: Tidak dilakukan di level cart (di payment time)
     * - Price consistency: Menggunakan product.sellingPrice
     *
     * **State Management:**
     * - Immutable cart updates (functional programming)
     * - Automatic totals calculation
     * - Reactive UI updates melalui StateFlow
     * - Error clearing untuk user feedback
     *
     * **Business Constraints:**
     * - Quantity harus > 0 (validated di CartItem constructor)
     * - Product harus valid (dari search results)
     * - No stock checking at cart level (optimistic approach)
     * - Real-time totals untuk user feedback
     *
     * **Performance Considerations:**
     * - O(n) cart search untuk existing items
     * - Efficient list operations dengan immutable updates
     * - Minimal state emissions untuk UI efficiency
     * - Memory efficient untuk large carts
     *
     * **UI Integration:**
     * - Triggers cart list updates
     * - Updates totals display
     * - Enables checkout button
     * - Provides quantity feedback
     *
     * **Usage Example:**
     * ```kotlin
     * // Add single product
     * viewModel.addProductToCart(selectedProduct)
     *
     * // Add multiple quantity
     * viewModel.addProductToCart(selectedProduct, quantity = 3)
     *
     * // From barcode scan
     * viewModel.addProductToCart(scannedProduct, quantity = 1)
     * ```
     *
     * @param product Product yang akan ditambahkan ke cart
     * @param quantity Jumlah product yang akan ditambahkan (default: 1)
     *
     * @throws IllegalArgumentException jika quantity <= 0 (via CartItem validation)
     *
     * @see CartItem
     * @see updateCartTotals
     * @see PosUiState.cartItems
     */
    fun addProductToCart(product: Product, quantity: Int = 1) {
        _uiState.update { currentState ->
            // Check if product already in cart
            val existingItem = currentState.cartItems.find { it.product.id == product.id }

            // Stock validation
            val currentQuantity = existingItem?.quantity ?: 0
            if (!isStockSufficient(product, currentQuantity + quantity)) {
                 return@update currentState.copy(error = "Stok tidak mencukupi. Sisa: ${product.stockQuantity}")
            }

            val updatedCartItems = if (existingItem != null) {
                // Update quantity
                val newQuantity = existingItem.quantity + quantity
                currentState.cartItems.map { item ->
                    if (item.product.id == product.id) {
                        item.updateQuantity(newQuantity)
                    } else {
                        item
                    }
>>>>>>> feat/ui-overhaul
                }
            }.onFailure { e ->
                updateState { it.copy(isSearching = false, error = "Gagal memindai: ${e.message}") }
            }
        }
    }

    fun addProductToCart(product: Produk, quantity: Int = 1) {
        val currentState = currentState
        val existingItem = currentState.cartItems.find { it.product.id == product.id }
        
        // Basic stock validation
        val currentQtyInCart = existingItem?.quantity ?: 0
        if (product.stockQuantity < currentQtyInCart + quantity) {
            updateState { it.copy(error = "Stok tidak mencukupi untuk ${product.name}") }
            return
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

        launchWithState {
            val subtotal = updatedCartItems.sumOf { it.totalPrice }
            val tax = subtotal * AppConstants.TAX_RATE
            val discount = promoService.calculateDiscount(subtotal)
            val total = subtotal + tax - discount
            updateState {
                it.copy(
                    cartItems = updatedCartItems,
                    subtotal = subtotal,
                    tax = tax,
                    discount = discount,
                    total = maxOf(0.0, total),
                    error = null
                )
            }
        }
    }

<<<<<<< HEAD
=======
    /**
     * Update quantity product di cart dengan validation
     *
     * **Business Logic Flow:**
     * 1. **Item Lookup**: Cari item di cart berdasarkan productId
     * 2. **Quantity Validation**: Validasi new quantity (0 = remove, >0 = update)
     * 3. **Item Update**: Update quantity atau remove jika 0
     * 4. **Totals Recalculation**: Update semua totals
     * 5. **State Update**: Emit new state untuk UI refresh
     *
     * **Quantity Rules:**
     * - quantity > 0: Update item quantity
     * - quantity = 0: Remove item dari cart
     * - quantity < 0: Invalid (tidak diizinkan)
     *
     * **Cart Management:**
     * - Immutable updates (functional programming)
     * - Automatic item removal untuk quantity 0
     * - Efficient list operations
     * - State consistency maintenance
     *
     * **Business Constraints:**
     * - Item harus exist di cart
     * - Quantity harus >= 0
     * - No stock validation (optimistic approach)
     * - Real-time totals untuk user feedback
     *
     * **Performance Considerations:**
     * - O(n) cart search untuk item lookup
     * - Efficient list operations dengan immutable updates
     * - Minimal state emissions
     * - Memory efficient untuk large carts
     *
     * **UI Integration:**
     * - Triggers cart list updates
     * - Updates totals display
     * - Handles item removal animations
     * - Provides quantity validation feedback
     *
     * **Error Handling:**
     * - Item not found: Silent ignore (UI consistency)
     * - Invalid quantity: Validation di UI level
     * - State corruption: Automatic recovery
     *
     * **Usage Example:**
     * ```kotlin
     * // Increase quantity
     * viewModel.updateCartItemQuantity(productId, 3)
     *
     * // Decrease quantity
     * viewModel.updateCartItemQuantity(productId, 1)
     *
     * // Remove item
     * viewModel.updateCartItemQuantity(productId, 0)
     * ```
     *
     * @param productId ID product yang akan diupdate
     * @param newQuantity Quantity baru (0 untuk remove)
     *
     * @see CartItem.updateQuantity
     * @see removeCartItem
     * @see updateCartTotals
     */
>>>>>>> feat/ui-overhaul
    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(productId)
            return
        }

        updateState { currentState ->
            val updatedCartItems = currentState.cartItems.map { item ->
                if (item.product.id == productId) {
                    // Re-validate stock
                    if (item.product.stockQuantity < newQuantity) {
                         return@updateState currentState.copy(error = "Stok tidak mencukupi untuk ${item.product.name}")
                    }
                    item.updateQuantity(newQuantity)
                } else {
                    item
                }
            }
            launchWithState {
                val subtotal = updatedCartItems.sumOf { it.totalPrice }
                val tax = subtotal * AppConstants.TAX_RATE
                val discount = promoService.calculateDiscount(subtotal)
                val total = subtotal + tax - discount
                updateState {
                    it.copy(
                        cartItems = updatedCartItems,
                        subtotal = subtotal,
                        tax = tax,
                        discount = discount,
                        total = maxOf(0.0, total)
                    )
                }
            }
            currentState
        }
    }

<<<<<<< HEAD
=======
    /**
     * Hapus product dari cart
     *
     * **Business Logic Flow:**
     * 1. **Item Removal**: Filter out item dari cart list
     * 2. **Totals Recalculation**: Update semua totals
     * 3. **State Update**: Emit new state untuk UI refresh
     *
     * **Cart Management:**
     * - Immutable removal (functional programming)
     * - Automatic totals recalculation
     * - State consistency maintenance
     * - Efficient list operations
     *
     * **Business Constraints:**
     * - Item harus exist di cart (tidak error jika tidak ada)
     * - No additional validation required
     * - Real-time totals untuk user feedback
     *
     * **Performance Considerations:**
     * - O(n) cart filtering
     * - Efficient immutable operations
     * - Minimal state emissions
     * - Memory efficient
     *
     * **UI Integration:**
     * - Triggers cart list updates
     * - Updates totals display
     * - Handles removal animations
     * - Updates checkout availability
     *
     * **Error Handling:**
     * - Item not found: Silent ignore (UI consistency)
     * - State corruption: Automatic recovery
     * - Concurrent modifications: StateFlow handles
     *
     * **Usage Example:**
     * ```kotlin
     * // Remove specific item
     * viewModel.removeCartItem(productId)
     *
     * // From UI swipe action
     * viewModel.removeCartItem(cartItem.productId)
     * ```
     *
     * @param productId ID product yang akan dihapus
     *
     * @see updateCartTotals
     * @see PosUiState.cartItems
     */
>>>>>>> feat/ui-overhaul
    fun removeCartItem(productId: Long) {
        updateState { currentState ->
            val updatedCartItems = currentState.cartItems.filter { it.product.id != productId }
            launchWithState {
                val subtotal = updatedCartItems.sumOf { it.totalPrice }
                val tax = subtotal * AppConstants.TAX_RATE
                val discount = promoService.calculateDiscount(subtotal)
                val total = subtotal + tax - discount
                updateState {
                    it.copy(
                        cartItems = updatedCartItems,
                        subtotal = subtotal,
                        tax = tax,
                        discount = discount,
                        total = maxOf(0.0, total)
                    )
                }
            }
            currentState
        }
    }

    /**
     * Clear semua items dari cart - reset ke state awal
     *
     * **Business Logic Flow:**
     * 1. **Cart Reset**: Empty cart items list
     * 2. **Totals Reset**: Reset semua totals ke 0
     * 3. **State Reset**: Clear error/success messages
     * 4. **UI Reset**: Emit clean state untuk UI refresh
     *
     * **Cart Management:**
     * - Complete cart reset (functional programming)
     * - State consistency maintenance
     * - Clean slate untuk new transaction
     * - Memory efficient (empty list)
     *
     * **Business Constraints:**
     * - No validation required (always allowed)
     * - Safe operation (no data loss concerns)
     * - User-initiated action
     *
     * **Performance Considerations:**
     * - O(1) operation (constant time)
     * - Minimal state emissions
     * - Memory efficient (empty collections)
     * - Fast UI reset
     *
     * **UI Integration:**
     * - Triggers complete cart refresh
     * - Resets totals display
     * - Disables checkout button
     * - Clears all cart-related UI elements
     *
     * **Error Handling:**
     * - No error conditions possible
     * - State corruption: Automatic recovery
     * - Concurrent modifications: StateFlow handles
     *
     * **Usage Example:**
     * ```kotlin
     * // Clear cart after transaction
     * viewModel.clearCart()
     *
     * // Reset for new customer
     * viewModel.clearCart()
     * ```
     *
     * @see PosUiState.cartItems
     * @see PosUiState.subtotal
     */
    fun clearCart() {
        updateState { currentState ->
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

    /**
     * Set metode pembayaran untuk transaksi
     *
     * **Business Logic:**
     * - Update payment method di UI state
     * - Validasi method (CASH/CARD) dilakukan di UI level
     * - Reactive update untuk payment button display
     *
     * **State Management:**
     * - Immediate state update
     * - Triggers UI refresh untuk payment options
     * - Maintains cart state consistency
     *
     * **Usage Example:**
     * ```kotlin
     * // Set cash payment
     * viewModel.setPaymentMethod("CASH")
     *
     * // Set card payment
     * viewModel.setPaymentMethod("CARD")
     * ```
     *
     * @param method Payment method ("CASH" or "CARD")
     *
     * @see PosUiState.paymentMethod
     * @see processPayment
     */
    fun setPaymentMethod(method: String) {
        updateState { it.copy(paymentMethod = method) }
    }

<<<<<<< HEAD
=======
    /**
     * Set discount amount untuk transaksi
     *
     * **Business Logic Flow:**
     * 1. **Discount Application**: Apply discount ke subtotal + tax
     * 2. **Total Recalculation**: total = subtotal + tax - discount
     * 3. **Minimum Total**: Ensure total tidak negatif (maxOf 0.0)
     * 4. **State Update**: Emit new state dengan updated totals
     *
     * **Discount Rules:**
     * - Discount applied after tax calculation
     * - Total cannot be negative (floor at 0.0)
     * - Real-time recalculation untuk user feedback
     * - Percentage or fixed amount support
     *
     * **State Management:**
     * - Immediate totals update
     * - Reactive UI refresh
     * - Maintains cart item integrity
     * - Error state clearing
     *
     * **Business Constraints:**
     * - Discount >= 0 (no negative discounts)
     * - Total cannot be negative
     * - Real-time validation
     *
     * **UI Integration:**
     * - Triggers totals display update
     * - Enables/disables payment based on total
     * - Shows discount impact immediately
     *
     * **Usage Example:**
     * ```kotlin
     * // Apply 10% discount
     * val discountAmount = subtotal * 0.1
     * viewModel.setDiscount(discountAmount)
     *
     * // Apply fixed discount
     * viewModel.setDiscount(5000.0)
     * ```
     *
     * @param discount Discount amount (positive value)
     *
     * @see PosUiState.discount
     * @see PosUiState.total
     * @see updateCartTotals
     */
    fun setDiscount(discount: Double) {
        _uiState.update { currentState ->
            val newTotal = currentState.subtotal + currentState.tax - discount
            currentState.copy(
                discount = discount,
                total = maxOf(0.0, newTotal)
            )
        }
    }

    /**
     * Process payment dan create sale transaction - CORE BUSINESS METHOD
     *
     * **Business Logic Flow:**
     * 1. **Validation**: Cek cart tidak kosong, payment method valid
     * 2. **Transaction Creation**: Convert cart items ke SaleItems
     * 3. **Sale Processing**: Create Sale dengan SaleService
     * 4. **Inventory Update**: Automatic stock reduction via SaleService
     * 5. **State Reset**: Clear cart, show success, enable receipt printing
     * 6. **Error Handling**: Comprehensive error handling dengan user feedback
     *
     * **Transaction Processing:**
     * - Atomic operation (all-or-nothing)
     * - Inventory validation dan update
     * - Sale record creation dengan audit trail
     * - Cashier attribution untuk accountability
     * - Payment method tracking
     *
     * **Business Rules:**
     * - Cart tidak boleh kosong
     * - Payment method harus valid (CASH/CARD)
     * - Stock availability validation
     * - Transaction timestamp recording
     * - Cashier authentication required
     *
     * **State Management:**
     * - Loading state selama processing
     * - Success state dengan sale ID
     * - Error state dengan descriptive messages
     * - Receipt dialog trigger
     * - Cart reset setelah success
     *
     * **Integration Points:**
     * - SaleService.createSale() untuk transaction processing
     * - Inventory automatic updates
     * - Receipt printing preparation
     * - UI state management untuk feedback
     *
     * **Performance Considerations:**
     * - Async processing untuk non-blocking UI
     * - Efficient cart-to-sale conversion
     * - Minimal state emissions
     * - Memory cleanup setelah transaction
     *
     * **Error Scenarios:**
     * - Empty cart: Validation error
     * - Insufficient stock: Business logic error
     * - Database errors: Technical error
     * - Network issues: Communication error
     * - Concurrent modifications: State conflict
     *
     * **UI Integration:**
     * - Payment button loading state
     * - Success confirmation dialog
     * - Receipt printing option
     * - Cart reset untuk next transaction
     * - Error message display
     *
     * **Audit Trail:**
     * - Sale record dengan timestamp
     * - Cashier attribution
     * - Payment method logging
     * - Transaction amount tracking
     *
     * **Usage Example:**
     * ```kotlin
     * // Process payment for current cashier
     * viewModel.processPayment(currentCashierId)
     *
     * // After payment success
     * if (uiState.completedSaleId != null) {
     *     // Show receipt dialog
     *     // Print receipt option
     * }
     * ```
     *
     * @param cashierId ID kasir yang memproses transaksi
     *
     * @throws ValidationError jika cart kosong
     * @throws BusinessLogicError jika stock insufficient
     * @throws DatabaseError jika transaction gagal
     *
     * @see SaleService.createSale
     * @see PosUiState.isProcessingPayment
     * @see PosUiState.completedSaleId
     * @see printReceipt
     */
>>>>>>> feat/ui-overhaul
    fun processPayment() {
        val currentState = currentState
        if (currentState.cartItems.isEmpty()) {
            updateState { it.copy(error = "Keranjang kosong") }
            return
        }

        launchWithState {
            updateState { it.copy(isProcessingPayment = true, error = null) }
            
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

<<<<<<< HEAD
            val result = saleService.createPenjualan(sale, items)
            result.onSuccess { completedSale ->
                updateState {
=======
            _uiState.update { it.copy(isProcessingPayment = true, error = null) }

            try {
                // Create sale items from cart
                val saleItems = currentState.cartItems.map { cartItem ->
                    SaleItem(
                        saleId = 0, // Will be set by repository
                        productId = cartItem.product.id,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.unitPrice,
                        totalPrice = cartItem.totalPrice
                    )
                }

                // Create sale
                val sale = Sale(
                    saleDate = java.util.Date(),
                    totalAmount = currentState.total,
                    tax = currentState.tax,
                    discount = currentState.discount,
                    paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.valueOf(currentState.paymentMethod),
                    cashierId = cashierId
                )

                saleService.createSale(sale, saleItems)
                    .onSuccess { saleWithItems ->
                        _uiState.update {
                            PosUiState(
                                successMessage = "Pembayaran berhasil diproses",
                                paymentMethod = currentState.paymentMethod,
                                completedSaleId = saleWithItems.sale.id,
                                showReceiptDialog = true
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isProcessingPayment = false,
                                error = error.message ?: "Gagal memproses pembayaran"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
>>>>>>> feat/ui-overhaul
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
<<<<<<< HEAD
            }.onFailure { e ->
                updateState { it.copy(isProcessingPayment = false, error = "Gagal memproses pembayaran: ${e.message}") }
=======
>>>>>>> feat/ui-overhaul
            }
        }
    }

<<<<<<< HEAD
=======
    /**
     * Start barcode scanning mode
     *
     * **Business Logic:**
     * - Enable camera scanning untuk barcode detection
     * - Update UI state untuk show scanner overlay
     * - Prepare untuk real-time barcode processing
     *
     * **Hardware Integration:**
     * - Camera permission required
     * - ZXing library untuk barcode detection
     * - Real-time preview dengan overlay
     *
     * **State Management:**
     * - Set isScanning = true
     * - Triggers UI camera preview
     * - Enables barcode processing
     *
     * **UI Integration:**
     * - Show camera preview
     * - Display scan instructions
     * - Enable cancel scanning option
     *
     * **Usage Example:**
     * ```kotlin
     * // Start scanning from UI button
     * viewModel.startScanning()
     *
     * // UI shows camera with overlay
     * if (uiState.isScanning) {
     *     CameraPreview()
     * }
     * ```
     *
     * @see stopScanning
     * @see onBarcodeScanned
     * @see PosUiState.isScanning
     */
    fun startScanning() {
        _uiState.update { it.copy(isScanning = true) }
    }

    /**
     * Stop barcode scanning mode
     *
     * **Business Logic:**
     * - Disable camera scanning
     * - Return to normal POS interface
     * - Clear scanning state
     *
     * **Hardware Integration:**
     * - Release camera resources
     * - Stop barcode detection
     * - Reset scanner state
     *
     * **State Management:**
     * - Set isScanning = false
     * - Triggers UI back to normal mode
     * - Maintains cart state
     *
     * **UI Integration:**
     * - Hide camera preview
     * - Show normal POS interface
     * - Reset scan instructions
     *
     * **Usage Example:**
     * ```kotlin
     * // Stop scanning from UI cancel button
     * viewModel.stopScanning()
     *
     * // UI returns to normal POS view
     * if (!uiState.isScanning) {
     *     PosInterface()
     * }
     * ```
     *
     * @see startScanning
     * @see PosUiState.isScanning
     */
    fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
    }

    /**
     * Handle barcode scanning result - CORE SCANNING METHOD
     *
     * **Business Logic Flow:**
     * 1. **Product Lookup**: Search product by exact barcode match
     * 2. **Validation**: Check if product exists dengan barcode tersebut
     * 3. **Cart Addition**: Add product to cart jika ditemukan
     * 4. **State Update**: Update UI state dengan success/error feedback
     * 5. **Scan Reset**: Stop scanning mode setelah processing
     *
     * **Barcode Processing:**
     * - Exact barcode matching (case-sensitive)
     * - Product search via ProductService
     * - Support multiple barcode formats (EAN-13, Code 128, etc.)
     * - Real-time processing untuk smooth UX
     *
     * **Business Rules:**
     * - Barcode harus exact match
     * - Product harus exist dan active
     * - Automatic cart addition dengan quantity 1
     * - Scan mode auto-stop setelah success/error
     *
     * **State Management:**
     * - Loading state selama search
     * - Success state dengan cart update
     * - Error state dengan descriptive messages
     * - Scan mode termination
     *
     * **Error Scenarios:**
     * - Product not found: User-friendly error message
     * - Search failure: Service error handling
     * - Invalid barcode: Format validation
     * - Network issues: Offline handling
     *
     * **UI Integration:**
     * - Visual feedback untuk scan result
     * - Automatic cart refresh
     * - Error message display
     * - Scan mode exit
     *
     * **Performance Considerations:**
     * - Async processing untuk non-blocking UI
     * - Efficient product search
     * - Minimal state emissions
     * - Fast barcode processing
     *
     * **Usage Example:**
     * ```kotlin
     * // From ZXing scanner callback
     * viewModel.onBarcodeScanned("8991234567890")
     *
     * // Handle result in UI
     * when {
     *     uiState.error?.contains("barcode") == true -> showError()
     *     cartItems.size > previousSize -> showSuccess()
     * }
     * ```
     *
     * @param barcode Detected barcode string dari scanner
     *
     * @throws ProductNotFound jika barcode tidak cocok dengan product manapun
     * @throws ServiceError jika product search gagal
     *
     * @see ProductService.searchProducts
     * @see addProductToCart
     * @see PosUiState.isScanning
     */
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            try {
                // Search product by barcode
                productService.searchProducts(barcode)
                    .onSuccess { products ->
                        val product = products.find { it.barcode == barcode }
                        if (product != null) {
                            addProductToCart(product)
                            _uiState.update {
                                it.copy(
                                    isScanning = false,
                                    error = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isScanning = false,
                                    error = "Product dengan barcode $barcode tidak ditemukan"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                error = "Gagal mencari product dengan barcode"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = "Error scanning: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear error message dari UI state
     *
     * **Business Logic:**
     * - Reset error state untuk clean UI
     * - Allow user to retry operations
     * - Clear error indicators
     *
     * **State Management:**
     * - Set error = null
     * - Maintains other state intact
     * - Triggers UI error clearing
     *
     * **UI Integration:**
     * - Hide error messages/snackbars
     * - Reset error styling
     * - Enable retry actions
     *
     * **Usage Example:**
     * ```kotlin
     * // Clear error on user action
     * viewModel.clearError()
     *
     * // UI error state reset
     * if (uiState.error == null) {
     *     // Hide error UI
     * }
     * ```
     *
     * @see PosUiState.error
     */
>>>>>>> feat/ui-overhaul
    fun clearError() {
        updateState { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        updateState { it.copy(successMessage = null) }
    }

    fun printReceipt() {
        val saleId = currentState.completedSaleId
        if (saleId == null) {
            updateState { it.copy(error = "ID penjualan tidak ditemukan") }
            return
        }
        launchWithState {
            updateState { it.copy(isPrintingReceipt = true) }
            val currentUser = authService.getCurrentUser()
            val cashierName = currentUser?.username ?: "Kasir"
            val result = saleService.cetakStruk(
                saleId = saleId,
                cashierName = cashierName
            )
            updateState {
                if (result.isSuccess) {
                    it.copy(
                        isPrintingReceipt = false,
                        successMessage = "Struk sedang dicetak"
                    )
                } else {
                    it.copy(
                        isPrintingReceipt = false,
                        error = "Gagal mencetak struk: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun startNewTransaction() {
        updateState {
            it.copy(
                completedSaleId = null,
                showReceiptDialog = false
            )
        }
        clearCart()
    }

    fun dismissReceiptDialog() {
        updateState { it.copy(showReceiptDialog = false) }
    }
}
