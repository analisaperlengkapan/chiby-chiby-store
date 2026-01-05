package com.chibychibystore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.error.ChibyChibyException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * UI State for Inventory Screen.
 */
data class InventoryUiState(
    val products: List<Produk> = emptyList(),
    val lowStockProducts: List<Produk> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Inventory Management Screen
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productService: ProductService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val _productsFlow = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                productService.observeProduks()
            } else {
                productService.observeSearchProduks(query)
            }
        }

    private val _lowStockFlow = productService.observeLowStockProduks()
        .catch { emit(emptyList()) }

    val uiState: StateFlow<InventoryUiState> = combine(
        _productsFlow,
        _lowStockFlow,
        _searchQuery,
        _isLoading,
        _error
    ) { products, lowStock, query, loading, error ->
        InventoryUiState(
            products = products,
            lowStockProducts = lowStock,
            searchQuery = query,
            isLoading = loading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        // Since we are using observe flows, refresh might just be a no-op or re-triggering the flows if needed.
        // For now, these flows are reactive, so they update automatically.
    }
}
