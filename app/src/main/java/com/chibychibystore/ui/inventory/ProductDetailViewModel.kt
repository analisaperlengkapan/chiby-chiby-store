package com.chibychibystore.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Category
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.service.ProductService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Product Detail Screen
 */
data class ProductDetailUiState(
    val product: Product? = null,
    val categories: List<Category> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel untuk Product Detail Screen
 * Mengelola create/edit/delete product
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productService: ProductService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    private val productId: String? = savedStateHandle["productId"]

    init {
        if (productId != null) {
            loadProduct(productId)
        } else {
            // Mode create baru
            _uiState.value = _uiState.value.copy(isEditing = true)
        }
    }

    /**
     * Load product berdasarkan ID
     */
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = productService.getProduct(productId)
                result.onSuccess { product ->
                    _uiState.value = _uiState.value.copy(
                        product = product,
                        isLoading = false,
                        isEditing = false
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Gagal memuat product"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Simpan product (create atau update)
     */
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, successMessage = null)

            try {
                val result = if (productId != null) {
                    // Update existing product
                    productService.updateProduct(product)
                } else {
                    // Create new product
                    productService.createProduct(product)
                }

                result.onSuccess { savedProduct ->
                    _uiState.value = _uiState.value.copy(
                        product = savedProduct,
                        isSaving = false,
                        isEditing = false,
                        successMessage = if (productId != null) "Product berhasil diperbarui" else "Product berhasil dibuat"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = exception.message ?: "Gagal menyimpan product"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Update stok product
     */
    fun updateStock(quantity: Int) {
        val currentProduct = _uiState.value.product ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, successMessage = null)

            try {
                val result = productService.updateStock(currentProduct.id.toString(), quantity)
                result.onSuccess {
                    // Reload product untuk mendapatkan data terbaru
                    loadProduct(currentProduct.id.toString())
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Stok berhasil diperbarui"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = exception.message ?: "Gagal memperbarui stok"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Hapus product
     */
    fun deleteProduct() {
        val currentProduct = _uiState.value.product ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, successMessage = null)

            try {
                val result = productService.deleteProduct(currentProduct.id.toString())
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Product berhasil dihapus"
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = exception.message ?: "Gagal menghapus product"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle edit mode
     */
    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(
            isEditing = !_uiState.value.isEditing,
            error = null,
            successMessage = null
        )
    }

    /**
     * Cancel editing
     */
    fun cancelEdit() {
        if (productId != null) {
            // Reload original product data
            loadProduct(productId)
        } else {
            // For new product, just exit edit mode
            _uiState.value = _uiState.value.copy(isEditing = false)
        }
    }

    /**
     * Update product in local UI state (not persisted) — used by UI form edits
     */
    fun updateLocalProduct(updatedProduct: Product) {
        _uiState.value = _uiState.value.copy(product = updatedProduct)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Validate product data sebelum save
     */
    fun validateProduct(product: Product): String? {
        if (product.name.isBlank()) {
            return "Nama product tidak boleh kosong"
        }
        if (product.barcode.isNullOrBlank()) {
            return "Barcode product tidak boleh kosong"
        }
        if (product.costPrice <= 0) {
            return "Harga beli harus lebih dari 0"
        }
        if (product.sellingPrice <= 0) {
            return "Harga jual harus lebih dari 0"
        }
        if (product.sellingPrice <= product.costPrice) {
            return "Harga jual harus lebih besar dari harga beli"
        }
        if (product.stockQuantity < 0) {
            return "Stok tidak boleh negatif"
        }
        return null // Valid
    }
}