# AI Coding Agent Instructions for Chiby Chiby Store

## Architecture Overview
This is an offline-first Android POS (Point of Sale) app for retail stores using MVVM architecture with Repository pattern. The app consists of 5 main modules: Inventory, Sales, Reporting, Barcode, and Financial management.

**Key Components:**
- **Database**: SQLite with Room ORM, normalized 3NF schema
- **UI**: Jetpack Compose with Material Design 3, bottom navigation
- **Business Logic**: Kotlin services with dependency injection (Hilt)
- **State Management**: ViewModel + StateFlow for reactive UI updates

**Data Flow**: UI → ViewModel → Repository → DAO → SQLite

## Key Patterns & Conventions

### Package Structure
Use Indonesian package names for domain entities:
- `pengguna` (users)
- `kategori` (categories) 
- `gudang` (warehouses)
- `produk` (products)
- `penjualan` (sales)
- `pembelian` (purchases)

### Authentication & Permissions
Role-based access control with 4 user roles:
- **Owner**: Full access including user management
- **Manager**: Reports, inventory view/edit, expense approval
- **Cashier**: POS operations, sales history view
- **Warehouse Staff**: Inventory management, barcode printing

Always check permissions before operations using `AuthService.hasPermission()`.

### Database Patterns
- Foreign key constraints with CASCADE delete protection
- CHECK constraints for data validation (e.g., non-negative amounts)
- Indexes on frequently queried columns (barcode, dates, categories)
- Unique constraints on business keys (product barcode)

Example entity relationship:
```
Users 1:N Sales/Purchases/Expenses
Categories 1:N Products  
Warehouses 1:N Products
Products 1:N SaleItems/PurchaseItems
```

### UI Patterns
- Bottom navigation for main modules (Inventory, Sales, Reports, Settings)
- Drawer navigation for advanced features (Barcode, Backup)
- Modal screens for details/forms
- Consistent component library: `CardItem`, `ButtonPrimary`, `TextFieldOutlined`

### Error Handling
Centralized error handling with user-friendly Indonesian messages:
- `ValidationError`: Input validation failures
- `DatabaseError`: SQLite constraint violations  
- `PermissionError`: Access denied
- `BusinessLogicError`: Invalid business rules

## Development Workflow

### Build & Run
Standard Android development:
- Use Android Studio for development
- Gradle for build system
- Minimum API 21 (Android 5.0)
- Kotlin coroutines for async operations

### Testing Strategy
- Unit tests for DAOs and services (>80% coverage target)
- Integration tests for module interactions
- UI tests for critical user flows
- Hardware integration tests (camera, Bluetooth printer)

### Hardware Integration
- **Barcode Scanning**: ZXing library with camera permission
- **Printing**: Bluetooth thermal printer with ESC/POS commands
- **Backup**: Encrypted JSON export to external storage

## Integration Points

### External Dependencies
- ZXing: Barcode generation/scanning
- BluetoothPrinter SDK: Thermal receipt/label printing
- Encryption library: AES for backup security

### Future Extensions
- API client for cloud synchronization
- Webhook support for external integrations
- Multi-device sync preparation

## Code Examples

### Repository Pattern
```kotlin
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    fun getProducts() = productDao.getAll()
    suspend fun createProduct(product: Product) = productDao.insert(product)
}
```

### ViewModel with StateFlow
```kotlin
class InventoryViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {
    
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products
    
    fun loadProducts() {
        viewModelScope.launch {
            _products.value = productRepository.getProducts()
        }
    }
}
```

### Permission Check
```kotlin
fun updateProduct(id: Int, product: Product) {
    if (!authService.hasPermission("EDIT_PRODUCT")) {
        throw PermissionError("Tidak memiliki izin mengedit produk")
    }
    // proceed with update
}
```

## Key Reference Files
- [requirements.md](requirements.md): Functional and non-functional requirements
- [database-design.md](database-design.md): Schema, relationships, validation rules  
- [backend-design.md](backend-design.md): API design, services, error handling
- [frontend-design.md](frontend-design.md): UI components, navigation, screens
- [tasks.md](tasks.md): Development roadmap and task breakdown