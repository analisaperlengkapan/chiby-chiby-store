package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ProductRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ProductService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation dari ProductService menggunakan ProductRepository
 *
 * Kelas ini mengimplementasikan interface [ProductService] dengan fokus pada:
 * - Validasi komprehensif untuk data product
 * - Business logic untuk inventory management
 * - Reactive streams untuk real-time UI updates
 * - Error handling yang konsisten
 *
 * **Komponen Utama:**
 * - [ProductRepository]: Interface untuk data access layer
 * - Validation logic: Business rules untuk product
 * - Flow transformations: Reactive data streams
 * - Error propagation: Konsisten Result-based error handling
 *
 * **Thread Safety:**
 * - Semua operasi suspend untuk coroutine safety
 * - Repository operations menggunakan Room's built-in thread safety
 * - Flow operations thread-safe untuk UI consumption
 *
 * **Performance Considerations:**
 * - Efficient database queries dengan proper indexing
 * - Lazy loading untuk large datasets
 * - Flow-based reactive updates untuk minimal UI refreshes
 *
 * **Dependencies:**
 * - Hilt untuk dependency injection
 * - Room database untuk persistence
 * - Kotlin Coroutines untuk async operations
 * - Kotlin Flow untuk reactive streams
 *
 * @property productRepository Repository untuk operasi database product
 *
 * @constructor Inject dependencies melalui Hilt
 * @param productRepository Instance ProductRepository yang diinject
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see ProductService
 * @see ProductRepository
 * @see Product
 */
@Singleton
class ProductServiceImpl @Inject constructor(
    private val productRepository: ProductRepository,
    private val authService: AuthService
) : ProductService {

    /**
     * Implementasi pembuatan product baru dengan validasi lengkap
     *
     * **Proses Validasi:**
     * 1. Validasi basic product data (nama, harga, dll)
     * 2. Cek uniqueness barcode jika disediakan
     * 3. Insert ke database melalui repository
     * 4. Return product dengan ID yang di-generate
     *
     * **Error Scenarios:**
     * - ValidationException: Data product tidak valid
     * - Exception: Barcode sudah digunakan
     * - Database errors: Connection issues, constraint violations
     *
     * @param product Data product baru tanpa ID
     * @return Result dengan product yang berhasil dibuat (dengan ID)
     */
    override suspend fun createProduct(product: Product): Result<Product> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengedit inventory"))
            }
            // Validasi input berdasarkan business rules
            validateProduct(product)

            // Pastikan barcode unik jika ada (GS1 compliance)
            // Pre-check for faster feedback, but handle race condition below
            if (!product.barcode.isNullOrBlank()) {
                val existingResult = productRepository.getProductByBarcode(product.barcode)
                if (existingResult is Result.Success && existingResult.data.id != 0L) {
                     return Result.failure(Exception("Barcode sudah digunakan oleh product lain"))
                }
            }

            try {
                val createResult = productRepository.createProduct(product)
                val createdProductId = (createResult as? Result.Success)?.data ?: return Result.failure((createResult as? Result.Failure)?.exception ?: Exception("Gagal membuat product"))

                val createdProductResult = productRepository.getProductById(createdProductId)
                val createdProduct = (createdProductResult as? Result.Success)?.data ?: return Result.failure((createdProductResult as? Result.Failure)?.exception ?: Exception("Gagal mengambil product yang dibuat"))
                Result.success(createdProduct)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Result.failure(Exception("Barcode sudah digunakan (Constraint Error)"))
            } catch (e: Exception) {
                 // Check message for constraint violation if SQLiteConstraintException isn't caught directly (wrapper issues)
                if (e.message?.contains("constraint", ignoreCase = true) == true) {
                    Result.failure(Exception("Barcode sudah digunakan"))
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengedit inventory"))
            }
            // Validasi input
            validateProduct(product)

            // Pastikan barcode unik jika diubah
            if (!product.barcode.isNullOrBlank()) {
                val existingResult = productRepository.getProductByBarcode(product.barcode)
                if (existingResult is Result.Success) {
                    val existing = existingResult.data
                    if (existing.id != product.id) {
                        return Result.failure(Exception("Barcode sudah digunakan oleh product lain"))
                    }
                }
            }

            val updateResult = productRepository.updateProduct(product)
            if (updateResult is Result.Failure) return Result.failure(updateResult.exception)

            val updatedProductResult = productRepository.getProductById(product.id)
            val updatedProduct = (updatedProductResult as? Result.Success)?.data ?: return Result.failure((updatedProductResult as? Result.Failure)?.exception ?: Exception("Gagal mengambil product yang diupdate"))
            Result.success(updatedProduct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengedit inventory"))
            }
            val idLong = id.toLongOrNull() ?: return Result.failure(Exception("ID Product tidak valid: $id"))
            val deleteResult = productRepository.deleteProduct(idLong)
            if (deleteResult is Result.Failure) return Result.failure(deleteResult.exception)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduct(id: String): Result<Product?> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat inventory"))
            }
            val idLong = id.toLongOrNull() ?: return Result.failure(Exception("ID Product tidak valid: $id"))
            val result = productRepository.getProductById(idLong)
            when (result) {
                is Result.Success -> Result.success(result.data)
                is Result.Failure -> Result.success(null) // Return null if not found, or propagate error? Interface says Result<Product?>
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProducts(
        categoryId: String?,
        warehouseId: String?,
        searchQuery: String?
    ): Result<List<Product>> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat inventory"))
            }
            val flow = when {
                !searchQuery.isNullOrBlank() -> productRepository.searchProducts(searchQuery)
                categoryId != null -> productRepository.getProductByCategory(categoryId.toLong())
                warehouseId != null -> productRepository.getProductByWarehouse(warehouseId.toLong())
                else -> productRepository.getAllProducts()
            }
            val products = flow.first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat inventory"))
            }
            val products = productRepository.searchProducts(query).first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStock(productId: String, newStock: Int): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengedit inventory"))
            }

            val idLong = productId.toLongOrNull() ?: return Result.failure(Exception("ID Product tidak valid: $productId"))

            if (newStock < 0) {
                return Result.failure(Exception("Stok tidak boleh negatif"))
            }

            // Ensure product exists
            val productResult = productRepository.getProductById(idLong)
            if (productResult is Result.Failure) {
                return Result.failure(Exception("Product tidak ditemukan"))
            }

            // Perform atomic update (Set Absolute Stock)
            val stockUpdateResult = productRepository.setStock(idLong, newStock)
            if (stockUpdateResult is Result.Failure) return Result.failure(stockUpdateResult.exception)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLowStockProducts(): Result<List<Product>> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat inventory"))
            }
            val products = productRepository.getLowStockProducts().first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProducts(): Flow<List<Product>> {
        return productRepository.getAllProducts()
    }

    override fun observeProductsByCategory(categoryId: String): Flow<List<Product>> {
        return productRepository.getProductByCategory(categoryId.toLong())
    }

    override fun observeProductsByWarehouse(warehouseId: String): Flow<List<Product>> {
        val idLong = warehouseId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return productRepository.getProductByWarehouse(idLong)
    }

    override fun observeSearchProducts(query: String): Flow<List<Product>> {
        return productRepository.searchProducts(query)
    }

    override fun observeLowStockProducts(): Flow<List<Product>> {
        return productRepository.getLowStockProducts()
    }

    /**
     * Validasi data product berdasarkan business rules retail
     *
     * **Validation Rules:**
     * - Nama product tidak boleh kosong/blank dan minimal 2 karakter
     * - Harga beli >= 0 (tidak boleh negatif)
     * - Harga jual >= 0 (tidak boleh negatif)
     * - Harga jual >= harga beli (untuk profitabilitas)
     * - Stok awal >= 0 (tidak boleh negatif)
     * - Minimum stok >= 0 (untak alert system)
     *
     * **Business Logic:**
     * - Mencegah product dengan harga jual < harga beli (rugi)
     * - Memastikan stok selalu dalam range valid
     * - Validasi nama untuk UI display
     *
     * **Error Types:**
     * - IllegalArgumentException untuk business rule violations
     * - Require() akan throw exception dengan message spesifik
     *
     * @param product Product yang akan divalidasi
     * @throws IllegalArgumentException jika ada data yang tidak valid
     */
    private fun validateProduct(product: Product) {
        require(product.name.isNotBlank()) { "Nama product tidak boleh kosong" }
        require(product.name.length >= 2) { "Nama product minimal 2 karakter" }
        require(product.costPrice >= 0) { "Harga beli tidak boleh negatif" }
        require(product.sellingPrice >= 0) { "Harga jual tidak boleh negatif" }
        require(product.stockQuantity >= 0) { "Stok tidak boleh negatif" }
        require(product.minStock >= 0) { "Minimum stok tidak boleh negatif" }

        // Business rule: harga jual harus >= harga beli untuk profit
        if (product.sellingPrice < product.costPrice) {
            throw IllegalArgumentException("Harga jual tidak boleh lebih rendah dari harga beli")
        }
    }
}
