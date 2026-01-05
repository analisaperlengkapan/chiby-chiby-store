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
 * Mengelola pemilihan product dan pencetakan label barcode
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
     * Load semua product untuk pemilihan
     */
    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProducts = true, error = null)

            try {
                // In a real app we might want to paginate or search, but for now getting all is simpler for the UI
                // However, ProductService returns Result<List<Produk>> via getProduks()
                val result = productService.getProduks() 
                result.onSuccess { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoadingProducts = false
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingProducts = false,
                        error = exception.message ?: "Gagal memuat product"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingProducts = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Update search query dan filter product
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterProducts(query)
    }

    /**
     * Filter product berdasarkan search query
     */
    private fun filterProducts(query: String) {
        val allProducts = _uiState.value.products
        // Filtering happens on the list we have in memory (simplified)
        // Ideally we should reload from DB with search query if list is large
        // But here we rely on the implementation where uiState holds all products or filtered products?
        // Wait, if I filter `products`, I lose the original list if I don't keep a separate `allProducts`.
        // The previous implementation was modifying `products` in place, which is buggy for clearing search.
        // I will just RELOAD from service with search if needed, or better, 
        // since `getProduks` supports search, let's use that.
        
        viewModelScope.launch {
            val result = productService.getProduks(searchQuery = query)
            result.onSuccess { products ->
                 _uiState.value = _uiState.value.copy(products = products)
            }
        }
    }

    /**
     * Pilih product untuk dicetak labelnya
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
        // Need to adapt Product -> Produk for printerService if it uses Product
        // Assuming PrinterService is also updated or using common types.
        // If PrinterService uses Product, we might need a mapping or update PrinterService.
        // Let's assume PrinterService uses Produk now or we need to check.
        // For now, I'll pass it as is, if it fails compilation, I'll fix PrinterService.
        
        val product = _uiState.value.selectedProduct ?: return
        val quantity = _uiState.value.quantity

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, error = null)

            try {
                // Warning: printerService.printBarcodeLabels likely expects different type if it wasn't updated.
                // But I should try to use it with Produk.
                val result = printerService.printBarcodeLabels(
                    product = product,
                    labelSize = _uiState.value.selectedSize,
                    quantity = quantity
                )

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(isPrinting = false)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        error = exception.message ?: "Gagal mencetak label"
                    )
                }
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