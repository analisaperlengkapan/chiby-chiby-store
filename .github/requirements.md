# Requirements Document

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Draft

## Change History
| Versi | Tanggal | Perubahan | Author |
|-------|---------|-----------|--------|
| 1.0   | December 11, 2025 | Initial requirements based on user analysis | AI Assistant |

## Daftar Isi
1. Project Overview
2. User Roles & Permissions
3. Functional Requirements
4. Non-Functional Requirements
5. Data Requirements
6. Integration Requirements
7. Constraints & Assumptions
8. Success Criteria

---

## 1. Project Overview
### 1.1 Project Goals
Aplikasi Android "Chiby Chiby Store" bertujuan untuk menyediakan sistem manajemen retail lengkap dalam satu aplikasi mobile offline-first, yang mencakup inventory, sales, reporting, dan financial management untuk meningkatkan efisiensi operasional toko retail.

### 1.2 Target Users
Pemilik toko retail kecil hingga menengah yang membutuhkan solusi POS mobile yang sederhana namun komprehensif.

### 1.3 Scope
- Single store management
- Offline-first mobile application
- Local data storage dengan backup/restore
- Hardware integration: Barcode scanner (camera), thermal printer
- Future-ready untuk online integration

### 1.4 Tech Stack
- Platform: Android (min API 21)
- Language: Kotlin
- Database: Room/SQLite
- UI Framework: Android Jetpack Compose
- Barcode: ZXing library
- Printing: Bluetooth thermal printer SDK

### 1.5 Project Structure
Modular architecture dengan 5 modul utama: Inventory, Sales, Reporting, Barcode, Financial.

## 2. User Roles & Permissions
### 2.1 Owner/Admin
- Akses penuh ke semua fitur
- Manajemen user dan permissions
- Backup/restore data
- Semua laporan dan konfigurasi

### 2.2 Manager
- Akses ke laporan penjualan dan keuangan
- Approval untuk transaksi besar
- Manajemen inventory (view dan edit)
- Tidak bisa hapus data atau ubah konfigurasi sistem

### 2.3 Cashier
- Checkout penjualan dengan barcode scan
- View inventory untuk penjualan
- Input manual penjualan
- View laporan penjualan harian

### 2.4 Warehouse Staff
- Manajemen inventory (add, edit, stock adjustment)
- Manajemen gudang (lokasi penyimpanan)
- Print barcode labels
- View inventory reports

## 3. Functional Requirements
### 3.1 Inventory Management
#### 3.1.1 Purchase Stock Management
- Tambah produk baru dengan detail (nama, harga beli/jual, kategori, barcode)
- Update stok dari pembelian supplier
- Track supplier information
- History pembelian dengan tanggal dan jumlah

#### 3.1.2 Warehouse Management
- Setup multiple warehouse locations
- Assign products to warehouse locations
- Stock transfer antar lokasi
- Low stock alerts
- Inventory audit functionality

### 3.2 Sales Management
#### 3.2.1 Sales Transaction
- Point of sale interface dengan search produk
- Barcode scanning untuk quick add
- Manual input untuk produk tanpa barcode
- Calculate total, tax, discount
- Payment methods: Cash, card (future)

#### 3.2.2 Sales History
- View daily/weekly/monthly sales
- Search sales by date, product, customer
- Refund/cancel transactions
- Sales receipt printing

### 3.3 Reporting
#### 3.3.1 Sales Reports
- Gross sales report
- Profit margin report
- Net profit report
- Sales by product/category
- Sales trend analysis

#### 3.3.2 Financial Reports
- Income statement
- Cash flow report
- Expense report
- Balance sheet
- Tax calculation reports

### 3.4 Barcode Management
#### 3.4.1 Label Printing
- Generate barcode sesuai standar Indonesia
- Multiple size options: Small (2x1 cm), Medium (3x2 cm), Large (3x2 cm), Large (5x3 cm), Extra Large (7x4 cm)
- Print multiple labels per product
- Custom label design (product name, price, barcode)

#### 3.4.2 Barcode Scanning
- Camera-based barcode scanning
- Support multiple barcode formats (EAN-13, Code 128, QR)
- Quick checkout process
- Inventory scanning for stock count

### 3.5 Financial Management
#### 3.5.1 Cash Management
- Cash register opening/closing
- Daily cash reconciliation
- Cash flow tracking
- Petty cash management

#### 3.5.2 Expense Management
- Record operational expenses
- Categorize expenses (utilities, supplies, etc.)
- Expense approval workflow
- Expense reports

#### 3.5.3 Balance Sheet
- Asset tracking (inventory value)
- Liability tracking
- Equity calculation
- Monthly balance sheet generation

## 4. Non-Functional Requirements
### 4.1 Performance
- App startup < 3 seconds
- Transaction processing < 1 second
- Support up to 10,000 products
- Support up to 100,000 transactions

### 4.2 Security
- User authentication with username/password
- Data encryption for sensitive information
- Role-based access control
- Secure backup files

### 4.3 Usability
- Intuitive mobile UI dengan bottom navigation
- Offline operation tanpa internet
- Responsive design untuk berbagai screen sizes
- Indonesian language interface

### 4.4 Reliability
- Data persistence during app crashes
- Automatic backup reminders
- Data integrity checks
- Recovery from corrupted data

## 5. Data Requirements
### 5.1 Data Entities
- Products (id, name, barcode, category, cost_price, selling_price, stock_quantity, warehouse_location)
- Suppliers (id, name, contact, address)
- Sales (id, date, items[], total_amount, payment_method, cashier_id)
- Purchases (id, date, supplier_id, items[], total_amount)
- Users (id, username, password_hash, role, permissions)
- Expenses (id, date, category, amount, description, approved_by)
- Warehouses (id, name, location, capacity)

### 5.2 Data Relationships
- Product belongs to Category (many-to-one)
- Product stored in Warehouse (many-to-one)
- Sales has many SaleItems (one-to-many)
- Purchase has many PurchaseItems (one-to-many)
- User creates Sales/Expenses (one-to-many)

### 5.3 Data Validation
- Product barcode must be unique
- Stock quantity cannot be negative
- Sales total must match item calculations
- User passwords must be hashed
- Financial amounts must be positive

## 6. Integration Requirements
### 6.1 Hardware Integration
- Bluetooth thermal printer untuk receipt dan label printing
- Camera API untuk barcode scanning
- Optional: External barcode scanner hardware

### 6.2 Future Online Integration
- API-ready architecture untuk cloud sync
- Data export/import in JSON/CSV format
- Webhook support untuk external integrations
- Multi-device sync preparation

## 7. Constraints & Assumptions
### 7.1 Technical Constraints
- Android only (no iOS support initially)
- Offline-first (no real-time sync required)
- Local storage only (no cloud dependency)
- Minimum Android API 21 (Android 5.0)

### 7.2 Business Constraints
- Single store operation only
- Indonesian market focus
- Retail business type (no service industry)
- Manual data entry for initial setup

### 7.3 Assumptions
- Users have basic Android device knowledge
- Internet available for app updates only
- Thermal printer uses standard ESC/POS commands
- Barcode standards follow GS1 Indonesia

## 8. Success Criteria
### 8.1 Functional Completeness
- All 5 main modules implemented and working
- User roles and permissions enforced
- Hardware integration functional
- Backup/restore working correctly

### 8.2 Performance Metrics
- Transaction processing < 1 second
- App startup < 3 seconds
- Zero data loss in normal operation
- Support 100 concurrent users (future)

### 8.3 User Satisfaction
- Intuitive UI reducing training time
- Reliable offline operation
- Accurate financial reporting
- Easy backup/restore process

### 8.4 Technical Quality
- Code coverage > 80%
- No critical bugs in production
- Secure data handling
- Future-ready architecture