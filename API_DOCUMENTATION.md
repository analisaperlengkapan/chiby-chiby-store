# API Documentation - Chiby Chiby Store

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Final
**Referensi Dokumen:** backend-design.md

## Daftar Isi
1. [Overview](#1-overview)
2. [Authentication API](#2-authentication-api)
3. [Product Management API](#3-product-management-api)
4. [Warehouse Management API](#4-warehouse-management-api)
5. [Sales Management API](#5-sales-management-api)
6. [Reporting API](#6-reporting-api)
7. [Financial Management API](#7-financial-management-api)
8. [Barcode Management API](#8-barcode-management-api)
9. [Backup & Restore API](#9-backup--restore-api)
10. [User Management API](#10-user-management-api)
11. [Data Seeding API](#11-data-seeding-api)
12. [Error Handling](#12-error-handling)
13. [Usage Examples](#13-usage-examples)

---

## 1. Overview

**Refs:** [BE 1.1, 1.2]

### 1.1 Architecture
Chiby Chiby Store menggunakan MVVM architecture dengan service layer yang menyediakan business logic untuk UI layer. Semua service menggunakan dependency injection dengan Hilt dan mengembalikan `Result<T>` untuk error handling yang konsisten.

### 1.2 Common Patterns
- **Result-based Error Handling**: Semua methods mengembalikan `Result<T>` untuk success/error handling
- **Reactive Programming**: Menggunakan `Flow<T>` untuk real-time data updates
- **Dependency Injection**: Semua services diinject melalui Hilt
- **Indonesian Localization**: Error messages dalam bahasa Indonesia

### 1.3 Data Types
- **Entities**: Room database entities (Pengguna, Produk, Penjualan, etc.)
- **Result<T>**: Custom Result class untuk error handling
- **Flow<T>**: Reactive streams untuk real-time updates

---

## 2. Authentication API

**Refs:** [BE 4.1, 4.2]

### AuthService Interface

```kotlin
interface AuthService {
    suspend fun login(username: String, password: String): Result<Pengguna>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Pengguna?
    suspend fun hasPermission(permission: String): Boolean
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    fun observeCurrentUser(): Flow<Pengguna?>
    suspend fun initializeSession(): Result<Unit>
}
```

### Methods

#### login(username: String, password: String): Result<Pengguna>
**Description:** Authenticate user dengan username dan password
**Parameters:**
- `username`: String - Username user
- `password`: String - Password user (plain text, akan di-hash)
**Returns:** `Result<Pengguna>` - User data jika berhasil, error jika gagal
**Throws:** `ValidationError`, `DatabaseError`

#### logout(): Result<Unit>
**Description:** Logout current user dan clear session
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `DatabaseError`

#### getCurrentUser(): Pengguna?
**Description:** Get current logged in user
**Returns:** `Pengguna?` - Current user atau null jika belum login

#### hasPermission(permission: String): Boolean
**Description:** Check jika current user memiliki permission tertentu
**Parameters:**
- `permission`: String - Permission name (e.g., "EDIT_PRODUCT", "VIEW_REPORTS")
**Returns:** `Boolean` - True jika user memiliki permission

#### changePassword(oldPassword: String, newPassword: String): Result<Unit>
**Description:** Change password untuk current user
**Parameters:**
- `oldPassword`: String - Password lama
- `newPassword`: String - Password baru
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `ValidationError`, `PermissionError`

#### observeCurrentUser(): Flow<Pengguna?>
**Description:** Observe changes pada current user state
**Returns:** `Flow<Pengguna?>` - Reactive stream untuk user state

#### initializeSession(): Result<Unit>
**Description:** Initialize session dari stored data saat app start
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `DatabaseError`

---

## 3. Product Management API

**Refs:** [BE 2.1]

### ProductService Interface

```kotlin
interface ProductService {
    suspend fun createProduct(product: Produk): Result<Produk>
    suspend fun updateProduct(product: Produk): Result<Produk>
    suspend fun deleteProduct(id: String): Result<Unit>
    suspend fun getProduct(id: String): Result<Produk?>
    suspend fun getProducts(categoryId: String?, warehouseId: String?, searchQuery: String?): Result<List<Produk>>
    suspend fun searchProducts(query: String): Result<List<Produk>>
    suspend fun updateStock(id: String, quantity: Int): Result<Unit>
    suspend fun getLowStockProducts(): Result<List<Produk>>
    fun observeProducts(): Flow<List<Produk>>
    fun observeProduct(id: String): Flow<Produk?>
}
```

### Methods

#### createProduct(product: Produk): Result<Produk>
**Description:** Membuat produk baru
**Parameters:**
- `product`: Produk - Product data (nama, harga, barcode, dll)
**Returns:** `Result<Produk>` - Created product dengan ID
**Throws:** `ValidationError`, `DatabaseError`

#### updateProduct(product: Produk): Result<Produk>
**Description:** Update produk existing
**Parameters:**
- `product`: Produk - Updated product data
**Returns:** `Result<Produk>` - Updated product
**Throws:** `ValidationError`, `DatabaseError`, `NotFoundError`

#### deleteProduct(id: String): Result<Unit>
**Description:** Hapus produk
**Parameters:**
- `id`: String - Product ID
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `DatabaseError`, `BusinessLogicError`

#### getProduct(id: String): Result<Produk?>
**Description:** Get produk by ID
**Parameters:**
- `id`: String - Product ID
**Returns:** `Result<Produk?>` - Product atau null jika tidak ditemukan

#### getProducts(categoryId: String?, warehouseId: String?, searchQuery: String?): Result<List<Produk>>
**Description:** Get semua produk dengan filter opsional
**Parameters:**
- `categoryId`: String? - Filter by category
- `warehouseId`: String? - Filter by warehouse
- `searchQuery`: String? - Search query
**Returns:** `Result<List<Produk>>` - List of products

#### searchProducts(query: String): Result<List<Produk>>
**Description:** Search produk berdasarkan nama atau barcode
**Parameters:**
- `query`: String - Search query
**Returns:** `Result<List<Produk>>` - Matching products

#### updateStock(id: String, quantity: Int): Result<Unit>
**Description:** Update stok produk
**Parameters:**
- `id`: String - Product ID
- `quantity`: Int - New stock quantity
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `ValidationError`, `DatabaseError`

#### getLowStockProducts(): Result<List<Produk>>
**Description:** Get produk dengan stok rendah (di bawah min_stock)
**Returns:** `Result<List<Produk>>` - Low stock products

#### observeProducts(): Flow<List<Produk>>
**Description:** Observe semua produk changes
**Returns:** `Flow<List<Produk>>` - Reactive product list

#### observeProduct(id: String): Flow<Produk?>
**Description:** Observe specific product changes
**Parameters:**
- `id`: String - Product ID
**Returns:** `Flow<Produk?>` - Reactive product data

---

## 4. Warehouse Management API

**Refs:** [BE 2.1.4]

### WarehouseService Interface

```kotlin
interface WarehouseService {
    suspend fun createWarehouse(warehouse: Gudang): Result<Gudang>
    suspend fun updateWarehouse(warehouse: Gudang): Result<Gudang>
    suspend fun deleteWarehouse(id: String): Result<Unit>
    suspend fun getWarehouse(id: String): Result<Gudang?>
    suspend fun getWarehouses(): Result<List<Gudang>>
    suspend fun assignProductToWarehouse(productId: String, warehouseId: String): Result<Unit>
    suspend fun transferStock(productId: String, fromWarehouseId: String, toWarehouseId: String, quantity: Int): Result<Unit>
    suspend fun getWarehouseStock(warehouseId: String): Result<List<Produk>>
    suspend fun getAllWarehouseStock(): Result<Map<String, List<Produk>>>
    fun observeWarehouses(): Flow<List<Gudang>>
    fun observeWarehouseStock(warehouseId: String): Flow<List<Produk>>
}
```

### Methods

#### createWarehouse(warehouse: Gudang): Result<Gudang>
**Description:** Membuat gudang baru
**Parameters:**
- `warehouse`: Gudang - Warehouse data
**Returns:** `Result<Gudang>` - Created warehouse
**Throws:** `ValidationError`, `DatabaseError`

#### assignProductToWarehouse(productId: String, warehouseId: String): Result<Unit>
**Description:** Assign produk ke gudang tertentu
**Parameters:**
- `productId`: String - Product ID
- `warehouseId`: String - Warehouse ID
**Returns:** `Result<Unit>` - Success jika berhasil

#### transferStock(productId: String, fromWarehouseId: String, toWarehouseId: String, quantity: Int): Result<Unit>
**Description:** Transfer stok antar gudang
**Parameters:**
- `productId`: String - Product ID
- `fromWarehouseId`: String - Source warehouse ID
- `toWarehouseId`: String - Target warehouse ID
- `quantity`: Int - Quantity to transfer
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `ValidationError`, `BusinessLogicError`

---

## 5. Sales Management API

**Refs:** [BE 2.2]

### SaleService Interface

```kotlin
interface SaleService {
    suspend fun createSale(sale: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems>
    suspend fun getSale(id: Long): Result<PenjualanWithItems?>
    suspend fun getSales(startDate: String?, endDate: String?, cashierId: Long?): Result<List<Penjualan>>
    suspend fun searchSales(query: String): Result<List<Penjualan>>
    suspend fun updateSale(id: Long, sale: Penjualan): Result<Penjualan>
    suspend fun deleteSale(id: Long): Result<Unit>
    suspend fun refundSale(id: Long): Result<Unit>
    suspend fun cancelSale(id: Long): Result<Unit>
    fun observeSales(): Flow<List<Penjualan>>
    fun observeSale(id: Long): Flow<PenjualanWithItems?>
}
```

### Methods

#### createSale(sale: Penjualan, items: List<ItemPenjualan>): Result<PenjualanWithItems>
**Description:** Membuat penjualan baru dengan items
**Parameters:**
- `sale`: Penjualan - Sale header data
- `items`: List<ItemPenjualan> - Sale items
**Returns:** `Result<PenjualanWithItems>` - Complete sale dengan items
**Throws:** `ValidationError`, `BusinessLogicError`

#### refundSale(id: Long): Result<Unit>
**Description:** Refund penjualan dan restore inventory
**Parameters:**
- `id`: Long - Sale ID
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `BusinessLogicError`, `DatabaseError`

#### cancelSale(id: Long): Result<Unit>
**Description:** Cancel penjualan dan restore inventory
**Parameters:**
- `id`: Long - Sale ID
**Returns:** `Result<Unit>` - Success jika berhasil
**Throws:** `BusinessLogicError`, `DatabaseError`

---

## 6. Reporting API

**Refs:** [BE 2.3]

### ReportingService Interface

```kotlin
interface ReportingService {
    suspend fun getGrossSalesReport(startDate: LocalDate?, endDate: LocalDate?): Result<GrossSalesReport>
    suspend fun getProfitMarginReport(startDate: LocalDate?, endDate: LocalDate?): Result<ProfitMarginReport>
    suspend fun getNetProfitReport(startDate: LocalDate?, endDate: LocalDate?): Result<NetProfitReport>
    suspend fun getSalesByProductReport(startDate: LocalDate?, endDate: LocalDate?): Result<List<ProductSales>>
    suspend fun getSalesByCategoryReport(startDate: LocalDate?, endDate: LocalDate?): Result<List<CategorySales>>
    suspend fun getSalesTrendReport(startDate: LocalDate?, endDate: LocalDate?): Result<List<TrendData>>
    suspend fun getIncomeStatementReport(startDate: LocalDate?, endDate: LocalDate?): Result<IncomeStatement>
    suspend fun getCashFlowReport(startDate: LocalDate?, endDate: LocalDate?): Result<CashFlow>
    suspend fun getExpenseReport(startDate: LocalDate?, endDate: LocalDate?): Result<ExpenseReport>
    suspend fun getBalanceSheetReport(date: LocalDate?): Result<BalanceSheet>
    suspend fun getTaxReport(startDate: LocalDate?, endDate: LocalDate?): Result<TaxReport>
}
```

### Data Classes

```kotlin
data class GrossSalesReport(
    val totalSales: Double,
    val totalTransactions: Int,
    val averageTransaction: Double,
    val period: String
)

data class ProfitMarginReport(
    val totalRevenue: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val profitMargin: Double,
    val period: String
)

data class ProductSales(
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalCost: Double,
    val profit: Double
)
```

### Methods

#### getGrossSalesReport(startDate: LocalDate?, endDate: LocalDate?): Result<GrossSalesReport>
**Description:** Get laporan penjualan kotor
**Parameters:**
- `startDate`: LocalDate? - Start date filter
- `endDate`: LocalDate? - End date filter
**Returns:** `Result<GrossSalesReport>` - Sales summary data

#### getSalesByProductReport(startDate: LocalDate?, endDate: LocalDate?): Result<List<ProductSales>>
**Description:** Get penjualan per produk
**Parameters:**
- `startDate`: LocalDate? - Start date filter
- `endDate`: LocalDate? - End date filter
**Returns:** `Result<List<ProductSales>>` - Product sales data

---

## 7. Financial Management API

**Refs:** [BE 2.5]

### ExpenseService Interface

```kotlin
interface ExpenseService {
    suspend fun createExpense(expense: Pengeluaran): Result<Pengeluaran>
    suspend fun updateExpense(expense: Pengeluaran): Result<Pengeluaran>
    suspend fun deleteExpense(id: Long): Result<Unit>
    suspend fun getExpense(id: Long): Result<Pengeluaran?>
    suspend fun getExpenses(startDate: LocalDate?, endDate: LocalDate?, category: String?): Result<List<Pengeluaran>>
    suspend fun approveExpense(id: Long): Result<Unit>
    fun observeAllPengeluaran(): Flow<List<Pengeluaran>>
    fun observeByCategory(category: String): Flow<List<Pengeluaran>>
    fun observeUnapproved(): Flow<List<Pengeluaran>>
}
```

### CashManagementService Interface

```kotlin
interface CashManagementService {
    suspend fun getOperatingCashFlow(startDate: LocalDate?, endDate: LocalDate?): Result<Double>
    suspend fun getInvestingCashFlow(startDate: LocalDate?, endDate: LocalDate?): Result<Double>
    suspend fun getFinancingCashFlow(startDate: LocalDate?, endDate: LocalDate?): Result<Double>
    suspend fun getNetCashFlow(startDate: LocalDate?, endDate: LocalDate?): Result<Double>
    suspend fun getCashPosition(): Result<Double>
}
```

### BalanceSheetService Interface

```kotlin
interface BalanceSheetService {
    suspend fun calculateAssets(): Result<Double>
    suspend fun calculateLiabilities(): Result<Double>
    suspend fun calculateEquity(): Result<Double>
    suspend fun generateBalanceSheet(date: LocalDate?): Result<BalanceSheet>
}
```

---

## 8. Barcode Management API

**Refs:** [BE 2.4]

### BarcodeService Interface

```kotlin
interface BarcodeService {
    suspend fun generateBarcode(productId: String, format: BarcodeFormat, size: LabelSize): Result<BarcodeData>
    suspend fun scanBarcode(imageData: ByteArray): Result<String>
    suspend fun validateBarcode(code: String): Result<Boolean>
    suspend fun getSupportedFormats(): Result<List<BarcodeFormat>>
    suspend fun generateEAN13Barcode(code: String): Result<BarcodeData>
    suspend fun generateCode128Barcode(code: String): Result<BarcodeData>
    suspend fun generateQRBarcode(code: String): Result<BarcodeData>
}
```

### Enums

```kotlin
enum class BarcodeFormat {
    EAN13, CODE128, QR, DATAMATRIX
}

enum class LabelSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}
```

### Methods

#### generateBarcode(productId: String, format: BarcodeFormat, size: LabelSize): Result<BarcodeData>
**Description:** Generate barcode untuk produk
**Parameters:**
- `productId`: String - Product ID
- `format`: BarcodeFormat - Barcode format
- `size`: LabelSize - Label size
**Returns:** `Result<BarcodeData>` - Barcode image data

#### scanBarcode(imageData: ByteArray): Result<String>
**Description:** Scan barcode dari image
**Parameters:**
- `imageData`: ByteArray - Image byte data
**Returns:** `Result<String>` - Decoded barcode string

---

## 9. Backup & Restore API

**Refs:** [BE 2.6]

### BackupService Interface

```kotlin
interface BackupService {
    suspend fun createBackup(): Result<BackupFile>
    suspend fun deleteBackup(filename: String): Result<Unit>
    suspend fun getBackupHistory(): Result<List<BackupInfo>>
    suspend fun validateBackup(filename: String): Result<BackupMetadata>
    fun observeBackupProgress(): Flow<BackupProgress>
}
```

### RestoreService Interface

```kotlin
interface RestoreService {
    suspend fun restoreFromBackup(filename: String): Result<Unit>
    suspend fun previewBackup(filename: String): Result<RestorePreview>
    suspend fun validateBackupFile(filename: String): Result<Boolean>
    fun observeRestoreProgress(): Flow<RestoreProgress>
}
```

---

## 10. User Management API

**Refs:** [BE 4.1]

### UserManagementService Interface

```kotlin
interface UserManagementService {
    suspend fun createUser(user: Pengguna): Result<Pengguna>
    suspend fun updateUser(user: Pengguna): Result<Pengguna>
    suspend fun deleteUser(id: Long): Result<Unit>
    suspend fun getUser(id: Long): Result<Pengguna?>
    suspend fun getUsers(): Result<List<Pengguna>>
    suspend fun changeUserPassword(id: Long, newPassword: String): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<Pengguna>>
    fun observeUsers(): Flow<List<Pengguna>>
}
```

---

## 11. Data Seeding API

**Refs:** [Req 7.3]

### DataSeedingService Interface

```kotlin
interface DataSeedingService {
    suspend fun seedAllData(): Result<Unit>
    suspend fun seedUsers(): Result<Unit>
    suspend fun seedCategories(): Result<Unit>
    suspend fun seedWarehouses(): Result<Unit>
    suspend fun seedSuppliers(): Result<Unit>
    suspend fun seedProducts(): Result<Unit>
    suspend fun clearAllData(): Result<Unit>
}
```

---

## 12. Error Handling

**Refs:** [BE 5.1, 5.2]

### Error Types

```kotlin
sealed class ChibyChibyException : Exception() {
    data class ValidationError(val message: String) : ChibyChibyException()
    data class DatabaseError(val message: String) : ChibyChibyException()
    data class PermissionError(val message: String) : ChibyChibyException()
    data class BusinessLogicError(val message: String) : ChibyChibyException()
    data class NetworkError(val message: String) : ChibyChibyException()
    data class NotFoundError(val message: String) : ChibyChibyException()
}
```

### Result<T> Class

```kotlin
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: ChibyChibyException) : Result<Nothing>()
}
```

### Error Handling Pattern

```kotlin
// Usage pattern
val result = productService.createProduct(product)
when (result) {
    is Result.Success -> {
        // Handle success
        val createdProduct = result.data
    }
    is Result.Error -> {
        // Handle error
        when (result.exception) {
            is ValidationError -> showValidationError(result.exception.message)
            is DatabaseError -> showDatabaseError(result.exception.message)
            else -> showGenericError(result.exception.message)
        }
    }
}
```

---

## 13. Usage Examples

**Refs:** [BE 1.2]

### 13.1 Authentication Flow

```kotlin
// Login
val loginResult = authService.login("admin", "password")
when (loginResult) {
    is Result.Success -> {
        val user = loginResult.data
        // Navigate to main screen
    }
    is Result.Error -> {
        // Show error message
    }
}

// Check permission
if (authService.hasPermission("EDIT_PRODUCT")) {
    // Show edit button
}
```

### 13.2 Product Management

```kotlin
// Create product
val product = Produk(
    nama = "Indomie Goreng",
    hargaBeli = 2500.0,
    hargaJual = 3000.0,
    barcode = "8996001600017",
    kategoriId = categoryId,
    gudangId = warehouseId
)

val result = productService.createProduct(product)
```

### 13.3 Sales Transaction

```kotlin
// Create sale
val sale = Penjualan(
    tanggalPenjualan = LocalDateTime.now(),
    totalAmount = 15000.0,
    metodePembayaran = "CASH",
    kasirId = currentUser.id
)

val items = listOf(
    ItemPenjualan(produkId = 1, jumlah = 2, hargaSatuan = 3000.0),
    ItemPenjualan(produkId = 2, jumlah = 3, hargaSatuan = 2500.0)
)

val result = saleService.createSale(sale, items)
```

### 13.4 Reactive Data Observation

```kotlin
// Observe products in ViewModel
class InventoryViewModel @Inject constructor(
    private val productService: ProductService
) : ViewModel() {

    val products = productService.observeProducts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lowStockProducts = productService.observeProducts()
        .map { products -> products.filter { it.stok < it.minStok } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

### 13.5 Reporting

```kotlin
// Get sales report
val reportResult = reportingService.getGrossSalesReport(
    startDate = LocalDate.now().minusDays(30),
    endDate = LocalDate.now()
)

when (reportResult) {
    is Result.Success -> {
        val report = reportResult.data
        // Display report data
    }
    is Result.Error -> {
        // Handle error
    }
}
```

### 13.6 Backup & Restore

```kotlin
// Create backup
val backupResult = backupService.createBackup()
when (backupResult) {
    is Result.Success -> {
        val backupFile = backupResult.data
        // Show success message
    }
    is Result.Error -> {
        // Handle backup error
    }
}

// Restore from backup
val restoreResult = restoreService.restoreFromBackup("backup_2025-12-11.enc")
```

---

## 14. Performance Considerations

### 14.1 Database Optimization
- Indexes pada kolom yang sering query (barcode, tanggal, kategori)
- Foreign key constraints untuk data integrity
- Transaction untuk operasi kompleks

### 14.2 Memory Management
- Flow untuk reactive updates tanpa memory leaks
- Coroutine scope management
- Efficient data structures

### 14.3 Error Handling
- Centralized error handling
- User-friendly error messages
- Logging untuk debugging

---

## 15. Security Considerations

### 15.1 Authentication
- Password hashing dengan SHA-256
- Session management
- Role-based access control

### 15.2 Data Protection
- AES256-GCM encryption untuk backup
- Secure key storage dengan Android KeyStore
- Input validation dan sanitization

### 15.3 Permissions
- Runtime permissions untuk camera dan storage
- Permission checks sebelum operasi sensitif
- Granular permission system

---

## 16. Testing Guidelines

### 16.1 Unit Testing
- Test semua service methods
- Mock dependencies dengan Mockito
- Test success dan error scenarios

### 16.2 Integration Testing
- Test service-to-database integration
- Test cross-service interactions
- Test reactive flows

### 16.3 UI Testing
- Test ViewModel state management
- Test user interaction flows
- Test error handling in UI

---

## 17. Migration Guide

### 17.1 Version Compatibility
- API contracts backward compatible
- Database migration dengan Room
- Configuration changes handling

### 17.2 Breaking Changes
- Major version changes untuk breaking changes
- Migration guides untuk upgrades
- Deprecation warnings untuk deprecated APIs

---

## 18. Support & Maintenance

### 18.1 Logging
- Comprehensive logging untuk debugging
- Error tracking dan reporting
- Performance monitoring

### 18.2 Monitoring
- Service health checks
- Database connection monitoring
- Memory usage tracking

### 18.3 Documentation Updates
- API documentation updates
- Code comments maintenance
- User guide updates

---

*API Documentation ini dibuat untuk developer yang akan mengintegrasikan atau mengembangkan fitur baru pada Chiby Chiby Store. Semua interface menggunakan Kotlin dengan coroutine untuk async operations dan Result<T> untuk error handling yang konsisten.*