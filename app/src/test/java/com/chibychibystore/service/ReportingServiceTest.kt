package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.Penjualan
import com.chibychibystore.data.model.ItemPenjualan
import com.chibychibystore.data.model.Produk
import com.chibychibystore.data.model.Pengeluaran
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

class ReportingServiceTest {

    @Mock
    private lateinit var penjualanRepository: PenjualanRepository

    @Mock
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository

    @Mock
    private lateinit var produkRepository: ProdukRepository

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    private lateinit var reportingService: ReportingService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        reportingService = ReportingServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository,
            pengeluaranRepository
        )
    }

    @Test
    fun `getGrossSales should calculate total sales correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = LocalDate.of(2025, 1, 15), totalAmount = 50000.0, metodePembayaran = "CASH", kasirId = 1),
            Penjualan(id = 2, tanggalPenjualan = LocalDate.of(2025, 1, 20), totalAmount = 75000.0, metodePembayaran = "CASH", kasirId = 1)
        )

        `when`(penjualanRepository.getPenjualanByDateRange(startDate, endDate)).thenReturn(mockSales)

        // When
        val result = reportingService.getGrossSales(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(125000.0, report["totalSales"])
        assertEquals(2, report["totalTransactions"])
        assertEquals(62500.0, report["averageTransaction"])
    }

    @Test
    fun `getGrossSales should return zero for no sales`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        `when`(penjualanRepository.getPenjualanByDateRange(startDate, endDate)).thenReturn(emptyList())

        // When
        val result = reportingService.getGrossSales(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(0.0, report["totalSales"])
        assertEquals(0, report["totalTransactions"])
    }

    @Test
    fun `getProfitMargin should calculate profit correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = LocalDate.of(2025, 1, 15), totalAmount = 100000.0, metodePembayaran = "CASH", kasirId = 1)
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = 1, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0)
        )
        val mockProduct = Produk(
            id = 1, nama = "Test Product", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1
        )

        `when`(penjualanRepository.getPenjualanByDateRange(startDate, endDate)).thenReturn(mockSales)
        `when`(itemPenjualanRepository.getItemPenjualanByPenjualanId(1)).thenReturn(mockSaleItems)
        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct)

        // When
        val result = reportingService.getProfitMargin(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(100000.0, report["revenue"]) // Sales revenue
        assertEquals(60000.0, report["costOfGoodsSold"]) // 2 * 30000
        assertEquals(40000.0, report["grossProfit"])
        assertEquals(40.0, report["marginPercentage"])
    }

    @Test
    fun `getNetProfit should include expenses in calculation`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        // Mock sales data (gross profit = 40000)
        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = LocalDate.of(2025, 1, 15), totalAmount = 100000.0, metodePembayaran = "CASH", kasirId = 1)
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = 1, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0)
        )
        val mockProduct = Produk(
            id = 1, nama = "Test Product", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1
        )
        val mockExpenses = listOf(
            Pengeluaran(id = 1, tanggalPengeluaran = LocalDate.of(2025, 1, 10), kategori = "Utilitas", amount = 10000.0, description = "Listrik", approvedBy = 1, createdBy = 1)
        )

        `when`(penjualanRepository.getPenjualanByDateRange(startDate, endDate)).thenReturn(mockSales)
        `when`(itemPenjualanRepository.getItemPenjualanByPenjualanId(1)).thenReturn(mockSaleItems)
        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct)
        `when`(pengeluaranRepository.getPengeluaranByDateRange(startDate, endDate)).thenReturn(mockExpenses)

        // When
        val result = reportingService.getNetProfit(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(40000.0, report["grossProfit"])
        assertEquals(10000.0, report["totalExpenses"])
        assertEquals(30000.0, report["netProfit"])
    }

    @Test
    fun `getSalesByProduct should aggregate product sales correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = 1, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0),
            ItemPenjualan(id = 2, penjualanId = 2, produkId = 1, quantity = 1, unitPrice = 50000.0, totalPrice = 50000.0),
            ItemPenjualan(id = 3, penjualanId = 3, produkId = 2, quantity = 3, unitPrice = 30000.0, totalPrice = 90000.0)
        )
        val mockProduct1 = Produk(
            id = 1, nama = "Product A", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1
        )
        val mockProduct2 = Produk(
            id = 2, nama = "Product B", barcode = "1234567890124",
            kategoriId = 1, hargaBeli = 20000.0, hargaJual = 30000.0,
            stokQuantity = 15, gudangId = 1
        )

        `when`(itemPenjualanRepository.getItemPenjualanByDateRange(startDate, endDate)).thenReturn(mockSaleItems)
        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct1)
        `when`(produkRepository.getProduk(2)).thenReturn(mockProduct2)

        // When
        val result = reportingService.getSalesByProduct(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data as List<Map<String, Any>>
        assertEquals(2, report.size) // Two products

        // Check Product A
        val productA = report.find { it["productName"] == "Product A" }
        assertNotNull(productA)
        assertEquals(3, productA?.get("quantity")) // 2 + 1
        assertEquals(150000.0, productA?.get("revenue")) // 100000 + 50000

        // Check Product B
        val productB = report.find { it["productName"] == "Product B" }
        assertNotNull(productB)
        assertEquals(3, productB?.get("quantity"))
        assertEquals(90000.0, productB?.get("revenue"))
    }

    @Test
    fun `getSalesTrend should return daily sales data`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 3)

        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = LocalDate.of(2025, 1, 1), totalAmount = 50000.0, metodePembayaran = "CASH", kasirId = 1),
            Penjualan(id = 2, tanggalPenjualan = LocalDate.of(2025, 1, 1), totalAmount = 30000.0, metodePembayaran = "CASH", kasirId = 1),
            Penjualan(id = 3, tanggalPenjualan = LocalDate.of(2025, 1, 2), totalAmount = 75000.0, metodePembayaran = "CASH", kasirId = 1)
        )

        `when`(penjualanRepository.getPenjualanByDateRange(startDate, endDate)).thenReturn(mockSales)

        // When
        val result = reportingService.getSalesTrend(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data as List<Map<String, Any>>
        assertEquals(3, report.size) // Three days

        // Check day 1
        val day1 = report.find { (it["date"] as LocalDate).dayOfMonth == 1 }
        assertNotNull(day1)
        assertEquals(80000.0, day1?.get("sales")) // 50000 + 30000

        // Check day 2
        val day2 = report.find { (it["date"] as LocalDate).dayOfMonth == 2 }
        assertNotNull(day2)
        assertEquals(75000.0, day2?.get("sales"))

        // Check day 3 (no sales)
        val day3 = report.find { (it["date"] as LocalDate).dayOfMonth == 3 }
        assertNotNull(day3)
        assertEquals(0.0, day3?.get("sales"))
    }

    @Test
    fun `getIncomeStatement should calculate comprehensive financial data`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 31)

        // Mock sales and cost data
        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = LocalDate.of(2025, 1, 15), totalAmount = 200000.0, metodePembayaran = "CASH", kasirId = 1)
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = 1, produkId = 1, quantity = 4, unitPrice = 50000.0, totalPrice = 200000.0)
        )
        val mockProduct = Produk(
            id = 1, nama = "Test Product", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1
        )
        val mockExpenses = listOf(
            Pengeluaran(id = 1, tanggalPengeluaran = LocalDate.of(2025, 1, 10), kategori = "Utilitas", amount = 50000.0, description = "Listrik", approvedBy = 1, createdBy = 1),
            Pengeluaran(id = 2, tanggalPengeluaran = LocalDate.of(2025, 1, 20), kategori = "Supplies", amount = 30000.0, description = "Kertas", approvedBy = 1, createdBy = 1)
        )

        `when`(penjualanRepository.getPenjualanByDateRange(LocalDate.of(2025, 1, 1), date)).thenReturn(mockSales)
        `when`(itemPenjualanRepository.getItemPenjualanByDateRange(LocalDate.of(2025, 1, 1), date)).thenReturn(mockSaleItems)
        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct)
        `when`(pengeluaranRepository.getPengeluaranByDateRange(LocalDate.of(2025, 1, 1), date)).thenReturn(mockExpenses)

        // When
        val result = reportingService.getIncomeStatement(date)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(200000.0, report["revenue"])
        assertEquals(120000.0, report["costOfGoodsSold"]) // 4 * 30000
        assertEquals(80000.0, report["grossProfit"])
        assertEquals(80000.0, report["operatingExpenses"]) // 50000 + 30000
        assertEquals(0.0, report["netIncome"])
    }

    @Test
    fun `getExpenseReport should categorize expenses correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)

        val mockExpenses = listOf(
            Pengeluaran(id = 1, tanggalPengeluaran = LocalDate.of(2025, 1, 10), kategori = "Utilitas", amount = 50000.0, description = "Listrik", approvedBy = 1, createdBy = 1),
            Pengeluaran(id = 2, tanggalPengeluaran = LocalDate.of(2025, 1, 20), kategori = "Utilitas", amount = 30000.0, description = "Air", approvedBy = 1, createdBy = 1),
            Pengeluaran(id = 3, tanggalPengeluaran = LocalDate.of(2025, 1, 15), kategori = "Supplies", amount = 25000.0, description = "Kertas", approvedBy = 1, createdBy = 1)
        )

        `when`(pengeluaranRepository.getPengeluaranByDateRange(startDate, endDate)).thenReturn(mockExpenses)

        // When
        val result = reportingService.getExpenseReport(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(105000.0, report["totalExpenses"])

        val expensesByCategory = report["expensesByCategory"] as Map<String, Double>
        assertEquals(80000.0, expensesByCategory["Utilitas"]) // 50000 + 30000
        assertEquals(25000.0, expensesByCategory["Supplies"])
    }

    @Test
    fun `getBalanceSheet should calculate assets and equity correctly`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 31)

        // Mock inventory value calculation
        val mockProducts = listOf(
            Produk(id = 1, nama = "Product A", barcode = "1234567890123", kategoriId = 1,
                   hargaBeli = 30000.0, hargaJual = 50000.0, stokQuantity = 10, gudangId = 1),
            Produk(id = 2, nama = "Product B", barcode = "1234567890124", kategoriId = 1,
                   hargaBeli = 20000.0, hargaJual = 30000.0, stokQuantity = 5, gudangId = 1)
        )

        `when`(produkRepository.getAllProduk()).thenReturn(mockProducts)

        // When
        val result = reportingService.getBalanceSheet(date)

        // Then
        assertTrue(result is Result.Success)
        val report = (result as Result.Success).data
        assertEquals(500000.0, report["inventoryValue"]) // (10*30000) + (5*20000) + markup
        assertEquals(500000.0, report["assets"]) // Simplified: just inventory
        assertEquals(0.0, report["liabilities"]) // No liabilities in this simple model
        assertEquals(500000.0, report["equity"]) // Assets - Liabilities
    }
}