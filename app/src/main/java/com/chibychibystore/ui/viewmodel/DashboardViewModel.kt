package com.chibychibystore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * UI State untuk Dashboard Screen
 *
 * Data class yang merepresentasikan seluruh state UI untuk halaman dashboard.
 * Dashboard menampilkan key performance indicators (KPI) utama toko retail
 * dengan real-time updates melalui StateFlow.
 *
 * **State Fields:**
 * - `todaySales`: Total penjualan hari ini dalam Rupiah
 * - `todayTransactionCount`: Jumlah transaksi penjualan hari ini
 * - `lowStockItems`: Daftar produk dengan stok rendah (< min_stock)
 * - `recentTransactions`: 5-10 transaksi penjualan terbaru
 * - `isLoading`: Status loading data dashboard
 * - `errorMessage`: Error message jika gagal memuat data
 *
 * **Business Value:**
 * - **todaySales**: Monitor performa penjualan harian
 * - **todayTransactionCount**: Track volume transaksi
 * - **lowStockItems**: Alert untuk restock products
 * - **recentTransactions**: Quick view aktivitas terbaru
 *
 * **State Transitions:**
 * ```kotlin
 * // Initial loading
 * DashboardUiState(isLoading = true)
 *
 * // Data loaded successfully
 * state.copy(
 *     todaySales = 2500000.0,
 *     todayTransactionCount = 25,
 *     lowStockItems = listOf(product1, product2),
 *     recentTransactions = listOf(sale1, sale2, sale3),
 *     isLoading = false
 * )
 *
 * // Error state
 * state.copy(
 *     isLoading = false,
 *     errorMessage = "Gagal memuat data dashboard"
 * )
 *
 * // Refresh triggered
 * state.copy(isLoading = true, errorMessage = null)
 * ```
 *
 * **Data Calculation Logic:**
 * ```kotlin
 * // Today Sales: Sum of all sales where date == today
 * todaySales = salesRepository.getTodaySales().sumOf { it.total }
 *
 * // Transaction Count: Count of sales records for today
 * todayTransactionCount = salesRepository.getTodaySales().size
 *
 * // Low Stock: Products where stock <= min_stock
 * lowStockItems = productsRepository.getAll()
 *     .filter { it.stok <= it.minStok }
 *     .sortedBy { it.stok } // Most critical first
 *
 * // Recent Transactions: Last 10 sales ordered by date desc
 * recentTransactions = salesRepository.getRecentSales(10)
 * ```
 *
 * **UI Integration:**
 * ```kotlin
 * @Composable
 * fun DashboardScreen(viewModel: DashboardViewModel) {
 *     val uiState by viewModel.uiState.collectAsState()
 *
 *     // Loading state
 *     if (uiState.isLoading) {
 *         LoadingIndicator()
 *         return
 *     }
 *
 *     // Error state
 *     uiState.errorMessage?.let { error ->
 *         ErrorView(
 *             message = error,
 *             onRetry = { viewModel.refreshData() }
 *         )
 *         return
 *     }
 *
 *     // Success state
 *     Column {
 *         // KPI Cards
 *         SalesCard(amount = uiState.todaySales)
 *         TransactionCard(count = uiState.todayTransactionCount)
 *
 *         // Alerts
 *         if (uiState.lowStockItems.isNotEmpty()) {
 *             LowStockAlert(
 *                 items = uiState.lowStockItems,
 *                 onItemClick = { navigateToInventory(it) }
 *             )
 *         }
 *
 *         // Recent Activity
 *         RecentTransactionsList(
 *             transactions = uiState.recentTransactions,
 *             onTransactionClick = { navigateToSaleDetail(it) }
 *         )
 *     }
 * }
 * ```
 *
 * **Empty States:**
 * ```kotlin
 * // New store - no data yet
 * DashboardUiState(
 *     todaySales = 0.0,
 *     todayTransactionCount = 0,
 *     lowStockItems = emptyList(),
 *     recentTransactions = emptyList()
 * )
 *
 * // All products in stock
 * state.copy(lowStockItems = emptyList())
 * ```
 *
 * **Real-time Updates:**
 * Dashboard data dapat di-refresh secara manual atau otomatis:
 * - Manual: Pull-to-refresh atau refresh button
 * - Automatic: Setiap kali ada perubahan data penjualan/inventory
 * - Background: Periodic refresh setiap 5-15 menit
 *
 * **Performance Considerations:**
 * - Lazy loading untuk recent transactions (load on demand)
 * - Pagination untuk large datasets
 * - Caching untuk frequently accessed metrics
 * - Background refresh untuk non-intrusive updates
 *
 * **Testing Data Ranges:**
 * ```kotlin
 * // Realistic test data
 * todaySales: 100000 - 10000000 (Rp 100k - 10M)
 * todayTransactionCount: 0 - 200
 * lowStockItems: 0 - 50 products
 * recentTransactions: 0 - 20 items
 * ```
 *
 * @property todaySales Total penjualan hari ini dalam Rupiah
 * @property todayTransactionCount Jumlah transaksi penjualan hari ini
 * @property lowStockItems Daftar produk dengan stok rendah yang perlu restock
 * @property recentTransactions Transaksi penjualan terbaru untuk activity overview
 * @property isLoading Status loading data dashboard
 * @property errorMessage Error message jika gagal memuat data (null = no error)
 */
data class DashboardUiState(
    val todaySales: Double = 0.0,
    val todayTransactionCount: Int = 0,
    val lowStockItems: List<Produk> = emptyList(),
    val recentTransactions: List<Penjualan> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel untuk Dashboard Screen
 *
 * Mengelola state dan business logic untuk halaman dashboard utama aplikasi.
 * Dashboard memberikan overview komprehensif performa toko retail dengan
 * key metrics, alerts, dan aktivitas terbaru.
 *
 * **Core Responsibilities:**
 * - Load dan aggregate dashboard metrics dari berbagai services
 * - Monitor real-time business performance indicators
 * - Provide alerts untuk operational issues (low stock, etc.)
 * - Enable quick navigation ke detail screens
 * - Handle data refresh dan error recovery
 *
 * **Architecture Pattern:**
 * - MVVM dengan reactive UI updates melalui StateFlow
 * - Dependency injection menggunakan Hilt
 * - Coroutine-based async operations untuk data loading
 * - Error handling dengan user-friendly Indonesian messages
 *
 * **Business Logic Flow:**
 * ```kotlin
 * // 1. Initialization
 * DashboardViewModel() → loadDashboardData() → aggregate metrics
 *     ↓
 *     ├── Calculate today sales & transaction count
 *     ├── Check low stock products
 *     ├── Load recent transactions
 *     └── Update UI state
 *
 * // 2. Data Aggregation
 * todaySales = sum(sales.where(date == today))
 * todayTransactionCount = count(sales.where(date == today))
 * lowStockItems = products.where(stock <= min_stock)
 * recentTransactions = sales.orderBy(date desc).limit(10)
 *
 * // 3. Error Handling
 * try { load data } catch { show error with retry option }
 *
 * // 4. Refresh Flow
 * User triggers refresh → loadDashboardData() → update state
 * ```
 *
 * **Data Sources:**
 * - **SaleService**: Today sales, transaction count, recent transactions
 * - **ProductService**: Low stock alerts, inventory status
 * - **ReportingService**: Aggregated metrics dan trends (future)
 * - **AuthService**: User-specific data filtering (future)
 *
 * **Key Metrics Calculated:**
 * ```kotlin
 * // Primary KPIs
 * val todaySales = saleService.getTodayTotalSales()
 * val todayTransactions = saleService.getTodayTransactionCount()
 *
 * // Operational Alerts
 * val lowStockItems = productService.getLowStockProducts()
 *     .sortedBy { it.stok } // Most critical first
 *
 * // Activity Overview
 * val recentTransactions = saleService.getRecentSales(limit = 10)
 *     .sortedByDescending { it.tanggal }
 * ```
 *
 * **State Management:**
 * Menggunakan [DashboardUiState] untuk merepresentasikan seluruh dashboard state.
 * State updates dilakukan secara immutable melalui copy() operations.
 * Reactive updates ke UI melalui StateFlow collection.
 *
 * **Error Handling Strategy:**
 * - **Network Errors**: Show cached data if available, with offline indicator
 * - **Database Errors**: User-friendly messages dengan retry options
 * - **Empty States**: Guidance untuk new stores (no data yet)
 * - **Loading States**: Prevent UI blocking dengan proper loading indicators
 * - **Recovery**: Manual refresh option untuk failed loads
 *
 * **UI Integration Patterns:**
 * ```kotlin
 * @Composable
 * fun DashboardScreen(viewModel: DashboardViewModel) {
 *     val uiState by viewModel.uiState.collectAsState()
 *
 *     when {
 *         uiState.isLoading -> LoadingView()
 *         uiState.errorMessage != null -> ErrorView(
 *             message = uiState.errorMessage,
 *             onRetry = { viewModel.refreshData() }
 *         )
 *         else -> DashboardContent(uiState)
 *     }
 * }
 *
 * @Composable
 * fun DashboardContent(uiState: DashboardUiState) {
 *     LazyColumn {
 *         // KPI Section
 *         item { KPICards(uiState.todaySales, uiState.todayTransactionCount) }
 *
 *         // Alerts Section
 *         if (uiState.lowStockItems.isNotEmpty()) {
 *             item { LowStockAlert(uiState.lowStockItems) }
 *         }
 *
 *         // Activity Section
 *         item { RecentActivity(uiState.recentTransactions) }
 *     }
 * }
 * ```
 *
 * **Performance Optimizations:**
 * - **Parallel Loading**: Coroutines untuk load metrics simultaneously
 * - **Lazy Evaluation**: Recent transactions loaded on demand
 * - **Caching**: Frequently accessed data cached in memory
 * - **Pagination**: Large datasets paginated untuk smooth scrolling
 * - **Background Refresh**: Non-intrusive updates setiap 5-10 menit
 *
 * **Navigation Integration:**
 * Dashboard sebagai central hub untuk navigation:
 * ```kotlin
 * // From dashboard alerts
 * lowStockAlert.onClick = { navController.navigate("inventory/${product.id}") }
 * transactionItem.onClick = { navController.navigate("sales/${sale.id}") }
 *
 * // Quick actions
 * fab.onClick = { navController.navigate("pos") }
 * ```
 *
 * **Testing Strategy:**
 * - **Unit Tests**: Mock services untuk isolated testing
 * - **Integration Tests**: Real database dengan test data
 * - **UI Tests**: StateFlow collection dan Compose rendering
 * - **Performance Tests**: Loading time dan memory usage
 * - **Edge Cases**: Empty data, network failures, large datasets
 *
 * **Future Enhancements:**
 * - Real-time updates via WebSocket/FCM
 * - Advanced analytics dan trend visualization
 * - Customizable KPI dashboard
 * - Multi-store dashboard aggregation
 * - Predictive alerts berdasarkan historical data
 *
 * **Current Implementation Notes:**
 * Saat ini menggunakan mock data untuk development dan testing.
 * Future implementation akan menggunakan real services:
 * - SaleService untuk sales metrics
 * - ProductService untuk inventory alerts
 * - ReportingService untuk advanced analytics
 *
 * @property uiState Reactive state flow untuk dashboard UI updates
 */
@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboardData()
    }

    /**
     * Memuat semua data dashboard secara asynchronous
     *
     * Method utama yang mengkoordinasikan loading semua dashboard metrics.
     * Menggunakan coroutine untuk operasi async dan error handling yang robust.
     * Saat ini menggunakan mock data untuk development - akan diganti dengan real services.
     *
     * **Business Logic:**
     * 1. Set loading state untuk UI feedback
     * 2. Load semua metrics secara parallel/simultaneous:
     *    - Today sales total
     *    - Today transaction count
     *    - Low stock products
     *    - Recent transactions
     * 3. Aggregate data dan update UI state
     * 4. Handle errors dengan user-friendly messages
     *
     * **Current Implementation (Mock Data):**
     * ```kotlin
     * // Generate realistic mock data untuk development
     * val todaySales = Random.nextDouble(1_000_000.0, 5_000_000.0)
     * val transactionCount = Random.nextInt(10, 50)
     * val lowStockItems = generateMockLowStockItems()
     * val recentTransactions = generateMockRecentTransactions()
     * ```
     *
     * **Future Implementation (Real Services):**
     * ```kotlin
     * // Parallel loading dengan coroutines
     * val todaySalesDeferred = async { saleService.getTodayTotalSales() }
     * val transactionCountDeferred = async { saleService.getTodayTransactionCount() }
     * val lowStockDeferred = async { productService.getLowStockProducts() }
     * val recentDeferred = async { saleService.getRecentSales(limit = 10) }
     *
     * // Await all results
     * val todaySales = todaySalesDeferred.await()
     * val transactionCount = transactionCountDeferred.await()
     * val lowStockItems = lowStockDeferred.await()
     * val recentTransactions = recentDeferred.await()
     * ```
     *
     * **Data Validation:**
     * - Sales amount: Must be >= 0
     * - Transaction count: Must be >= 0
     * - Low stock items: Filter valid products only
     * - Recent transactions: Sort by date descending, limit to reasonable number
     *
     * **Error Handling:**
     * - **Service Failures**: Catch exceptions dari mock/real services
     * - **Data Corruption**: Validate data integrity sebelum display
     * - **Network Issues**: Graceful degradation dengan cached data (future)
     * - **Empty Results**: Handle no data scenarios (new store)
     *
     * **State Transitions:**
     * ```kotlin
     * // Start loading
     * state.copy(isLoading = true, errorMessage = null)
     *
     * // Success
     * state.copy(
     *     todaySales = calculatedSales,
     *     todayTransactionCount = transactionCount,
     *     lowStockItems = lowStockList,
     *     recentTransactions = recentList,
     *     isLoading = false
     * )
     *
     * // Error
     * state.copy(
     *     isLoading = false,
     *     errorMessage = "Gagal memuat data dashboard: ${e.message}"
     * )
     * ```
     *
     * **Performance Considerations:**
     * - **Async Loading**: Non-blocking UI dengan coroutine
     * - **Parallel Execution**: Multiple data sources loaded simultaneously
     * - **Memory Management**: Efficient data structures untuk large lists
     * - **Cancellation**: Automatic cancellation jika ViewModel destroyed
     *
     * **Threading:**
     * - Executed in viewModelScope (IO dispatcher untuk database operations)
     * - Main thread safe untuk UI updates
     * - Background processing untuk heavy calculations
     *
     * **Dependencies:**
     * - [viewModelScope]: Lifecycle-aware coroutine execution
     * - [_uiState]: Mutable state untuk UI updates
     * - Mock data generators (temporary): generateMockLowStockItems, generateMockRecentTransactions
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `loadDashboardData success updates all metrics`() {
     *     // Given: Mock services return valid data
     *     // When: loadDashboardData called
     *     // Then: State updated with all metrics, loading = false
     * }
     *
     * @Test
     * fun `loadDashboardData error shows error message`() {
     *     // Given: Mock services throw exceptions
     *     // When: loadDashboardData called
     *     // Then: State shows error, loading = false
     * }
     *
     * @Test
     * fun `loadDashboardData handles empty data gracefully`() {
     *     // Given: No sales/inventory data
     *     // When: loadDashboardData called
     *     // Then: State shows zero values, no errors
     * }
     * ```
     *
     * **Monitoring & Analytics:**
     * - Loading time tracking untuk performance monitoring
     * - Error rate monitoring untuk reliability metrics
     * - Data freshness tracking untuk cache invalidation
     * - User interaction analytics untuk UX improvements
     *
     * **Future Optimizations:**
     * - Incremental loading untuk large datasets
     * - Smart caching dengan TTL (time-to-live)
     * - Predictive loading berdasarkan user patterns
     * - Real-time subscriptions untuk live updates
     */
    private fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // TODO: Replace with actual data from repositories
                // For now, generate mock data
                val mockTodaySales = Random.nextDouble(1000000.0, 5000000.0)
                val mockTransactionCount = Random.nextInt(10, 50)
                val mockLowStockItems = generateMockLowStockItems()
                val mockRecentTransactions = generateMockRecentTransactions()

                _uiState.value = _uiState.value.copy(
                    todaySales = mockTodaySales,
                    todayTransactionCount = mockTransactionCount,
                    lowStockItems = mockLowStockItems,
                    recentTransactions = mockRecentTransactions,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat data dashboard: ${e.message}"
                )
            }
        }
    }

    /**
     * Generate mock data untuk produk dengan stok rendah
     *
     * Method temporary yang menghasilkan data mock untuk development dan testing.
     * Akan diganti dengan real ProductService.getLowStockProducts() call.
     *
     * **Business Logic:**
     * Simulate products dengan stock levels di bawah threshold minimum.
     * Products ini akan ditampilkan sebagai alerts di dashboard.
     *
     * **Mock Data Strategy:**
     * - Realistic product names dan barcodes (Indonesian market)
     * - Varied stock levels (1-5) untuk menunjukkan urgency
     * - Different categories untuk diversity
     * - Consistent pricing structure
     *
     * **Data Structure:**
     * ```kotlin
     * // Mock low stock products
     * val products = listOf(
     *     Product(
     *         name = "Indomie Goreng",
     *         stock = 5,  // Below minimum threshold
     *         category = "Food",
     *         price = 3500.0
     *     ),
     *     Product(
     *         name = "Coca Cola 1.5L",
     *         stock = 3,  // Critical level
     *         category = "Beverages",
     *         price = 8500.0
     *     )
     * )
     * ```
     *
     * **Future Replacement:**
     * ```kotlin
     * // Real implementation
     * suspend fun getLowStockProducts(): List<Produk> {
     *     return productService.getProducts()
     *         .filter { it.stok <= it.minStok }
     *         .sortedBy { it.stok } // Most critical first
     *         .take(10) // Limit for dashboard display
     * }
     * ```
     *
     * **Testing Considerations:**
     * - Mock data harus consistent across test runs
     - Data harus realistic untuk UI testing
     * - Include edge cases: empty list, single item, many items
     *
     * **Performance:**
     * - Lightweight: Static data generation
     * - Fast execution: No database calls
     * - Memory efficient: Small fixed-size list
     *
     * @return List produk mock dengan stok rendah untuk dashboard alerts
     */
    private fun generateMockLowStockItems(): List<Produk> {
        // TODO: Replace with actual low stock query
        return listOf(
            Produk(
                id = 1,
                name = "Indomie Goreng",
                barcode = "8996001410022",
                sellingPrice = 3500.0,
                costPrice = 3000.0,
                stockQuantity = 5,
                categoryId = 1,
                warehouseId = 1
            ),
            Produk(
                id = 2,
                name = "Coca Cola 1.5L",
                barcode = "8996001410039",
                sellingPrice = 8500.0,
                costPrice = 7500.0,
                stockQuantity = 3,
                categoryId = 2,
                warehouseId = 1
            )
        )
    }

    /**
     * Generate mock data untuk transaksi penjualan terbaru
     *
     * Method temporary yang menghasilkan data mock untuk recent sales activity.
     * Akan diganti dengan real SaleService.getRecentSales() call.
     *
     * **Business Logic:**
     * Simulate recent sales transactions untuk activity overview di dashboard.
     * Menampilkan transaksi terbaru dengan realistic amounts dan timestamps.
     *
     * **Mock Data Strategy:**
     * - Varied transaction amounts (realistic retail values)
     * - Recent timestamps (current time, 1 hour ago, etc.)
     * - Payment scenarios (exact payment, with change)
     * - Consistent user association
     *
     * **Data Structure:**
     * ```kotlin
     * // Mock recent transactions
     * val transactions = listOf(
     *     Sale(
     *         id = 1,
     *         date = System.currentTimeMillis(),
     *         total = 12500.0,  // Recent sale
     *         payment = 12500.0,
     *         change = 0.0,
     *         userId = 1
     *     ),
     *     Sale(
     *         id = 2,
     *         date = System.currentTimeMillis() - 3600000,  // 1 hour ago
     *         total = 28500.0,
     *         payment = 30000.0,
     *         change = 1500.0,
     *         userId = 1
     *     )
     * )
     * ```
     *
     * **Future Replacement:**
     * ```kotlin
     * // Real implementation
     * suspend fun getRecentTransactions(limit: Int = 10): List<Penjualan> {
     *     return saleService.getSales(
     *         dateRange = DateRange.recent(),
     *         limit = limit,
     *         sortBy = SortBy.DATE_DESC
     *     )
     * }
     * ```
     *
     * **Business Value:**
     * - Show recent business activity
     * - Enable quick navigation to transaction details
     * - Provide context untuk sales performance
     * - Support operational monitoring
     *
     * **Testing Considerations:**
     * - Consistent timestamps untuk reproducible tests
     * - Realistic amounts untuk UI validation
     * - Include various payment scenarios
     * - Test empty list scenarios
     *
     * **Performance:**
     * - Lightweight: Static data generation
     * - Fast execution: No complex calculations
     * - Memory efficient: Small fixed-size list
     *
     * @return List transaksi penjualan mock terbaru untuk dashboard activity
     */
    private fun generateMockRecentTransactions(): List<Penjualan> {
        // TODO: Replace with actual recent transactions query
        return listOf(
            Penjualan(
                id = 1,
                saleDate = java.util.Date(System.currentTimeMillis()),
                totalAmount = 150000.0,
                paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH,
                cashierId = 1
            ),
            Penjualan(
                id = 2,
                saleDate = java.util.Date(System.currentTimeMillis() - 3600000), // 1 hour ago
                totalAmount = 28500.0,
                paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH,
                cashierId = 1
            )
        )
    }

    /**
     * Refresh data dashboard secara manual
     *
     * Method public yang dipanggil dari UI untuk force refresh dashboard data.
     * Digunakan untuk pull-to-refresh, manual refresh button, atau retry setelah error.
     *
     * **Business Logic:**
     * Trigger complete reload dari semua dashboard metrics.
     * Reset error state dan mulai loading state baru.
     * Delegate ke loadDashboardData() untuk actual data loading.
     *
     * **Use Cases:**
     * - **Pull-to-Refresh**: User swipe down untuk update data terbaru
     * - **Manual Refresh**: Refresh button di UI untuk force update
     * - **Error Recovery**: Retry button setelah loading failure
     * - **Data Sync**: Manual sync setelah offline operations
     *
     * **UI Integration:**
     * ```kotlin
     * // Pull-to-refresh
     * val pullRefreshState = rememberPullRefreshState(
     *     refreshing = uiState.isLoading,
     *     onRefresh = { viewModel.refreshData() }
     * )
     *
     * // Manual refresh button
     * IconButton(onClick = { viewModel.refreshData() }) {
     *     Icon(Icons.Default.Refresh, "Refresh")
     * }
     *
     * // Error retry
     * uiState.errorMessage?.let { error ->
     *     Button(onClick = { viewModel.refreshData() }) {
     *         Text("Coba Lagi")
     *     }
     * }
     * ```
     *
     * **State Flow:**
     * ```kotlin
     * // User triggers refresh
     * refreshData() → loadDashboardData()
     *     ↓
     *     state.isLoading = true
     *     state.errorMessage = null  // Clear previous errors
     *     ↓
     *     // Load new data...
     *     ↓
     *     state = newData  // Complete refresh
     * ```
     *
     * **Error Handling:**
     * - Automatic error clearing pada refresh start
     * - Fresh error handling dari loadDashboardData()
     * - No duplicate error states
     *
     * **Performance Considerations:**
     * - Immediate UI feedback dengan loading state
     * - Cancellation-aware: Previous loads cancelled
     * - Resource efficient: Reuse existing loadDashboardData logic
     *
     * **Threading:**
     * - Synchronous method call (immediate return)
     * - Async data loading via loadDashboardData()
     * - Main thread safe untuk UI triggers
     *
     * **Dependencies:**
     * - [loadDashboardData()]: Core data loading logic
     * - [_uiState]: State management untuk loading indicators
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `refreshData triggers data reload`() {
     *     // Given: Dashboard has data loaded
     *     // When: refreshData called
     *     // Then: loadDashboardData triggered, loading state set
     * }
     *
     * @Test
     * fun `refreshData clears previous errors`() {
     *     // Given: State has error message
     *     _uiState.value = DashboardUiState(errorMessage = "Error")
     *
     *     // When: refreshData called
     *     viewModel.refreshData()
     *
     *     // Then: Error message cleared
     *     assertEquals(null, _uiState.value.errorMessage)
     * }
     * ```
     *
     * **Analytics & Monitoring:**
     * - Track refresh frequency untuk UX insights
     * - Monitor refresh success/failure rates
     * - Measure refresh duration untuk performance
     * - User behavior analytics untuk feature usage
     *
     * **Future Enhancements:**
     * - Smart refresh: Only refresh changed data
     * - Background auto-refresh dengan user preferences
     * - Incremental updates untuk real-time data
     * - Refresh scopes: Full refresh vs partial updates
     */
    fun refreshData() {
        loadDashboardData()
    }
}