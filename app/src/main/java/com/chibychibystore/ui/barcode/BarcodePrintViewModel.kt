package com.chibychibystore.ui.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.printer.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Barcode Print Screen
 */
data class BarcodePrintUiState(
    val products: List<Produk> = emptyList(),
    val selectedProduct: Produk? = null,
    val searchQuery: String = "",
    val selectedSize: LabelSize = LabelSize.MEDIUM,
    val quantity: Int = 1,
    val isLoadingProducts: Boolean = false,
    val isPrinting: Boolean = false,
    val error: String? = null
)

/**
 * Helper data class to group user inputs for combine
 */
private data class UserInputs(
    val selectedProduct: Produk?,
    val searchQuery: String,
    val selectedSize: LabelSize,
    val quantity: Int,
    val isPrinting: Boolean
)

/**
 * ViewModel untuk Barcode Print Screen
 * Mengelola pemilihan product dan pencetakan label barcode
 */
@HiltViewModel
class BarcodePrintViewModel @Inject constructor(
    private val productService: ProductService,
    private val printerService: PrinterService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedProduct = MutableStateFlow<Produk?>(null)
    private val _selectedSize = MutableStateFlow(LabelSize.MEDIUM)
    private val _quantity = MutableStateFlow(1)
    private val _isPrinting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoadingProducts = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val _productsFlow = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            _isLoadingProducts.value = true
            if (query.isBlank()) {
                productService.observeProduks()
            } else {
                productService.observeSearchProduks(query)
            }
        }
        .onEach { _isLoadingProducts.value = false }
        .catch {
            _isLoadingProducts.value = false
            _error.value = "Gagal memuat product: ${it.message}"
            emit(emptyList())
        }

    // Combine user inputs first to avoid exceeding combine argument limit (max 5)
    private val _userInputs = combine(
        _selectedProduct,
        _searchQuery,
        _selectedSize,
        _quantity,
        _isPrinting
    ) { selectedProduct, searchQuery, selectedSize, quantity, isPrinting ->
        UserInputs(selectedProduct, searchQuery, selectedSize, quantity, isPrinting)
    }

    val uiState: StateFlow<BarcodePrintUiState> = combine(
        _productsFlow,
        _userInputs,
        _isLoadingProducts,
        _error
    ) { products, userInputs, isLoadingProducts, error ->
        BarcodePrintUiState(
            products = products,
            selectedProduct = userInputs.selectedProduct,
            searchQuery = userInputs.searchQuery,
            selectedSize = userInputs.selectedSize,
            quantity = userInputs.quantity,
            isLoadingProducts = isLoadingProducts,
            isPrinting = userInputs.isPrinting,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BarcodePrintUiState(isLoadingProducts = true)
    )

    /**
     * Update search query dan filter product
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Pilih product untuk dicetak labelnya
     */
    fun selectProduct(product: Produk) {
        _selectedProduct.value = product
    }

    /**
     * Pilih ukuran label
     */
    fun selectLabelSize(size: LabelSize) {
        _selectedSize.value = size
    }

    /**
     * Update jumlah label yang akan dicetak
     */
    fun updateQuantity(quantity: Int) {
        _quantity.value = quantity.coerceIn(1, 99)
    }

    /**
     * Cetak label barcode
     */
    fun printLabels() {
        val product = _selectedProduct.value ?: return
        val quantity = _quantity.value
        val size = _selectedSize.value

        viewModelScope.launch {
            _isPrinting.value = true
            _error.value = null

            try {
                val result = printerService.printBarcodeLabels(
                    product = product,
                    labelSize = size,
                    quantity = quantity
                )

                result.onSuccess {
                    _isPrinting.value = false
                }.onFailure { exception ->
                    _isPrinting.value = false
                    _error.value = exception.message ?: "Gagal mencetak label"
                }
            } catch (e: Exception) {
                _isPrinting.value = false
                _error.value = "Terjadi kesalahan: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset state
     */
    fun resetState() {
        _selectedProduct.value = null
        _searchQuery.value = ""
        _selectedSize.value = LabelSize.MEDIUM
        _quantity.value = 1
        _error.value = null
        // Products will reload automatically due to search query change
    }
}
