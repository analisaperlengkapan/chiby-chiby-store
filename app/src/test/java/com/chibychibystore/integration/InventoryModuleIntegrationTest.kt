package com.chibychibystore.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.KategoriRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.WarehouseService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

/**
 * Integration test untuk Inventory Module
 * 
 * Test ini memastikan integrasi end-to-end antara:
 * - Database (Room)
 * - Repository layer
 * - Service layer
 * - Business logic
 */
@RunWith(RobolectricTestRunner::class)
class InventoryModuleIntegrationTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var gudangRepository: GudangRepository
    private lateinit var kategoriRepository: KategoriRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService

    @Before
    fun setup() {
        // Setup in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        // Setup repositories
        gudangRepository = GudangRepository(database.gudangDao())
        kategoriRepository = KategoriRepository(database.kategoriDao())
        produkRepository = ProdukRepository(database.produkDao())

        // Setup services
        warehouseService = WarehouseService(gudangRepository, produkRepository)
        productService = ProductService(produkRepository, kategoriRepository, gudangRepository)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `complete inventory workflow should work end-to-end`() = runTest {
        // 1. Create kategori
        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )
        kategoriRepository.insertKategori(kategori)

        // 2. Create gudang
        val gudang = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        val createWarehouseResult = warehouseService.createWarehouse(gudang)
        assertTrue(createWarehouseResult.isSuccess)

        // 3. Create produk
        val produk = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 10,
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        val createProductResult = productService.createProduct(produk)
        assertTrue(createProductResult.isSuccess)

        // 4. Verify data integrity
        val allWarehouses = gudangRepository.getAllGudang().first()
        assertEquals(1, allWarehouses.size)
        assertEquals("Gudang Utama", allWarehouses[0].name)

        val allKategori = kategoriRepository.getAllKategori().first()
        assertEquals(1, allKategori.size)
        assertEquals("Elektronik", allKategori[0].nama)

        val allProducts = produkRepository.getAllProduk().first()
        assertEquals(1, allProducts.size)
        assertEquals("Laptop", allProducts[0].nama)
        assertEquals(10, allProducts[0].stok)

        // 5. Update stok
        val updatedProduct = produk.copy(stok = 15)
        produkRepository.updateProduk(updatedProduct)

        val retrievedProduct = produkRepository.getProdukById(1)
        assertEquals(15, retrievedProduct?.stok)

        // 6. Check warehouse stock
        val warehouseStockResult = warehouseService.getWarehouseStock(1)
        assertTrue(warehouseStockResult.isSuccess)
        val warehouseStock = warehouseStockResult.getOrNull()
        assertNotNull(warehouseStock)
        assertEquals(1, warehouseStock?.size)
        assertEquals("Laptop", warehouseStock?.get(0)?.nama)
    }

    @Test
    fun `warehouse transfer should update stock correctly`() = runTest {
        // Setup: Create 2 warehouses and 1 product
        val gudang1 = Gudang(
            id = 1,
            name = "Gudang A",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        val gudang2 = Gudang(
            id = 2,
            name = "Gudang B",
            location = "Bandung",
            capacity = 1000,
            createdAt = Date()
        )
        
        warehouseService.createWarehouse(gudang1)
        warehouseService.createWarehouse(gudang2)

        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )
        kategoriRepository.insertKategori(kategori)

        val produk = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 10,
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduct(produk)

        // Transfer 5 units from Gudang A to Gudang B
        val transferResult = warehouseService.transferStock(
            productId = 1,
            fromWarehouseId = 1,
            toWarehouseId = 2,
            quantity = 5
        )

        assertTrue(transferResult.isSuccess)

        // Verify stock in Gudang A decreased
        val productInGudangA = produkRepository.getProdukById(1)
        assertEquals(5, productInGudangA?.stok)

        // Note: In real implementation, we would need to track stock per warehouse
        // This is a simplified test
    }

    @Test
    fun `product with low stock should be identified`() = runTest {
        // Create kategori and gudang
        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )
        kategoriRepository.insertKategori(kategori)

        val gudang = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        warehouseService.createWarehouse(gudang)

        // Create product with low stock
        val produk = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 1, // Below minStok
            minStok = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduct(produk)

        // Get low stock products
        val lowStockResult = productService.getLowStockProducts()
        assertTrue(lowStockResult.isSuccess)
        
        val lowStockProducts = lowStockResult.getOrNull()
        assertNotNull(lowStockProducts)
        assertEquals(1, lowStockProducts?.size)
        assertEquals("Laptop", lowStockProducts?.get(0)?.nama)
    }

    @Test
    fun `product search by barcode should work`() = runTest {
        // Setup
        val kategori = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )
        kategoriRepository.insertKategori(kategori)

        val gudang = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        warehouseService.createWarehouse(gudang)

        val produk = Produk(
            id = 1,
            nama = "Laptop",
            barcode = "LP001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 5000000.0,
            hargaJual = 7000000.0,
            stok = 10,
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduct(produk)

        // Search by barcode
        val searchResult = productService.getProductByBarcode("LP001")
        assertTrue(searchResult.isSuccess)
        
        val foundProduct = searchResult.getOrNull()
        assertNotNull(foundProduct)
        assertEquals("Laptop", foundProduct?.nama)
        assertEquals("LP001", foundProduct?.barcode)
    }
}
