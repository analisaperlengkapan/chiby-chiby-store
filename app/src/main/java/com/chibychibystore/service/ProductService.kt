package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product Service untuk manajemen product di Chiby Chiby Store
 *
 * Service ini menangani semua operasi CRUD untuk product retail, termasuk:
 * - Manajemen inventory dan stok
 * - Validasi data product (harga, barcode, dll)
 * - Search dan filtering product
 * - Real-time updates melalui Flow
 * - Business rules untuk inventory management
 *
 * **Fitur Utama:**
 * - CRUD operations untuk product
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
 * - Product harus memiliki kategori dan warehouse
 *
 * **Integration Points:**
 * - [ProductRepository] untuk data access
 * - [SaleService] untuk stock updates saat penjualan
 * - [PurchaseService] untuk stock updates saat pembelian
 * - UI components untuk reactive updates
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see Product
 * @see ProductRepository
 * @see SaleService
 */
interface ProductService {

    /**
     * Membuat product baru dengan validasi komprehensif
     *
     * **Validasi yang dilakukan:**
     * - Nama product tidak kosong
     * - Harga beli dan jual > 0
     * - Harga jual >= harga beli
     * - Barcode unik (jika disediakan)
     * - Kategori dan warehouse valid
     * - Stok awal >= 0
     *
     * @param product Data product yang akan dibuat
     * @return Result dengan product yang berhasil dibuat atau exception
     *
     * @throws IllegalArgumentException jika validasi gagal
     * @throws Exception jika barcode sudah digunakan
     *
     * @sample
     * ```kotlin
     * val newProduct = Product(
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
    suspend fun createProduct(product: Product): Result<Product>

    /**
     * Update data product existing
     *
     * Method ini memperbarui semua field product kecuali ID.
     * Semua validasi yang sama dengan createProduct diterapkan.
     *
     * @param product Product dengan data terbaru (ID harus valid)
     * @return Result dengan product yang berhasil diupdate
     *
     * @throws IllegalArgumentException jika validasi gagal
     * @throws Exception jika product tidak ditemukan atau barcode konflik
     */
    suspend fun updateProduct(product: Product): Result<Product>

    /**
     * Menghapus product dari sistem
     *
     * **Business Rules:**
     * - Product tidak boleh memiliki transaksi penjualan yang terkait
     * - Foreign key constraints akan mencegah delete jika masih ada referensi
     *
     * @param id ID product yang akan dihapus
     * @return Result menunjukkan keberhasilan operasi
     *
     * @throws Exception jika product masih memiliki referensi atau tidak ditemukan
     */
    suspend fun deleteProduct(id: String): Result<Unit>

    /**
     * Mendapatkan product berdasarkan ID
     *
     * @param id ID product yang dicari
     * @return Result dengan product atau null jika tidak ditemukan
     */
    suspend fun getProduct(id: String): Result<Product?>

    /**
     * Mendapatkan daftar product dengan filtering opsional
     *
     * **Filter Options:**
     * - categoryId: Filter berdasarkan kategori product
     * - warehouseId: Filter berdasarkan lokasi warehouse
     * - searchQuery: Pencarian berdasarkan nama product
     *
     * @param categoryId Filter kategori (optional)
     * @param warehouseId Filter warehouse (optional)
     * @param searchQuery Query pencarian (optional)
     * @return Result dengan list product yang sesuai filter
     */
    suspend fun getProducts(
        categoryId: String? = null,
        warehouseId: String? = null,
        searchQuery: String? = null
    ): Result<List<Product>>

    /**
     * Pencarian product berdasarkan query text
     *
     * Mencari product berdasarkan nama atau barcode dengan case-insensitive matching.
     *
     * @param query String pencarian (nama product atau barcode)
     * @return Result dengan list product yang match
     */
    suspend fun searchProducts(query: String): Result<List<Product>>

    /**
     * Update jumlah stok product
     *
     * **Business Rules:**
     * - Stok akhir tidak boleh negatif
     * - Update stok akan tercatat dalam history
     * - Memicu low stock alerts jika diperlukan
     *
     * @param productId ID product yang stoknya akan diupdate
     * @param newStock Jumlah stok baru (>= 0)
     * @return Result menunjukkan keberhasilan operasi
     *
     * @throws IllegalArgumentException jika newStock < 0
     * @throws Exception jika product tidak ditemukan
     */
    suspend fun updateStock(productId: String, newStock: Int): Result<Unit>

    /**
     * Mendapatkan product dengan stok rendah
     *
     * Mengembalikan product yang stoknya <= minStock atau stok kritis (< 10).
     * Berguna untuk inventory alerts dan reorder planning.
     *
     * @return Result dengan list product stok rendah
     */
    suspend fun getLowStockProducts(): Result<List<Product>>

    /**
     * Observable stream untuk semua product
     *
     * Flow ini akan emit setiap kali ada perubahan pada data product.
     * Berguna untuk reactive UI updates di inventory screens.
     *
     * @return Flow yang emit list semua product
     */
    fun observeProducts(): Flow<List<Product>>

    /**
     * Observable stream untuk product berdasarkan kategori
     *
     * @param categoryId ID kategori yang akan difilter
     * @return Flow yang emit product dalam kategori tersebut
     */
    fun observeProductsByCategory(categoryId: String): Flow<List<Product>>

    /**
     * Observable stream untuk product berdasarkan warehouse
     *
     * @param warehouseId ID warehouse yang akan difilter
     * @return Flow yang emit product di warehouse tersebut
     */
    fun observeProductsByWarehouse(warehouseId: String): Flow<List<Product>>

    /**
     * Observable stream untuk pencarian product
     *
     * @param query String pencarian
     * @return Flow yang emit hasil pencarian
     */
    fun observeSearchProducts(query: String): Flow<List<Product>>

    /**
     * Observable stream untuk product stok rendah
     *
     * @return Flow yang emit list product stok rendah
     */
    fun observeLowStockProducts(): Flow<List<Product>>
}
