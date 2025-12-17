package com.chibychibystore.ui.pos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productService: ProductService
    private lateinit var saleService: SaleService
    private lateinit var authService: AuthService
    private lateinit var viewModel: PosViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        productService = mock()
        saleService = mock()

        // default auth service returns a logged in cashier
        authService = object : AuthService {
            override suspend fun login(username: String, password: String) = Result.failure(Exception("not implemented"))
            override suspend fun logout() = Result.success(Unit)
            override suspend fun getCurrentUser() = Pengguna(id = 1L, username = "kasir", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.CASHIER)
            override suspend fun hasPermission(permission: String) = true
            override suspend fun changePassword(oldPassword: String, newPassword: String) = Result.success(Unit)
            override fun observeCurrentUser() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun initializeSession() = Result.success(Unit)
            override suspend fun isSessionExpired() = false
            override suspend fun extendSession() = Result.success(Unit)
            override suspend fun forceLogoutAll() = Result.success(Unit)
        }

        viewModel = PosViewModel(productService, saleService, authService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addProductToCart should add product and update totals`() {
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 1,
            createdAt = Date(),
            updatedAt = Date()
        )

        viewModel.addProductToCart(product)

        val state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        assertEquals(product, state.cartItems[0].product)
        assertEquals(15000.0, state.subtotal, 0.01)
        assertEquals(1500.0, state.tax, 0.01)
        assertEquals(16500.0, state.total, 0.01)
    }

    @Test
    fun `updateCartItemQuantity should update quantity and totals`() {
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 1,
            createdAt = Date(),
            updatedAt = Date()
        )

        viewModel.addProductToCart(product)
        viewModel.updateCartItemQuantity(product.id, 3)

        val state = viewModel.uiState.value
        assertEquals(3, state.cartItems[0].quantity)
        assertEquals(45000.0, state.subtotal, 0.01)
        assertEquals(4500.0, state.tax, 0.01)
        assertEquals(49500.0, state.total, 0.01)
    }

    @Test
    fun `processPayment should set success state when saleService returns success`() = runTest {
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 1,
            createdAt = Date(),
            updatedAt = Date()
        )

        viewModel.addProductToCart(product)
        viewModel.setPaymentMethod("CASH")

        // Mock saleService.createSale to return success with a Penjualan id
        val savedSale = Penjualan(saleDate = Date(), totalAmount = viewModel.uiState.value.total, paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH, cashierId = 1L)
        val fakeSaleWithItems = com.chibychibystore.data.local.entity.PenjualanWithItems(savedSale, emptyList())
        whenever(saleService.createSale(any(), any())).thenReturn(Result.success(fakeSaleWithItems))

        viewModel.processPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showReceiptDialog)
        assertNotNull(state.successMessage)
        assertEquals("CASH", state.paymentMethod)
    }

    @Test
    fun `processPayment should set error when no current user`() = runTest {
        // Create an auth service that returns null user
        val unauth = object : AuthService {
            override suspend fun login(username: String, password: String) = Result.failure(Exception("not impl"))
            override suspend fun logout() = Result.success(Unit)
            override suspend fun getCurrentUser() = null
            override suspend fun hasPermission(permission: String) = true
            override suspend fun changePassword(oldPassword: String, newPassword: String) = Result.success(Unit)
            override fun observeCurrentUser() = kotlinx.coroutines.flow.flowOf(null)
            override suspend fun initializeSession() = Result.success(Unit)
            override suspend fun isSessionExpired() = false
            override suspend fun extendSession() = Result.success(Unit)
            override suspend fun forceLogoutAll() = Result.success(Unit)
        }

        viewModel = PosViewModel(productService, saleService, unauth)

        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 1,
            createdAt = Date(),
            updatedAt = Date()
        )

        viewModel.addProductToCart(product)
        viewModel.processPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("User tidak terautentikasi", state.error)
    }

    @Test
    fun `onBarcodeScanned should add product when found`() = runTest {
        val product = Produk(
            id = 1L,
            name = "Barcode Product",
            barcode = "899123",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 20000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 1,
            createdAt = Date(),
            updatedAt = Date()
        )

        whenever(productService.searchProducts("899123")).thenReturn(Result.success(listOf(product)))

        viewModel.startScanning()
        viewModel.onBarcodeScanned("899123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertTrue(state.cartItems.any { it.product.id == product.id })
    }

    @Test
    fun `printReceipt should return error when no sale id`() = runTest {
        // no completedSaleId set
        viewModel.printReceipt()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ID penjualan tidak ditemukan", state.error)
    }

    @Test
    fun `printReceipt should call saleService and set success on success`() = runTest {
        // Create a sale first by processing payment
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 1,
            createdAt = Date(),
            updatedAt = Date()
        )

        viewModel.addProductToCart(product)
        whenever(saleService.createSale(any(), any())).thenReturn(Result.success(com.chibychibystore.data.local.entity.PenjualanWithItems(Penjualan(saleDate = Date(), totalAmount = viewModel.uiState.value.total, paymentMethod = com.chibychibystore.data.local.entity.PaymentMethod.CASH, cashierId = 1L), emptyList())))

        viewModel.processPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        // Now sale should be created and completedSaleId set
        val saleId = viewModel.uiState.value.completedSaleId
        assertNotNull("completedSaleId should be set after successful payment", saleId)

        whenever(saleService.printReceipt(any(), any(), any(), any())).thenReturn(Result.success(Unit))

        viewModel.printReceipt()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isPrintingReceipt)
        assertEquals("Receipt berhasil dicetak", state.successMessage)
        verify(saleService).printReceipt(saleId!!, "Chiby Chiby Store", "Jl. Example No. 123, Jakarta", "kasir")
    }
}
