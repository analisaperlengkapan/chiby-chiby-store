package com.chibychibystore.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.impl.ProductServiceImpl
import com.chibychibystore.service.impl.SaleServiceImpl
import com.chibychibystore.service.impl.PromoServiceImpl
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.repository.PromotionRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.SaleService
import com.chibychibystore.ui.pos.PosViewModel
import com.chibychibystore.service.printer.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.chibychibystore.testutils.BaseTest

import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PosEndToEndIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var produkRepo: ProdukRepository
    private lateinit var penjualanRepo: PenjualanRepository
    private lateinit var itemPenjualanRepo: ItemPenjualanRepository
    private lateinit var productService: ProductServiceImpl
    private lateinit var saleService: SaleService
    private lateinit var authService: AuthService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        produkRepo = ProdukRepository(db.produkDao())
        penjualanRepo = PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao())
        itemPenjualanRepo = ItemPenjualanRepository(db.itemPenjualanDao())

        authService = Mockito.mock(AuthService::class.java)
        runBlocking {
            Mockito.`when`(authService.hasPermission(Mockito.anyString())).thenReturn(true)
        }

        val stokGudangRepo = StokGudangRepository(db.stokGudangDao(), db.produkDao())
        productService = ProductServiceImpl(produkRepo, stokGudangRepo, authService)
        val printer = Mockito.mock(PrinterService::class.java)
        val promotionRepo = PromotionRepository(db.promotionDao())
        val promoService = PromoServiceImpl(promotionRepo)
        saleService = SaleServiceImpl(db, penjualanRepo, itemPenjualanRepo, produkRepo, stokGudangRepo, authService, printer, promoService)
    }

    @After
    fun teardown() {
        db.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pos end-to-end flow via viewmodel creates sale and updates inventory`() = runTest {
        // Seed category and warehouse
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "Food"))
        val gudangId = db.gudangDao().insertGudang(com.chibychibystore.data.local.entity.Gudang(name = "Main"))

        // Seed product
        val prod = Produk(
            name = "E2E Product",
            barcode = "899000",
            categoryId = kategoriId,
            costPrice = 10000.0,
            sellingPrice = 20000.0,
            stockQuantity = 5,
            warehouseId = gudangId
        )
        val prodRes = productService.createProduk(prod)
        assertTrue("Product creation failed in E2E test", prodRes.isSuccess)
        val prodId = prodRes.getOrNull()?.id ?: error("product id null")
        val savedProd = db.produkDao().getProdukById(prodId)!!

        // Insert a cashier user
        val cashierId = db.penggunaDao().insertPengguna(Pengguna(username = "e2e_cashier", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER))
        val cashier = db.penggunaDao().getPenggunaById(cashierId)
        assertNotNull(cashier)

        // Fake AuthService returning the cashier
        val auth = object : AuthService {
            override suspend fun login(username: String, password: String) = com.chibychibystore.data.model.Result.failure(Exception("not used"))
            override suspend fun logout() = com.chibychibystore.data.model.Result.success(Unit)
            override suspend fun getCurrentUser() = cashier
            override suspend fun hasPermission(permission: String) = true
            override suspend fun changePassword(oldPassword: String, newPassword: String) = com.chibychibystore.data.model.Result.success(Unit)
            override fun observeCurrentUser() = kotlinx.coroutines.flow.flowOf(cashier)
            override suspend fun initializeSession() = com.chibychibystore.data.model.Result.success(Unit)
            override suspend fun isSessionExpired() = false
            override suspend fun extendSession() = com.chibychibystore.data.model.Result.success(Unit)
            override suspend fun forceLogoutAll() = com.chibychibystore.data.model.Result.success(Unit)
        }

        val spySaleService = com.chibychibystore.testutils.SaleServiceSpy(saleService)

        val promotionRepo = PromotionRepository(db.promotionDao())
        val promoService = PromoServiceImpl(promotionRepo)

        // Ensure ViewModel coroutines run on test dispatcher
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))

        val viewModel = PosViewModel(productService, spySaleService, auth, promoService)

        try {
            // Add product to cart and process payment
            viewModel.addProductToCart(savedProd)
            testScheduler.advanceUntilIdle() // Ensure cart item is added

            val state = viewModel.uiState.value
            assertEquals("Cart should have 1 item before payment. Error: ${state.error}", 1, state.cartItems.size)

            viewModel.setPaymentMethod("CASH")
            viewModel.processPayment()

            // Wait for the SaleService.createSale to complete via spy
            val completed = spySaleService.awaitInvocation(10_000)
            val stateAtTimeout = viewModel.uiState.value
            assertTrue("SaleService.createSale did not complete within timeout. State error=${stateAtTimeout.error}, cartSize=${stateAtTimeout.cartItems.size}", completed)

            // advance until viewmodel coroutine work completes
            testScheduler.advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }

        val state = viewModel.uiState.value
        // Diagnostic checks: if sale did not complete, surface the error for debugging
        if (state.completedSaleId == null) {
            fail("Sale did not complete. UI error=${state.error}, isProcessing=${state.isProcessingPayment}")
        }

        assertTrue("Sale should trigger receipt dialog", state.showReceiptDialog)
        assertNotNull("completedSaleId should be set", state.completedSaleId)

        // verify DB: sale persisted and stock reduced
        val created = db.penjualanDao().getPenjualanById(state.completedSaleId!!)
        assertNotNull("Penjualan should be persisted", created)

        val updatedProd = db.produkDao().getProdukById(prodId)!!
        assertEquals("Stock should be decremented by 1. Original was 5.", 4, updatedProd.stockQuantity)
        assertEquals("createSale (createPenjualan) should be called exactly once", 1, spySaleService.invocationCount)
    }
}
