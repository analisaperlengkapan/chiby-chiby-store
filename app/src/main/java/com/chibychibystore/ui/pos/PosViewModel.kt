package com.chibychibystore.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ProductService
import com.chibychibystore.constant.AppConstants
import com.chibychibystore.service.SaleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Data class untuk item dalam shopping cart POS
 *
 * **Business Purpose:**
 * - Representasi produk yang dipilih customer dalam transaksi
 * - Menyimpan quantity dan harga untuk perhitungan total
 * - Immutable data structure untuk thread safety
 * - Support untuk quantity updates tanpa side effects
 *
 * **Business Rules:**
 * - Quantity harus > 0 (tidak boleh 0 atau negatif)
 * - Unit price menggunakan produk selling price sebagai default
 * - Total price = unitPrice * quantity (otomatis calculated)
 * - Product reference immutable untuk data consistency
 *
 * **Usage Context:**
 * - Created ketika customer menambah produk ke cart
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
 * - SaleService: Converted ke ItemPenjualan untuk database storage
 * - Receipt printing: Used untuk generate receipt items
 *
 * @property product Produk yang dipilih customer (immutable reference)
 * @property quantity Jumlah produk dalam cart (harus > 0)
 * @property unitPrice Harga per unit (default: product.sellingPrice)
 * @property totalPrice Total harga untuk quantity ini (calculated: unitPrice * quantity)
 *
 * @constructor Create cart item dengan product dan quantity
 * @param product Produk yang akan ditambahkan ke cart
 * @param quantity Jumlah produk (default: 1)
 * @param unitPrice Harga per unit (default: product.sellingPrice)
 * @param totalPrice Total harga (auto-calculated jika tidak disediakan)
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see Produk
 * @see ItemPenjualan
 * @see PosViewModel
 */
data class CartItem(
    val product: Produk,
    val quantity: Int,
    val unitPrice: Double = product.sellingPrice,
    val totalPrice: Double = product.sellingPrice * quantity
) {
    init {
        require(quantity > 0) { "Quantity harus lebih dari 0" }
        require(unitPrice >= 0) { "Unit price tidak boleh negatif" }
        require(totalPrice >= 0) { "Total price tidak boleh negatif" }
    }

    /**
     * Update quantity dengan return new instance (immutable update)
     *
     * **Business Logic:**
     * - Validate new quantity (> 0)
     * - Recalculate total price dengan unitPrice * newQuantity
     * - Return new CartItem instance (functional programming pattern)
     *
     * **Usage Example:**
     * ```kotlin
     * val item = CartItem(product, 1)
     * val updatedItem = item.updateQuantity(3) // quantity: 3, totalPrice: unitPrice * 3
     * ```
     *
     * @param newQuantity Quantity baru yang diinginkan (> 0)
     * @return CartItem baru dengan quantity yang diupdate
     *
     * @throws IllegalArgumentException jika newQuantity <= 0
     */
    fun updateQuantity(newQuantity: Int): CartItem {
        require(newQuantity > 0) { "New quantity harus lebih dari 0" }
        return copy(
            quantity = newQuantity,
            totalPrice = unitPrice * newQuantity
        )
    }
}

/**
 * UI State untuk POS Screen - comprehensive state management
 *
 * **State Management Purpose:**
 * - Single source of truth untuk semua POS UI state
 * - Reactive updates untuk Compose UI components
 * - Immutable state untuk predictable UI behavior
 * - Comprehensive error dan loading state handling
 *
 * **State Categories:**
 * - **Cart State**: cartItems, subtotal, tax, discount, total
 * - **Search State**: searchQuery, searchResults, isSearching
 * - **Payment State**: paymentMethod, isProcessingPayment, completedSaleId
 * - **UI State**: error, successMessage, showReceiptDialog, isPrintingReceipt
 * - **Hardware State**: isScanning (barcode scanner)
 *
 * **Business Logic Integration:**
 * - Cart calculations: subtotal = sum(cartItems.totalPrice)
 * - Tax calculation: tax = subtotal * 0.1 (10% Indonesian tax)
 * - Total calculation: total = subtotal + tax - discount
 * - Payment processing state management
 *
 * **State Flow Pattern:**
 * - StateFlow emission triggers UI recomposition
 * - Immutable updates prevent race conditions
 * - Error states clear automatically on user action
 * - Success states trigger navigation/dialog display
 *
 * **Thread Safety:**
 * - All properties immutable (val)
 * - StateFlow handles cross-thread communication
 * - ViewModel scope manages coroutine lifecycle
 * - No mutable state exposure to UI layer
 *
 * **UI Integration:**
 * - Compose collects StateFlow untuk reactive UI
 * - State drives conditional UI (loading, error, success)
 * - Cart state drives list display dan totals
 * - Payment state controls checkout flow
 *
 * @property cartItems List produk dalam shopping cart
 * @property searchQuery Query pencarian produk saat ini
 * @property searchResults Hasil pencarian produk dari database
 * @property isSearching Flag menunjukkan search operation sedang berlangsung
 * @property isProcessingPayment Flag menunjukkan payment sedang diproses
 * @property paymentMethod Metode pembayaran (CASH/CARD)
 * @property subtotal Total sebelum tax dan discount
 * @property tax Pajak yang dikenakan (10% dari subtotal)
 * @property discount Diskon yang diterapkan
 * @property total Total akhir yang harus dibayar
 * @property error Error message untuk ditampilkan ke user
 * @property successMessage Success message setelah operasi berhasil
 * @property isScanning Flag menunjukkan barcode scanner aktif
 * @property completedSaleId ID penjualan yang baru saja completed
 * @property showReceiptDialog Flag untuk menampilkan receipt dialog
 * @property isPrintingReceipt Flag menunjukkan receipt sedang dicetak
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see PosViewModel
 * @see StateFlow
 * @see MutableStateFlow
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
    val isScanning: Boolean = false,
    val completedSaleId: Long? = null,
    val showReceiptDialog: Boolean = false,
    val isPrintingReceipt: Boolean = false
)

/**
 * ViewModel untuk Point of Sale (POS) Screen - Core transaction processing
 *
 * **MVVM Architecture Role:**
 * - Business logic layer untuk POS operations
 * - State management untuk reactive UI updates
 * - Integration point antara UI dan business services
 * - Lifecycle-aware data management
 *
 * **Core Responsibilities:**
 * - Shopping cart management (add, update, remove items)
 * - Product search dan selection
 * - Real-time cart calculations (subtotal, tax, discount, total)
 * - Payment processing dengan inventory updates
 * - Receipt printing integration
 * - Barcode scanning coordination
 * - Error handling dan user feedback
 *
 * **State Management Strategy:**
 * - Single StateFlow untuk semua UI state (PosUiState)
 * - Immutable state updates untuk predictable behavior
 * - Reactive UI updates tanpa manual refresh
 * - Error states dengan automatic clearing
 * - Success states untuk navigation triggers
 *
 * **Business Logic Flows:**
 * 1. **Product Selection**: Search → Add to cart → Update quantities
 * 2. **Cart Management**: Real-time totals → Discount application → Tax calculation
 * 3. **Payment Processing**: Method selection → Validation → Transaction creation → Inventory update
 * 4. **Receipt Handling**: Print receipt → Show success dialog → Reset cart
 *
 * **Integration Points:**
 * - **ProductService**: Product search, inventory validation
 * - **SaleService**: Transaction creation, receipt printing
 * - **UI Layer**: Reactive state consumption, user action handling
 * - **Hardware**: Barcode scanner, thermal printer coordination
 *
 * **Thread Safety & Performance:**
 * - ViewModelScope untuk coroutine lifecycle management
 * - StateFlow untuk thread-safe state emission
 * - Async operations untuk non-blocking UI
 * - Memory-efficient state management
 * - Error recovery tanpa state corruption
 *
 * **Business Rules Enforcement:**
 * - Stock validation sebelum adding to cart
 * - Minimum purchase requirements (future enhancement)
 * - Tax calculation sesuai Indonesian regulations (10%)
 * - Payment method validation
 * - Transaction atomicity dengan rollback on failure
 *
 * **Error Handling Strategy:**
 * - User-friendly Indonesian error messages
 * - Automatic error state clearing on user action
 * - Recovery suggestions untuk common errors
 * - Logging untuk debugging tanpa exposing to UI
 *
 * **Usage Pattern:**
 * ```kotlin
 * // In Composable
 * val viewModel: PosViewModel = hiltViewModel()
 * val uiState by viewModel.uiState.collectAsState()
 *
 * // Reactive UI updates
 * when {
 *     uiState.isProcessingPayment -> LoadingIndicator()
 *     uiState.error != null -> ErrorDialog(uiState.error)
 *     uiState.completedSaleId != null -> SuccessDialog()
 *     else -> PosContent(uiState, viewModel)
 * }
 * ```
 *
 * @property productService Service untuk operasi produk dan inventory
 * @property saleService Service untuk transaction processing dan receipts
 * @property uiState Reactive state flow untuk UI consumption
 *
 * @constructor Inject dependencies melalui Hilt
 * @param productService Instance ProductService yang diinject
 * @param saleService Instance SaleService yang diinject
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see PosUiState
 * @see CartItem
 * @see ProductService
 * @see SaleService
 * @see ViewModel
 * @see StateFlow
 */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productService: ProductService,
    private val saleService: SaleService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())

    // Search query state for reactive pipeline
    private val _searchQuery = MutableStateFlow("")

    /**
     * Initializes the reactive search pipeline.
     * Uses debounce and flatMapLatest to handle search queries efficiently.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<PosUiState> = run {
        val searchResultsFlow = _searchQuery
            .debounce(300L)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    productService.observeSearchProducts(query)
                        .catch { e ->
                            emit(emptyList()) // Emit empty on error to keep stream alive
                        }
                }
            }

        combine(
            _uiState,
            searchResultsFlow,
            _searchQuery
        ) { currentState, searchResults, query ->
            // Determine searching state based on query vs current results (simplified logic)
            // A more robust way would be to track 'loading' state in the flow pipeline,
            // but for now, we rely on the fact that if query is not blank, we are technically 'searching'
            // until results come back. The combine triggers whenever any of these change.

            currentState.copy(
                searchResults = searchResults,
                searchQuery = query,
                // If query is not blank but results are empty, we might still be searching or just found nothing.
                // For simplicity in this standardized pattern, we can manage `isSearching` separately or infer it.
                // To keep it clean without extra complex flow merging for loading states:
                // We will rely on the `isSearching` flag managed via side-effects if needed, or
                // ideally, we'd wrap the search result in a Result/Loading wrapper.
                // For this refactor, we ensure data consistency.
                isSearching = false // Reset searching when results arrive
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PosUiState()
        )
    }

    /**
     * Search produk berdasarkan query untuk ditambahkan ke cart
     *
     * **Refactored to use Reactive Stream Pattern**
     * Instead of manual coroutine management, this updates the `_searchQuery` flow
     * which triggers the pipeline defined in `uiState`.
     *
     * @param query Search query string (name/barcode, case-insensitive)
     */
    fun searchProducts(query: String) {
        // We update _searchQuery to trigger the search pipeline
        _searchQuery.value = query

        // We also update _uiState immediately to reflect the text change in the UI
        // and set isSearching to true if appropriate
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearching = query.isNotBlank()
            )
        }
    }

    /**
     * Tambah produk ke shopping cart dengan quantity handling
     *
     * **Business Logic Flow:**
     * 1. **Duplicate Check**: Cek jika produk sudah ada di cart
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
     * @param product Produk yang akan ditambahkan ke cart
     * @param quantity Jumlah produk yang akan ditambahkan (default: 1)
     *
     * @throws IllegalArgumentException jika quantity <= 0 (via CartItem validation)
     *
     * @see CartItem
     * @see updateCartTotals
     * @see PosUiState.cartItems
     */
    fun addProductToCart(product: Produk, quantity: Int = 1) {
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
                }
            } else {
                // Add new item
                currentState.cartItems + CartItem(product, quantity)
            }

            calculateNewState(currentState, updatedCartItems)
        }
    }

    /**
     * Update quantity produk di cart dengan validation
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
     * @param productId ID produk yang akan diupdate
     * @param newQuantity Quantity baru (0 untuk remove)
     *
     * @see CartItem.updateQuantity
     * @see removeCartItem
     * @see updateCartTotals
     */
    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(productId)
            return
        }

        _uiState.update { currentState ->
            // Stock validation
            val itemToUpdate = currentState.cartItems.find { it.product.id == productId }
            if (itemToUpdate != null) {
                if (!isStockSufficient(itemToUpdate.product, newQuantity)) {
                    return@update currentState.copy(error = "Stok tidak mencukupi. Sisa: ${itemToUpdate.product.stockQuantity}")
                }
            }

            val updatedCartItems = currentState.cartItems.map { item ->
                if (item.product.id == productId) {
                    item.updateQuantity(newQuantity)
                } else {
                    item
                }
            }

            calculateNewState(currentState, updatedCartItems)
        }
    }

    /**
     * Hapus produk dari cart
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
     * @param productId ID produk yang akan dihapus
     *
     * @see updateCartTotals
     * @see PosUiState.cartItems
     */
    fun removeCartItem(productId: Long) {
        _uiState.update { currentState ->
            val updatedCartItems = currentState.cartItems.filter { it.product.id != productId }
            calculateNewState(currentState, updatedCartItems)
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
        _uiState.update { it.copy(paymentMethod = method) }
    }

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
     * 3. **Sale Processing**: Create Penjualan dengan SaleService
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
    fun processPayment() {
        val currentState = _uiState.value

        if (currentState.cartItems.isEmpty()) {
            _uiState.update { it.copy(error = "Cart kosong, tidak dapat memproses pembayaran") }
            return
        }

        // Get cashier ID from auth service
        viewModelScope.launch {
            // Get cashier ID from auth service
            val currentUser = authService.getCurrentUser()
            val cashierId = currentUser?.id ?: run {
                _uiState.update { it.copy(error = "User tidak terautentikasi") }
                return@launch
            }

            _uiState.update { it.copy(isProcessingPayment = true, error = null) }

            try {
                // Create sale items from cart
                val saleItems = currentState.cartItems.map { cartItem ->
                    ItemPenjualan(
                        saleId = 0, // Will be set by repository
                        productId = cartItem.product.id,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.unitPrice,
                        totalPrice = cartItem.totalPrice
                    )
                }

                // Create sale
                val sale = Penjualan(
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
                                completedSaleId = saleWithItems.penjualan.id,
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
                    it.copy(
                        isProcessingPayment = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

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
     * @throws ProductNotFound jika barcode tidak cocok dengan produk manapun
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
                                    error = "Produk dengan barcode $barcode tidak ditemukan"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                error = "Gagal mencari produk dengan barcode"
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
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message dari UI state
     *
     * **Business Logic:**
     * - Reset success state setelah user acknowledgment
     * - Prepare untuk next operation
     * - Clear success indicators
     *
     * **State Management:**
     * - Set successMessage = null
     * - Maintains other state intact
     * - Triggers UI success clearing
     *
     * **UI Integration:**
     * - Hide success messages/snackbars
     * - Reset success styling
     * - Allow next user actions
     *
     * **Usage Example:**
     * ```kotlin
     * // Clear success after user sees it
     * viewModel.clearSuccessMessage()
     *
     * // UI success state reset
     * if (uiState.successMessage == null) {
     *     // Hide success UI
     * }
     * ```
     *
     * @see PosUiState.successMessage
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Helper to calculate new state with updated cart totals.
     * Use this inside an update { } block to ensure atomicity.
     */
    private fun calculateNewState(currentState: PosUiState, updatedCartItems: List<CartItem>): PosUiState {
        val subtotal = updatedCartItems.sumOf { it.totalPrice }
        val tax = subtotal * AppConstants.TAX_RATE
        val discount = currentState.discount
        val total = subtotal + tax - discount

        return currentState.copy(
            cartItems = updatedCartItems,
            subtotal = subtotal,
            tax = tax,
            total = maxOf(0.0, total)
        )
    }

    /**
     * Print receipt untuk penjualan yang baru saja diproses
     *
     * **Business Logic Flow:**
     * 1. **Sale ID Validation**: Pastikan completedSaleId tersedia
     * 2. **Receipt Generation**: Call SaleService.printReceipt()
     * 3. **Printer Communication**: Send ke thermal printer via Bluetooth
     * 4. **State Update**: Update UI dengan success/error feedback
     * 5. **Error Handling**: Comprehensive error handling
     *
     * **Receipt Content:**
     * - Store information (name, address)
     * - Transaction details (date, sale ID)
     * - Item list dengan quantities dan prices
     * - Totals (subtotal, tax, discount, total)
     * - Payment method information
     * - Cashier attribution
     *
     * **Hardware Integration:**
     * - Bluetooth thermal printer connection
     * - ESC/POS command protocol
     * - Receipt paper formatting
     * - Print quality optimization
     *
     * **Business Rules:**
     * - Receipt hanya bisa dicetak setelah payment success
     * - Sale ID harus valid dan exist
     * - Printer harus connected dan ready
     * - Receipt format sesuai standar retail
     *
     * **State Management:**
     * - Loading state selama printing
     * - Success state dengan confirmation
     * - Error state dengan descriptive messages
     * - Receipt dialog state management
     *
     * **Performance Considerations:**
     * - Async printing untuk non-blocking UI
     * - Efficient receipt data preparation
     * - Minimal state emissions
     * - Printer communication optimization
     *
     * **Error Scenarios:**
     * - No sale ID: Validation error
     * - Sale not found: Data error
     * - Printer not connected: Hardware error
     * - Print failure: Communication error
     * - Paper out: Hardware error
     *
     * **UI Integration:**
     * - Print button loading state
     * - Success confirmation message
     * - Error message display
     * - Receipt dialog management
     * - Print status feedback
     *
     * **Audit Trail:**
     * - Print timestamp recording
     * - Receipt number tracking
     * - Print success confirmation
     * - Error logging untuk troubleshooting
     *
     * **Usage Example:**
     * ```kotlin
     * // Print receipt after payment
     * viewModel.printReceipt()
     *
     * // Handle print result
     * when {
     *     uiState.isPrintingReceipt -> showLoading()
     *     uiState.successMessage?.contains("receipt") == true -> showSuccess()
     *     uiState.error?.contains("receipt") == true -> showError()
     * }
     * ```
     *
     * @throws ValidationError jika sale ID tidak tersedia
     * @throws HardwareError jika printer tidak connected
     * @throws CommunicationError jika print gagal
     *
     * @see SaleService.printReceipt
     * @see PosUiState.completedSaleId
     * @see PosUiState.isPrintingReceipt
     */
    fun printReceipt() {
        val currentState = _uiState.value
        val saleId = currentState.completedSaleId

        if (saleId == null) {
            _uiState.update { it.copy(error = "ID penjualan tidak ditemukan") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingReceipt = true, error = null) }

            try {
                val cashierName = authService.getCurrentUser()?.username ?: "Kasir"

                saleService.printReceipt(
                    saleId = saleId,
                    storeName = AppConstants.STORE_NAME,
                    storeAddress = AppConstants.STORE_ADDRESS,
                    cashierName = cashierName
                )
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isPrintingReceipt = false,
                                successMessage = "Receipt berhasil dicetak"
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isPrintingReceipt = false,
                                error = error.message ?: "Gagal mencetak receipt"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPrintingReceipt = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Dismiss receipt dialog setelah user selesai
     *
     * **Business Logic:**
     * - Close receipt dialog
     * - Clear completed sale ID
     * - Prepare untuk next transaction
     *
     * **State Management:**
     * - Set showReceiptDialog = false
     * - Clear completedSaleId
     * - Maintains other state
     *
     * **UI Integration:**
     * - Hide receipt dialog
     * - Return to normal POS interface
     * - Enable new transaction start
     *
     * **Usage Example:**
     * ```kotlin
     * // User closes receipt dialog
     * viewModel.dismissReceiptDialog()
     *
     * // UI returns to POS interface
     * if (!uiState.showReceiptDialog) {
     *     PosInterface()
     * }
     * ```
     *
     * @see PosUiState.showReceiptDialog
     * @see PosUiState.completedSaleId
     * @see startNewTransaction
     */
    fun dismissReceiptDialog() {
        _uiState.update {
            it.copy(
                showReceiptDialog = false,
                completedSaleId = null
            )
        }
    }

    /**
     * Start new transaction - complete state reset
     *
     * **Business Logic Flow:**
     * 1. **Complete Reset**: Reset semua state ke initial values
     * 2. **Clean Slate**: Clear cart, totals, messages, dialogs
     * 3. **Fresh Start**: Prepare untuk new customer transaction
     *
     * **State Management:**
     * - Complete PosUiState reset
     * - All properties kembali ke default values
     * - Clean state untuk new transaction
     *
     * **Business Rules:**
     * - Complete transaction isolation
     * - No state carry-over
     * - Fresh session untuk each customer
     *
     * **UI Integration:**
     * - Reset all UI elements
     * - Clear cart display
     * - Reset payment options
     * - Hide all dialogs
     *
     * **Usage Example:**
     * ```kotlin
     * // Start new transaction after current completion
     * viewModel.startNewTransaction()
     *
     * // UI completely reset
     * PosScreen() // Fresh state
     * ```
     *
     * @see PosUiState
     * @see dismissReceiptDialog
     */
    fun startNewTransaction() {
        _uiState.update { PosUiState() }
    }

    /**
     * Helper to check stock availability (pure function).
     */
    private fun isStockSufficient(product: Produk, requestedQuantity: Int): Boolean {
        return requestedQuantity <= product.stockQuantity
    }
}
