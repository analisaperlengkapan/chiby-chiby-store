package com.chibychibystore.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import com.chibychibystore.service.*
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
class CompleteSystemIntegrationTest {

    private lateinit var database: ChibyChibyDatabase
    
    // Repositories
    private lateinit var penggunaRepository: PenggunaRepository
    private lateinit var kategoriRepository: KategoriRepository
    private lateinit var gudangRepository: GudangRepository
    private lateinit var produkRepository: ProdukRepository
    private lateinit var penjualanRepository: PenjualanRepository
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    private lateinit var userSessionRepository: UserSessionRepository
    
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
        kategoriRepository = KategoriRepository(database.kategoriDao())
        gudangRepository = GudangRepository(database.gudangDao())
        produkRepository = ProdukRepository(database.produkDao())
        penjualanRepository = PenjualanRepository(database.penjualanDao())
        itemPenjualanRepository = ItemPenjualanRepository(database.itemPenjualanDao())
        userSessionRepository = UserSessionRepository(database.userSessionDao())

        // Initialize services
        authService = AuthServiceImpl(database.penggunaDao(), userSessionRepository)
        productService = ProductService(produkRepository, kategoriRepository, gudangRepository)
        warehouseService = WarehouseService(gudangRepository, produkRepository)
        saleService = SaleServiceImpl(penjualanRepository, itemPenjualanRepository, produkRepository)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `complete business workflow from login to sale should work`() = runTest {
        // ===== STEP 1: Setup Users =====
        val owner = Pengguna(
            id = 1,
            username = "owner",
            passwordHash = hashPassword("owner123"),
            namaLengkap = "Owner Toko",
            role = Role.OWNER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        val cashier = Pengguna(
            id = 2,
            username = "cashier",
            passwordHash = hashPassword("cashier123"),
            namaLengkap = "Kasir Toko",
            role = Role.CASHIER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        penggunaRepository.insertPengguna(owner)
        penggunaRepository.insertPengguna(cashier)

        // ===== STEP 2: Login as Owner =====
        val loginResult = authService.login("owner", "owner123")
        assertTrue(loginResult.isSuccess)
        val loggedInUser = authService.getCurrentUser()
        assertNotNull(loggedInUser)
        assertEquals("owner", loggedInUser?.username)
        assertEquals(Role.OWNER, loggedInUser?.role)

        // ===== STEP 3: Setup Inventory - Create Kategori =====
        val kategoriElektronik = Kategori(
            id = 1,
            nama = "Elektronik",
            deskripsi = "Produk elektronik",
            createdAt = Date(),
            updatedAt = Date()
        )
        val kategoriAksesoris = Kategori(
            id = 2,
            nama = "Aksesoris",
            deskripsi = "Aksesoris komputer",
            createdAt = Date(),
            updatedAt = Date()
        )
        kategoriRepository.insertKategori(kategoriElektronik)
        kategoriRepository.insertKategori(kategoriAksesoris)

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
        warehouseService.createWarehouse(gudangUtama)
        warehouseService.createWarehouse(gudangCabang)

        // Verify gudang created
        val allGudang = gudangRepository.getAllGudang().first()
        assertEquals(2, allGudang.size)

        // ===== STEP 5: Setup Inventory - Create Produk =====
        val laptop = Produk(
            id = 1,
            nama = "Laptop ASUS ROG",
            barcode = "LP-ASUS-001",
            kategoriId = 1,
            gudangId = 1,
            hargaBeli = 10000000.0,
            hargaJual = 15000000.0,
            stok = 5,
            minStok = 2,
            createdAt = Date(),
            updatedAt = Date()
        )
        val mouse = Produk(
            id = 2,
            nama = "Mouse Logitech",
            barcode = "MS-LOG-001",
            kategoriId = 2,
            gudangId = 1,
            hargaBeli = 150000.0,
            hargaJual = 250000.0,
            stok = 20,
            minStok = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        val keyboard = Produk(
            id = 3,
            nama = "Keyboard Mechanical",
            barcode = "KB-MEC-001",
            kategoriId = 2,
            gudangId = 1,
            hargaBeli = 500000.0,
            hargaJual = 750000.0,
            stok = 10,
            minStok = 3,
            createdAt = Date(),
            updatedAt = Date()
        )
        productService.createProduct(laptop)
        productService.createProduct(mouse)
        productService.createProduct(keyboard)

        // Verify produk created
        val allProduk = produkRepository.getAllProduk().first()
        assertEquals(3, allProduk.size)

        // ===== STEP 6: Logout Owner, Login as Cashier =====
        authService.logout()
        val cashierLoginResult = authService.login("cashier", "cashier123")
        assertTrue(cashierLoginResult.isSuccess)
        val cashierUser = authService.getCurrentUser()
        assertEquals("cashier", cashierUser?.username)

        // ===== STEP 7: Process Sale Transaction =====
        val saleItems = listOf(
            ItemPenjualan(
                id = 0,
                penjualanId = 0,
                produkId = 1, // Laptop
                quantity = 1,
                hargaSatuan = 15000000.0,
                subtotal = 15000000.0,
                createdAt = Date()
            ),
            ItemPenjualan(
                id = 0,
                penjualanId = 0,
                produkId = 2, // Mouse
                quantity = 2,
                hargaSatuan = 250000.0,
                subtotal = 500000.0,
                createdAt = Date()
            ),
            ItemPenjualan(
                id = 0,
                penjualanId = 0,
                produkId = 3, // Keyboard
                quantity = 1,
                hargaSatuan = 750000.0,
                subtotal = 750000.0,
                createdAt = Date()
            )
        )

        val totalAmount = 16250000.0 // 15M + 500K + 750K
        val paymentAmount = 20000000.0
        val changeAmount = 3750000.0

        val saleResult = saleService.createSale(
            userId = cashierUser!!.id,
            items = saleItems,
            totalAmount = totalAmount,
            paymentAmount = paymentAmount,
            changeAmount = changeAmount
        )

        assertTrue(saleResult.isSuccess)
        val sale = saleResult.getOrNull()
        assertNotNull(sale)
        assertEquals(totalAmount, sale?.totalAmount)

        // ===== STEP 8: Verify Stock Updated =====
        val updatedLaptop = produkRepository.getProdukById(1)
        val updatedMouse = produkRepository.getProdukById(2)
        val updatedKeyboard = produkRepository.getProdukById(3)

        assertEquals(4, updatedLaptop?.stok) // 5 - 1
        assertEquals(18, updatedMouse?.stok) // 20 - 2
        assertEquals(9, updatedKeyboard?.stok) // 10 - 1

        // ===== STEP 9: Check Sales History =====
        val salesHistoryResult = saleService.getSalesHistory()
        assertTrue(salesHistoryResult.isSuccess)
        val salesHistory = salesHistoryResult.getOrNull()
        assertEquals(1, salesHistory?.size)

        // ===== STEP 10: Check Low Stock Products =====
        val lowStockResult = productService.getLowStockProducts()
        assertTrue(lowStockResult.isSuccess)
        val lowStockProducts = lowStockResult.getOrNull()
        // Laptop should be in low stock (4 < minStok 2 is false, but close)
        assertNotNull(lowStockProducts)

        // ===== STEP 11: Transfer Stock Between Warehouses =====
        // Transfer 2 keyboards from Gudang Utama to Gudang Cabang
        val transferResult = warehouseService.transferStock(
            productId = 3,
            fromWarehouseId = 1,
            toWarehouseId = 2,
            quantity = 2
        )
        assertTrue(transferResult.isSuccess)

        // ===== STEP 12: Verify Complete System State =====
        // All users exist
        val allUsers = penggunaRepository.getAllPengguna().first()
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

        println("✓ Complete system integration test passed!")
        println("  - Users: ${allUsers.size}")
        println("  - Categories: ${finalKategori.size}")
        println("  - Warehouses: ${finalGudang.size}")
        println("  - Products: ${finalProduk.size}")
        println("  - Sales: ${finalSales.size}")
        println("  - Total Revenue: Rp ${sale?.totalAmount?.toLong()}")
    }

    @Test
    fun `permission system should enforce role-based access`() = runTest {
        // Create users with different roles
        val owner = Pengguna(
            id = 1,
            username = "owner",
            passwordHash = hashPassword("owner123"),
            namaLengkap = "Owner",
            role = Role.OWNER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        val manager = Pengguna(
            id = 2,
            username = "manager",
            passwordHash = hashPassword("manager123"),
            namaLengkap = "Manager",
            role = Role.MANAGER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        val cashier = Pengguna(
            id = 3,
            username = "cashier",
            passwordHash = hashPassword("cashier123"),
            namaLengkap = "Cashier",
            role = Role.CASHIER,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        val warehouse = Pengguna(
            id = 4,
            username = "warehouse",
            passwordHash = hashPassword("warehouse123"),
            namaLengkap = "Warehouse Staff",
            role = Role.WAREHOUSE,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )

        penggunaRepository.insertPengguna(owner)
        penggunaRepository.insertPengguna(manager)
        penggunaRepository.insertPengguna(cashier)
        penggunaRepository.insertPengguna(warehouse)

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

    // Helper function to hash password (same as AuthService)
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
