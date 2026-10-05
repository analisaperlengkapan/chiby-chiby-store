package com.chibychibystore.ui.inventory

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productService: ProductService
    private lateinit var viewModel: InventoryViewModel

    private val apple = Produk(id = 1, name = "Apple", barcode = "111", categoryId = 1, costPrice = 10_000.0, sellingPrice = 15_000.0, stockQuantity = 10, warehouseId = 1)
    private val banana = Produk(id = 2, name = "Banana", barcode = "222", categoryId = 1, costPrice = 20_000.0, sellingPrice = 25_000.0, stockQuantity = 5, warehouseId = 1)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        productService = mock()
        whenever(productService.observeProduks()).thenReturn(flowOf(listOf(apple, banana)))
        whenever(productService.observeSearchProduks("Apple")).thenReturn(flowOf(listOf(apple)))
        whenever(productService.observeLowStockProduks()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state streams all products`() = runTest {
        viewModel = InventoryViewModel(productService)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(apple, banana), state.products)
        assertEquals(emptyList<Produk>(), state.lowStockProducts)
    }

    @Test
    fun `updateSearchQuery switches to the search stream`() = runTest {
        viewModel = InventoryViewModel(productService)

        advanceUntilIdle()
        assertEquals(listOf(apple, banana), viewModel.uiState.value.products)

        viewModel.updateSearchQuery("Apple")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Apple", state.searchQuery)
        assertEquals(listOf(apple), state.products)
    }

    @Test
    fun `clearing the query restores the full stream`() = runTest {
        viewModel = InventoryViewModel(productService)

        viewModel.updateSearchQuery("Apple")
        advanceUntilIdle()
        assertEquals(listOf(apple), viewModel.uiState.value.products)

        viewModel.updateSearchQuery("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(listOf(apple, banana), state.products)
    }
}
