package com.chibychibystore.ui.pos

import androidx.lifecycle.viewModelScope
import com.chibychibystore.constant.AppConstants
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.PromoService
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.WarehouseService
import com.chibychibystore.ui.base.BaseViewModel
import com.chibychibystore.ui.base.UiState
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
    val isPrintingReceipt: Boolean = false,
    val isScanning: Boolean = false,
    val selectedPelanggan: com.chibychibystore.data.local.entity.Pelanggan? = null,
    val pointsToRedeem: Int = 0,
    val isRedeemingPoints: Boolean = false,
    val pelangganList: List<com.chibychibystore.data.local.entity.Pelanggan> = emptyList(),
    // Warehouse the cashier is currently selling from. Defaults to 1L for backward
    // compatibility with single-warehouse deployments and existing data; users with
    // multiple warehouses can switch via the warehouse selector in the POS UI.
    // The sale's warehouseId drives stock validation and inventory adjustment in
    // SaleServiceImpl, so without this selector inventory added to non-default
    // warehouses (via PurchaseServiceImpl.createPembelianWithWarehouse) could
    // never be sold.
    val selectedWarehouseId: Long = 1L,
    val warehouses: List<com.chibychibystore.data.local.entity.Gudang> = emptyList()
) : UiState

/**
 * ViewModel for Point of Sale (POS) Screen
 */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val productService: ProductService,
    private val saleService: SaleService,
    private val authService: AuthService,
    private val promoService: PromoService,
    private val pelangganService: com.chibychibystore.service.PelangganService,
    private val warehouseService: WarehouseService,
    private val stokGudangRepository: StokGudangRepository
) : BaseViewModel<PosUiState>(PosUiState()) {

    private val _searchQuery = MutableStateFlow("")

    init {
        setupSearch()
        loadPelanggan()
        loadWarehouses()
    }

    private fun loadPelanggan() {
        viewModelScope.launch {
            pelangganService.ambilSemuaPelanggan().collect { list ->
                updateState { it.copy(pelangganList = list) }
            }
        }
    }

    private fun loadWarehouses() {
        viewModelScope.launch {
            warehouseService.observeGudangs().collect { list ->
                updateState { state ->
                    // Auto-select the first available warehouse if the current
                    // selection no longer exists (e.g. the default id=1L was
                    // never created or was deleted), so the cashier doesn't get
                    // stuck with an invalid warehouseId that would FK-fail at
                    // sale creation.
                    val newSelected = if (list.any { it.id == state.selectedWarehouseId }) {
                        state.selectedWarehouseId
                    } else {
                        list.firstOrNull()?.id ?: state.selectedWarehouseId
                    }
                    state.copy(warehouses = list, selectedWarehouseId = newSelected)
                }
            }
        }
    }

    fun selectPelanggan(pelanggan: com.chibychibystore.data.local.entity.Pelanggan?) {
        updateState {
            it.copy(
                selectedPelanggan = pelanggan,
                isRedeemingPoints = false,
                pointsToRedeem = 0
            )
        }
        recalculateTotals()
    }

    fun togglePointRedemption(redeem: Boolean) {
        val pelanggan = currentState.selectedPelanggan ?: return
        if (redeem && pelanggan.point <= 0) return

        updateState { it.copy(isRedeemingPoints = redeem) }

        launchWithState {
            if (redeem) {
                val pointsToRedeem = computePointsToRedeem(
                    subtotal = currentState.subtotal,
                    tax = currentState.tax,
                    availablePoints = pelanggan.point
                )
                updateState { it.copy(pointsToRedeem = pointsToRedeem) }
            } else {
                updateState { it.copy(pointsToRedeem = 0) }
            }
            recalculateTotalsInternal()
        }
    }

    /**
     * Recomputes how many points should be redeemed given the current cart
     * totals and the customer's available point balance. Used both by the
     * initial toggle and by cart mutators (add / update / remove / warehouse
     * switch) so that the displayed point discount stays in sync with the cart.
     * Otherwise a previously-computed `pointsToRedeem` becomes stale as the
     * cart changes and the discount displayed to the cashier no longer matches
     * what the service layer will actually apply (see SaleServiceImpl's own
     * re-cap against monetary amount and DB balance).
     *
     * Calculates max points that can be redeemed based on the amount remaining
     * after the promo discount. Without subtracting the promo discount,
     * customers would burn points covering an amount that's already been
     * discounted, with no additional benefit.
     */
    private suspend fun computePointsToRedeem(
        subtotal: Double,
        tax: Double,
        availablePoints: Int
    ): Int {
        val promoDiscount = promoService.calculateDiscount(subtotal)
        val maxDiscountNeeded = maxOf(0.0, subtotal + tax - promoDiscount)
        val pointsNeededForFullDiscount = (maxDiscountNeeded / AppConstants.POINT_REDEMPTION_VALUE).toInt()
        return minOf(availablePoints, pointsNeededForFullDiscount)
    }

    private fun recalculateTotals() {
        launchWithState { recalculateTotalsInternal() }
    }

    private suspend fun recalculateTotalsInternal() {
        val subtotal = currentState.cartItems.sumOf { it.totalPrice }
        val tax = subtotal * AppConstants.TAX_RATE
        val promoDiscount = promoService.calculateDiscount(subtotal)

        // Re-cap pointsToRedeem against the latest cart totals so the displayed
        // point discount cannot exceed the new remaining balance. Without this,
        // shrinking the cart after opting into redemption would keep the old
        // (larger) pointsToRedeem, and removing items would show a discount
        // larger than the remaining subtotal+tax-promoDiscount.
        val pointsToRedeem = if (currentState.isRedeemingPoints) {
            val pelanggan = currentState.selectedPelanggan
            if (pelanggan != null) {
                computePointsToRedeem(subtotal, tax, pelanggan.point)
            } else {
                0
            }
        } else {
            0
        }

        val pointDiscount = pointsToRedeem * AppConstants.POINT_REDEMPTION_VALUE
        val totalDiscount = promoDiscount + pointDiscount
        val total = maxOf(0.0, subtotal + tax - totalDiscount)

        updateState {
            it.copy(
                subtotal = subtotal,
                tax = tax,
                discount = totalDiscount,
                total = total,
                pointsToRedeem = pointsToRedeem
            )
        }
    }

    fun selectWarehouse(warehouseId: Long) {
        // Re-validate any existing cart items against the newly selected warehouse's
        // per-warehouse stock. Items added before the switch were validated against
        // the previous warehouse (see addProductToCart / updateCartItemQuantity), so
        // without this pass a cashier could carry over a cart that exceeds the new
        // warehouse's stock and only discover the mismatch at payment time when
        // SaleServiceImpl's per-warehouse validation rejects the sale.
        //
        // Strategy per item:
        //   - If stock in the new warehouse is 0 or unavailable, drop the item.
        //   - If stock is below the cart quantity, cap the cart quantity at the
        //     available stock so the line stays in the cart but is sellable.
        //   - Otherwise leave the line untouched.
        //
        // We surface a non-blocking message in `successMessage` (or `error` if
        // every line had to be dropped) so the cashier sees what changed.
        launchWithState {
            val previousWarehouseId = currentState.selectedWarehouseId
            if (previousWarehouseId == warehouseId) return@launchWithState

            val items = currentState.cartItems
            if (items.isEmpty()) {
                updateState { it.copy(selectedWarehouseId = warehouseId) }
                return@launchWithState
            }

            val adjustedItems = mutableListOf<CartItem>()
            val droppedNames = mutableListOf<String>()
            val cappedNames = mutableListOf<String>()

            for (item in items) {
                val stockResult = stokGudangRepository.getStock(item.product.id, warehouseId)
                val available = if (stockResult is Result.Success) {
                    stockResult.data?.quantity ?: 0
                } else {
                    0
                }

                when {
                    available <= 0 -> droppedNames.add(item.product.name)
                    available < item.quantity -> {
                        adjustedItems.add(item.updateQuantity(available))
                        cappedNames.add(item.product.name)
                    }
                    else -> adjustedItems.add(item)
                }
            }

            val notice = buildString {
                if (droppedNames.isNotEmpty()) {
                    append("Item dihapus karena stok kosong di gudang ini: ")
                    append(droppedNames.joinToString(", "))
                }
                if (cappedNames.isNotEmpty()) {
                    if (isNotEmpty()) append(". ")
                    append("Jumlah disesuaikan dengan stok gudang: ")
                    append(cappedNames.joinToString(", "))
                }
            }.takeIf { it.isNotBlank() }

            updateState {
                it.copy(
                    selectedWarehouseId = warehouseId,
                    cartItems = adjustedItems,
                    successMessage = notice ?: it.successMessage
                )
            }
            // Recompute subtotal/tax/discount/total and re-cap pointsToRedeem
            // against the adjusted cart so the displayed point discount matches
            // the new cart contents.
            recalculateTotalsInternal()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun setupSearch() {
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
                }
            }
        }
    }

    fun addProductToCart(product: Produk, quantity: Int = 1) {
        launchWithState {
            val currentState = currentState
            val existingItem = currentState.cartItems.find { it.product.id == product.id }
            val currentQtyInCart = existingItem?.quantity ?: 0

            // Validate against per-warehouse stock rather than the global
            // `product.stockQuantity` total. The sale will be created against
            // `currentState.selectedWarehouseId` and SaleServiceImpl validates
            // stock per-warehouse via stokGudangRepository.getStocks(...). Using
            // the global total here would let a cashier add items to the cart
            // that aren't actually available in the selected warehouse, only to
            // get a stock error at payment time.
            val stockResult = stokGudangRepository.getStock(
                product.id,
                currentState.selectedWarehouseId
            )
            val warehouseStock = if (stockResult is Result.Success) {
                stockResult.data?.quantity ?: 0
            } else {
                0
            }

            if (warehouseStock < currentQtyInCart + quantity) {
                updateState { it.copy(error = "Stok tidak mencukupi untuk ${product.name} di gudang ini") }
                return@launchWithState
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

            updateState {
                it.copy(
                    cartItems = updatedCartItems,
                    error = null
                )
            }
            recalculateTotalsInternal()
        }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(productId)
            return
        }

        launchWithState {
            val currentItems = currentState.cartItems
            val targetItem = currentItems.find { it.product.id == productId } ?: return@launchWithState

            // Validate against per-warehouse stock for the same reason as in
            // addProductToCart: the sale targets a specific warehouse and the
            // service-layer validation is per-warehouse. Checking the global
            // total here would mismatch the actual stock available for sale.
            val stockResult = stokGudangRepository.getStock(
                productId,
                currentState.selectedWarehouseId
            )
            val warehouseStock = if (stockResult is Result.Success) {
                stockResult.data?.quantity ?: 0
            } else {
                0
            }

            if (warehouseStock < newQuantity) {
                updateState { it.copy(error = "Stok tidak mencukupi untuk ${targetItem.product.name} di gudang ini") }
                return@launchWithState
            }

            val updatedCartItems = currentItems.map { item ->
                if (item.product.id == productId) {
                    item.updateQuantity(newQuantity)
                } else {
                    item
                }
            }

            updateState { it.copy(cartItems = updatedCartItems) }
            recalculateTotalsInternal()
        }
    }

    fun removeCartItem(productId: Long) {
        val updatedCartItems = currentState.cartItems.filter { it.product.id != productId }
        launchWithState {
            updateState { it.copy(cartItems = updatedCartItems) }
            recalculateTotalsInternal()
        }
    }

    fun clearCart() {
        updateState { currentState ->
            currentState.copy(
                cartItems = emptyList(),
                subtotal = 0.0,
                tax = 0.0,
                discount = 0.0,
                total = 0.0,
                error = null,
                successMessage = null,
                pointsToRedeem = 0,
                isRedeemingPoints = false
            )
        }
    }

    fun setPaymentMethod(method: String) {
        updateState { it.copy(paymentMethod = method) }
    }

    fun setDiscount(discount: Double) {
        updateState { currentState ->
            val newTotal = currentState.subtotal + currentState.tax - discount
            currentState.copy(
                discount = discount,
                total = maxOf(0.0, newTotal)
            )
        }
    }

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

            // Look up the cashier's open shift so the sale can be linked for shift
            // reconciliation. If none is open, the sale still proceeds (so a cashier
            // can serve customers even if they forgot to open a shift), but it will
            // NOT be counted in any shift's totalSales/expectedCash. We surface this
            // as a non-blocking warning in `successMessage` so the cashier knows to
            // open a shift before the next transaction.
            val openShift = saleService.getOpenShift(userId).getOrNull()
            val noShiftWarning = if (openShift == null) {
                "Peringatan: Tidak ada shift aktif. Penjualan ini tidak akan tercatat dalam rekonsiliasi shift."
            } else null

            val sale = Penjualan(
                saleDate = Date(),
                totalAmount = currentState.total,
                tax = currentState.tax,
                discount = currentState.discount,
                paymentMethod = try { PaymentMethod.valueOf(currentState.paymentMethod) } catch(e: Exception) { PaymentMethod.CASH },
                cashierId = userId,
                shiftId = openShift?.id,
                pelangganId = currentState.selectedPelanggan?.id,
                // Use the warehouse selected by the cashier rather than relying on
                // the entity default of 1L. With multi-warehouse purchases now
                // supported (see PurchaseServiceImpl.createPembelianWithWarehouse),
                // hardcoding warehouseId here would make stock added to non-default
                // warehouses unsellable through POS.
                warehouseId = currentState.selectedWarehouseId,
                pointsRedeemed = currentState.pointsToRedeem
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
                val message = if (noShiftWarning != null) {
                    "Pembayaran berhasil. $noShiftWarning"
                } else {
                    "Pembayaran berhasil"
                }
                updateState {
                    it.copy(
                        isProcessingPayment = false,
                        successMessage = message,
                        completedSaleId = completedSale.sale.id,
                        showReceiptDialog = true,
                        cartItems = emptyList(),
                        subtotal = 0.0,
                        tax = 0.0,
                        discount = 0.0,
                        total = 0.0,
                        selectedPelanggan = null,
                        pointsToRedeem = 0,
                        isRedeemingPoints = false
                    )
                }
            }.onFailure { e ->
                updateState { it.copy(isProcessingPayment = false, error = "Gagal memproses pembayaran: ${e.message}") }
            }
        }
    }

    fun startScanning() {
        updateState { it.copy(isScanning = true) }
    }

    fun stopScanning() {
        updateState { it.copy(isScanning = false) }
    }

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
