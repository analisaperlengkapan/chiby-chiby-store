package com.chibychibystore.ui.inventory

import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
<<<<<<< HEAD
import com.chibychibystore.ui.base.BaseViewModel
import com.chibychibystore.ui.base.UiState
=======
>>>>>>> feat/ui-overhaul
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
<<<<<<< HEAD
 * UI State for Inventory Screen.
 */
data class InventoryUiState(
    val products: List<Produk> = emptyList(),
    val lowStockProducts: List<Produk> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
) : UiState
=======
 * Standardized UI State for Inventory Screen.
 */
data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
>>>>>>> feat/ui-overhaul

/**
 * ViewModel for Inventory Management Screen
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productService: ProductService
) : BaseViewModel<InventoryUiState>(InventoryUiState()) {

    private val _searchQuery = MutableStateFlow("")

<<<<<<< HEAD
    init {
        setupDataStreams()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun setupDataStreams() {
        val productsFlow = _searchQuery
            .debounce(300L)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    productService.observeProduks()
                } else {
                    productService.observeSearchProduks(query)
                }
            }
            .catch { emit(emptyList()) }

        val lowStockFlow = productService.observeLowStockProduks()
            .catch { emit(emptyList()) }

        viewModelScope.launch {
            combine(
                productsFlow,
                lowStockFlow,
                _searchQuery
            ) { products, lowStock, query ->
                InventoryUiState(
                    products = products,
                    lowStockProducts = lowStock,
                    searchQuery = query
                )
            }.collect { newState ->
                updateState { newState }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        updateState { it.copy(error = null) }
=======
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
>>>>>>> feat/ui-overhaul
    }
}
