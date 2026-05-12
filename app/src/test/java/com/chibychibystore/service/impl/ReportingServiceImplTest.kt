package com.chibychibystore.service.impl
import org.robolectric.annotation.Config

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.PaymentMethod
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.ProdukTerpopulerDto
import com.chibychibystore.repository.*
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.service.CashManagementService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class ReportingServiceImplTest {

    @Mock private lateinit var penjualanRepository: PenjualanRepository
    @Mock private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    @Mock private lateinit var produkRepository: ProdukRepository
    @Mock private lateinit var pengeluaranRepository: PengeluaranRepository
    @Mock private lateinit var pembelianRepository: PembelianRepository
    @Mock private lateinit var balanceSheetService: BalanceSheetService
    @Mock private lateinit var cashManagementService: CashManagementService
    @Mock private lateinit var authService: AuthService
    // db is required by the ReportingServiceImpl constructor (used in getStockMovementReport);
    // it is not exercised by the tests below but must be provided for instantiation.
    @Mock private lateinit var db: ChibyChibyDatabase

    private lateinit var reportingService: ReportingServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        reportingService = ReportingServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository,
            pengeluaranRepository,
            pembelianRepository,
            balanceSheetService,
            cashManagementService,
            authService,
            db
        )
    }

    private fun localDateToDate(date: LocalDate): Date =
        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())

    companion object {
        /** Product ID that is intentionally absent from the products map, simulating a deleted product. */
        private const val MISSING_PRODUCT_ID = 99L
    }

    @Test
    fun `getInventoryReport should return failure when no permission`() = runBlocking {
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(false)
        val result = reportingService.getInventoryReport()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getInventoryReport should return data when permitted`() = runBlocking {
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(true)
        whenever(produkRepository.getProdukCount()).thenReturn(Result.success(10))
        whenever(produkRepository.countLowStock()).thenReturn(Result.success(1))
        whenever(produkRepository.countOutOfStock()).thenReturn(Result.success(0))

        val result = reportingService.getInventoryReport()

        assertTrue(result.isSuccess)
        val report = (result as Result.Success).data
        assertEquals(10, report.totalProduk)
        assertEquals(1, report.stokRendahCount)
        assertEquals(0, report.stokHabisCount)
    }

    @Test
    fun `getTopSellingProducts should return hydrated data`() = runBlocking {
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)).thenReturn(true)

        val topDto = listOf(ProdukTerpopulerDto(1L, 50, 50000.0))
        whenever(itemPenjualanRepository.getTopSellingProduks(10)).thenReturn(Result.success(topDto))

        val product = Produk(
            id = 1L,
            name = "Test Product",
            sellingPrice = 1000.0,
            costPrice = 800.0,
            stockQuantity = 50,
            minStock = 5,
            barcode = "123",
            categoryId = 1L,
            warehouseId = 1L
        )
        whenever(produkRepository.getProductsByIds(listOf(1L))).thenReturn(Result.success(listOf(product)))

        val result = reportingService.getTopSellingProducts(10)

        assertTrue(result.isSuccess)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Test Product", list[0].namaProduk)
        assertEquals(50, list[0].jumlahTerjual)
    }

    // ── getPeriodicPerformanceSummary tests ──────────────────────────────────

    @Test
    fun `getPeriodicPerformanceSummary returns failure when no permission`() = runBlocking {
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(false)
        val result = reportingService.getPeriodicPerformanceSummary(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `getPeriodicPerformanceSummary aggregates by DAY when range is 31 days or less`() = runBlocking {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 15)   // 14 days between → DAY bucket (range ≤ 31)
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(true)

        val saleDate = localDateToDate(startDate)
        val sale = Penjualan(
            id = 1L, saleDate = saleDate, totalAmount = 11000.0, tax = 1000.0,
            paymentMethod = PaymentMethod.CASH, cashierId = 1L
        )
        val item = ItemPenjualan(id = 1L, saleId = 1L, productId = 1L, quantity = 2, unitPrice = 5500.0, totalPrice = 11000.0)
        val saleWithItems = PenjualanWithItems(sale = sale, items = listOf(item))

        whenever(pengeluaranRepository.getPengeluaransByDateRangeList(any(), any())).thenReturn(emptyList())
        whenever(penjualanRepository.getSalesWithItemsInDateRange(startDate, endDate)).thenReturn(listOf(saleWithItems))
        val product = Produk(id = 1L, name = "P", sellingPrice = 5500.0, costPrice = 4000.0, stockQuantity = 10, minStock = 1, barcode = "B1", categoryId = 1L, warehouseId = 1L)
        whenever(produkRepository.getProductsByIds(listOf(1L))).thenReturn(Result.success(listOf(product)))

        val result = reportingService.getPeriodicPerformanceSummary(startDate, endDate)

        assertTrue(result.isSuccess)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        // Period key should be full date (DAY aggregation): "2025-01-01"
        assertEquals("2025-01-01", list[0].period)
        assertEquals(1, list[0].transactionCount)
    }

    @Test
    fun `getPeriodicPerformanceSummary aggregates by MONTH when range is 32 to 365 days`() = runBlocking {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 6, 1)   // ~150 days — MONTH bucket
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(true)

        val saleDate = localDateToDate(LocalDate.of(2025, 2, 14))
        val sale = Penjualan(id = 1L, saleDate = saleDate, totalAmount = 5000.0, tax = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)
        val saleWithItems = PenjualanWithItems(sale = sale, items = emptyList())

        whenever(pengeluaranRepository.getPengeluaransByDateRangeList(any(), any())).thenReturn(emptyList())
        whenever(penjualanRepository.getSalesWithItemsInDateRange(startDate, endDate)).thenReturn(listOf(saleWithItems))
        whenever(produkRepository.getProductsByIds(emptyList())).thenReturn(Result.success(emptyList()))

        val result = reportingService.getPeriodicPerformanceSummary(startDate, endDate)

        assertTrue(result.isSuccess)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        // Period key should be "YYYY-MM" (MONTH aggregation)
        assertEquals("2025-02", list[0].period)
    }

    @Test
    fun `getPeriodicPerformanceSummary aggregates by YEAR when range exceeds 365 days`() = runBlocking {
        val startDate = LocalDate.of(2023, 1, 1)
        val endDate = LocalDate.of(2025, 1, 1)   // ~730 days — YEAR bucket
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(true)

        val saleDate = localDateToDate(LocalDate.of(2024, 6, 15))
        val sale = Penjualan(id = 1L, saleDate = saleDate, totalAmount = 8000.0, tax = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)
        val saleWithItems = PenjualanWithItems(sale = sale, items = emptyList())

        whenever(pengeluaranRepository.getPengeluaransByDateRangeList(any(), any())).thenReturn(emptyList())
        whenever(penjualanRepository.getSalesWithItemsInDateRange(startDate, endDate)).thenReturn(listOf(saleWithItems))
        whenever(produkRepository.getProductsByIds(emptyList())).thenReturn(Result.success(emptyList()))

        val result = reportingService.getPeriodicPerformanceSummary(startDate, endDate)

        assertTrue(result.isSuccess)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        // Period key should be "YYYY" (YEAR aggregation)
        assertEquals("2024", list[0].period)
    }

    @Test
    fun `getPeriodicPerformanceSummary calculates revenue excluding tax`() = runBlocking {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 15)
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(true)

        val saleDate = localDateToDate(startDate)
        val sale = Penjualan(id = 1L, saleDate = saleDate, totalAmount = 11000.0, tax = 1000.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)
        val saleWithItems = PenjualanWithItems(sale = sale, items = emptyList())

        whenever(pengeluaranRepository.getPengeluaransByDateRangeList(any(), any())).thenReturn(emptyList())
        whenever(penjualanRepository.getSalesWithItemsInDateRange(startDate, endDate)).thenReturn(listOf(saleWithItems))
        whenever(produkRepository.getProductsByIds(emptyList())).thenReturn(Result.success(emptyList()))

        val result = reportingService.getPeriodicPerformanceSummary(startDate, endDate)

        assertTrue(result.isSuccess)
        val summary = (result as Result.Success).data[0]
        // Revenue must exclude tax: 11000 - 1000 = 10000
        assertEquals(10000.0, summary.sales, 0.001)
    }

    @Test
    fun `getPeriodicPerformanceSummary uses costPrice for COGS and falls back to zero when product missing`() = runBlocking {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 15)
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(true)

        val saleDate = localDateToDate(startDate)
        val sale = Penjualan(id = 1L, saleDate = saleDate, totalAmount = 10000.0, tax = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)
        // MISSING_PRODUCT_ID will not be in the products map, simulating a deleted product
        val item = ItemPenjualan(id = 1L, saleId = 1L, productId = MISSING_PRODUCT_ID, quantity = 2, unitPrice = 5000.0, totalPrice = 10000.0)
        val saleWithItems = PenjualanWithItems(sale = sale, items = listOf(item))

        whenever(pengeluaranRepository.getPengeluaransByDateRangeList(any(), any())).thenReturn(emptyList())
        whenever(penjualanRepository.getSalesWithItemsInDateRange(startDate, endDate)).thenReturn(listOf(saleWithItems))
        // Product MISSING_PRODUCT_ID is not in the result — simulates a deleted product
        whenever(produkRepository.getProductsByIds(listOf(MISSING_PRODUCT_ID))).thenReturn(Result.success(emptyList()))

        val result = reportingService.getPeriodicPerformanceSummary(startDate, endDate)

        assertTrue(result.isSuccess)
        val summary = (result as Result.Success).data[0]
        // COGS should fall back to 0 (not unitPrice), so netProfit == revenue
        assertEquals(10000.0, summary.sales, 0.001)
        assertEquals(10000.0, summary.netProfit, 0.001)
    }

    @Test
    fun `getPeriodicPerformanceSummary only includes OPERATING_EXPENSE_CATEGORIES in net profit`() = runBlocking {
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 15)
        whenever(authService.hasPermission("VIEW_FINANCIAL_REPORTS")).thenReturn(true)

        val saleDate = localDateToDate(startDate)
        val sale = Penjualan(id = 1L, saleDate = saleDate, totalAmount = 10000.0, tax = 0.0, paymentMethod = PaymentMethod.CASH, cashierId = 1L)
        val saleWithItems = PenjualanWithItems(sale = sale, items = emptyList())

        val expenseDate = localDateToDate(startDate)
        val operatingExpense = Pengeluaran(id = 1L, expenseDate = expenseDate, category = KategoriPengeluaran.RENT_LEASE, amount = 500.0, createdBy = 1L)
        val inventoryExpense = Pengeluaran(id = 2L, expenseDate = expenseDate, category = KategoriPengeluaran.INVENTORY_PURCHASES, amount = 3000.0, createdBy = 1L)
        val dividendExpense = Pengeluaran(id = 3L, expenseDate = expenseDate, category = KategoriPengeluaran.DIVIDEND, amount = 2000.0, createdBy = 1L)

        whenever(pengeluaranRepository.getPengeluaransByDateRangeList(any(), any()))
            .thenReturn(listOf(operatingExpense, inventoryExpense, dividendExpense))
        whenever(penjualanRepository.getSalesWithItemsInDateRange(startDate, endDate)).thenReturn(listOf(saleWithItems))
        whenever(produkRepository.getProductsByIds(emptyList())).thenReturn(Result.success(emptyList()))

        val result = reportingService.getPeriodicPerformanceSummary(startDate, endDate)

        assertTrue(result.isSuccess)
        val summary = (result as Result.Success).data[0]
        // Only RENT_LEASE (500) should reduce net profit; INVENTORY_PURCHASES and DIVIDEND must be excluded
        assertEquals(10000.0 - 500.0, summary.netProfit, 0.001)
    }
}
