package com.chibychibystore.ui.inventory

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.WarehouseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WarehouseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var warehouseService: WarehouseService

    private val sampleWarehouse = Gudang(id = 1, name = "Main Warehouse", location = "Jakarta", capacity = 1000)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        warehouseService = mock()
        whenever(warehouseService.observeGudangs()).thenReturn(flowOf(listOf(sampleWarehouse)))
        whenever(warehouseService.observeStokGudang(any())).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads warehouses`() = runTest {
        val viewModel = WarehouseViewModel(warehouseService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(sampleWarehouse), state.warehouses)
        assertNull(state.selectedWarehouse)
    }

    @Test
    fun `selectWarehouse updates selection and streams its stock`() = runTest {
        val product = Produk(id = 1, name = "Prod1", barcode = "111", categoryId = 1, costPrice = 1000.0, sellingPrice = 2000.0, stockQuantity = 10, warehouseId = 1)
        whenever(warehouseService.observeStokGudang(1)).thenReturn(flowOf(listOf(product)))

        val viewModel = WarehouseViewModel(warehouseService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectWarehouse(sampleWarehouse)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleWarehouse, state.selectedWarehouse)
        assertEquals(listOf(product), state.products)
    }

    @Test
    fun `createWarehouse delegates to the service and reports success`() = runTest {
        whenever(warehouseService.createGudang(any())).thenReturn(Result.success(sampleWarehouse.copy(id = 2, name = "New WH")))

        val viewModel = WarehouseViewModel(warehouseService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.createWarehouse("New WH", "Loc", 100)
        advanceUntilIdle()

        verify(warehouseService).createGudang(any())
        assertEquals("Gudang 'New WH' berhasil dibuat", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `deleteWarehouse delegates to the service`() = runTest {
        whenever(warehouseService.deleteGudang(1)).thenReturn(Result.success(Unit))

        val viewModel = WarehouseViewModel(warehouseService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.deleteWarehouse(1)
        advanceUntilIdle()

        verify(warehouseService).deleteGudang(1)
    }

    @Test
    fun `deleteWarehouse failure surfaces the error message`() = runTest {
        whenever(warehouseService.deleteGudang(1)).thenReturn(Result.failure(Exception("Gagal menghapus")))

        val viewModel = WarehouseViewModel(warehouseService)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.deleteWarehouse(1)
        advanceUntilIdle()

        assertEquals("Gagal menghapus", viewModel.uiState.value.error)
    }
}
