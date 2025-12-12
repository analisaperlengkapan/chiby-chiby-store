package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.repository.GudangRepository
import com.chibychibystore.data.repository.ProdukRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class WarehouseServiceTest {

    @Mock
    private lateinit var gudangRepository: GudangRepository

    @Mock
    private lateinit var produkRepository: ProdukRepository

    private lateinit var warehouseService: WarehouseService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        warehouseService = WarehouseServiceImpl(gudangRepository, produkRepository)
    }

    @Test
    fun `createWarehouse should return success when warehouse is valid and name is unique`() = runTest {
        // Given
        val warehouse = Gudang(
            id = "1",
            nama = "Main Warehouse",
            lokasi = "Jakarta",
            kapasitas = 1000
        )

        `when`(gudangRepository.getWarehouseByName("Main Warehouse")).thenReturn(null)
        `when`(gudangRepository.createWarehouse(warehouse)).thenReturn(warehouse)

        // When
        val result = warehouseService.createWarehouse(warehouse)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(warehouse, result.getOrNull())
        verify(gudangRepository).createWarehouse(warehouse)
    }

    @Test
    fun `createWarehouse should return failure when name already exists`() = runTest {
        // Given
        val existingWarehouse = Gudang("existing", "Main Warehouse", "Jakarta", 1000)
        val newWarehouse = Gudang("new", "Main Warehouse", "Bandung", 500)

        `when`(gudangRepository.getWarehouseByName("Main Warehouse")).thenReturn(existingWarehouse)

        // When
        val result = warehouseService.createWarehouse(newWarehouse)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Nama gudang sudah digunakan", result.exceptionOrNull()?.message)
        verify(gudangRepository, never()).createWarehouse(any())
    }

    @Test
    fun `createWarehouse should return failure when name is blank`() = runTest {
        // Given
        val warehouse = Gudang("1", "", "Jakarta", 1000)

        // When
        val result = warehouseService.createWarehouse(warehouse)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Nama gudang tidak boleh kosong", result.exceptionOrNull()?.message)
        verify(gudangRepository, never()).createWarehouse(any())
    }

    @Test
    fun `createWarehouse should return failure when capacity is negative`() = runTest {
        // Given
        val warehouse = Gudang("1", "Main Warehouse", "Jakarta", -100)

        // When
        val result = warehouseService.createWarehouse(warehouse)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Kapasitas gudang tidak boleh negatif", result.exceptionOrNull()?.message)
        verify(gudangRepository, never()).createWarehouse(any())
    }

    @Test
    fun `deleteWarehouse should return success when warehouse has no products`() = runTest {
        // Given
        val warehouseId = "1"

        `when`(produkRepository.getProductsByWarehouse(warehouseId)).thenReturn(emptyList())

        // When
        val result = warehouseService.deleteWarehouse(warehouseId)

        // Then
        assertTrue(result.isSuccess)
        verify(gudangRepository).deleteWarehouse(warehouseId)
    }

    @Test
    fun `deleteWarehouse should return failure when warehouse has products`() = runTest {
        // Given
        val warehouseId = "1"
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, warehouseId)
        )

        `when`(produkRepository.getProductsByWarehouse(warehouseId)).thenReturn(products)

        // When
        val result = warehouseService.deleteWarehouse(warehouseId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Tidak dapat menghapus gudang yang masih memiliki produk", result.exceptionOrNull()?.message)
        verify(gudangRepository, never()).deleteWarehouse(any())
    }

    @Test
    fun `assignProductToWarehouse should return success when both warehouse and product exist`() = runTest {
        // Given
        val productId = "1"
        val warehouseId = "1"
        val warehouse = Gudang(warehouseId, "Main Warehouse", "Jakarta", 1000)
        val product = Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, "old_warehouse")

        `when`(gudangRepository.getWarehouse(warehouseId)).thenReturn(warehouse)
        `when`(produkRepository.getProduct(productId)).thenReturn(product)

        // When
        val result = warehouseService.assignProductToWarehouse(productId, warehouseId)

        // Then
        assertTrue(result.isSuccess)
        verify(produkRepository).assignProductToWarehouse(productId, warehouseId)
    }

    @Test
    fun `assignProductToWarehouse should return failure when warehouse does not exist`() = runTest {
        // Given
        val productId = "1"
        val warehouseId = "nonexistent"

        `when`(gudangRepository.getWarehouse(warehouseId)).thenReturn(null)

        // When
        val result = warehouseService.assignProductToWarehouse(productId, warehouseId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Gudang tidak ditemukan", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).assignProductToWarehouse(any(), any())
    }

    @Test
    fun `assignProductToWarehouse should return failure when product does not exist`() = runTest {
        // Given
        val productId = "nonexistent"
        val warehouseId = "1"
        val warehouse = Gudang(warehouseId, "Main Warehouse", "Jakarta", 1000)

        `when`(gudangRepository.getWarehouse(warehouseId)).thenReturn(warehouse)
        `when`(produkRepository.getProduct(productId)).thenReturn(null)

        // When
        val result = warehouseService.assignProductToWarehouse(productId, warehouseId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Produk tidak ditemukan", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).assignProductToWarehouse(any(), any())
    }

    @Test
    fun `transferStock should return success when all conditions are met`() = runTest {
        // Given
        val productId = "1"
        val fromWarehouseId = "1"
        val toWarehouseId = "2"
        val quantity = 5

        val fromWarehouseProducts = listOf(
            Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, fromWarehouseId)
        )

        `when`(produkRepository.getProductsByWarehouse(fromWarehouseId)).thenReturn(fromWarehouseProducts)

        // When
        val result = warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)

        // Then
        assertTrue(result.isSuccess)
        verify(produkRepository).transferStock(productId, fromWarehouseId, toWarehouseId, quantity)
    }

    @Test
    fun `transferStock should return failure when quantity is not positive`() = runTest {
        // Given
        val productId = "1"
        val fromWarehouseId = "1"
        val toWarehouseId = "2"
        val quantity = 0

        // When
        val result = warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Jumlah transfer harus lebih dari 0", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).transferStock(any(), any(), any(), any())
    }

    @Test
    fun `transferStock should return failure when source and destination warehouses are the same`() = runTest {
        // Given
        val productId = "1"
        val warehouseId = "1"
        val quantity = 5

        // When
        val result = warehouseService.transferStock(productId, warehouseId, warehouseId, quantity)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Gudang asal dan tujuan tidak boleh sama", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).transferStock(any(), any(), any(), any())
    }

    @Test
    fun `transferStock should return failure when source warehouse has insufficient stock`() = runTest {
        // Given
        val productId = "1"
        val fromWarehouseId = "1"
        val toWarehouseId = "2"
        val quantity = 15 // More than available stock

        val fromWarehouseProducts = listOf(
            Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, fromWarehouseId)
        )

        `when`(produkRepository.getProductsByWarehouse(fromWarehouseId)).thenReturn(fromWarehouseProducts)

        // When
        val result = warehouseService.transferStock(productId, fromWarehouseId, toWarehouseId, quantity)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Stok gudang asal tidak mencukupi", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).transferStock(any(), any(), any(), any())
    }

    @Test
    fun `getWarehouseStock should return products in warehouse`() = runTest {
        // Given
        val warehouseId = "1"
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, warehouseId),
            Produk("2", "Product 2", "222", "1", 20000.0, 25000.0, 5, warehouseId)
        )

        `when`(produkRepository.getProductsByWarehouse(warehouseId)).thenReturn(products)

        // When
        val result = warehouseService.getWarehouseStock(warehouseId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(products, result.getOrNull())
        verify(produkRepository).getProductsByWarehouse(warehouseId)
    }

    @Test
    fun `getAllWarehouseStock should return map of warehouse to products`() = runTest {
        // Given
        val warehouse1 = Gudang("1", "Warehouse 1", "Jakarta", 1000)
        val warehouse2 = Gudang("2", "Warehouse 2", "Bandung", 500)

        val warehouses = listOf(warehouse1, warehouse2)
        val products1 = listOf(Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1"))
        val products2 = listOf(Produk("2", "Product 2", "222", "1", 20000.0, 25000.0, 5, "2"))

        `when`(gudangRepository.getWarehouses()).thenReturn(warehouses)
        `when`(produkRepository.getProductsByWarehouse("1")).thenReturn(products1)
        `when`(produkRepository.getProductsByWarehouse("2")).thenReturn(products2)

        // When
        val result = warehouseService.getAllWarehouseStock()

        // Then
        assertTrue(result.isSuccess)
        val stockMap = result.getOrNull()
        assertEquals(products1, stockMap?.get(warehouse1))
        assertEquals(products2, stockMap?.get(warehouse2))
    }

    @Test
    fun `observeWarehouses should return flow from repository`() = runTest {
        // Given
        val warehouses = listOf(
            Gudang("1", "Warehouse 1", "Jakarta", 1000),
            Gudang("2", "Warehouse 2", "Bandung", 500)
        )
        val flow = flowOf(warehouses)

        `when`(gudangRepository.observeWarehouses()).thenReturn(flow)

        // When
        val result = warehouseService.observeWarehouses().first()

        // Then
        assertEquals(warehouses, result)
        verify(gudangRepository).observeWarehouses()
    }

    @Test
    fun `observeWarehouseStock should return flow from repository`() = runTest {
        // Given
        val warehouseId = "1"
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, warehouseId)
        )
        val flow = flowOf(products)

        `when`(produkRepository.observeProductsByWarehouse(warehouseId)).thenReturn(flow)

        // When
        val result = warehouseService.observeWarehouseStock(warehouseId).first()

        // Then
        assertEquals(products, result)
        verify(produkRepository).observeProductsByWarehouse(warehouseId)
    }
}