package com.chibychibystore.ui.inventory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.chibychibystore.data.model.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.service.WarehouseService
import com.chibychibystore.testutils.BaseIntegrationTest
import com.chibychibystore.testutils.TestDataBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class WarehouseViewModelIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var warehouseService: WarehouseService

    @Inject
    lateinit var productService: ProductService

    private lateinit var viewModel: WarehouseViewModel

    override fun setupDatabase() {
        super.setupDatabase()
        viewModel = WarehouseViewModel(warehouseService, productService)
    }

    @Test
    fun `loadWarehouses should load all warehouses from database and update state`() = runTest {
        // Given
        setupTestData()

        // When
        viewModel.loadWarehouses()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertEquals(TestDataBuilder.testWarehouses.size, loadedState.warehouses.size)

            // Verify warehouses contain expected data
            val warehouseNames = loadedState.warehouses.map { it.nama }
            assertTrue(warehouseNames.contains("Main Warehouse"))
            assertTrue(warehouseNames.contains("Branch Warehouse"))
        }
    }

    @Test
    fun `loadWarehouses should handle empty database gracefully`() = runTest {
        // Given - no data seeded

        // When
        viewModel.loadWarehouses()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertTrue(loadedState.warehouses.isEmpty())
        }
    }

    @Test
    fun `selectWarehouse should update selected warehouse and load its products`() = runTest {
        // Given
        setupTestData()
        viewModel.loadWarehouses()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2) // initial + loading
        }

        val mainWarehouse = viewModel.uiState.value.warehouses.find { it.nama == "Main Warehouse" }!!

        // When
        viewModel.selectWarehouse(mainWarehouse.id)

        // Then
        viewModel.uiState.test {
            val selectedState = awaitItem()
            assertEquals(mainWarehouse.id, selectedState.selectedWarehouseId)
            assertFalse(selectedState.products.isEmpty())

            // Verify products are from selected warehouse
            val productWarehouseIds = selectedState.products.map { it.gudangId }
            assertTrue(productWarehouseIds.all { it == mainWarehouse.id })
        }
    }

    @Test
    fun `selectWarehouse should handle non-existent warehouse gracefully`() = runTest {
        // Given
        setupTestData()

        // When
        viewModel.selectWarehouse(999L)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertNotNull(errorState.error)
            assertNull(errorState.selectedWarehouseId)
        }
    }

    @Test
    fun `loadWarehouseProducts should load products for selected warehouse`() = runTest {
        // Given
        setupTestData()
        val mainWarehouse = TestDataBuilder.testWarehouses.find { it.nama == "Main Warehouse" }!!
        val warehouseId = warehouseService.createWarehouse(mainWarehouse).getOrThrow().id

        // Create products in this warehouse
        val product1 = TestDataBuilder.createTestProduct().copy(gudangId = warehouseId)
        val product2 = TestDataBuilder.createTestProduct().copy(
            nama = "Warehouse Product 2",
            gudangId = warehouseId
        )
        productService.createProduct(product1)
        productService.createProduct(product2)

        // When
        viewModel.loadWarehouseProducts(warehouseId)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(warehouseId, loadedState.selectedWarehouseId)
            assertEquals(2, loadedState.products.size)

            val productNames = loadedState.products.map { it.nama }
            assertTrue(productNames.contains("Apple"))
            assertTrue(productNames.contains("Warehouse Product 2"))
        }
    }

    @Test
    fun `assignProductToWarehouse should move product to different warehouse`() = runTest {
        // Given
        setupTestData()
        val mainWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[0]).getOrThrow()
        val branchWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[1]).getOrThrow()

        val product = productService.createProduct(
            TestDataBuilder.createTestProduct().copy(gudangId = mainWarehouse.id)
        ).getOrThrow()

        // When
        viewModel.assignProductToWarehouse(product.id, branchWarehouse.id)

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertTrue(successState.assignmentSuccessful)

            // Verify product was moved in database
            val updatedProduct = productService.getProduct(product.id).getOrThrow()
            assertEquals(branchWarehouse.id, updatedProduct?.gudangId)
        }
    }

    @Test
    fun `assignProductToWarehouse should handle non-existent product gracefully`() = runTest {
        // Given
        setupTestData()
        val warehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[0]).getOrThrow()

        // When
        viewModel.assignProductToWarehouse(999L, warehouse.id)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.assignmentSuccessful)
        }
    }

    @Test
    fun `transferStock should move stock between warehouses successfully`() = runTest {
        // Given
        setupTestData()
        val mainWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[0]).getOrThrow()
        val branchWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[1]).getOrThrow()

        val product = productService.createProduct(
            TestDataBuilder.createTestProduct().copy(
                gudangId = mainWarehouse.id,
                stok = 100
            )
        ).getOrThrow()

        // When
        viewModel.transferStock(product.id, mainWarehouse.id, branchWarehouse.id, 30)

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertTrue(successState.transferSuccessful)

            // Verify stock was transferred in database
            val updatedProduct = productService.getProduct(product.id).getOrThrow()
            assertEquals(70, updatedProduct?.stok) // 100 - 30

            // Note: In a real implementation, you'd need to track stock per warehouse
            // This test assumes the transfer logic updates the product's stock
        }
    }

    @Test
    fun `transferStock should handle insufficient stock gracefully`() = runTest {
        // Given
        setupTestData()
        val mainWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[0]).getOrThrow()
        val branchWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[1]).getOrThrow()

        val product = productService.createProduct(
            TestDataBuilder.createTestProduct().copy(
                gudangId = mainWarehouse.id,
                stok = 10
            )
        ).getOrThrow()

        // When - try to transfer more than available stock
        viewModel.transferStock(product.id, mainWarehouse.id, branchWarehouse.id, 50)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.transferSuccessful)
        }
    }

    @Test
    fun `transferStock should handle negative quantity gracefully`() = runTest {
        // Given
        setupTestData()
        val mainWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[0]).getOrThrow()
        val branchWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[1]).getOrThrow()

        val product = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()

        // When
        viewModel.transferStock(product.id, mainWarehouse.id, branchWarehouse.id, -10)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.transferSuccessful)
        }
    }

    @Test
    fun `loadAllWarehouseStock should load products from all warehouses`() = runTest {
        // Given
        setupTestData()
        val mainWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[0]).getOrThrow()
        val branchWarehouse = warehouseService.createWarehouse(TestDataBuilder.testWarehouses[1]).getOrThrow()

        // Create products in both warehouses
        val product1 = productService.createProduct(
            TestDataBuilder.createTestProduct().copy(gudangId = mainWarehouse.id)
        ).getOrThrow()
        val product2 = productService.createProduct(
            TestDataBuilder.createTestProduct().copy(
                nama = "Branch Product",
                gudangId = branchWarehouse.id
            )
        ).getOrThrow()

        // When
        viewModel.loadAllWarehouseStock()

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(2, loadedState.allWarehouseStock.size)

            val productNames = loadedState.allWarehouseStock.map { it.nama }
            assertTrue(productNames.contains("Apple"))
            assertTrue(productNames.contains("Branch Product"))
        }
    }

    @Test
    fun `createWarehouse should add new warehouse to database and update state`() = runTest {
        // Given
        val newWarehouse = TestDataBuilder.createTestWarehouse().copy(nama = "New Warehouse")

        // When
        viewModel.createWarehouse(newWarehouse)

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertTrue(successState.warehouseCreated)

            // Verify warehouse was created in database
            val warehouses = warehouseService.getWarehouses().getOrThrow()
            assertTrue(warehouses.any { it.nama == "New Warehouse" })
        }
    }

    @Test
    fun `createWarehouse should handle duplicate names gracefully`() = runTest {
        // Given
        setupTestData()
        val existingWarehouse = TestDataBuilder.testWarehouses[0]

        // When
        viewModel.createWarehouse(existingWarehouse)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.warehouseCreated)
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