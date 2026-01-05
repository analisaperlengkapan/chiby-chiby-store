# Chiby Chiby Store

Chiby Chiby Store is a comprehensive Point of Sale (POS) and Store Management Android application designed to streamline retail operations. It provides a robust suite of tools for inventory tracking, sales management, supplier coordination, and financial reporting.

## Features

### 1. Dashboard
- **Overview**: Provides a snapshot of the store's performance.
- **Key Metrics**: View daily sales, total revenue, and quick access to critical actions.
- **Charts**: Visual representation of sales trends (e.g., last 7 days).

### 2. Point of Sale (POS)
- **Cart Management**: Easily add products to the cart via barcode scanning or manual search.
- **Checkout**: Calculate totals automatically, including tax and discounts.
- **Receipt Printing**: Integrated support for Bluetooth thermal printers to generate physical receipts.
- **Stock Validation**: Real-time checks to prevent selling out-of-stock items.

### 3. Inventory Management
- **Product List**: View, search, and filter products by category or warehouse.
- **Product Details**: Comprehensive view of product information including pricing, cost, and stock levels.
- **Stock Adjustment**: Tools to manually adjust stock levels for corrections or audits.
- **Stock Transfer**: Move stock between different warehouses.
- **Barcode Support**: Assign and manage barcodes for quick product retrieval.

### 4. Warehouse Management
- **Multi-Warehouse Support**: Manage inventory across multiple storage locations.
- **Warehouse Tracking**: Keep track of stock levels specific to each warehouse.

### 5. Supplier Management
- **Supplier Database**: Maintain a list of suppliers with contact details.
- **Coordination**: Link products to suppliers for easier restocking.

### 6. Expense Management
- **Expense Tracking**: Record and categorize store expenses (e.g., Rent, Utilities, Salaries).
- **Reporting**: View expense summaries to better understand operational costs.

### 7. User Management
- **Role-Based Access**: Manage users with specific roles (e.g., Admin, Cashier) and permissions.
- **Security**: Secure login and access control to sensitive features.
- **Audit**: Track user activities (implied by individual user accounts).

### 8. Sales History & Reporting
- **Transaction History**: View a detailed list of past sales transactions.
- **Refunds**: Process refunds for returned items.
- **Comprehensive Reports**: Generate reports for Sales, Inventory, and Expenses.
- **PDF Export**: Export reports to PDF for sharing or archival.

### 9. Settings & Backup
- **Configuration**: Customize store settings (Tax rates, Store details).
- **Data Safety**: Backup and restore functionality to safeguard your data.

## Tech Stack

This project is built using modern Android development practices and libraries:

-   **Language**: Kotlin
-   **UI Framework**: Jetpack Compose (Material 3)
-   **Architecture**: MVVM (Model-View-ViewModel)
-   **Dependency Injection**: Hilt
-   **Local Database**: Room (SQLite)
-   **Navigation**: Navigation Compose
-   **Barcode Scanning**: ZXing
-   **Printing**: ESC/POS Thermal Printer Library
-   **PDF Generation**: iText 7
-   **Asynchronous Programming**: Kotlin Coroutines & Flow

## Screenshots

*Please place the corresponding images in `docs/screenshots/` to verify the UI.*

| Feature | Screenshot |
| :--- | :--- |
| **Login** | ![Login Screen](docs/screenshots/login.png) |
| **Dashboard** | ![Dashboard](docs/screenshots/dashboard.png) |
| **Point of Sale** | ![POS Screen](docs/screenshots/pos.png) |
| **Checkout** | ![Checkout Dialog](docs/screenshots/checkout.png) |
| **Product List** | ![Inventory Screen](docs/screenshots/inventory.png) |
| **Product Detail** | ![Product Detail](docs/screenshots/product_detail.png) |
| **Add Product** | ![Add Product](docs/screenshots/add_product.png) |
| **Sales History** | ![Sales History](docs/screenshots/sales_history.png) |
| **Reports** | ![Reports Screen](docs/screenshots/reports.png) |
| **Suppliers** | ![Supplier List](docs/screenshots/suppliers.png) |
| **Expenses** | ![Expense List](docs/screenshots/expenses.png) |
| **User Management** | ![User List](docs/screenshots/users.png) |
| **Settings** | ![Settings Screen](docs/screenshots/settings.png) |

## Setup Instructions

1.  **Prerequisites**:
    *   Android Studio (latest stable version recommended).
    *   JDK 17 (or compatible version for Kotlin/Gradle).

2.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-repo/chiby-chiby-store.git
    cd chiby-chiby-store
    ```

3.  **Open in Android Studio**:
    *   Select "Open" and choose the project directory.
    *   Allow Gradle to sync dependencies.

4.  **Run the App**:
    *   Connect an Android device or start an Emulator.
    *   Click the "Run" button (Green arrow) in Android Studio.

5.  **Tests**:
    *   Run unit tests via command line: `./gradlew testDebugUnitTest`

## License

[Add License Information Here]
