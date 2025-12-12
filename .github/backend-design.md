# Backend Design Document

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Draft
**Referensi Dokumen:** requirements.md, database-design.md

## 1. Architecture Overview
**Refs:** [Req 4.1, 4.3] [DB 1.1]
### 1.1 Architecture Pattern
MVVM dengan Repository pattern, offline-first dengan local database.

### 1.2 Tech Stack
- Kotlin Coroutines untuk async operations
- Room DAO untuk data access
- Dependency injection dengan Hilt
- Business logic dalam Use Cases/Services

## 4. Authentication & Authorization
**Refs:** [Req 2.1, 2.2, 4.2] [DB 2.1]
### 4.1 Auth Service
- login(username, password): User?
- logout(): void
- getCurrentUser(): User?
- hasPermission(permission): boolean
- changePassword(old, new): boolean

### 4.2 Authorization Logic
- Role-based access control
- Permission checking sebelum setiap operation
- Session management dengan SharedPreferences

## 2. API Design
### 2.1 Inventory Management API
**Refs:** [Req 3.1] [DB 2.4, 2.5, 2.6, 2.7]
#### 2.1.1 Product Service
- createProduct(product): Product
- updateProduct(id, product): Product
- deleteProduct(id): boolean
- getProduct(id): Product?
- getProducts(filter): List<Product>
- searchProducts(query): List<Product>
- updateStock(id, quantity): boolean
- getLowStockProducts(): List<Product>

#### 2.1.2 Supplier Service
- createSupplier(supplier): Supplier
- updateSupplier(id, supplier): Supplier
- getSuppliers(): List<Supplier>

#### 2.1.3 Purchase Service
- createPurchase(purchase): Purchase
- getPurchases(dateRange): List<Purchase>
- getPurchaseDetails(id): PurchaseWithItems

#### 2.1.4 Warehouse Service
- createWarehouse(warehouse): Warehouse
- assignProductToWarehouse(productId, warehouseId): boolean
- transferStock(productId, fromWarehouse, toWarehouse, quantity): boolean
- getWarehouseStock(warehouseId): List<Product>

### 2.2 Sales Management API
**Refs:** [Req 3.2] [DB 2.8, 2.9]
#### 2.2.1 Sales Service
- createSale(sale): Sale
- getSale(id): SaleWithItems?
- getSales(dateRange): List<Sale>
- searchSales(query): List<Sale>
- refundSale(id): boolean
- cancelSale(id): boolean
- printReceipt(saleId): boolean

#### 2.2.2 Sales Logic Flow
1. Start transaction
2. Add products (scan barcode or manual search)
3. Calculate totals (subtotal, tax, discount)
4. Process payment
5. Update inventory stock
6. Generate receipt
7. Commit transaction

### 2.3 Reporting API
**Refs:** [Req 3.3] [DB 2.8, 2.9, 2.10]
#### 2.3.1 Sales Reports Service
- getGrossSales(dateRange): Report
- getProfitMargin(dateRange): Report
- getNetProfit(dateRange): Report
- getSalesByProduct(dateRange): List<ProductSales>
- getSalesByCategory(dateRange): List<CategorySales>
- getSalesTrend(period): List<TrendData>

#### 2.3.2 Financial Reports Service
- getIncomeStatement(dateRange): IncomeStatement
- getCashFlow(dateRange): CashFlow
- getExpenseReport(dateRange): ExpenseReport
- getBalanceSheet(date): BalanceSheet
- getTaxReport(dateRange): TaxReport

#### 2.3.3 Report Logic
- Aggregate data from sales, purchases, expenses
- Calculate profit = sales - cost_of_goods_sold - expenses
- Generate charts data for UI
- Export reports to PDF/Excel format

### 2.4 Barcode Management API
**Refs:** [Req 3.4] [DB 2.4]
#### 2.4.1 Barcode Service
- generateBarcode(productId, format, size): BarcodeData
- scanBarcode(image): Product?
- validateBarcode(code): boolean
- getSupportedFormats(): List<String>

#### 2.4.2 Label Printing Service
- printLabel(productId, size, quantity): boolean
- printReceipt(saleId): boolean
- getPrinterStatus(): PrinterStatus
- connectPrinter(address): boolean

#### 2.4.3 Barcode Logic Flow
1. Generate barcode image using ZXing
2. Format label dengan product info
3. Send to Bluetooth printer
4. Handle print errors and retries

### 2.5 Financial Management API
**Refs:** [Req 3.5] [DB 2.10]
#### 2.5.1 Cash Management Service
- createExpense(expense): Expense
- approveExpense(id): boolean
- getExpenses(dateRange): List<Expense>
- getExpenseCategories(): List<String>

#### 2.5.2 Balance Sheet Service
- calculateAssets(): double
- calculateLiabilities(): double
- calculateEquity(): double
- generateBalanceSheet(date): BalanceSheet

#### 2.5.3 Financial Logic
- Track inventory value as assets
- Calculate COGS from purchases
- Monitor cash flow daily
- Generate monthly financial statements

### 2.6 Backup/Restore API
**Refs:** [Req 4.4, 7.1]
#### 2.6.1 Backup Service
- createBackup(): BackupFile
- scheduleBackup(frequency): boolean
- getBackupHistory(): List<Backup>
- validateBackup(file): boolean

#### 2.6.2 Restore Service
- restoreFromBackup(file): boolean
- partialRestore(data, dateRange): boolean
- getRestorePreview(file): RestorePreview

#### 2.6.3 Backup Logic
1. Export all tables to JSON
2. Encrypt dengan AES
3. Save to external storage
4. Verify integrity

## 5. Error Handling
**Refs:** [Req 4.4]
### 5.1 Error Types
- ValidationError: Invalid input data
- DatabaseError: SQLite constraint violations
- NetworkError: Printer connectivity issues
- PermissionError: Access denied
- BusinessLogicError: Invalid business rules

### 5.2 Error Handling Strategy
- Centralized error handler
- User-friendly error messages in Indonesian
- Automatic retry untuk transient errors
- Logging untuk debugging

## 6. Services & Dependencies
**Refs:** [Req 6.1, 6.2]
### 6.1 Core Services
- DatabaseService: Room database instance
- AuthService: Authentication management
- PermissionService: Access control
- NotificationService: Local notifications

### 6.2 External Dependencies
- ZXing: Barcode generation/scanning
- BluetoothPrinter SDK: Thermal printing
- Encryption library: Data security
- PDF generator: Report export

### 6.3 Future Integration Services
- ApiClient: HTTP client untuk cloud sync
- SyncService: Data synchronization
- WebhookService: External integrations
- CloudStorage: Backup to cloud