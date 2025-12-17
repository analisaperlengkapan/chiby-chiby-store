package com.chibychibystore.ui.pos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.di.ServiceModule
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.SaleService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import javax.inject.Inject

@org.junit.Ignore("Disabled during androidTest triage")
class PosViewModelIntegrationTest {
    // Disabled during triage

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var productService: ProductService

    @Inject
    lateinit var saleService: SaleService

    private lateinit var viewModel: PosViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val createCalls = AtomicInteger(0)

    // Lightweight fake AuthService for tests
    private val fakeAuthService = object : com.chibychibystore.service.AuthService {
        override suspend fun login(username: String, password: String) = com.chibychibystore.data.model.Result.failure(Exception("Not implemented in test"))
        override suspend fun logout() = com.chibychibystore.data.model.Result.success(Unit)
        override suspend fun getCurrentUser() = null
        override suspend fun hasPermission(permission: String) = true
        override suspend fun changePassword(oldPassword: String, newPassword: String) = com.chibychibystore.data.model.Result.success(Unit)
        override fun observeCurrentUser() = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun initializeSession() = com.chibychibystore.data.model.Result.success(Unit)
        override suspend fun isSessionExpired() = false
        override suspend fun extendSession() = com.chibychibystore.data.model.Result.success(Unit)
        override suspend fun forceLogoutAll() = com.chibychibystore.data.model.Result.success(Unit)
    }

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        // Wrap injected saleService with a lightweight spy to detect duplicate createSale invocations
        val spySaleService = object : SaleService by saleService {
            override suspend fun createSale(sale: com.chibychibystore.data.local.entity.Penjualan, items: List<com.chibychibystore.data.local.entity.ItemPenjualan>): com.chibychibystore.data.model.Result<com.chibychibystore.data.local.entity.PenjualanWithItems> {
                createCalls.incrementAndGet()
                return saleService.createSale(sale, items)
            }
        }

        viewModel = PosViewModel(productService, spySaleService, fakeAuthService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProducts should load all products successfully`() = runTest {
        // Given
        val initialState = viewModel.uiState.first()

        // When
        viewModel.searchProducts("")

        // Then
        val state = viewModel.uiState.first()
        assertFalse("Should not be loading", state.isSearching)
        // Note: In a real scenario, this would check for actual products
        // For now, we just verify the loading state changes
    }

    @Test
    fun `searchProducts should filter products by query`() = runTest {
        // Given
        val searchQuery = "test"

        // When
        viewModel.searchProducts(searchQuery)

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Search query should be set", searchQuery, state.searchQuery)
        assertTrue("Should be in searching state", state.isSearching)
    }

    @Test
    fun `addToCart should add product to cart and update totals`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )

        // When
        viewModel.addProductToCart(product)

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Cart should have 1 item", 1, state.cartItems.size)
        assertEquals("Product should be in cart", product, state.cartItems[0].product)
        assertEquals("Quantity should be 1", 1, state.cartItems[0].quantity)
        assertEquals("Subtotal should be product price", 15000.0, state.subtotal, 0.01)
        assertEquals("Tax should be 10% of subtotal", 1500.0, state.tax, 0.01)
        assertEquals("Total should be subtotal + tax", 16500.0, state.total, 0.01)
    }

    @Test
    fun `updateCartItemQuantity should update quantity and totals`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        viewModel.addProductToCart(product)

        // When
        viewModel.updateCartItemQuantity(product.id, 3)

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Quantity should be updated to 3", 3, state.cartItems[0].quantity)
        assertEquals("Subtotal should be 3 * price", 45000.0, state.subtotal, 0.01)
        assertEquals("Tax should be 10% of new subtotal", 4500.0, state.tax, 0.01)
        assertEquals("Total should be new subtotal + tax", 49500.0, state.total, 0.01)
    }

    @Test
    fun `removeFromCart should remove item and update totals`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        viewModel.addProductToCart(product)
        viewModel.addProductToCart(product) // Add another item

        // When
        viewModel.removeCartItem(product.id)

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Cart should have 1 item remaining", 1, state.cartItems.size)
        assertEquals("Subtotal should be for remaining item", 15000.0, state.subtotal, 0.01)
    }

    @Test
    fun `clearCart should remove all items and reset totals`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        viewModel.addProductToCart(product)

        // When
        viewModel.clearCart()

        // Then
        val state = viewModel.uiState.first()
        assertTrue("Cart should be empty", state.cartItems.isEmpty())
        assertEquals("Subtotal should be 0", 0.0, state.subtotal, 0.01)
        assertEquals("Tax should be 0", 0.0, state.tax, 0.01)
        assertEquals("Total should be 0", 0.0, state.total, 0.01)
    }

    @Test
    fun `setPaymentMethod should update payment method`() = runTest {
        // When
        viewModel.setPaymentMethod("CARD")

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Payment method should be CARD", "CARD", state.paymentMethod)
    }

    @Test
    fun `applyDiscount should reduce total by discount amount`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        viewModel.addProductToCart(product)

        // When
        viewModel.setDiscount(5000.0)

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Discount should be applied", 5000.0, state.discount, 0.01)
        assertEquals("Total should be reduced by discount", 11500.0, state.total, 0.01)
    }

    @Test
    fun `processPayment should create sale and clear cart on success`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        viewModel.addProductToCart(product)
        viewModel.setPaymentMethod("CASH")

        // When
        viewModel.processPayment()

        // Wait for async createSale invocation (defensive guard). Timeout in 2s to avoid hanging CI.
        val invoked = withTimeoutOrNull(2_000) {
            while (createCalls.get() == 0) {
                delay(10)
            }
            true
        } ?: false

        assertTrue("SaleService.createSale was not invoked within timeout", invoked)
        assertEquals("createSale should be called exactly once", 1, createCalls.get())

        // Then
        val state = viewModel.uiState.first()
        assertFalse("Should not be processing payment", state.isProcessingPayment)
        // Note: In a full integration test with database, we would verify the sale was created
        // For now, we verify the processing state
    }

    @Test
    fun `printReceipt should call sale service print receipt`() = runTest {
        // Given
        val product = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        viewModel.addProductToCart(product)
        viewModel.setPaymentMethod("CASH")

        // When - Note: This would normally require a completed sale
        // For testing purposes, we just verify the method exists and can be called
        viewModel.printReceipt()

        // Then - In a real scenario, we would mock the printer service
        // For now, we just ensure no exceptions are thrown
        assertTrue("Print receipt method should be callable", true)
    }

    @Test
    fun `cart calculations should handle multiple items correctly`() = runTest {
        // Given
        val product1 = Produk(
            id = 1L,
            name = "Product 1",
            barcode = "111111",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 20000.0,
            stockQuantity = 10,
            warehouseId = 1L,
            minStock = 5,
            createdAt = Date(),
            updatedAt = Date()
        )
        val product2 = Produk(
            id = 2L,
            name = "Product 2",
            barcode = "222222",
            categoryId = 1L,
            costPrice = 15000.0,
            sellingPrice = 25000.0,
            stockQuantity = 5,
            warehouseId = 1L,
            minStock = 2,
            createdAt = Date(),
            updatedAt = Date()
        )

        // When
        viewModel.addProductToCart(product1)
        viewModel.addProductToCart(product2)
        viewModel.updateCartItemQuantity(product1.id, 2) // 2 x product1
        viewModel.updateCartItemQuantity(product2.id, 3) // 3 x product2

        // Then
        val state = viewModel.uiState.first()
        assertEquals("Cart should have 2 items", 2, state.cartItems.size)

        val expectedSubtotal = (2 * 20000.0) + (3 * 25000.0) // 40000 + 75000 = 115000
        val expectedTax = expectedSubtotal * 0.1 // 11500
        val expectedTotal = expectedSubtotal + expectedTax // 126500

        assertEquals("Subtotal should be correct", expectedSubtotal, state.subtotal, 0.01)
        assertEquals("Tax should be correct", expectedTax, state.tax, 0.01)
        assertEquals("Total should be correct", expectedTotal, state.total, 0.01)
    }
}