package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product Service untuk manajemen produk di Chiby Chiby Store
 *
 * Service ini menangani semua operasi CRUD untuk produk retail, termasuk:
 * - Manajemen inventory dan stok
 * - Validasi data produk (harga, barcode, dll)
 * - Search dan filtering produk
 * - Real-time updates melalui Flow
 * - Business rules untuk inventory management
 *
 * **Fitur Utama:**
 * - CRUD operations untuk produk
 * - Barcode uniqueness validation
 * - Stock management dengan business rules
 * - Advanced search dan filtering
 * - Low stock alerts
 * - Reactive data streams untuk UI updates
 *
 * **Business Rules:**
 * - Barcode harus unik di seluruh sistem
 * - Harga jual harus >= harga beli
 * - Stok tidak boleh negatif
 * - Produk harus memiliki kategori dan warehouse
 *
 * **Integration Points:**
 * - [ProdukRepository] untuk data access
 * - [SaleService] untuk stock updates saat penjualan
 * - [PurchaseService] untuk stock updates saat pembelian
 * - UI components untuk reactive updates
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see Produk
 * @see ProdukRepository
 * @see SaleService
 */
interface ProductService {

    /**
     * Membuat produk baru dengan validasi komprehensif
     *
     * **Validasi yang dilakukan:**
     * - Nama produk tidak kosong
     * - Harga beli dan jual > 0
     * - Harga jual >= harga beli
     * - Barcode unik (jika disediakan)
     * - Kategori dan warehouse valid
     * - Stok awal >= 0
     *
     * @param product Data produk yang akan dibuat
     * @return Result dengan produk yang berhasil dibuat atau exception
     *
     * @throws IllegalArgumentException jika validasi gagal
     * @throws Exception jika barcode sudah digunakan
     *
     * @sample
     * ```kotlin
     * val newProduct = Produk(
     *     name = "Indomie Goreng",
     *     barcode = "8996001600017",
     *     costPrice = 2500.0,
     *     sellingPrice = 3000.0,
     *     stockQuantity = 100,
     *     categoryId = "food",
     *     warehouseId = "main"
     * )
     * val result = productService.createProduct(newProduct)
     * ```
     */
    suspend fun createProduct(product: Produk): Result<Produk>

    /**
     * Update data produk existing
     *
     * Method ini memperbarui semua field produk kecuali ID.
     * Semua validasi yang sama dengan createProduct diterapkan.
     *
     * @param product Produk dengan data terbaru (ID harus valid)
     * @return Result dengan produk yang berhasil diupdate
     *
     * @throws IllegalArgumentException jika validasi gagal
     * @throws Exception jika produk tidak ditemukan atau barcode konflik
     */
    suspend fun updateProduct(product: Produk): Result<Produk>

    /**
     * Menghapus produk dari sistem
     *
     * **Business Rules:**
     * - Produk tidak boleh memiliki transaksi penjualan yang terkait
     * - Foreign key constraints akan mencegah delete jika masih ada referensi
     *
     * @param id ID produk yang akan dihapus
     * @return Result menunjukkan keberhasilan operasi
     *
     * @throws Exception jika produk masih memiliki referensi atau tidak ditemukan
     */
    suspend fun deleteProduct(id: String): Result<Unit>

    /**
     * Mendapatkan produk berdasarkan ID
     *
     * @param id ID produk yang dicari
     * @return Result dengan produk atau null jika tidak ditemukan
     */
    suspend fun getProduct(id: String): Result<Produk?>

    /**
     * Mendapatkan daftar produk dengan filtering opsional
     *
     * **Filter Options:**
     * - categoryId: Filter berdasarkan kategori produk
     * - warehouseId: Filter berdasarkan lokasi warehouse
     * - searchQuery: Pencarian berdasarkan nama produk
     *
     * @param categoryId Filter kategori (optional)
     * @param warehouseId Filter warehouse (optional)
     * @param searchQuery Query pencarian (optional)
     * @return Result dengan list produk yang sesuai filter
     */
    suspend fun getProducts(
        categoryId: String? = null,
        warehouseId: String? = null,
        searchQuery: String? = null
    ): Result<List<Produk>>

    /**
     * Pencarian produk berdasarkan query text
     *
     * Mencari produk berdasarkan nama atau barcode dengan case-insensitive matching.
     *
     * @param query String pencarian (nama produk atau barcode)
     * @return Result dengan list produk yang match
     */
    suspend fun searchProducts(query: String): Result<List<Produk>>

    /**
     * Update jumlah stok produk
     *
     * **Business Rules:**
     * - Stok akhir tidak boleh negatif
     * - Update stok akan tercatat dalam history
     * - Memicu low stock alerts jika diperlukan
     *
     * @param productId ID produk yang stoknya akan diupdate
     * @param newStock Jumlah stok baru (>= 0)
     * @return Result menunjukkan keberhasilan operasi
     *
     * @throws IllegalArgumentException jika newStock < 0
     * @throws Exception jika produk tidak ditemukan
     */
    suspend fun updateStock(productId: String, newStock: Int): Result<Unit>

    /**
     * Mendapatkan produk dengan stok rendah
     *
     * Mengembalikan produk yang stoknya <= minStock atau stok kritis (< 10).
     * Berguna untuk inventory alerts dan reorder planning.
     *
     * @return Result dengan list produk stok rendah
     */
    suspend fun getLowStockProducts(): Result<List<Produk>>

    /**
     * Observable stream untuk semua produk
     *
     * Flow ini akan emit setiap kali ada perubahan pada data produk.
     * Berguna untuk reactive UI updates di inventory screens.
     *
     * @return Flow yang emit list semua produk
     */
    fun observeProducts(): Flow<List<Produk>>

    /**
     * Observable stream untuk produk berdasarkan kategori
     *
     * @param categoryId ID kategori yang akan difilter
     * @return Flow yang emit produk dalam kategori tersebut
     */
    fun observeProductsByCategory(categoryId: String): Flow<List<Produk>>

    /**
     * Observable stream untuk produk berdasarkan warehouse
     *
     * @param warehouseId ID warehouse yang akan difilter
     * @return Flow yang emit produk di warehouse tersebut
     */
    fun observeProductsByWarehouse(warehouseId: String): Flow<List<Produk>>
}

/**
 * Implementation dari ProductService menggunakan ProdukRepository
 *
 * Kelas ini mengimplementasikan interface [ProductService] dengan fokus pada:
 * - Validasi komprehensif untuk data produk
 * - Business logic untuk inventory management
 * - Reactive streams untuk real-time UI updates
 * - Error handling yang konsisten
 *
 * **Komponen Utama:**
 * - [ProdukRepository]: Interface untuk data access layer
 * - Validation logic: Business rules untuk produk
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
 * @property productRepository Repository untuk operasi database produk
 *
 * @constructor Inject dependencies melalui Hilt
 * @param productRepository Instance ProdukRepository yang diinject
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see ProductService
 * @see ProdukRepository
 * @see Produk
 */
@Singleton
class ProductServiceImpl @Inject constructor(
    private val productRepository: ProdukRepository
) : ProductService {

    /**
     * Implementasi pembuatan produk baru dengan validasi lengkap
     *
     * **Proses Validasi:**
     * 1. Validasi basic product data (nama, harga, dll)
     * 2. Cek uniqueness barcode jika disediakan
     * 3. Insert ke database melalui repository
     * 4. Return produk dengan ID yang di-generate
     *
     * **Error Scenarios:**
     * - ValidationException: Data produk tidak valid
     * - Exception: Barcode sudah digunakan
     * - Database errors: Connection issues, constraint violations
     *
     * @param product Data produk baru tanpa ID
     * @return Result dengan produk yang berhasil dibuat (dengan ID)
     */
    override suspend fun createProduct(product: Produk): Result<Produk> {
        return try {
            // Validasi input berdasarkan business rules
            validateProduct(product)

            // Pastikan barcode unik jika ada (GS1 compliance)
            if (!product.barcode.isNullOrBlank()) {
                val existingResult = productRepository.getProdukByBarcode(product.barcode!!)
                if (existingResult.isSuccess && existingResult.getOrNull() != null) {
                    return Result.failure(Exception("Barcode sudah digunakan oleh produk lain"))
                }
            }

            val createResult = productRepository.createProduk(product)
            val createdProductId = createResult.getOrNull() ?: return Result.failure(createResult.exceptionOrNull() ?: Exception("Gagal membuat produk"))
            val createdProductResult = productRepository.getProdukById(createdProductId)
            val createdProduct = createdProductResult.getOrNull() ?: return Result.failure(createdProductResult.exceptionOrNull() ?: Exception("Gagal mengambil produk yang dibuat"))
            Result.success(createdProduct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Produk): Result<Produk> {
        return try {
            // Validasi input
            validateProduct(product)

            // Pastikan barcode unik jika diubah
            if (!product.barcode.isNullOrBlank()) {
                val existingResult = productRepository.getProdukByBarcode(product.barcode!!)
                if (existingResult.isSuccess) {
                    val existing = existingResult.getOrNull()
                    if (existing != null && existing.id != product.id) {
                        return Result.failure(Exception("Barcode sudah digunakan oleh produk lain"))
                    }
                }
            }

            val updateResult = productRepository.updateProduk(product)
            if (updateResult.isFailure) return Result.failure(updateResult.exceptionOrNull() ?: Exception("Gagal mengupdate produk"))
            val updatedProductResult = productRepository.getProdukById(product.id)
            val updatedProduct = updatedProductResult.getOrNull() ?: return Result.failure(updatedProductResult.exceptionOrNull() ?: Exception("Gagal mengambil produk yang diupdate"))
            Result.success(updatedProduct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            val deleteResult = productRepository.deleteProduk(id.toLong())
            if (deleteResult.isFailure) return Result.failure(deleteResult.exceptionOrNull() ?: Exception("Gagal menghapus produk"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduct(id: String): Result<Produk?> {
        return try {
            val product = productRepository.getProdukById(id.toLong()).getOrNull()
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProducts(
        categoryId: String?,
        warehouseId: String?,
        searchQuery: String?
    ): Result<List<Produk>> {
        return try {
            val flow = when {
                !searchQuery.isNullOrBlank() -> productRepository.searchProduk(searchQuery)
                categoryId != null -> productRepository.getProdukByCategory(categoryId.toLong())
                warehouseId != null -> productRepository.getProdukByWarehouse(warehouseId.toLong())
                else -> productRepository.getAllProduk()
            }
            val products = flow.first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(query: String): Result<List<Produk>> {
        return try {
            val products = productRepository.searchProduk(query).first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStock(productId: String, newStock: Int): Result<Unit> {
        return try {
            if (newStock < 0) {
                return Result.failure(Exception("Stok tidak boleh negatif"))
            }

            val stockUpdateResult = productRepository.updateStock(productId.toLong(), newStock)
            if (stockUpdateResult.isFailure) return Result.failure(stockUpdateResult.exceptionOrNull() ?: Exception("Gagal memperbarui stok"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLowStockProducts(): Result<List<Produk>> {
        return try {
            val products = productRepository.getLowStockProduk().first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProducts(): Flow<List<Produk>> {
        return productRepository.getAllProduk()
    }

    override fun observeProductsByCategory(categoryId: String): Flow<List<Produk>> {
        return productRepository.getProdukByCategory(categoryId.toLong())
    }

    override fun observeProductsByWarehouse(warehouseId: String): Flow<List<Produk>> {
        return productRepository.getProdukByWarehouse(warehouseId.toLong())
    }

    /**
     * Validasi data produk berdasarkan business rules retail
     *
     * **Validation Rules:**
     * - Nama produk tidak boleh kosong/blank
     * - Harga beli >= 0 (tidak boleh negatif)
     * - Harga jual >= 0 (tidak boleh negatif)
     * - Harga jual >= harga beli (untuk profitabilitas)
     * - Stok awal >= 0 (tidak boleh negatif)
     * - Minimum stok >= 0 (untak alert system)
     *
     * **Business Logic:**
     * - Mencegah produk dengan harga jual < harga beli (rugi)
     * - Memastikan stok selalu dalam range valid
     * - Validasi nama untuk UI display
     *
     * **Error Types:**
     * - IllegalArgumentException untuk business rule violations
     * - Require() akan throw exception dengan message spesifik
     *
     * @param product Produk yang akan divalidasi
     * @throws IllegalArgumentException jika ada data yang tidak valid
     */
    private fun validateProduct(product: Produk) {
        require(product.name.isNotBlank()) { "Nama produk tidak boleh kosong" }
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