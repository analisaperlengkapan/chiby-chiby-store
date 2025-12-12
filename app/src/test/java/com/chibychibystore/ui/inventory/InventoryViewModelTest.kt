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
        viewModel = InventoryViewModel(productService)
    }

    @Test
    fun `initial state should be loading`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        `when`(productService.getProducts()).thenReturn(Result.success(products))
        `when`(productService.observeProducts()).thenReturn(flowOf(products))
        `when`(productService.getLowStockProducts()).thenReturn(Result.success(emptyList()))

        // When - ViewModel is initialized

        // Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertTrue(initialState.products.isEmpty())

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(products, loadedState.products)
        }
    }

    @Test
    fun `loadProducts should update state with products on success`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1"),
            Produk("2", "Product 2", "222", "1", 20000.0, 25000.0, 5, "1")
        )
        `when`(productService.getProducts()).thenReturn(Result.success(products))
        `when`(productService.observeProducts()).thenReturn(flowOf(products))
        `when`(productService.getLowStockProducts()).thenReturn(Result.success(emptyList()))

        // When
        viewModel.loadProducts()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(products, loadedState.products)
            assertNull(loadedState.error)
        }

        verify(productService).getProducts()
    }

    @Test
    fun `loadProducts should update state with error on failure`() = runTest {
        // Given
        val errorMessage = "Database error"
        `when`(productService.getProducts()).thenReturn(Result.failure(Exception(errorMessage)))
        `when`(productService.getLowStockProducts()).thenReturn(Result.success(emptyList()))

        // When
        viewModel.loadProducts()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
            assertTrue(errorState.products.isEmpty())
        }
    }

    @Test
    fun `updateSearchQuery should filter products when query is not blank`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Apple", "111", "1", 10000.0, 15000.0, 10, "1"),
            Produk("2", "Banana", "222", "1", 20000.0, 25000.0, 5, "1"),
            Produk("3", "Orange", "333", "1", 15000.0, 20000.0, 8, "1")
        )

        // Set initial products
        viewModel.uiState.value.copy(products = products)

        // When
        viewModel.updateSearchQuery("Apple")

        // Then
        viewModel.uiState.test {
            val filteredState = awaitItem()
            assertEquals(1, filteredState.products.size)
            assertEquals("Apple", filteredState.products.first().nama)
            assertEquals("Apple", filteredState.searchQuery)
        }
    }

    @Test
    fun `updateSearchQuery should load all products when query is blank`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Apple", "111", "1", 10000.0, 15000.0, 10, "1"),
            Produk("2", "Banana", "222", "1", 20000.0, 25000.0, 5, "1")
        )
        `when`(productService.getProducts()).thenReturn(Result.success(products))

        // When
        viewModel.updateSearchQuery("")

        // Then
        verify(productService).getProducts()
    }

    @Test
    fun `searchProducts should update state with filtered products on success`() = runTest {
        // Given
        val query = "test"
        val products = listOf(
            Produk("1", "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        `when`(productService.searchProducts(query)).thenReturn(Result.success(products))

        // When
        viewModel.searchProducts(query)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val searchState = awaitItem()
            assertFalse(searchState.isLoading)
            assertEquals(products, searchState.products)
            assertEquals(query, searchState.searchQuery)
        }

        verify(productService).searchProducts(query)
    }

    @Test
    fun `searchProducts should load all products when query is blank`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        `when`(productService.getProducts()).thenReturn(Result.success(products))

        // When
        viewModel.searchProducts("")

        // Then
        verify(productService).getProducts()
        verify(productService, never()).searchProducts(any())
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given
        viewModel.uiState.value.copy(error = "Test error")

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `refresh should reload products and low stock products`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        val lowStockProducts = listOf(
            Produk("2", "Low Stock Product", "222", "1", 1000.0, 1500.0, 1, "1", minStok = 5)
        )
        `when`(productService.getProducts()).thenReturn(Result.success(products))
        `when`(productService.getLowStockProducts()).thenReturn(Result.success(lowStockProducts))

        // When
        viewModel.refresh()

        // Then
        verify(productService).getProducts()
        verify(productService).getLowStockProducts()
    }
}