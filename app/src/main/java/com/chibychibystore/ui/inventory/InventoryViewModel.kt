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
 * Represents the distinct states the UI can be in.
 */
sealed interface InventoryUiState {
    data object Loading : InventoryUiState

    data class Success(
        val products: List<Product>,
        val searchQuery: String = "",
        val lowStockProducts: List<Product> = emptyList()
    ) : InventoryUiState

    data class Error(
        val message: String,
        val searchQuery: String = "" // Preserve search query on error
    ) : InventoryUiState
}

/**
 * ViewModel for Inventory Management Screen
 * Refactored to use standard MVI/UDF pattern with sealed interface state.
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

    /**
     * Public immutable state flow for UI consumption
     */
    val uiState: StateFlow<InventoryUiState> = combine(
        _productsFlow,
        _lowStockFlow,
        _searchQuery
    ) { products, lowStock, query ->
        InventoryUiState.Success(
            products = products,
            searchQuery = query,
            lowStockProducts = lowStock
        )
    }.catch { e ->
        // Convert stream errors to Error state, preserving current query if possible (accessed via side channel or just empty)
        // Since catch handles the upstream, we don't have easy access to the latest query emission here without extra state.
        // However, we can use the current value of _searchQuery
        emit(InventoryUiState.Error(e.message ?: "Unknown error occurred", _searchQuery.value))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState.Loading
    )

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }
}
