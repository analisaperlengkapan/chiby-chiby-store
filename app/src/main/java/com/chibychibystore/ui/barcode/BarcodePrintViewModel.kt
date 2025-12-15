package com.chibychibystore.ui.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.printer.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 * ViewModel untuk Barcode Print Screen
 * Mengelola pemilihan produk dan pencetakan label barcode
 */
@HiltViewModel
class BarcodePrintViewModel @Inject constructor(
    private val productService: ProductService,
    private val printerService: PrinterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodePrintUiState())
    val uiState: StateFlow<BarcodePrintUiState> = _uiState

    init {
        loadProducts()
    }

    /**
     * Load semua produk untuk pemilihan
     */
    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProducts = true, error = null)

            try {
                val result = productService.getProducts()
                result.fold(
                    onSuccess = { products ->
                        _uiState.value = _uiState.value.copy(
                            products = products,
                            isLoadingProducts = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingProducts = false,
                            error = exception.message ?: "Gagal memuat produk"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProducts = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Update search query dan filter produk
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterProducts(query)
    }

    /**
     * Filter produk berdasarkan search query
     */
    private fun filterProducts(query: String) {
        val allProducts = _uiState.value.products
        val filteredProducts = if (query.isBlank()) {
            allProducts
        } else {
            allProducts.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                (product.barcode?.contains(query, ignoreCase = true) ?: false)
            }
        }
        _uiState.value = _uiState.value.copy(products = filteredProducts)
    }

    /**
     * Pilih produk untuk dicetak labelnya
     */
    fun selectProduct(product: Produk) {
        _uiState.value = _uiState.value.copy(selectedProduct = product)
    }

    /**
     * Pilih ukuran label
     */
    fun selectLabelSize(size: LabelSize) {
        _uiState.value = _uiState.value.copy(selectedSize = size)
    }

    /**
     * Update jumlah label yang akan dicetak
     */
    fun updateQuantity(quantity: Int) {
        _uiState.value = _uiState.value.copy(quantity = quantity.coerceIn(1, 99))
    }

    /**
     * Cetak label barcode
     */
    fun printLabels() {
        val product = _uiState.value.selectedProduct ?: return
        val quantity = _uiState.value.quantity

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, error = null)

            try {
                val result = printerService.printBarcodeLabels(
                    product = product,
                    labelSize = _uiState.value.selectedSize,
                    quantity = quantity
                )

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isPrinting = false)
                        // Could show success message here
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isPrinting = false,
                            error = exception.message ?: "Gagal mencetak label"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
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
     * Reset state
     */
    fun resetState() {
        _uiState.value = BarcodePrintUiState()
        loadProducts()
    }
}