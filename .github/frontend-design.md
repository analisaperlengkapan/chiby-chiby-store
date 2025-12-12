# Frontend Design Document

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Draft
**Referensi Dokumen:** requirements.md, database-design.md, backend-design.md

## 1. Overview
**Refs:** [Req 1.4, 4.3] [DB 1.1] [BE 1.1]
### 1.1 Design Principles
- Material Design 3 dengan Jetpack Compose
- Bottom navigation untuk main modules
- Offline-first UI dengan local state
- Responsive design untuk berbagai screen sizes

### 1.2 Tech Stack
- UI Framework: Android Jetpack Compose
- Navigation: Compose Navigation
- State Management: ViewModel + StateFlow
- Theme: MaterialTheme dengan custom colors
- Icons: Material Icons

### 1.3 Navigation Structure
- Bottom Navigation: Inventory, Sales, Reports, Settings
- Drawer Navigation: Advanced features (Barcode, Backup)
- Modal screens: Login, Product Detail, Sale Detail

## 3. Component Library
**Refs:** [Req 4.3] [BE 1.2]
### 3.1 Shared Components
- AppTopBar: Custom top app bar dengan title dan actions
- BottomNavBar: Bottom navigation dengan 4 tabs
- CardItem: Reusable card untuk list items
- ButtonPrimary: Primary action button
- ButtonSecondary: Secondary action button
- TextFieldOutlined: Outlined text field dengan validation
- DropdownMenu: Selection dropdown
- LoadingIndicator: Circular progress indicator
- ErrorMessage: Error display component
- EmptyState: Empty list state
- SearchBar: Search input dengan filter

### 3.2 Form Components
- ProductForm: Form untuk add/edit product
- SaleForm: POS interface components
- LoginForm: Authentication form
- ReportFilters: Date range and category filters

### 3.3 Dialog Components
- ConfirmDialog: Confirmation dialogs
- DatePickerDialog: Date selection
- NumberPickerDialog: Quantity selection
- PrinterDialog: Printer selection

### 3.4 Special Components
- BarcodeScanner: Camera preview dengan overlay
- ReceiptPreview: Receipt display
- ChartView: Simple bar/line charts untuk reports
- BackupProgress: Progress indicator untuk backup/restore

### 3.5 Theme & Styling
- ColorScheme: Primary (Blue), Secondary (Green), Error (Red)
- Typography: Display, Headline, Title, Body, Label
- Shapes: Rounded corners untuk cards dan buttons
- Dark mode support (future)

### 2.1 Login Page
**Refs:** [Req 2.1, 4.2] [BE 4.1]
**Route:** /login
**Components:** LoginForm, ButtonPrimary, ErrorMessage
**API:** AuthService.login()
**State:** LoginViewModel (username, password, loading, error)
**Layout:** Centered form dengan logo, username/password fields, login button

### 2.2 Dashboard Page
**Refs:** [Req 1.5, 3.3] [BE 2.3]
**Route:** /dashboard
**Components:** CardItem, ChartView, BottomNavBar
**API:** ReportingService.getSummary()
**State:** DashboardViewModel (todaySales, lowStock, recentTransactions)
**Layout:** Grid cards showing key metrics, recent activity list, quick actions

### 2.3 Settings Page
**Refs:** [Req 2.1, 4.4] [BE 4.1, 2.6]
**Route:** /settings
**Components:** CardItem, ButtonSecondary, ConfirmDialog
**API:** AuthService.changePassword(), BackupService.createBackup()
**State:** SettingsViewModel (currentUser, backupStatus)
**Layout:** List of settings options: Profile, Backup, About, Logout
**Features:** Change password, manual backup, app info, logout confirmation

### 2.4 Inventory List Page
**Refs:** [Req 3.1] [BE 2.1.1]
**Route:** /inventory
**Components:** SearchBar, CardItem, ButtonPrimary, EmptyState
**API:** ProductService.getProducts(), ProductService.searchProducts()
**State:** InventoryViewModel (products, searchQuery, loading)
**Layout:** List of products dengan search bar, add button, product cards showing name, stock, price
**Features:** Search by name/barcode, filter by category, low stock indicators

### 2.5 Product Detail Page
**Refs:** [Req 3.1.1] [BE 2.1.1]
**Route:** /product/{id}
**Components:** ProductForm, ButtonPrimary, ButtonSecondary, ConfirmDialog
**API:** ProductService.getProduct(), ProductService.updateProduct()
**State:** ProductDetailViewModel (product, editing, saving)
**Layout:** Product details view/edit form, stock history, barcode display
**Features:** View mode and edit mode, update stock, generate barcode, delete product

### 2.6 Warehouse Page
**Refs:** [Req 3.1.2] [BE 2.1.4]
**Route:** /warehouse
**Components:** CardItem, ButtonPrimary, DropdownMenu
**API:** WarehouseService.getWarehouseStock(), WarehouseService.transferStock()
**State:** WarehouseViewModel (warehouses, selectedWarehouse, products)
**Layout:** Warehouse selector, product list per warehouse, transfer buttons
**Features:** Switch warehouses, view stock per location, stock transfer between warehouses

### 2.7 POS Page
**Refs:** [Req 3.2.1] [BE 2.2.1]
**Route:** /pos
**Components:** SaleForm, BarcodeScanner, CardItem, ButtonPrimary
**API:** SaleService.createSale(), ProductService.getProduct()
**State:** PosViewModel (cartItems, total, paymentMethod, scanning)
**Layout:** Product search/add area, cart list, total display, payment buttons
**Features:** Barcode scan, manual search, quantity adjustment, discount, payment selection

### 2.8 Sales History Page
**Refs:** [Req 3.2.2] [BE 2.2.1]
**Route:** /sales
**Components:** SearchBar, CardItem, DatePickerDialog, EmptyState
**API:** SaleService.getSales(), SaleService.searchSales()
**State:** SalesHistoryViewModel (sales, dateRange, searchQuery)
**Layout:** Sales list dengan date filter, search, sale details on tap
**Features:** Filter by date, search by receipt number, view sale details, print receipt

### 2.9 Reports Page
**Refs:** [Req 3.3] [BE 2.3]
**Route:** /reports
**Components:** ReportFilters, ChartView, CardItem, ButtonSecondary
**API:** ReportingService.getGrossSales(), etc.
**State:** ReportsViewModel (selectedReport, dateRange, reportData)
**Layout:** Report type selector, filters, chart display, export options
**Features:** Multiple report types, date range selection, chart visualization, PDF export

### 2.10 Barcode Scanner Page
**Refs:** [Req 3.4.2] [BE 2.4.1]
**Route:** /barcode/scan
**Components:** BarcodeScanner, ButtonSecondary, Text display
**API:** BarcodeService.scanBarcode(), ProductService.getProduct()
**State:** BarcodeScannerViewModel (scanning, lastScanned, product)
**Layout:** Full screen camera preview dengan scan overlay, result display
**Features:** Continuous scanning, manual entry fallback, product lookup

### 2.11 Barcode Print Page
**Refs:** [Req 3.4.1] [BE 2.4.2]
**Route:** /barcode/print
**Components:** ProductForm, NumberPickerDialog, PrinterDialog, ButtonPrimary
**API:** BarcodeService.generateBarcode(), LabelPrintingService.printLabel()
**State:** BarcodePrintViewModel (selectedProduct, size, quantity, printing)
**Layout:** Product selector, size options, quantity input, print preview
**Features:** Size selection, quantity setting, printer connection, print status

### 2.12 Backup Page
**Refs:** [Req 4.4] [BE 2.6]
**Route:** /backup
**Components:** BackupProgress, CardItem, ButtonPrimary, ConfirmDialog
**API:** BackupService.createBackup(), RestoreService.restoreFromBackup()
**State:** BackupViewModel (backupHistory, progress, restoring)
**Layout:** Backup history list, create backup button, restore options
**Features:** Manual backup, scheduled backup settings, restore from file

### 2.13 User Management Page
**Refs:** [Req 2.1] [BE 4.1]
**Route:** /users
**Components:** CardItem, ButtonPrimary, ConfirmDialog, DropdownMenu
**API:** AuthService (future multi-user)
**State:** UserManagementViewModel (users, selectedUser)
**Layout:** User list, add user form, role management
**Features:** Add/edit users, role assignment, password reset, user deactivation

**Note:** User management untuk future multi-user support, saat ini single user only.