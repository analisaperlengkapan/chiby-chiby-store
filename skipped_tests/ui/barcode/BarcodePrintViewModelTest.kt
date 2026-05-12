package com.chibychibystore.ui.barcode

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.printer.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodePrintViewModelTest {

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var printerService: PrinterService

    private lateinit var viewModel: BarcodePrintViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock default flows
        `when`(productService.observeProduks()).thenReturn(flowOf(emptyList()))

        viewModel = BarcodePrintViewModel(productService, printerService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // We need to collect the flow to trigger updates
        val job = launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(LabelSize.MEDIUM, state.selectedSize)
        assertEquals(1, state.quantity)

        job.cancel()
    }

    @Test
    fun `updateSearchQuery updates query and products`() = runTest {
        val produk = Produk(
            id = 1L,
            name = "Test Product",
            barcode = "123456",
            categoryId = 1L,
            costPrice = 8000.0,
            sellingPrice = 10000.0,
            stockQuantity = 10,
            warehouseId = 1L
        )

        `when`(productService.observeSearchProduks("Test")).thenReturn(flowOf(listOf(produk)))

        // Start collection
        val job = launch { viewModel.uiState.collect {} }

        viewModel.updateSearchQuery("Test")

        // Advance time for debounce (300ms)
        testDispatcher.scheduler.advanceTimeBy(301L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Test", state.searchQuery)
        assertEquals(listOf(produk), state.products)

        job.cancel()
    }

    @Test
    fun `clearing search query reloads all products`() = runTest {
        val allProducts = listOf(
             Produk(
                id = 1L,
                name = "Product 1",
                categoryId = 1L,
                costPrice = 1000.0,
                sellingPrice = 2000.0,
                warehouseId = 1L
            )
        )

        `when`(productService.observeProduks()).thenReturn(flowOf(allProducts))
        `when`(productService.observeSearchProduks("something")).thenReturn(flowOf(emptyList()))

        val job = launch { viewModel.uiState.collect {} }

        // First set search
        viewModel.updateSearchQuery("something")
        testDispatcher.scheduler.advanceTimeBy(301L)
        testDispatcher.scheduler.runCurrent()

        // Clear search
        viewModel.updateSearchQuery("")
        testDispatcher.scheduler.advanceTimeBy(301L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(allProducts, state.products)

        job.cancel()
    }

    @Test
    fun `selectProduct updates selectedProduct`() = runTest {
        val produk = Produk(
            id = 1L,
            name = "Test Product",
            categoryId = 1L,
            costPrice = 1000.0,
            sellingPrice = 2000.0,
            warehouseId = 1L
        )

        val job = launch { viewModel.uiState.collect {} }

        viewModel.selectProduct(produk)
        testDispatcher.scheduler.runCurrent()

        assertEquals(produk, viewModel.uiState.value.selectedProduct)

        job.cancel()
    }

    @Test
    fun `resetState resets all fields`() = runTest {
        val produk = Produk(
            id = 1L,
            name = "Test Product",
            categoryId = 1L,
            costPrice = 1000.0,
            sellingPrice = 2000.0,
            warehouseId = 1L
        )

        val job = launch { viewModel.uiState.collect {} }

        viewModel.selectProduct(produk)
        viewModel.updateSearchQuery("search")
        viewModel.updateQuantity(5)
        testDispatcher.scheduler.advanceTimeBy(301L)
        testDispatcher.scheduler.runCurrent()

        viewModel.resetState()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.selectedProduct)
        assertEquals("", state.searchQuery)
        assertEquals(1, state.quantity)

        job.cancel()
    }
}
