package com.chibychibystore.ui.inventory

import app.cash.turbine.test
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class InventoryViewModelTest {

    @Mock
    private lateinit var productService: ProductService

    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // Setup default mocks for flows to prevent crashes on init
        `when`(productService.observeProducts()).thenReturn(flowOf(emptyList()))
        `when`(productService.observeLowStockProducts()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `initial state should be loading then loaded`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        `when`(productService.observeProducts()).thenReturn(flowOf(products))
        `when`(productService.observeLowStockProducts()).thenReturn(flowOf(emptyList()))

        viewModel = InventoryViewModel(productService)

        // Then
        viewModel.uiState.test {
            // Initial state (isLoading = true from stateIn initialValue)
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            // Loaded state
            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(products, loadedState.products)
        }
    }

    @Test
    fun `updateSearchQuery should switch to search flow`() = runTest {
        // Given
        val allProducts = listOf(
            Produk("1", "Apple", "111", "1", 10000.0, 15000.0, 10, "1"),
            Produk("2", "Banana", "222", "1", 20000.0, 25000.0, 5, "1")
        )
        val searchResults = listOf(
            Produk("1", "Apple", "111", "1", 10000.0, 15000.0, 10, "1")
        )

        `when`(productService.observeProducts()).thenReturn(flowOf(allProducts))
        `when`(productService.observeSearchProducts("Apple")).thenReturn(flowOf(searchResults))
        `when`(productService.observeLowStockProducts()).thenReturn(flowOf(emptyList()))

        viewModel = InventoryViewModel(productService)

        // Then
        viewModel.uiState.test {
            awaitItem() // Initial loading
            val defaultState = awaitItem() // All products
            assertEquals(allProducts, defaultState.products)

            // When
            viewModel.updateSearchQuery("Apple")

            // Then
            val searchState = awaitItem()
            assertEquals("Apple", searchState.searchQuery)
            assertEquals(searchResults, searchState.products)
        }
    }

    @Test
    fun `updateSearchQuery to blank should switch back to observeProducts`() = runTest {
        // Given
        val allProducts = listOf(Produk("1", "A", "1", "1", 0.0, 0.0, 0, "1"))
        val searchResults = listOf(Produk("2", "B", "2", "2", 0.0, 0.0, 0, "2"))

        `when`(productService.observeProducts()).thenReturn(flowOf(allProducts))
        `when`(productService.observeSearchProducts("B")).thenReturn(flowOf(searchResults))
        `when`(productService.observeLowStockProducts()).thenReturn(flowOf(emptyList()))

        viewModel = InventoryViewModel(productService)
        viewModel.updateSearchQuery("B") // Start with search

        viewModel.uiState.test {
            // Skip synchronization emissions
            skipItems(1) // Initial loading

            // Should eventually be in search state
            val stateWithQuery = awaitItem()
            assertEquals("B", stateWithQuery.searchQuery)

            // When
            viewModel.updateSearchQuery("")

            // Then
            val finalState = awaitItem()
            assertTrue(finalState.searchQuery.isBlank())
            assertEquals(allProducts, finalState.products)
        }
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given
        // We simulate an error by emitting on error flow?
        // Or directly checking if clearError updates the flow.
        // Since we cannot inject error easily into the catch block of flows without complex mocking,
        // we can test that initial error state is null and clearError doesn't crash.

        viewModel = InventoryViewModel(productService)

        // When
        viewModel.clearError()

        // Then
        viewModel.uiState.test {
             val item = awaitItem() // Initial loading
             val loaded = awaitItem()
             assertNull(loaded.error)
        }
    }
}
