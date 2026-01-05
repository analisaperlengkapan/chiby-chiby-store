package com.chibychibystore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.error.ChibyChibyException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Inventory Screen
 *
 * Menyimpan state lengkap dari inventory screen termasuk:
 * - Daftar produk yang ditampilkan
 * - Query pencarian aktif
 * - Status loading dan error
 * - Produk dengan stok rendah untuk alerts
 *
 * @property products List produk yang sedang ditampilkan (filtered by search)
 * @property searchQuery Query pencarian yang aktif
 * @property isLoading Flag untuk menampilkan loading indicator
 * @property error Message error jika ada kegagalan operasi
 * @property lowStockProducts List produk dengan stok rendah untuk dashboard alerts
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
 * - Loading dan display daftar produk
 * - Real-time search dan filtering
 * - Low stock alerts dan monitoring
 * - Reactive UI updates melalui StateFlow
 * - Error handling dan user feedback
 *
 * **Architecture Pattern:**
 * - MVVM dengan reactive state management
 * - StateFlow untuk immutable state updates
 * - Coroutine-based async operations
 * - Dependency injection melalui Hilt
 *
 * **Key Features:**
 * - Real-time product list updates
 * - Instant search dengan case-insensitive matching
 * - Low stock monitoring untuk inventory alerts
 * - Error recovery dan user-friendly messages
 * - Memory-efficient filtering tanpa re-queries
 *
 * **Integration Points:**
 * - [ProductService] untuk data operations
 * - InventoryScreen Composable untuk UI binding
 * - Navigation untuk product detail screens
 *
 * **Threading:**
 * - viewModelScope untuk lifecycle-aware coroutines
 * - Main thread untuk UI updates
 * - IO dispatcher untuk service calls (handled by service)
 *
 * @property productService Service untuk operasi produk
 *
 * @constructor Inject dependencies melalui Hilt
 * @param productService Instance ProductService yang diinject
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see InventoryUiState
 * @see ProductService
 * @see InventoryScreen
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val productService: ProductService
) : ViewModel() {

    /**
     * Internal mutable state flow untuk state management
     * Private untuk encapsulation, exposed sebagai immutable StateFlow
     */
    private val _uiState = MutableStateFlow(InventoryUiState())

    /**
     * Public immutable state flow untuk UI consumption
     * UI akan react otomatis setiap kali state berubah
     */
    val uiState: StateFlow<InventoryUiState> = _uiState

    init {
        // Load initial data saat ViewModel dibuat
        loadProducts()
        loadLowStockProducts()
    }

    /**
     * Load semua produk untuk inventory display - CORE LOADING METHOD
     *
     * **Business Logic Flow:**
     * 1. **State Preparation**: Set loading state, clear previous errors
     * 2. **Data Fetching**: Call ProductService.getProducts() untuk semua produk
     * 3. **State Update**: Update products list, clear loading state
     * 4. **Reactive Setup**: Enable observeProducts() untuk real-time updates
     * 5. **Error Handling**: Comprehensive error handling dengan user feedback
     *
     * **Data Loading Strategy:**
     * - Full product list loading (no pagination for inventory management)
     * - One-time initial load dengan reactive updates thereafter
     * - Memory-efficient untuk typical retail inventory sizes
     * - Cached data access melalui ProductService
     *
     * **State Management:**
     * - Loading state untuk UI feedback
     * - Error state untuk failure handling
     * - Products list untuk UI display
     * - Reactive updates setup untuk real-time sync
     *
     * **Integration Points:**
     * - ProductService.getProducts() untuk data access
     * - observeProducts() untuk reactive updates
     * - InventoryScreen untuk UI binding
     * - Error handling untuk user feedback
     *
     * **Performance Considerations:**
     * - Async loading untuk non-blocking UI
     * - Efficient state updates (minimal emissions)
     * - Memory management untuk large product lists
     * - Lifecycle-aware coroutine cancellation
     *
     * **Error Scenarios:**
     * - Network failures (though offline-first)
     * - Database corruption
     * - Permission issues
     * - Memory constraints
     *
     * **UI Integration:**
     * - Loading spinner selama fetch
     * - Error messages untuk failures
     * - Product list refresh setelah success
     * - Real-time updates setelah initial load
     *
     * **Usage Example:**
     * ```kotlin
     * // Initial load di ViewModel init
     * init {
     *     loadProducts()
     * }
     *
     * // Manual refresh dari UI
     * viewModel.loadProducts()
     *
     * // Handle loading state
     * if (uiState.isLoading) showLoading()
     * ```
     *
     * @throws Exception jika ProductService gagal
     *
     * @see ProductService.getProducts
     * @see observeProducts
     * @see InventoryUiState.isLoading
     * @see InventoryUiState.products
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = productService.getProducts()
                result.onSuccess { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoading = false
                    )
                    // Setup reactive updates untuk real-time UI
                    observeProducts()
                }.onFailure { exception ->
                    val errorMessage = if (exception is ChibyChibyException) {
                        exception.message
                    } else {
                        "Gagal memuat produk: ${exception.message}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan sistem: ${e.message}"
                )
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productService.observeProducts()
                .catch { e ->
                    // Handle flow exceptions
                    val errorMessage = if (e is ChibyChibyException) {
                        e.message
                    } else {
                        "Gagal mengamati perubahan produk: ${e.message}"
                    }
                    _uiState.value = _uiState.value.copy(
                        error = errorMessage
                    )
                }
                .collectLatest { products ->
                    // Terapkan client-side filtering untuk instant search
                    val filteredProducts = if (_uiState.value.searchQuery.isBlank()) {
                        products
                    } else {
                        products.filter { product ->
                            product.name.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                            product.barcode?.contains(_uiState.value.searchQuery, ignoreCase = true) ?: false
                        }
                    }

                    _uiState.value = _uiState.value.copy(products = filteredProducts)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        // Re-trigger filtering dengan query baru
        // observeProducts() akan handle filtering otomatis
        val filteredProducts = if (query.isBlank()) {
            // Jika query kosong, load ulang semua produk
            viewModelScope.launch {
                    try {
                    val result = productService.getProducts()
                    result.onSuccess { products ->
                        _uiState.value = _uiState.value.copy(products = products)
                    }.onFailure { /* Ignore error saat filtering */ }
                } catch (e: Exception) {
                    // Ignore error saat filtering
                }
            }
            emptyList()
        } else {
            _uiState.value.products.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                (product.barcode?.contains(query, ignoreCase = true) ?: false)
            }
        }

        if (query.isNotBlank()) {
            _uiState.value = _uiState.value.copy(products = filteredProducts)
        }
    }

    fun searchProducts(query: String) {
        if (query.isBlank()) {
            loadProducts()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = productService.searchProducts(query)
                result.onSuccess { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        searchQuery = query,
                        isLoading = false
                    )
                }.onFailure { exception ->
                    val errorMessage = if (exception is ChibyChibyException) {
                        exception.message
                    } else {
                        "Gagal mencari produk: ${exception.message}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Terjadi kesalahan sistem: ${e.message}"
                )
            }
        }
    }

    /**
     * Load produk dengan stok rendah untuk inventory alerts - PRIVATE UTILITY METHOD
     *
     * **Business Logic Flow:**
     * 1. **Service Call**: Call ProductService.getLowStockProducts()
     * 2. **State Update**: Update lowStockProducts list di UI state
     * 3. **Silent Operation**: No loading states, silent failures
     * 4. **Background Processing**: Non-blocking inventory monitoring
     *
     * **Low Stock Logic:**
     * - Products below minimum stock threshold
     * - Configurable low stock alerts
     * - Real-time inventory monitoring
     * - Dashboard alerts untuk reordering
     *
     * **State Management:**
     * - Low stock products list update
     * - No loading/error states (silent operation)
     * - Reactive UI updates untuk alerts
     * - Memory efficient untuk alert data
     *
     * **Integration Points:**
     * - ProductService.getLowStockProducts() untuk alert data
     * - InventoryScreen untuk alert display
     * - Dashboard untuk low stock warnings
     * - Notification system untuk alerts
     *
     * **Performance Considerations:**
     * - Async loading untuk non-blocking UI
     * - Silent failures (don't interrupt main flow)
     * - Efficient for frequent monitoring
     * - Minimal state emissions
     *
     * **Error Handling:**
     * - Silent failures (non-critical feature)
     * - No user disruption untuk low stock checks
     * - Graceful degradation jika service unavailable
     * - Logging untuk debugging purposes
     *
     * **UI Integration:**
     * - Alert badges pada inventory screen
     * - Dashboard low stock indicators
     * - Notification triggers
     * - Reorder suggestions
     *
     * **Usage Example:**
     * ```kotlin
     * // Called automatically dari loadProducts
     * loadLowStockProducts() // Background monitoring
     *
     * // UI displays alerts
     * if (uiState.lowStockProducts.isNotEmpty()) {
     *     showLowStockAlert(uiState.lowStockProducts.size)
     * }
     * ```
     *
     * @see ProductService.getLowStockProducts
     * @see InventoryUiState.lowStockProducts
     * @see loadProducts
     */
    private fun loadLowStockProducts() {
        viewModelScope.launch {
            try {
                val result = productService.getLowStockProducts()
                result.onSuccess { lowStockProducts ->
                    _uiState.value = _uiState.value.copy(lowStockProducts = lowStockProducts)
                }.onFailure { /* Ignore error untuk low stock */ }
            } catch (e: Exception) {
                // Ignore error untuk low stock
            }
        }
    }

    /**
     * Clear error message dari UI state
     *
     * **Business Logic:**
     * - Reset error state untuk clean UI
     * - Allow user to retry operations
     * - Clear error indicators
     *
     * **State Management:**
     * - Set error = null
     * - Maintains other state intact
     * - Triggers UI error clearing
     *
     * **UI Integration:**
     * - Hide error messages/snackbars
     * - Reset error styling
     * - Enable retry actions
     *
     * **Usage Example:**
     * ```kotlin
     * // Clear error on user action
     * viewModel.clearError()
     *
     * // UI error state reset
     * if (uiState.error == null) {
     *     // Hide error UI
     * }
     * ```
     *
     * @see InventoryUiState.error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Refresh semua inventory data - COMPLETE DATA REFRESH
     *
     * **Business Logic Flow:**
     * 1. **Product Refresh**: Call loadProducts() untuk fresh product list
     * 2. **Low Stock Refresh**: Call loadLowStockProducts() untuk updated alerts
     * 3. **State Synchronization**: Ensure all data is current
     * 4. **UI Update**: Trigger complete UI refresh
     *
     * **Refresh Strategy:**
     * - Complete data reload untuk accuracy
     * - Both products dan low stock alerts
     * - User-initiated refresh action
     * - Force synchronization dengan database
     *
     * **State Management:**
     * - Triggers loading states
     * - Updates all product-related data
     * - Refreshes low stock monitoring
     * - Maintains search/filter state
     *
     * **Integration Points:**
     * - loadProducts() untuk main data refresh
     * - loadLowStockProducts() untuk alert refresh
     * - InventoryScreen untuk pull-to-refresh
     * - Manual refresh buttons
     *
     * **Performance Considerations:**
     * - Multiple async operations
     * - Parallel loading untuk efficiency
     * - User-triggered (not automatic)
     * - Complete data synchronization
     *
     * **UI Integration:**
     * - Pull-to-refresh gestures
     * - Manual refresh buttons
     * - Loading indicators
     * - Data update feedback
     *
     * **Usage Example:**
     * ```kotlin
     * // Pull-to-refresh dari UI
     * viewModel.refresh()
     *
     * // Manual refresh button
     * viewModel.refresh()
     *
     * // Complete data sync
     * viewModel.refresh() // Products + alerts
     * ```
     *
     * @see loadProducts
     * @see loadLowStockProducts
     */
    fun refresh() {
        loadProducts()
        loadLowStockProducts()
    }
}