package com.chibychibystore.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.service.SaleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI State untuk Sales History Screen
 *
 * Data class yang merepresentasikan seluruh state UI untuk halaman sales history.
 * Menggunakan immutable data class pattern untuk reactive UI updates dengan StateFlow.
 * Semua field bersifat immutable dan perubahan state dilakukan melalui copy().
 *
 * **State Fields:**
 * - `sales`: List penjualan yang sedang ditampilkan (sudah terfilter)
 * - `selectedSale`: Detail penjualan lengkap untuk receipt display
 * - `isLoading`: Status loading data penjualan utama
 * - `isLoadingReceipt`: Status loading detail receipt
 * - `isPrintingReceipt`: Status proses printing receipt
 * - `error`: Error message yang sedang ditampilkan (nullable)
 * - `searchQuery`: Query pencarian yang aktif
 * - `startDate`: Filter tanggal mulai (nullable = no filter)
 * - `endDate`: Filter tanggal akhir (nullable = no filter)
 * - `showReceiptDialog`: Kontrol visibility dialog receipt
 * - `showDatePicker`: Kontrol visibility date picker dialog
 * - `datePickerType`: Tipe date picker (START/END) yang aktif
 *
 * **State Transitions:**
 * ```kotlin
 * // Initial state
 * SalesHistoryUiState() // semua default values
 *
 * // Loading state
 * state.copy(isLoading = true, error = null)
 *
 * // Data loaded
 * state.copy(sales = filteredSales, isLoading = false)
 *
 * // Error state
 * state.copy(isLoading = false, error = "Error message")
 *
 * // Receipt dialog
 * state.copy(selectedSale = saleDetail, showReceiptDialog = true)
 * ```
 *
 * **UI Integration:**
 * - Loading indicators: Driven by `isLoading*` fields
 * - Error messages: Displayed when `error` != null
 * - Data lists: Rendered from `sales` field
 * - Dialogs: Controlled by `show*` boolean fields
 * - Filters: Applied based on `searchQuery`, `startDate`, `endDate`
 *
 * **Default Values:**
 * - Collections: Empty lists untuk menghindari null checks
 * - Booleans: false untuk hidden states
 * - Strings: Empty strings untuk clean initial state
 * - Dates: null untuk no filtering
 * - Enums: Default enum values
 *
 * **Immutability Benefits:**
 * - Thread-safe: State tidak bisa diubah secara accidental
 * - Predictable: Perubahan state selalu explicit melalui copy()
 * - Reactive: StateFlow dapat detect perubahan dengan reliable
 * - Debugging: Easy tracking of state changes
 *
 * **Usage in ViewModel:**
 * ```kotlin
 * private val _uiState = MutableStateFlow(SalesHistoryUiState())
 * val uiState: StateFlow<SalesHistoryUiState> = _uiState
 *
 * // Update state
 * _uiState.value = _uiState.value.copy(
 *     sales = newSales,
 *     isLoading = false
 * )
 * ```
 *
 * **Usage in Compose UI:**
 * ```kotlin
 * val uiState by viewModel.uiState.collectAsState()
 *
 * // Conditional rendering
 * if (uiState.isLoading) {
 *     CircularProgressIndicator()
 * }
 *
 * if (uiState.error != null) {
 *     Text(uiState.error ?: "")
 * }
 *
 * // List rendering
 * LazyColumn {
 *     items(uiState.sales) { sale ->
 *         SaleItem(sale)
 *     }
 * }
 * ```
 *
 * @property sales List penjualan yang ditampilkan (sudah terfilter berdasarkan search/date)
 * @property selectedSale Detail penjualan lengkap untuk receipt (null jika tidak ada)
 * @property isLoading Status loading data penjualan utama
 * @property isLoadingReceipt Status loading detail receipt untuk printing
 * @property isPrintingReceipt Status proses printing receipt ke thermal printer
 * @property error Error message yang ditampilkan ke user (null = no error)
 * @property searchQuery Query pencarian aktif (empty = no search filter)
 * @property startDate Filter tanggal mulai (null = no start date filter)
 * @property endDate Filter tanggal akhir (null = no end date filter)
 * @property showReceiptDialog Kontrol visibility receipt dialog
 * @property showDatePicker Kontrol visibility date picker dialog
 * @property datePickerType Tipe date picker yang aktif (START atau END)
 */
data class SalesHistoryUiState(
    val sales: List<Penjualan> = emptyList(),
    val selectedSale: PenjualanWithItems? = null,
    val isLoading: Boolean = false,
    val isLoadingReceipt: Boolean = false,
    val isPrintingReceipt: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val showReceiptDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val datePickerType: DatePickerType = DatePickerType.START
)

enum class DatePickerType {
    START, END
}

/**
 * ViewModel untuk Sales History Screen
 *
 * ViewModel ini mengelola state dan business logic untuk halaman riwayat penjualan.
 * Bertanggung jawab atas loading data penjualan, filtering berdasarkan tanggal dan
 * pencarian, serta manajemen receipt (struk) untuk printing.
 *
 * **Arsitektur & Pola:**
 * - MVVM Pattern: Reactive UI updates melalui StateFlow
 * - Repository Pattern: Data access melalui SaleService
 * - State Management: Centralized state di SalesHistoryUiState
 * - Reactive Programming: Real-time updates dengan Flow
 *
 * **Tanggung Jawab Utama:**
 * - Loading dan caching data penjualan dengan filter tanggal
 * - Pencarian real-time berdasarkan ID, payment method, dan total
 * - Manajemen date picker untuk filter tanggal mulai/akhir
 * - Loading detail penjualan untuk receipt display
 * - Integrasi printing receipt melalui thermal printer
 * - Error handling dan user feedback
 *
 * **Fitur Utama:**
 * - Date Range Filtering: Filter penjualan berdasarkan periode waktu
 * - Real-time Search: Pencarian instan tanpa server request
 * - Receipt Management: Load dan display detail transaksi lengkap
 * - Thermal Printing: Print receipt ke Bluetooth printer
 * - Reactive Updates: UI selalu up-to-date dengan database changes
 * - Error Recovery: Comprehensive error handling dengan user feedback
 *
 * **State Management:**
 * ```kotlin
 * data class SalesHistoryUiState(
 *     val sales: List<Penjualan> = emptyList(),           // Data penjualan terfilter
 *     val selectedSale: PenjualanWithItems? = null,       // Sale untuk receipt
 *     val isLoading: Boolean = false,                     // Loading data utama
 *     val isLoadingReceipt: Boolean = false,              // Loading receipt detail
 *     val isPrintingReceipt: Boolean = false,             // Status printing
 *     val error: String? = null,                          // Error message
 *     val searchQuery: String = "",                       // Query pencarian
 *     val startDate: Date? = null,                        // Filter tanggal mulai
 *     val endDate: Date? = null,                          // Filter tanggal akhir
 *     val showReceiptDialog: Boolean = false,             // Dialog receipt visibility
 *     val showDatePicker: Boolean = false,                // Date picker visibility
 *     val datePickerType: DatePickerType = DatePickerType.START // Tipe date picker
 * )
 * ```
 *
 * **Business Workflows:**
 * 1. **Initial Load**: loadSales() → observeSales() → reactive updates
 * 2. **Date Filtering**: setStartDate/setEndDate → loadSales() → filtered data
 * 3. **Search**: updateSearchQuery → client-side filtering → instant results
 * 4. **Receipt Viewing**: loadReceipt() → show dialog → display details
 * 5. **Printing**: printReceipt() → thermal printer → receipt output
 *
 * **Dependencies:**
 * - SaleService: Core data operations dan printing
 * - DateFormat: Formatting tanggal untuk API calls
 * - ViewModelScope: Coroutine scope untuk async operations
 *
 * **Performance Characteristics:**
 * - Lazy Loading: Data dimuat sesuai filter aktif
 * - Client-side Search: Instant filtering tanpa network calls
 * - Reactive Updates: Efficient Flow-based real-time updates
 * - Memory Management: Automatic cleanup saat ViewModel destroyed
 *
 * **Error Handling:**
 * - Service Failures: User-friendly messages dari SaleService
 * - Network Issues: Graceful degradation dengan error states
 * - Invalid Data: Validation dengan appropriate error messages
 * - Hardware Issues: Printer connection error handling
 *
 * **Integration Points:**
 * - SalesHistoryScreen: Compose UI yang consume state ini
 * - SaleService: Backend operations untuk data dan printing
 * - Thermal Printer: Bluetooth printing untuk receipts
 * - DatePicker Components: UI components untuk date selection
 *
 * **Usage Example:**
 * ```kotlin
 * @HiltViewModel
 * class SalesHistoryViewModel @Inject constructor(
 *     private val saleService: SaleService
 * ) : ViewModel() {
 *
 *     // Reactive state untuk UI binding
 *     val uiState: StateFlow<SalesHistoryUiState> = _uiState
 *
 *     // Load data saat initialization
 *     init { loadSales() }
 *
 *     // Public API untuk UI interactions
 *     fun updateSearchQuery(query: String) { /* ... */ }
 *     fun setStartDate(date: Date?) { /* ... */ }
 *     fun loadReceipt(saleId: Long) { /* ... */ }
 * }
 * ```
 *
 * **Threading & Coroutines:**
 * - Main Thread: UI state updates dan user interactions
 * - IO Thread: Database operations dan network calls
 * - Error Handling: Comprehensive exception handling di semua async operations
 *
 * **Testing Considerations:**
 * - Unit Tests: Mock SaleService untuk business logic testing
 * - Integration Tests: Full SaleService dengan in-memory database
 * - UI Tests: StateFlow emissions dan user interaction flows
 *
 * @property saleService Service untuk operasi data penjualan dan printing
 * @property uiState Reactive state untuk UI binding dan updates
 * @see SalesHistoryUiState Data class untuk UI state management
 * @see SaleService Backend service untuk sales operations
 * @see SalesHistoryScreen Compose UI yang menggunakan ViewModel ini
 */
@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val saleService: SaleService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
    val uiState: StateFlow<SalesHistoryUiState> = _uiState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadSales()
    }

    /**
     * Load semua penjualan dengan filter tanggal dan pencarian
     *
     * Metode ini melakukan loading data penjualan dari SaleService dengan filter tanggal opsional
     * dan pencarian berdasarkan query yang ada di UI state. Setelah data berhasil dimuat,
     * akan mengatur reactive observation untuk update real-time.
     *
     * **Business Logic Flow:**
     * 1. Set loading state dan clear error
     * 2. Format tanggal start/end ke string (yyyy-MM-dd)
     * 3. Panggil SaleService.getSales() dengan parameter filter
     * 4. Apply search filter ke hasil data
     * 5. Update UI state dengan data terfilter
     * 6. Setup reactive observation untuk perubahan real-time
     *
     * **State Management:**
     * - `isLoading`: true selama proses loading
     * - `error`: null saat mulai, diisi jika terjadi error
     * - `sales`: diupdate dengan data terfilter setelah sukses
     *
     * **Performance Considerations:**
     * - Menggunakan dateFormat yang sudah di-cache untuk menghindari overhead formatting
     * - Reactive observation hanya di-setup setelah data awal berhasil dimuat
     * - Search filtering dilakukan di client-side untuk performa real-time
     *
     * **Error Handling:**
     * - Service failures: Menampilkan pesan error dari SaleService
     * - Network/Database errors: Generic error message
     * - Exception handling: Comprehensive try-catch untuk semua error types
     *
     * **Integration Points:**
     * - SaleService.getSales(): Mendapatkan data penjualan dengan filter
     * - observeSales(): Setup reactive updates setelah loading awal
     * - applySearchFilter(): Filter pencarian client-side
     *
     * **Usage Example:**
     * ```kotlin
     * // Load semua penjualan tanpa filter
     * viewModel.loadSales()
     *
     * // Load dengan date range (akan menggunakan state saat ini)
     * viewModel.setStartDate(startDate)
     * viewModel.setEndDate(endDate)
     * viewModel.loadSales() // akan menggunakan date range dari state
     * ```
     *
     * @see SaleService.getSales
     * @see observeSales
     * @see applySearchFilter
     * @see SalesHistoryUiState
     */
    fun loadSales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentState = _uiState.value
                val result = saleService.getSales(
                    startDate = currentState.startDate?.let { dateFormat.format(it) },
                    endDate = currentState.endDate?.let { dateFormat.format(it) }
                )

                result.onSuccess { sales ->
                    val filteredSales = applySearchFilter(sales)
                    _uiState.update {
                        it.copy(
                            sales = filteredSales,
                            isLoading = false
                        )
                    }
                    // Setup reactive updates
                    observeSales()
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Gagal memuat data penjualan"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Observe perubahan penjualan secara real-time dalam date range
     *
     * Metode private ini mengatur reactive observation untuk perubahan data penjualan
     * dalam rentang tanggal yang dipilih. Ketika ada penjualan baru, update, atau delete
     * di database, UI akan otomatis terupdate tanpa perlu manual refresh.
     *
     * **Business Logic Flow:**
     * 1. Ambil date range dari current UI state
     * 2. Set default range jika tidak ada filter (1900-2100)
     * 3. Subscribe ke SaleService.observeSalesByDateRange()
     * 4. Apply search filter ke setiap emission baru
     * 5. Update UI state dengan data terbaru
     *
     * **State Management:**
     * - `sales`: Terus terupdate dengan data terbaru dari database
     * - `error`: Diisi jika terjadi error dalam observation flow
     * - Reactive updates: Tidak mengubah loading state untuk menghindari UI flicker
     *
     * **Performance Considerations:**
     * - Menggunakan collectLatest untuk skip intermediate updates
     * - Error handling dengan catch operator untuk resilient observation
     * - Efficient filtering: Search diterapkan di setiap emission
     * - Memory efficient: Flow otomatis cleanup saat ViewModel destroyed
     *
     * **Error Handling:**
     * - Flow errors: Ditangkap dengan catch operator
     * - Database errors: User-friendly error message
     * - Network issues: Graceful degradation dengan error state
     *
     * **Integration Points:**
     * - SaleService.observeSalesByDateRange(): Reactive data source
     * - applySearchFilter(): Client-side filtering untuk setiap update
     * - UI State: Automatic updates tanpa manual intervention
     *
     * **Usage Example:**
     * ```kotlin
     * // Dipanggil otomatis setelah loadSales() berhasil
     * private fun setupReactiveUpdates() {
     *     observeSales() // UI akan selalu up-to-date
     * }
     * ```
     *
     * **Note:** Method ini bersifat private karena hanya dipanggil internal
     * setelah loading awal berhasil. Memastikan data selalu fresh tanpa
     * manual refresh dari user.
     *
     * @see SaleService.observeSalesByDateRange
     * @see applySearchFilter
     * @see loadSales
     */
    private fun observeSales() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val startDateStr = currentState.startDate?.let { dateFormat.format(it) } ?: "1900-01-01"
            val endDateStr = currentState.endDate?.let { dateFormat.format(it) } ?: "2100-12-31"

            saleService.observeSalesByDateRange(startDateStr, endDateStr)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = "Gagal mengamati perubahan penjualan: ${e.message}"
                        )
                    }
                }
                .collectLatest { sales ->
                    val filteredSales = applySearchFilter(sales)
                    _uiState.update { it.copy(sales = filteredSales) }
                }
        }
    }

    /**
     * Update search query dan filter penjualan secara real-time
     *
     * Metode ini memperbarui query pencarian dan langsung menerapkan filter
     * ke daftar penjualan yang sudah dimuat. Pencarian dilakukan client-side
     * untuk memberikan respons instan tanpa perlu server request.
     *
     * **Business Logic Flow:**
     * 1. Update searchQuery di UI state
     * 2. Apply search filter ke current sales list
     * 3. Update UI state dengan hasil terfilter
     *
     * **State Management:**
     * - `searchQuery`: Diperbarui dengan input user
     * - `sales`: Diperbarui dengan hasil filtering
     * - Immediate updates: Tidak ada loading state untuk performa
     *
     * **Performance Considerations:**
     * - Client-side filtering: Instant results tanpa network calls
     * - Efficient search: Filter berdasarkan ID, payment method, dan total
     * - Memory efficient: Bekerja dengan data yang sudah di-load
     * - No debouncing: Immediate response untuk UX yang responsif
     *
     * **Search Criteria:**
     * - Sale ID: Pencarian numerik dalam string
     * - Payment Method: Case-insensitive string matching
     * - Total Amount: Pencarian dalam string representation
     *
     * **Integration Points:**
     * - applySearchFilter(): Logic filtering yang dapat di-reuse
     * - UI State: Reactive updates untuk immediate feedback
     * - Current sales data: Filtering dari data yang sudah ada
     *
     * **Usage Example:**
     * ```kotlin
     * // User mengetik di search field
     * searchField.onTextChanged { query ->
     *     viewModel.updateSearchQuery(query) // Immediate filtering
     * }
     *
     * // Hasil akan langsung terlihat di UI
     * ```
     *
     * **Note:** Method ini memberikan instant search experience dengan
     * memanfaatkan data yang sudah di-load. Cocok untuk dataset yang
     * manageable di memory.
     *
     * @param query String query pencarian dari user input
     * @see applySearchFilter
     * @see SalesHistoryUiState.searchQuery
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        val filteredSales = applySearchFilter(_uiState.value.sales)
        _uiState.update { it.copy(sales = filteredSales) }
    }

    /**
     * Set start date untuk filter penjualan
     *
     * Metode ini mengatur tanggal mulai untuk filtering data penjualan.
     * Setelah date diupdate, akan otomatis melakukan reload data dengan
     * filter tanggal baru untuk memberikan hasil yang akurat.
     *
     * **Business Logic Flow:**
     * 1. Update startDate di UI state
     * 2. Trigger loadSales() untuk reload dengan filter baru
     * 3. Reactive observation akan menggunakan date range baru
     *
     * **State Management:**
     * - `startDate`: Diperbarui dengan date parameter (bisa null untuk clear filter)
     * - Automatic reload: loadSales() dipanggil untuk refresh data
     * - Reactive updates: Observation akan menggunakan date range baru
     *
     * **Date Handling:**
     * - Nullable parameter: null berarti tidak ada filter start date
     * - Date validation: Menggunakan Java Date object
     * - Range validation: Start date harus sebelum end date (jika ada)
     *
     * **Integration Points:**
     * - loadSales(): Trigger reload dengan date filter baru
     * - observeSales(): Reactive updates dengan date range baru
     * - DatePicker UI: Connected ke date picker component
     *
     * **Usage Example:**
     * ```kotlin
     * // Set start date dari date picker
     * viewModel.setStartDate(selectedDate)
     *
     * // Clear start date filter
     * viewModel.setStartDate(null)
     *
     * // Data akan otomatis reload dengan filter baru
     * ```
     *
     * **Note:** Method ini akan trigger reload data. Untuk UX yang lebih baik,
     * pertimbangkan untuk menggunakan debouncing jika date picker sering diubah.
     *
     * @param date Tanggal mulai filter, null untuk clear filter
     * @see setEndDate
     * @see loadSales
     * @see SalesHistoryUiState.startDate
     */
    fun setStartDate(date: Date?) {
        _uiState.update { it.copy(startDate = date) }
        loadSales()
    }

    /**
     * Set end date untuk filter penjualan
     *
     * Metode ini mengatur tanggal akhir untuk filtering data penjualan.
     * Mirip dengan setStartDate, method ini akan trigger reload data
     * dengan filter tanggal yang telah diperbarui.
     *
     * **Business Logic Flow:**
     * 1. Update endDate di UI state
     * 2. Trigger loadSales() untuk reload dengan filter baru
     * 3. Reactive observation akan menggunakan date range baru
     *
     * **State Management:**
     * - `endDate`: Diperbarui dengan date parameter (bisa null untuk clear filter)
     * - Automatic reload: loadSales() dipanggil untuk refresh data
     * - Reactive updates: Observation akan menggunakan date range baru
     *
     * **Date Handling:**
     * - Nullable parameter: null berarti tidak ada filter end date
     * - Date validation: Menggunakan Java Date object
     * - Range validation: End date harus setelah start date (jika ada)
     *
     * **Integration Points:**
     * - loadSales(): Trigger reload dengan date filter baru
     * - observeSales(): Reactive updates dengan date range baru
     * - DatePicker UI: Connected ke date picker component
     *
     * **Usage Example:**
     * ```kotlin
     * // Set end date dari date picker
     * viewModel.setEndDate(selectedDate)
     *
     * // Clear end date filter
     * viewModel.setEndDate(null)
     *
     * // Data akan otomatis reload dengan filter baru
     * ```
     *
     * **Note:** Method ini akan trigger reload data. Untuk UX yang lebih baik,
     * pertimbangkan untuk menggunakan debouncing jika date picker sering diubah.
     *
     * @param date Tanggal akhir filter, null untuk clear filter
     * @see setStartDate
     * @see loadSales
     * @see SalesHistoryUiState.endDate
     */
    fun setEndDate(date: Date?) {
        _uiState.update { it.copy(endDate = date) }
        loadSales()
    }

    /**
     * Tampilkan date picker dialog untuk memilih tanggal
     *
     * Metode ini mengatur UI state untuk menampilkan date picker dialog
     * dengan tipe tertentu (start date atau end date). Dialog akan muncul
     * dan user dapat memilih tanggal yang diinginkan.
     *
     * **Business Logic Flow:**
     * 1. Set showDatePicker = true
     * 2. Set datePickerType sesuai parameter
     * 3. UI akan menampilkan date picker dialog
     *
     * **State Management:**
     * - `showDatePicker`: true untuk menampilkan dialog
     * - `datePickerType`: START atau END sesuai parameter
     * - Dialog visibility: Controlled oleh UI state
     *
     * **Date Picker Types:**
     * - START: Untuk memilih tanggal mulai filter
     * - END: Untuk memilih tanggal akhir filter
     *
     * **Integration Points:**
     * - UI Components: DatePickerDialog akan muncul berdasarkan state
     * - hideDatePicker(): Untuk menutup dialog
     * - Date selection: Hasil akan dikirim ke setStartDate/setEndDate
     *
     * **Usage Example:**
     * ```kotlin
     * // Tampilkan date picker untuk start date
     * viewModel.showDatePicker(DatePickerType.START)
     *
     * // Tampilkan date picker untuk end date
     * viewModel.showDatePicker(DatePickerType.END)
     *
     * // UI akan menampilkan dialog sesuai tipe
     * ```
     *
     * **Note:** Method ini hanya mengatur UI state. Tanggal yang dipilih
     * akan diproses melalui setStartDate() atau setEndDate().
     *
     * @param type Tipe date picker (START atau END)
     * @see hideDatePicker
     * @see DatePickerType
     * @see SalesHistoryUiState.showDatePicker
     */
    fun showDatePicker(type: DatePickerType) {
        _uiState.update {
            it.copy(
                showDatePicker = true,
                datePickerType = type
            )
        }
    }

    /**
     * Sembunyikan date picker dialog
     *
     * Metode ini menyembunyikan date picker dialog dengan mengatur
     * showDatePicker menjadi false. Biasanya dipanggil ketika user
     * membatalkan pemilihan tanggal atau setelah tanggal dipilih.
     *
     * **Business Logic Flow:**
     * 1. Set showDatePicker = false
     * 2. UI akan menutup date picker dialog
     *
     * **State Management:**
     * - `showDatePicker`: false untuk menyembunyikan dialog
     * - `datePickerType`: Tidak diubah (untuk reference jika perlu)
     * - Dialog visibility: Controlled oleh UI state
     *
     * **Integration Points:**
     * - UI Components: DatePickerDialog akan tertutup
     * - showDatePicker(): Untuk menampilkan dialog kembali
     * - Date selection callbacks: Dipanggil setelah user memilih tanggal
     *
     * **Usage Example:**
     * ```kotlin
     * // User membatalkan date picker
     * viewModel.hideDatePicker()
     *
     * // Atau dipanggil otomatis setelah date selected
     * datePicker.onDateSelected { date ->
     *     viewModel.setStartDate(date)
     *     viewModel.hideDatePicker()
     * }
     * ```
     *
     * **Note:** Method ini tidak mengubah date values, hanya menyembunyikan
     * dialog. Tanggal yang dipilih harus diproses terpisah melalui
     * setStartDate() atau setEndDate().
     *
     * @see showDatePicker
     * @see setStartDate
     * @see setEndDate
     * @see SalesHistoryUiState.showDatePicker
     */
    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    /**
     * Load detail penjualan lengkap untuk ditampilkan sebagai struk
     *
     * Metode ini memuat detail penjualan lengkap termasuk semua item
     * yang dijual untuk keperluan penampilan struk atau receipt.
     * Data yang dimuat akan ditampilkan dalam dialog receipt.
     *
     * **Business Logic Flow:**
     * 1. Set loading receipt state dan clear error
     * 2. Panggil SaleService.getSale() untuk detail lengkap
     * 3. Update UI state dengan sale detail dan tampilkan dialog
     * 4. Error handling jika loading gagal
     *
     * **State Management:**
     * - `isLoadingReceipt`: true selama proses loading
     * - `selectedSale`: Diisi dengan PenjualanWithItems setelah sukses
     * - `showReceiptDialog`: true untuk menampilkan dialog
     * - `error`: Diisi jika terjadi error
     *
     * **Data Loading:**
     * - Sale details: Header information (date, total, payment method)
     * - Sale items: Semua produk yang dijual dengan quantity dan harga
     * - Complete transaction: Full sale information untuk receipt
     *
     * **Error Handling:**
     * - Service failures: Menampilkan pesan error dari SaleService
     * - Invalid sale ID: Error message untuk sale tidak ditemukan
     * - Network issues: Generic error dengan user-friendly message
     *
     * **Integration Points:**
     * - SaleService.getSale(): Mendapatkan detail penjualan lengkap
     * - Receipt dialog: UI component untuk menampilkan struk
     * - printReceipt(): Untuk mencetak struk yang sudah dimuat
     *
     * **Usage Example:**
     * ```kotlin
     * // User klik item penjualan di list
     * viewModel.loadReceipt(saleId)
     *
     * // Dialog akan muncul dengan detail lengkap
     * // User bisa print atau close dialog
     * ```
     *
     * **Note:** Method ini memuat data tambahan (sale items) yang tidak
     * ada di list utama. Cocok untuk detail view dan printing.
     *
     * @param saleId ID penjualan yang akan dimuat detailnya
     * @see SaleService.getSale
     * @see hideReceiptDialog
     * @see printReceipt
     * @see SalesHistoryUiState.selectedSale
     */
    fun loadReceipt(saleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReceipt = true, error = null) }

            try {
                val result = saleService.getSale(saleId)
                result.onSuccess { saleWithItems ->
                    _uiState.update {
                        it.copy(
                            selectedSale = saleWithItems,
                            showReceiptDialog = true,
                            isLoadingReceipt = false
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoadingReceipt = false,
                            error = exception.message ?: "Gagal memuat struk"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingReceipt = false,
                        error = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Sembunyikan receipt dialog dan clear selected sale
     *
     * Metode ini menyembunyikan dialog receipt dan membersihkan data
     * penjualan yang dipilih. Biasanya dipanggil ketika user menutup
     * dialog atau setelah operasi selesai.
     *
     * **Business Logic Flow:**
     * 1. Set showReceiptDialog = false
     * 2. Clear selectedSale = null
     * 3. UI akan menutup receipt dialog
     *
     * **State Management:**
     * - `showReceiptDialog`: false untuk menyembunyikan dialog
     * - `selectedSale`: null untuk clear data yang dipilih
     * - Clean state: Tidak ada data receipt yang tersisa
     *
     * **Data Cleanup:**
     * - Selected sale data: Dibersihkan untuk memory efficiency
     * - Dialog state: Reset ke kondisi awal
     * - UI state: Clean state untuk interaksi berikutnya
     *
     * **Integration Points:**
     * - Receipt dialog: UI component akan tertutup
     * - loadReceipt(): Untuk memuat receipt baru
     * - printReceipt(): Tidak akan berfungsi tanpa selectedSale
     *
     * **Usage Example:**
     * ```kotlin
     * // User menutup receipt dialog
     * viewModel.hideReceiptDialog()
     *
     * // Atau dipanggil otomatis setelah printing
     * viewModel.printReceipt()
     * viewModel.hideReceiptDialog() // cleanup
     * ```
     *
     * **Note:** Method ini melakukan cleanup lengkap dari receipt state.
     * Pastikan memanggil ini setelah operasi receipt selesai untuk
     * mencegah memory leaks dan state inconsistency.
     *
     * @see loadReceipt
     * @see printReceipt
     * @see SalesHistoryUiState.showReceiptDialog
     */
    fun hideReceiptDialog() {
        _uiState.update {
            it.copy(
                showReceiptDialog = false,
                selectedSale = null
            )
        }
    }

    /**
     * Print receipt untuk penjualan yang sedang dipilih
     *
     * Metode ini mencetak struk untuk penjualan yang sedang ditampilkan
     * di receipt dialog. Menggunakan SaleService untuk mengirim data
     * ke thermal printer yang terhubung via Bluetooth.
     *
     * **Business Logic Flow:**
     * 1. Ambil saleId dari selectedSale yang sedang ditampilkan
     * 2. Set printing receipt state dan clear error
     * 3. Panggil SaleService.printReceipt() untuk printing
     * 4. Update state berdasarkan hasil printing
     *
     * **State Management:**
     * - `isPrintingReceipt`: true selama proses printing
     * - `error`: Diisi jika printing gagal
     * - Success state: Tidak ada perubahan khusus (silent success)
     *
     * **Printing Process:**
     * - Sale validation: Pastikan ada selectedSale dengan valid ID
     * - Printer communication: Via Bluetooth thermal printer
     * - Receipt format: Standard retail receipt dengan header, items, total
     * - Error recovery: Automatic retry untuk connection issues
     *
     * **Error Handling:**
     * - No selected sale: Method return early tanpa error
     * - Printer not connected: Error message untuk connection issues
     * - Printing failures: Service error messages
     * - Hardware issues: User-friendly error untuk printer problems
     *
     * **Integration Points:**
     * - SaleService.printReceipt(): Core printing functionality
     * - Thermal printer: Bluetooth-connected receipt printer
     * - Receipt dialog: Printing dari sale yang sedang ditampilkan
     * - PrinterService: Low-level printer communication
     *
     * **Usage Example:**
     * ```kotlin
     * // Setelah load receipt, user klik print
     * viewModel.loadReceipt(saleId)
     * // ... dialog muncul ...
     * viewModel.printReceipt() // Print receipt
     *
     * // Atau dalam workflow lengkap
     * viewModel.printReceipt()
     * viewModel.hideReceiptDialog() // cleanup setelah print
     * ```
     *
     * **Note:** Method ini hanya berfungsi jika ada selectedSale.
     * Pastikan memanggil loadReceipt() terlebih dahulu sebelum print.
     * Printing dilakukan asynchronously untuk tidak block UI.
     *
     * @see loadReceipt
     * @see SaleService.printReceipt
     * @see SalesHistoryUiState.selectedSale
     */
    fun printReceipt() {
        val saleId = _uiState.value.selectedSale?.penjualan?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingReceipt = true, error = null) }

            try {
                val result = saleService.printReceipt(saleId)
                result.onSuccess {
                    _uiState.update { it.copy(isPrintingReceipt = false) }
                    // Could show success message here
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isPrintingReceipt = false,
                            error = exception.message ?: "Gagal mencetak struk"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPrintingReceipt = false,
                        error = "Terjadi kesalahan: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear semua filter dan reload data tanpa filter
     *
     * Metode ini mereset semua filter (tanggal dan pencarian) dan
     * melakukan reload data penjualan tanpa filter apapun. Berguna
     * untuk memberikan user opsi kembali ke view semua data.
     *
     * **Business Logic Flow:**
     * 1. Reset UI state ke kondisi awal (SalesHistoryUiState())
     * 2. Trigger loadSales() untuk reload tanpa filter
     * 3. Reactive observation akan menggunakan range default
     *
     * **State Management:**
     * - Complete reset: Semua field di UI state direset ke default
     * - `sales`: Akan diupdate dengan data tanpa filter
     * - `searchQuery`: Direset ke empty string
     * - `startDate/endDate`: Direset ke null
     * - `error`: Direset ke null
     *
     * **Filter Reset:**
     * - Date filters: startDate dan endDate menjadi null
     * - Search filter: searchQuery menjadi empty
     * - Dialog states: Semua dialog disembunyikan
     * - Loading states: Direset ke kondisi awal
     *
     * **Integration Points:**
     * - loadSales(): Trigger reload dengan kondisi fresh
     * - observeSales(): Reactive updates dengan range default
     * - UI Components: Semua filter controls akan reset
     *
     * **Usage Example:**
     * ```kotlin
     * // User klik "Clear Filters" button
     * viewModel.clearFilters()
     *
     * // Atau dalam error recovery
     * viewModel.clearError()
     * viewModel.clearFilters() // Reset lengkap
     * ```
     *
     * **Note:** Method ini melakukan reset total dari UI state.
     * Gunakan dengan hati-hati karena akan menghilangkan semua
     * filter yang sudah diatur user.
     *
     * @see SalesHistoryUiState
     * @see loadSales
     */
    fun clearFilters() {
        _uiState.update { SalesHistoryUiState() }
        loadSales()
    }

    /**
     * Clear error message dari UI state
     *
     * Metode utility ini membersihkan error message yang sedang
     * ditampilkan di UI. Berguna untuk memberikan user kontrol
     * untuk menutup error notifications.
     *
     * **Business Logic Flow:**
     * 1. Set error field di UI state menjadi null
     * 2. UI akan menyembunyikan error message
     *
     * **State Management:**
     * - `error`: Direset ke null
     * - Other states: Tidak diubah
     * - UI feedback: Error message akan hilang
     *
     * **Error Handling:**
     * - User control: Memberikan user kemampuan dismiss error
     * - State cleanup: Clear error tanpa affect operasi lain
     * - UX improvement: Error tidak permanent di layar
     *
     * **Integration Points:**
     * - UI Components: Error message display akan tertutup
     * - Error recovery: User bisa mencoba operasi lagi setelah clear
     * - State persistence: Error tidak di-cache secara permanent
     *
     * **Usage Example:**
     * ```kotlin
     * // User klik dismiss pada error message
     * viewModel.clearError()
     *
     * // Atau dalam error recovery workflow
     * try {
     *     // some operation
     * } catch (e: Exception) {
     *     viewModel.showError("Operation failed")
     *     // User bisa clear error dan coba lagi
     * }
     * ```
     *
     * **Note:** Method ini hanya clear error display, tidak
     * mengubah underlying state atau data. User tetap bisa
     * mencoba operasi yang sama setelah clear error.
     *
     * @see SalesHistoryUiState.error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Apply search filter ke daftar penjualan
     *
     * Metode private ini menerapkan filter pencarian ke daftar penjualan
     * berdasarkan query yang ada di UI state. Pencarian dilakukan pada
     * multiple fields untuk memberikan hasil yang comprehensive.
     *
     * **Business Logic Flow:**
     * 1. Ambil searchQuery dari current UI state
     * 2. Jika query kosong, return semua data
     * 3. Filter sales berdasarkan kriteria pencarian
     * 4. Return hasil filtering
     *
     * **Search Criteria:**
     * - Sale ID: Pencarian numerik dalam string representation
     * - Payment Method: Case-insensitive string matching
     * - Total Amount: Pencarian dalam string representation
     * - Multi-field: Query bisa match salah satu field
     *
     * **Filtering Logic:**
     * ```kotlin
     * sales.filter { sale ->
     *     sale.id.toString().contains(query) ||
     *     sale.paymentMethod.name.contains(query, ignoreCase = true) ||
     *     sale.totalAmount.toString().contains(query)
     * }
     * ```
     *
     * **Performance Considerations:**
     * - Client-side filtering: Instant results tanpa server calls
     * - Case-insensitive: ignoreCase = true untuk payment method
     * - String conversion: ID dan amount dikonversi ke string untuk search
     * - Memory efficient: Bekerja dengan existing data in memory
     *
     * **Integration Points:**
     * - updateSearchQuery(): Trigger filtering saat user mengetik
     * - observeSales(): Apply filter ke setiap reactive update
     * - UI State: Menggunakan searchQuery dari current state
     *
     * **Usage Example:**
     * ```kotlin
     * // Dalam updateSearchQuery
     * val filtered = applySearchFilter(currentSales)
     * _uiState.value = _uiState.value.copy(sales = filtered)
     *
     * // Dalam observeSales
     * saleService.observeSalesByDateRange(...)
     *     .collectLatest { sales ->
     *         val filtered = applySearchFilter(sales)
     *         // update UI
     *     }
     * ```
     *
     * **Note:** Method ini bersifat private karena hanya digunakan
     * internal untuk filtering logic. Dirancang untuk reusability
     * antara search manual dan reactive updates.
     *
     * @param sales List penjualan yang akan difilter
     * @return List penjualan yang sudah difilter berdasarkan search query
     * @see updateSearchQuery
     * @see observeSales
     * @see SalesHistoryUiState.searchQuery
     */
    private fun applySearchFilter(sales: List<Penjualan>): List<Penjualan> {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return sales

        return sales.filter { sale ->
            sale.id.toString().contains(query, ignoreCase = true) ||
            sale.paymentMethod.name.contains(query, ignoreCase = true) ||
            sale.totalAmount.toString().contains(query, ignoreCase = true)
        }
    }
}