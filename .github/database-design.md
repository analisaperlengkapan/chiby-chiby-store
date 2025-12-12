# Database Design Document

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Draft
**Referensi Dokumen:** requirements.md
**Refs:** [Req 5.1, 5.2, 5.3]

## 1. Overview
**Refs:** [Req 5.1]
### 1.1 Database Type
SQLite dengan Room ORM untuk Android, offline-first dengan local storage.

### 1.2 Design Principles
- Normalisasi 3NF untuk data integrity
- Foreign keys untuk referential integrity
- Indexes pada kolom yang sering query (barcode, date, category)
- Data types sesuai Android Room

## 2. Entity Definitions
### 2.1 Users
**Refs:** [Req 2.1, 2.2, 2.3, 2.4]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| username | TEXT | UNIQUE, NOT NULL | Username untuk login |
| password_hash | TEXT | NOT NULL | Hash password (SHA-256) |
| role | TEXT | NOT NULL, CHECK(role IN ('OWNER', 'MANAGER', 'CASHIER', 'WAREHOUSE')) | Role user |
| permissions | TEXT |  | JSON string permissions |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal diupdate |

**Indexes:**
- idx_users_username ON (username)

### 2.2 Categories
**Refs:** [Req 3.1.1, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| name | TEXT | UNIQUE, NOT NULL | Nama kategori |
| description | TEXT |  | Deskripsi kategori |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |

**Indexes:**
- idx_categories_name ON (name)

### 2.3 Warehouses
**Refs:** [Req 3.1.2, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| name | TEXT | UNIQUE, NOT NULL | Nama gudang |
| location | TEXT |  | Lokasi gudang |
| capacity | INTEGER | DEFAULT 0 | Kapasitas gudang |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |

**Indexes:**
- idx_warehouses_name ON (name)

### 2.4 Products
**Refs:** [Req 3.1.1, 3.1.2, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| name | TEXT | NOT NULL | Nama produk |
| barcode | TEXT | UNIQUE | Barcode produk |
| category_id | INTEGER | FK to categories.id | ID kategori |
| cost_price | REAL | NOT NULL, CHECK(cost_price >= 0) | Harga beli |
| selling_price | REAL | NOT NULL, CHECK(selling_price >= 0) | Harga jual |
| stock_quantity | INTEGER | NOT NULL, DEFAULT 0, CHECK(stock_quantity >= 0) | Jumlah stok |
| warehouse_id | INTEGER | FK to warehouses.id | ID gudang |
| min_stock | INTEGER | DEFAULT 0 | Minimum stok alert |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal diupdate |

**Indexes:**
- idx_products_barcode ON (barcode)
- idx_products_category ON (category_id)
- idx_products_warehouse ON (warehouse_id)

### 2.5 Suppliers
**Refs:** [Req 3.1.1, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| name | TEXT | NOT NULL | Nama supplier |
| contact | TEXT |  | Kontak supplier |
| address | TEXT |  | Alamat supplier |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |

**Indexes:**
- idx_suppliers_name ON (name)

### 2.6 Purchases
**Refs:** [Req 3.1.1, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| supplier_id | INTEGER | FK to suppliers.id | ID supplier |
| purchase_date | DATETIME | NOT NULL | Tanggal pembelian |
| total_amount | REAL | NOT NULL, CHECK(total_amount >= 0) | Total pembelian |
| created_by | INTEGER | FK to users.id | User yang membuat |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |

**Indexes:**
- idx_purchases_date ON (purchase_date)
- idx_purchases_supplier ON (supplier_id)

### 2.7 PurchaseItems
**Refs:** [Req 3.1.1, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| purchase_id | INTEGER | FK to purchases.id | ID pembelian |
| product_id | INTEGER | FK to products.id | ID produk |
| quantity | INTEGER | NOT NULL, CHECK(quantity > 0) | Jumlah dibeli |
| unit_price | REAL | NOT NULL, CHECK(unit_price >= 0) | Harga per unit |
| total_price | REAL | NOT NULL, CHECK(total_price >= 0) | Total harga |

**Indexes:**
- idx_purchase_items_purchase ON (purchase_id)
- idx_purchase_items_product ON (product_id)

### 2.8 Sales
**Refs:** [Req 3.2.1, 3.2.2, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| sale_date | DATETIME | NOT NULL | Tanggal penjualan |
| total_amount | REAL | NOT NULL, CHECK(total_amount >= 0) | Total penjualan |
| payment_method | TEXT | NOT NULL, CHECK(payment_method IN ('CASH', 'CARD')) | Metode pembayaran |
| cashier_id | INTEGER | FK to users.id | ID kasir |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |

**Indexes:**
- idx_sales_date ON (sale_date)
- idx_sales_cashier ON (cashier_id)

### 2.9 SaleItems
**Refs:** [Req 3.2.1, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| sale_id | INTEGER | FK to sales.id | ID penjualan |
| product_id | INTEGER | FK to products.id | ID produk |
| quantity | INTEGER | NOT NULL, CHECK(quantity > 0) | Jumlah terjual |
| unit_price | REAL | NOT NULL, CHECK(unit_price >= 0) | Harga per unit |
| total_price | REAL | NOT NULL, CHECK(total_price >= 0) | Total harga |

**Indexes:**
- idx_sale_items_sale ON (sale_id)
- idx_sale_items_product ON (product_id)

### 2.10 Expenses
**Refs:** [Req 3.5.2, 5.1]

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, AUTO_INCREMENT | Primary key |
| expense_date | DATETIME | NOT NULL | Tanggal pengeluaran |
| category | TEXT | NOT NULL | Kategori pengeluaran |
| amount | REAL | NOT NULL, CHECK(amount >= 0) | Jumlah pengeluaran |
| description | TEXT |  | Deskripsi pengeluaran |
| approved_by | INTEGER | FK to users.id | User yang approve |
| created_by | INTEGER | FK to users.id | User yang membuat |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | Tanggal dibuat |

**Indexes:**
- idx_expenses_date ON (expense_date)
- idx_expenses_category ON (category)

## 3. Relationships
**Refs:** [Req 5.2]

### 3.1 Core Relationships
- Users 1:N Sales (cashier_id)
- Users 1:N Purchases (created_by)
- Users 1:N Expenses (created_by, approved_by)
- Categories 1:N Products (category_id)
- Warehouses 1:N Products (warehouse_id)
- Suppliers 1:N Purchases (supplier_id)
- Purchases 1:N PurchaseItems (purchase_id)
- Products 1:N PurchaseItems (product_id)
- Sales 1:N SaleItems (sale_id)
- Products 1:N SaleItems (product_id)

## 4. ERD (Entity Relationship Diagram)
**Refs:** [Req 5.1, 5.2]

```
[Users] --1:N-- [Sales]
[Users] --1:N-- [Purchases]
[Users] --1:N-- [Expenses]

[Categories] --1:N-- [Products]
[Warehouses] --1:N-- [Products]

[Suppliers] --1:N-- [Purchases]
[Purchases] --1:N-- [PurchaseItems]
[Products] --1:N-- [PurchaseItems]

[Sales] --1:N-- [SaleItems]
[Products] --1:N-- [SaleItems]
```

## 5. Data Validation Rules
**Refs:** [Req 5.3]

### 5.1 Business Rules
- Product barcode must be unique across all products
- Stock quantity cannot be negative (enforced by CHECK constraint)
- Sales total must equal sum of SaleItems total_price
- Purchase total must equal sum of PurchaseItems total_price
- Expense amounts must be positive
- User passwords must be hashed before storage
- Financial calculations must use DECIMAL precision

### 5.2 Referential Integrity
- Cannot delete Category if Products exist
- Cannot delete Warehouse if Products exist
- Cannot delete Supplier if Purchases exist
- Cannot delete User if Sales/Purchases/Expenses exist
- Cannot delete Product if SaleItems/PurchaseItems exist

## 6. Migration Strategy
**Refs:** [Req 7.1]

### 6.1 Initial Setup
- Create database schema on first app launch
- Populate default data (admin user, default categories)
- Backup existing data before migration

### 6.2 Future Migrations
- Use Room migration framework for schema changes
- Support data transformation during upgrades
- Provide rollback capability for failed migrations
- Version control for database schema

### 6.3 Backup/Restore
- Export all data to encrypted JSON file
- Import with validation and conflict resolution
- Support partial restore (by date range)
- Automatic backup scheduling (weekly)