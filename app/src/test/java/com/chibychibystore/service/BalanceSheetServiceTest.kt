package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

@ExperimentalCoroutinesApi
class BalanceSheetServiceTest {

    @Mock
    private lateinit var productRepository: ProdukRepository

    @Mock
    private lateinit var cashManagementService: CashManagementService

    private lateinit var balanceSheetService: BalanceSheetService

    private val testProduct = Produk(
        id = 1,
        name = "Test Product",
        barcode = "123456789",
        categoryId = 1,
        costPrice = 10000.0,
        sellingPrice = 15000.0,
        stockQuantity = 10,
        warehouseId = 1,
        minStock = 5,
        createdAt = java.util.Date(),
        updatedAt = java.util.Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        balanceSheetService = BalanceSheetService(productRepository, cashManagementService)
    }

    @Test
    fun `calculateTotalAssets should return correct total`() = runTest {
        // Given
        val products = listOf(testProduct)
        `when`(productRepository.getAllProduk()).thenReturn(products)
        `when`(cashManagementService.getCurrentCashPosition()).thenReturn(Result.Success(50000.0))

        // When
        val result = balanceSheetService.calculateTotalAssets(LocalDate.now())

        // Then
        assertTrue(result is Result.Success)
        val totalAssets = (result as Result.Success).data
        // Assets = cash (50000) + inventory value (10000 * 10 = 100000) = 150000
        assertEquals(150000.0, totalAssets, 0.01)
    }

    @Test
    fun `calculateInventoryValue should return correct value`() = runTest {
        // Given
        val products = listOf(testProduct)
        `when`(productRepository.getAllProduk()).thenReturn(products)

        // When
        val result = balanceSheetService.calculateInventoryValue()

        // Then
        assertTrue(result is Result.Success)
        val inventoryValue = (result as Result.Success).data
        // Inventory value = 10000 * 10 = 100000
        assertEquals(100000.0, inventoryValue, 0.01)
    }

    @Test
    fun `calculateTotalLiabilities should return zero for now`() = runTest {
        // When
        val result = balanceSheetService.calculateTotalLiabilities()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data, 0.01)
    }

    @Test
    fun `calculateEquity should return assets minus liabilities`() = runTest {
        // Given
        val products = listOf(testProduct)
        `when`(productRepository.getAllProduk()).thenReturn(products)
        `when`(cashManagementService.getCurrentCashPosition()).thenReturn(Result.Success(50000.0))

        // When
        val result = balanceSheetService.calculateEquity(LocalDate.now())

        // Then
        assertTrue(result is Result.Success)
        val equity = (result as Result.Success).data
        // Equity = Assets (150000) - Liabilities (0) = 150000
        assertEquals(150000.0, equity, 0.01)
    }

    @Test
    fun `generateBalanceSheet should return complete balance sheet`() = runTest {
        // Given
        val products = listOf(testProduct)
        `when`(productRepository.getAllProduk()).thenReturn(products)
        `when`(cashManagementService.getCurrentCashPosition()).thenReturn(Result.Success(50000.0))

        // When
        val result = balanceSheetService.generateBalanceSheet(LocalDate.now())

        // Then
        assertTrue(result is Result.Success)
        val balanceSheet = (result as Result.Success).data
        assertEquals(150000.0, balanceSheet.totalAssets, 0.01)
        assertEquals(0.0, balanceSheet.totalLiabilities, 0.01)
        assertEquals(150000.0, balanceSheet.totalEquity, 0.01)
        assertEquals(100000.0, balanceSheet.inventoryValue, 0.01)
    }
}