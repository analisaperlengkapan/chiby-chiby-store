package com.chibychibystore.ui.inventory

import app.cash.turbine.test
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.WarehouseService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class WarehouseViewModelTest {

    @Mock
    private lateinit var warehouseService: WarehouseService

    private lateinit var viewModel: WarehouseViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = WarehouseViewModel(warehouseService)
    }

    @Test
    fun `init should load warehouses automatically`() = runTest {
        // Given
        val warehouses = listOf(
            Gudang("1", "Main Warehouse", "Jakarta", 1000),
            Gudang("2", "Branch Warehouse", "Bandung", 500)
        )
        `when`(warehouseService.getWarehouses()).thenReturn(Result.success(warehouses))
        `when`(warehouseService.observeWarehouses()).thenReturn(flowOf(warehouses))
        `when`(warehouseService.getAllWarehouseStock()).thenReturn(Result.success(emptyMap()))

        // When - ViewModel is initialized

        // Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(warehouses, loadedState.warehouses)
        }

        verify(warehouseService).getWarehouses()
    }

    @Test
    fun `loadWarehouses should update state with warehouses on success`() = runTest {
        // Given
        val warehouses = listOf(
            Gudang("1", "Main Warehouse", "Jakarta", 1000),
            Gudang("2", "Branch Warehouse", "Bandung", 500)
        )
        `when`(warehouseService.getWarehouses()).thenReturn(Result.success(warehouses))
        `when`(warehouseService.observeWarehouses()).thenReturn(flowOf(warehouses))
        `when`(warehouseService.getAllWarehouseStock()).thenReturn(Result.success(emptyMap()))

        // When
        viewModel.loadWarehouses()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(warehouses, loadedState.warehouses)
        }

        verify(warehouseService).getWarehouses()
        verify(warehouseService).getAllWarehouseStock()
    }

    @Test
    fun `selectWarehouse should update selected warehouse and load its stock`() = runTest {
        // Given
        val warehouse = Gudang("1", "Main Warehouse", "Jakarta", 1000)
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        `when`(warehouseService.getWarehouseStock("1")).thenReturn(Result.success(products))
        `when`(warehouseService.observeWarehouseStock("1")).thenReturn(flowOf(products))

        // When
        viewModel.selectWarehouse(warehouse)

        // Then
        viewModel.uiState.test {
            val selectedState = awaitItem()
            assertEquals(warehouse, selectedState.selectedWarehouse)
            assertEquals(products, selectedState.products)
        }

        verify(warehouseService).getWarehouseStock("1")
    }

    @Test
    fun `transferStock should update state with success message on success`() = runTest {
        // Given
        val productId = "1"
        val fromWarehouseId = "1"
        val toWarehouseId = "2"
        val quantity = 5

        `when`(warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity))
            .thenReturn(Result.success(Unit))
        `when`(warehouseService.getAllWarehouseStock()).thenReturn(Result.success(emptyMap()))

        // When
        viewModel.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val transferringState = awaitItem()
            assertTrue(transferringState.isTransferring)

            val successState = awaitItem()
            assertFalse(successState.isTransferring)
            assertEquals("Transfer stok berhasil", successState.successMessage)
        }

        verify(warehouseService).transferStock(productId, fromWarehouseId, toWarehouseId, quantity)
        verify(warehouseService).getAllWarehouseStock()
    }

    @Test
    fun `transferStock should update state with error on failure`() = runTest {
        // Given
        val productId = "1"
        val fromWarehouseId = "1"
        val toWarehouseId = "2"
        val quantity = 5
        val errorMessage = "Insufficient stock"

        `when`(warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity))
            .thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val transferringState = awaitItem()
            assertTrue(transferringState.isTransferring)

            val errorState = awaitItem()
            assertFalse(errorState.isTransferring)
            assertEquals(errorMessage, errorState.error)
        }
    }

    @Test
    fun `assignProductToWarehouse should update state with success message on success`() = runTest {
        // Given
        val productId = "1"
        val warehouseId = "1"

        `when`(warehouseService.assignProductToWarehouse(productId, warehouseId))
            .thenReturn(Result.success(Unit))
        `when`(warehouseService.getAllWarehouseStock()).thenReturn(Result.success(emptyMap()))

        // When
        viewModel.assignProductToWarehouse(productId, warehouseId)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals("Produk berhasil dipindahkan ke gudang", successState.successMessage)
        }

        verify(warehouseService).assignProductToWarehouse(productId, warehouseId)
        verify(warehouseService).getAllWarehouseStock()
    }

    @Test
    fun `assignProductToWarehouse should update state with error on failure`() = runTest {
        // Given
        val productId = "1"
        val warehouseId = "1"
        val errorMessage = "Warehouse not found"

        `when`(warehouseService.assignProductToWarehouse(productId, warehouseId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.assignProductToWarehouse(productId, warehouseId)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
        }
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
    fun `clearSuccessMessage should clear success message`() = runTest {
        // Given
        viewModel.uiState.value.copy(successMessage = "Test success")

        // When
        viewModel.clearSuccessMessage()

        // Then
        assertNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `refresh should reload warehouses`() = runTest {
        // Given
        val warehouses = listOf(
            Gudang("1", "Main Warehouse", "Jakarta", 1000)
        )
        `when`(warehouseService.getWarehouses()).thenReturn(Result.success(warehouses))
        `when`(warehouseService.observeWarehouses()).thenReturn(flowOf(warehouses))
        `when`(warehouseService.getAllWarehouseStock()).thenReturn(Result.success(emptyMap()))

        // When
        viewModel.refresh()

        // Then
        verify(warehouseService).getWarehouses()
        verify(warehouseService).getAllWarehouseStock()
    }

    @Test
    fun `getWarehouseById should return warehouse with matching id`() = runTest {
        // Given
        val warehouses = listOf(
            Gudang("1", "Main Warehouse", "Jakarta", 1000),
            Gudang("2", "Branch Warehouse", "Bandung", 500)
        )
        viewModel.uiState.value.copy(warehouses = warehouses)

        // When
        val result = viewModel.getWarehouseById("2")

        // Then
        assertNotNull(result)
        assertEquals("Branch Warehouse", result?.nama)
    }

    @Test
    fun `getWarehouseById should return null when warehouse not found`() = runTest {
        // Given
        val warehouses = listOf(
            Gudang("1", "Main Warehouse", "Jakarta", 1000)
        )
        viewModel.uiState.value.copy(warehouses = warehouses)

        // When
        val result = viewModel.getWarehouseById("999")

        // Then
        assertNull(result)
    }

    @Test
    fun `getProductById should return product with matching id from selected warehouse`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1"),
            Produk("2", "Product 2", "222", "1", 20000.0, 25000.0, 5, "1")
        )
        viewModel.uiState.value.copy(products = products)

        // When
        val result = viewModel.getProductById("2")

        // Then
        assertNotNull(result)
        assertEquals("Product 2", result?.nama)
    }

    @Test
    fun `getProductById should return null when product not found`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        viewModel.uiState.value.copy(products = products)

        // When
        val result = viewModel.getProductById("999")

        // Then
        assertNull(result)
    }
}