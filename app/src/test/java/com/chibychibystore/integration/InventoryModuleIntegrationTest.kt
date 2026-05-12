package com.chibychibystore.integration
import org.robolectric.annotation.Config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.KategoriRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.impl.ProductServiceImpl
import com.chibychibystore.service.impl.WarehouseServiceImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import com.chibychibystore.testutils.BaseTest
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
@Config(sdk = [34])
class InventoryModuleIntegrationTest : BaseTest() {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var gudangRepository: GudangRepository
    private lateinit var kategoriRepository: KategoriRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var stokGudangRepository: StokGudangRepository
    private lateinit var warehouseService: WarehouseServiceImpl
    private lateinit var productService: ProductServiceImpl

    @Before
    fun setup() {
        // Setup in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        // Setup repositories
        gudangRepository = GudangRepository(database.gudangDao(), database.produkDao())
        kategoriRepository = KategoriRepository(database.kategoriDao(), database.produkDao())
        produkRepository = ProdukRepository(database.produkDao())
        stokGudangRepository = StokGudangRepository(database.stokGudangDao(), database.produkDao())

        // Setup services
        val authService = Mockito.mock(com.chibychibystore.service.AuthService::class.java)
        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }
        warehouseService = WarehouseServiceImpl(gudangRepository, produkRepository, stokGudangRepository, authService, database)
        productService = ProductServiceImpl(produkRepository, stokGudangRepository, authService)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `complete inventory workflow should work end-to-end`() = runTest {
        // 1. Create kategori
        val kategori = Kategori(id = 0L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())
        val createKategoriRes = kategoriRepository.createKategori(kategori)
        assertTrue(createKategoriRes.isSuccess)
        val kategoriId = createKategoriRes.getOrNull() ?: error("kategori id null")

        // 2. Create gudang
        val gudang = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        val createWarehouseResult = warehouseService.createGudang(gudang)
        assertTrue(createWarehouseResult.isSuccess)

        // 3. Create produk
        val produk = Produk(
            id = 0L,
            name = "Laptop",
            barcode = "LP001",
            categoryId = kategoriId,
            costPrice = 5000000.0,
            sellingPrice = 7000000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        val createProductResult = productService.createProduk(produk)
        assertTrue(createProductResult.isSuccess)

        // 4. Verify data integrity
        val allWarehouses = gudangRepository.getAllGudang().first()
        assertEquals(1, allWarehouses.size)
        assertEquals("Gudang Utama", allWarehouses[0].name)

        val allKategori = kategoriRepository.getAllKategori().first()
        assertEquals(1, allKategori.size)
        assertEquals("Elektronik", allKategori[0].name)

        val allProducts = produkRepository.getAllProduk().first()
        assertEquals(1, allProducts.size)
        assertEquals("Laptop", allProducts[0].name)
        assertEquals(10, allProducts[0].stockQuantity)

        // 5. Update stok - use actual product id
        val createdProducts = produkRepository.getAllProduk().first()
        val existingProduct = createdProducts.firstOrNull() ?: error("No product found")
        val updateRes = productService.adjustStock(existingProduct.id.toString(), 15)
        assertTrue(updateRes.isSuccess)

        val retrievedProduct = produkRepository.getProdukById(existingProduct.id).getOrNull()
        assertEquals(15, retrievedProduct?.stockQuantity)

        // 6. Check warehouse stock
        val warehouseStockResult = warehouseService.getStokGudang(1L)
        assertTrue(warehouseStockResult.isSuccess)
        val warehouseStock = warehouseStockResult.getOrNull()
        assertNotNull(warehouseStock)
        assertEquals(1, warehouseStock?.size)
        assertEquals("Laptop", warehouseStock?.get(0)?.name)
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

        warehouseService.createGudang(gudang1)
        warehouseService.createGudang(gudang2)

        val kategori = Kategori(
            id = 1L,
            name = "Elektronik",
            description = "Produk elektronik",
            createdAt = Date()
        )
        kategoriRepository.createKategori(kategori)

        val produk = Produk(
            id = 1L,
            name = "Laptop",
            barcode = "LP001",
            categoryId = 1L,
            costPrice = 5000000.0,
            sellingPrice = 7000000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduk(produk)

        // Transfer 5 units from Gudang A to Gudang B
        val transferResult = warehouseService.transferStok(
            produkId = 1L,
            dariGudangId = 1L,
            keGudangId = 2L,
            jumlah = 5
        )

        assertTrue(transferResult.isSuccess)

        // Verify stock in Gudang A decreased
        val stockInGudangA = stokGudangRepository.getStock(1L, 1L).getOrNull()
        assertEquals(5, stockInGudangA?.quantity)

        // Verify stock in Gudang B increased
        val stockInGudangB = stokGudangRepository.getStock(1L, 2L).getOrNull()
        assertEquals(5, stockInGudangB?.quantity)
    }

    @Test
    fun `product with low stock should be identified`() = runTest {
        // Create kategori and gudang
        val kategori = Kategori(
            id = 1L,
            name = "Elektronik",
            description = "Produk elektronik",
            createdAt = Date()
        )
        kategoriRepository.createKategori(kategori)

        val gudang = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        warehouseService.createGudang(gudang)

        // Create product with low stock
        val produk = Produk(
            id = 1L,
            name = "Laptop",
            barcode = "LP001",
            categoryId = 1L,
            costPrice = 5000000.0,
            sellingPrice = 7000000.0,
            stockQuantity = 1, // Below minStock
            minStock = 5,
            warehouseId = 1L,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduk(produk)

        // Get low stock products
        val lowStockResult = productService.getLowStockProduks()
        assertTrue(lowStockResult.isSuccess)

        val lowStockProducts = lowStockResult.getOrNull()
        assertNotNull(lowStockProducts)
        assertEquals(1, lowStockProducts?.size)
        assertEquals("Laptop", lowStockProducts?.get(0)?.name)
    }

    @Test
    fun `product search by barcode should work`() = runTest {
        // Setup
        val kategori = Kategori(
            id = 1L,
            name = "Elektronik",
            description = "Produk elektronik",
            createdAt = Date()
        )
        kategoriRepository.createKategori(kategori)

        val gudang = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta",
            capacity = 1000,
            createdAt = Date()
        )
        warehouseService.createGudang(gudang)

        val produk = Produk(
            id = 1L,
            name = "Laptop",
            barcode = "LP001",
            categoryId = 1L,
            costPrice = 5000000.0,
            sellingPrice = 7000000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduk(produk)

        // Search by barcode
        val searchResult = produkRepository.getProdukByBarcode("LP001")
        assertTrue(searchResult.isSuccess)

        val foundProduct = searchResult.getOrNull()
        assertNotNull(foundProduct)
        assertEquals("Laptop", foundProduct?.name)
        assertEquals("LP001", foundProduct?.barcode)
    }
}
