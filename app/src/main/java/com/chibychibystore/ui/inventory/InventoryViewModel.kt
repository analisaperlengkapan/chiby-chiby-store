package com.chibychibystore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.service.ProductService
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Standardized UI State for Inventory Screen.
 */
data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
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

    // Search query state
    private val _searchQuery = MutableStateFlow("")

    // Derived Product Stream
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val _productsFlow = _searchQuery
        .debounce(300L) // Debounce search input
        .flatMapLatest { query ->
            if (query.isBlank()) {
                productService.observeProducts()
            } else {
                productService.observeSearchProducts(query)
            }
        }

    // Low Stock Stream
    private val _lowStockFlow = productService.observeLowStockProducts()
        .catch { emit(emptyList()) }

    // Loading State
    private val _isLoading = MutableStateFlow(false) 
    // Note: To truly track loading from productService, we might need a different approach or wrapper. 
    // For now, we assume fast local DB or acceptable delay. 
    // If strict loading state is needed, we'd wrap flows.

    /**
     * Public immutable state flow for UI consumption
     */
    val uiState: StateFlow<InventoryUiState> = combine(
        _productsFlow,
        _lowStockFlow,
        _searchQuery
    ) { products, lowStock, query ->
        InventoryUiState(
            products = products,
            searchQuery = query,
            lowStockProducts = lowStock,
            isLoading = false, // Set to true when mapped if needed
            error = null
        )
    }.catch { e ->
        emit(InventoryUiState(error = e.message ?: "Unknown error occurred", searchQuery = _searchQuery.value))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState(isLoading = true)
    )

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }
}
