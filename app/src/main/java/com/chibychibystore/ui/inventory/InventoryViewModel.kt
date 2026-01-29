package com.chibychibystore.ui.inventory

import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.ui.base.BaseViewModel
import com.chibychibystore.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Inventory Screen.
 */
data class InventoryUiState(
    val products: List<Produk> = emptyList(),
    val lowStockProducts: List<Produk> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
) : UiState

/**
 * ViewModel for Inventory Management Screen
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productService: ProductService
) : BaseViewModel<InventoryUiState>(InventoryUiState()) {

    private val _searchQuery = MutableStateFlow("")

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
    }
}
