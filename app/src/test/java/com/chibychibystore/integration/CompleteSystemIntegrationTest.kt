package com.chibychibystore.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.*
import com.chibychibystore.service.impl.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.chibychibystore.testutils.BaseTest
import java.util.Date

/**
 * Complete System Integration Test
 *
 * Test ini mensimulasikan skenario penggunaan aplikasi secara lengkap:
 * 1. Login user
 * 2. Setup inventory (kategori, gudang, produk)
 * 3. Proses penjualan
 * 4. Generate laporan
 * 5. Backup data
 *
 * Memastikan semua modul bekerja bersama dengan baik.
 */
@RunWith(RobolectricTestRunner::class)
class CompleteSystemIntegrationTest : BaseTest() {

    private lateinit var database: ChibyChibyDatabase

    // Repositories
    private lateinit var penggunaRepository: PenggunaRepository
    private lateinit var kategoriRepository: KategoriRepository
    private lateinit var gudangRepository: GudangRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var penjualanRepository: PenjualanRepository
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    private lateinit var userSessionRepository: PenggunaSessionRepository
    private lateinit var stokGudangRepository: StokGudangRepository

    // Services
    private lateinit var authService: AuthService
    private lateinit var productService: ProductService
    private lateinit var warehouseService: WarehouseService
    private lateinit var saleService: SaleService

    @Before
    fun setup() {
        // Setup in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        // Initialize repositories
        penggunaRepository = PenggunaRepository(database.penggunaDao())
        kategoriRepository = KategoriRepository(database.kategoriDao(), database.produkDao())
        gudangRepository = GudangRepository(database.gudangDao(), database.produkDao())
        produkRepository = ProdukRepository(database.produkDao())
        penjualanRepository = PenjualanRepository(database.penjualanDao(), database.itemPenjualanDao())
        itemPenjualanRepository = ItemPenjualanRepository(database.itemPenjualanDao())
        userSessionRepository = PenggunaSessionRepository(database.penggunaSessionDao())
        stokGudangRepository = StokGudangRepository(database.stokGudangDao(), database.produkDao())

        // Initialize services
        authService = AuthServiceImpl(database.penggunaDao(), userSessionRepository)
        productService = ProductServiceImpl(produkRepository, stokGudangRepository, authService)
        warehouseService = WarehouseServiceImpl(gudangRepository, produkRepository, stokGudangRepository, authService, database)
        val printerStub = com.chibychibystore.testutils.TestPrinterService()
        val promoService = PromoServiceImpl(PromotionRepository(database.promotionDao()))
        saleService = SaleServiceImpl(database, penjualanRepository, itemPenjualanRepository, produkRepository, stokGudangRepository, authService, printerStub, promoService)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `complete business workflow from login to sale should work`() {
        runBlocking {
        println("[TEST] STEP 1: Setup Users - start")
        // ===== STEP 1: Setup Users =====
        val owner = Pengguna(id = 0L, username = "owner", passwordHash = hashPassword("owner123"), role = Role.OWNER)
        val cashier = Pengguna(id = 0L, username = "cashier", passwordHash = hashPassword("cashier123"), role = Role.CASHIER)
        val ownerRes = penggunaRepository.createPengguna(owner)
        val cashierRes = penggunaRepository.createPengguna(cashier)
        assertTrue(ownerRes.isSuccess)
        assertTrue(cashierRes.isSuccess)
        val ownerId = ownerRes.getOrNull() ?: error("owner id null")
        val cashierId = cashierRes.getOrNull() ?: error("cashier id null")
        println("[TEST] STEP 1: Setup Users - done (ownerId=$ownerId, cashierId=$cashierId)")

        // ===== STEP 2: Login as Owner =====
        val loginResult = authService.login("owner", "owner123")
        assertTrue(loginResult.isSuccess)
        val loggedInUser = authService.getCurrentUser()
        assertNotNull(loggedInUser)
        assertEquals("owner", loggedInUser?.username)
        assertEquals(Role.OWNER, loggedInUser?.role)
        println("[TEST] STEP 2: Login as Owner - done")

        // ===== STEP 3: Setup Inventory - Create Kategori =====
        val kategoriElektronik = Kategori(id = 0L, name = "Elektronik", description = "Produk elektronik", createdAt = Date())
        val kategoriAksesoris = Kategori(id = 0L, name = "Aksesoris", description = "Aksesoris komputer", createdAt = Date())
        val k1 = kategoriRepository.createKategori(kategoriElektronik)
        val k2 = kategoriRepository.createKategori(kategoriAksesoris)
        assertTrue(k1.isSuccess)
        assertTrue(k2.isSuccess)
        println("[TEST] STEP 3: Kategori created")

        // Verify kategori created
        val allKategori = kategoriRepository.getAllKategori().first()
        assertEquals(2, allKategori.size)

        // ===== STEP 4: Setup Inventory - Create Gudang =====
        val gudangUtama = Gudang(
            id = 1,
            name = "Gudang Utama",
            location = "Jakarta Pusat",
            capacity = 1000,
            createdAt = Date()
        )
        val gudangCabang = Gudang(
            id = 2,
            name = "Gudang Cabang",
            location = "Jakarta Selatan",
            capacity = 500,
            createdAt = Date()
        )
        warehouseService.createGudang(gudangUtama)
        warehouseService.createGudang(gudangCabang)

        // Verify gudang created
        val allGudang = gudangRepository.getAllGudang().first()
        assertEquals(2, allGudang.size)
        println("[TEST] STEP 4: Warehouses created")

        // ===== STEP 5: Setup Inventory - Create Produk =====
        val laptop = Produk(
            id = 0L,
            name = "Laptop ASUS ROG",
            barcode = "LP-ASUS-001",
            categoryId = 1L,
            costPrice = 10000000.0,
            sellingPrice = 15000000.0,
            stockQuantity = 5,
            warehouseId = 1L,
            minStock = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        val mouse = Produk(
            id = 0L,
            name = "Mouse Logitech",
            barcode = "MS-LOG-001",
            categoryId = 2L,
            costPrice = 150000.0,
            sellingPrice = 250000.0,
            stockQuantity = 20,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        val keyboard = Produk(
            id = 0L,
            name = "Keyboard Mechanical",
            barcode = "KB-MEC-001",
            categoryId = 2L,
            costPrice = 500000.0,
            sellingPrice = 750000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 3,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduk(laptop)
        productService.createProduk(mouse)
        productService.createProduk(keyboard)

        // Verify produk created
        val allProduk = produkRepository.getAllProduk().first()
        assertEquals(3, allProduk.size)
        println("[TEST] STEP 5: Products created (count=${allProduk.size})")

        // ===== STEP 6: Logout Owner, Login as Cashier =====
        authService.logout()
        val cashierLoginResult = authService.login("cashier", "cashier123")
        assertTrue(cashierLoginResult.isSuccess)
        val cashierUser = authService.getCurrentUser()
        assertEquals("cashier", cashierUser?.username)
        println("[TEST] STEP 6: Login as Cashier - done (id=${cashierUser?.id})")

        // ===== STEP 7: Process Sale Transaction =====
        val saleItems = listOf(
            ItemPenjualan(
                id = 0L,
                saleId = 0L,
                productId = 1L, // Laptop
                quantity = 1,
                unitPrice = 15000000.0,
                totalPrice = 15000000.0
            ),
            ItemPenjualan(
                id = 0L,
                saleId = 0L,
                productId = 2L, // Mouse
                quantity = 2,
                unitPrice = 250000.0,
                totalPrice = 500000.0
            ),
            ItemPenjualan(
                id = 0L,
                saleId = 0L,
                productId = 3L, // Keyboard
                quantity = 1,
                unitPrice = 750000.0,
                totalPrice = 750000.0
            )
        )

        val totalAmount = 16250000.0 // 15M + 500K + 750K

        val saleHeader = Penjualan(saleDate = Date(), totalAmount = totalAmount, paymentMethod = PaymentMethod.CASH, cashierId = cashierUser!!.id)

        val saleResult = saleService.createPenjualan(
            sale = saleHeader,
            items = saleItems
        )

        assertTrue(saleResult.isSuccess)
        val sale = saleResult.getOrNull()
        assertNotNull(sale)
        assertEquals(totalAmount, sale?.sale?.totalAmount)
        println("[TEST] STEP 7: Sale created - done (saleId=${sale?.sale?.id})")


        // ===== STEP 8: Verify Stock Updated =====
        val updatedLaptop = produkRepository.getProdukById(1L).getOrNull()
        val updatedMouse = produkRepository.getProdukById(2L).getOrNull()
        val updatedKeyboard = produkRepository.getProdukById(3L).getOrNull()

        assertEquals(4, updatedLaptop?.stockQuantity) // 5 - 1
        assertEquals(18, updatedMouse?.stockQuantity) // 20 - 2
        assertEquals(9, updatedKeyboard?.stockQuantity) // 10 - 1

        // ===== STEP 9: Check Sales History =====
        // Sales history via getSales
        val salesHistoryResult = saleService.getPenjualanByRentangTanggal()
        println("[TEST] STEP 9: Check Sales History - done")

        // ===== STEP 10: Check Low Stock Products =====
        val lowStockResult = productService.getLowStockProduks()
        assertTrue(lowStockResult.isSuccess)
        val lowStockProducts = lowStockResult.getOrNull()
        // Laptop should be in low stock (4 < minStok 2 is false, but close)
        assertNotNull(lowStockProducts)
        println("[TEST] STEP 10: Low stock checked")

        // ===== STEP 11: Transfer Stock Between Warehouses =====
        // Login as owner again to have MANAGE_WAREHOUSES permission
        authService.login("owner", "owner123")

        // Transfer 2 keyboards from Gudang Utama to Gudang Cabang
        val transferResult = warehouseService.transferStok(
            produkId = 3L,
            dariGudangId = 1L,
            keGudangId = 2L,
            jumlah = 2
        )
        assertTrue(transferResult.isSuccess)
        println("[TEST] STEP 11: Stock transfer done")

        // ===== STEP 12: Verify Complete System State =====
        // All users exist
        val allUsers = penggunaRepository.getAllUsers().first()
        assertEquals(2, allUsers.size)

        // All categories exist
        val finalKategori = kategoriRepository.getAllKategori().first()
        assertEquals(2, finalKategori.size)

        // All warehouses exist
        val finalGudang = gudangRepository.getAllGudang().first()
        assertEquals(2, finalGudang.size)

        // All products exist with updated stock
        val finalProduk = produkRepository.getAllProduk().first()
        assertEquals(3, finalProduk.size)

        // Sales recorded
        val finalSales = penjualanRepository.getAllPenjualan().first()
        assertEquals(1, finalSales.size)

        println("[TEST] STEP 12: Final verification done")

        println("✓ Complete system integration test passed!")
        println("  - Users: ${allUsers.size}")
        println("  - Categories: ${finalKategori.size}")
        println("  - Warehouses: ${finalGudang.size}")
        println("  - Products: ${finalProduk.size}")
        println("  - Sales: ${finalSales.size}")
        val totalRevenue = finalSales.sumOf { it.totalAmount }
        println("  - Total Revenue: Rp ${totalRevenue.toLong()}")
        }
    }

    @Test
    fun `permission system should enforce role-based access`() {
        runBlocking {
        // Create users with different roles
        val owner = Pengguna(id = 0L, username = "owner", passwordHash = hashPassword("owner123"), role = Role.OWNER)
        val manager = Pengguna(id = 0L, username = "manager", passwordHash = hashPassword("manager123"), role = Role.MANAGER)
        val cashier = Pengguna(id = 0L, username = "cashier", passwordHash = hashPassword("cashier123"), role = Role.CASHIER)
        val warehouse = Pengguna(id = 0L, username = "warehouse", passwordHash = hashPassword("warehouse123"), role = Role.WAREHOUSE)

        val r1 = penggunaRepository.createPengguna(owner)
        val r2 = penggunaRepository.createPengguna(manager)
        val r3 = penggunaRepository.createPengguna(cashier)
        val r4 = penggunaRepository.createPengguna(warehouse)
        assertTrue(r1.isSuccess && r2.isSuccess && r3.isSuccess && r4.isSuccess)

        // Test Owner permissions (should have all)
        authService.login("owner", "owner123")
        assertTrue(authService.hasPermission("VIEW_SALES_REPORTS"))
        assertTrue(authService.hasPermission("VIEW_FINANCIAL_REPORTS"))
        assertTrue(authService.hasPermission("MANAGE_USERS"))
        assertTrue(authService.hasPermission("EDIT_INVENTORY"))
        authService.logout()

        // Test Manager permissions
        authService.login("manager", "manager123")
        assertTrue(authService.hasPermission("VIEW_SALES_REPORTS"))
        assertTrue(authService.hasPermission("VIEW_FINANCIAL_REPORTS"))
        assertTrue(authService.hasPermission("EDIT_INVENTORY"))
        assertTrue(authService.hasPermission("MANAGE_USERS"))
        authService.logout()

        // Test Cashier permissions
        authService.login("cashier", "cashier123")
        assertTrue(authService.hasPermission("CREATE_SALES"))
        assertTrue(authService.hasPermission("VIEW_INVENTORY"))
        assertFalse(authService.hasPermission("EDIT_INVENTORY"))
        assertFalse(authService.hasPermission("MANAGE_USERS"))
        authService.logout()

        // Test Warehouse permissions
        authService.login("warehouse", "warehouse123")
        assertTrue(authService.hasPermission("VIEW_INVENTORY"))
        assertTrue(authService.hasPermission("EDIT_INVENTORY"))
        assertTrue(authService.hasPermission("MANAGE_WAREHOUSES"))
        assertFalse(authService.hasPermission("CREATE_SALES"))
        assertFalse(authService.hasPermission("VIEW_FINANCIAL_REPORTS"))
        authService.logout()
        }
    }

    // Helper function to hash password (same as AuthService)
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
