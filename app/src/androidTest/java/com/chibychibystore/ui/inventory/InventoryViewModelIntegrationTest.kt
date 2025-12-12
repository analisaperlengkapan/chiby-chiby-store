package com.chibychibystore.ui.inventory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.chibychibystore.service.ProductService
import com.chibychibystore.testutils.BaseIntegrationTest
import com.chibychibystore.testutils.TestDataBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class InventoryViewModelIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var productService: ProductService

    private lateinit var viewModel: InventoryViewModel

    override fun setupDatabase() {
        super.setupDatabase()
        viewModel = InventoryViewModel(productService)
    }

    @Test
    fun `loadProducts should load all products from database and update state`() = runTest {
        // Given
        setupTestData()

        // When
        viewModel.loadProducts()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertEquals(TestDataBuilder.testProducts.size, loadedState.products.size)

            // Verify products contain expected data
            val productNames = loadedState.products.map { it.nama }
            assertTrue(productNames.contains("Apple"))
            assertTrue(productNames.contains("Orange"))
            assertTrue(productNames.contains("Coca Cola"))
        }
    }

    @Test
    fun `loadProducts should handle empty database gracefully`() = runTest {
        // Given - no data seeded

        // When
        viewModel.loadProducts()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertTrue(loadedState.products.isEmpty())
        }
    }

    @Test
    fun `updateSearchQuery should filter products by name`() = runTest {
        // Given
        setupTestData()
        viewModel.loadProducts()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2) // initial + loading states
        }

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
    fun `updateSearchQuery should filter products by barcode`() = runTest {
        // Given
        setupTestData()
        viewModel.loadProducts()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2)
        }

        // When
        viewModel.updateSearchQuery("111111111111")

        // Then
        viewModel.uiState.test {
            val filteredState = awaitItem()
            assertEquals(1, filteredState.products.size)
            assertEquals("111111111111", filteredState.products.first().barcode)
        }
    }

    @Test
    fun `updateSearchQuery should show all products when query is cleared`() = runTest {
        // Given
        setupTestData()
        viewModel.loadProducts()

        // Skip to loaded state and apply filter
        viewModel.uiState.test {
            skipItems(2)
        }
        viewModel.updateSearchQuery("Apple")

        // When
        viewModel.updateSearchQuery("")

        // Then
        viewModel.uiState.test {
            val clearedState = awaitItem()
            assertEquals("", clearedState.searchQuery)
            assertEquals(TestDataBuilder.testProducts.size, clearedState.products.size)
        }
    }

    @Test
    fun `searchProducts should update state with filtered products on success`() = runTest {
        // Given
        setupTestData()

        // When
        viewModel.searchProducts("Coca")

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val searchState = awaitItem()
            assertFalse(searchState.isLoading)
            assertEquals(1, searchState.products.size)
            assertEquals("Coca Cola", searchState.products.first().nama)
            assertEquals("Coca", searchState.searchQuery)
        }
    }

    @Test
    fun `searchProducts should load all products when query is blank`() = runTest {
        // Given
        setupTestData()

        // When
        viewModel.searchProducts("")

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(TestDataBuilder.testProducts.size, loadedState.products.size)
            assertEquals("", loadedState.searchQuery)
        }
    }

    @Test
    fun `low stock products should be loaded and displayed`() = runTest {
        // Given
        setupTestData()

        // When - ViewModel initializes and loads low stock products

        // Then
        viewModel.uiState.test {
            // Skip to state with low stock data
            skipItems(2) // initial + loaded states

            val stateWithLowStock = awaitItem()
            assertFalse(stateWithLowStock.lowStockProducts.isEmpty())

            // Verify low stock products
            val lowStockNames = stateWithLowStock.lowStockProducts.map { it.nama }
            assertTrue(lowStockNames.contains("Low Stock Item"))
            assertTrue(lowStockNames.contains("Out of Stock Item"))
        }
    }

    @Test
    fun `refresh should reload products and low stock data`() = runTest {
        // Given
        setupTestData()
        viewModel.loadProducts()

        // When
        viewModel.refresh()

        // Then
        viewModel.uiState.test {
            // Skip existing states
            skipItems(3) // initial + loaded + refresh loading

            val refreshedState = awaitItem()
            assertFalse(refreshedState.isLoading)
            assertEquals(TestDataBuilder.testProducts.size, refreshedState.products.size)
            assertFalse(refreshedState.lowStockProducts.isEmpty())
        }
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given - simulate error state
        viewModel.uiState.value.copy(error = "Test error")

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }
}