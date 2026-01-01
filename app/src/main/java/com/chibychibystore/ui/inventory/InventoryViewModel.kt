package com.chibychibystore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI State untuk Inventory Screen
 *
 * Menyimpan state lengkap dari inventory screen termasuk:
 * - Daftar produk yang ditampilkan
 * - Query pencarian aktif
 * - Status loading dan error
 * - Produk dengan stok rendah untuk alerts
 */
data class InventoryUiState(
    val products: List<Produk> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val lowStockProducts: List<Produk> = emptyList()
)

/**
 * ViewModel untuk Inventory Management Screen
 *
 * Mengelola state dan business logic untuk inventory screen dengan fitur:
 * - Reactive search dan filtering
 * - Low stock monitoring via reactive stream
 * - Integration dengan ProductService
 *
 * **Architecture Pattern:**
 * - MVVM dengan reactive state management (Unidirectional Data Flow)
 * - StateFlow derived from combined flows using `stateIn`
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productService: ProductService
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")

    // Loading state (managed manually for now as flows from Room are instant/always loaded)
    private val _isLoading = MutableStateFlow(true)

    // Error state
    private val _error = MutableStateFlow<String?>(null)

    // Derived Product Stream
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            productService.observeProducts()
        } else {
            productService.observeSearchProducts(query)
        }
    }.catch { e ->
        _error.value = "Gagal memuat produk: ${e.message}"
        emit(emptyList())
    }

    // Low Stock Stream
    private val _lowStockFlow = productService.observeLowStockProducts()
        .catch { e ->
            // Silent error for low stock alert, or log it
            emit(emptyList())
        }

    /**
     * Public immutable state flow untuk UI consumption
     *
     * Combines:
     * - Products stream (filtered by query)
     * - Low stock stream
     * - Search query
     * - Loading/Error states
     */
    val uiState: StateFlow<InventoryUiState> = combine(
        _productsFlow,
        _lowStockFlow,
        _searchQuery,
        _isLoading,
        _error
    ) { products, lowStock, query, isLoading, error ->
        InventoryUiState(
            products = products,
            searchQuery = query,
            isLoading = false, // Flow emitted, so we are not loading anymore
            error = error,
            lowStockProducts = lowStock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Refresh logic (Optional for local DB, but kept for compatibility/re-sync)
     * For Room, flows update automatically, so this might just clear errors.
     */
    fun refresh() {
        _error.value = null
        // If we had a network fetch, we would trigger it here.
    }
}
