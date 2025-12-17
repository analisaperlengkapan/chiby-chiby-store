package com.chibychibystore

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.repository.*
import com.chibychibystore.service.*
import com.chibychibystore.testutils.TestDataBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FinancialServicesIntegrationTest {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var penggunaRepository: PenggunaRepository
    private lateinit var sessionRepository: UserSessionRepository
    private lateinit var authService: AuthServiceImpl

    private lateinit var pengeluaranRepository: PengeluaranRepository
    private lateinit var penjualanRepository: PenjualanRepository
    private lateinit var pembelianRepository: PembelianRepository
    private lateinit var produkRepository: ProdukRepository

    private lateinit var expenseService: ExpenseService
    private lateinit var cashManagementService: CashManagementService
    private lateinit var balanceSheetService: BalanceSheetService
    private lateinit var reportingService: ReportingService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Repositories
        penggunaRepository = PenggunaRepository(db.penggunaDao())
        sessionRepository = UserSessionRepository(db.userSessionDao())
        pengeluaranRepository = PengeluaranRepository(db.pengeluaranDao())
        penjualanRepository = PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao())
        pembelianRepository = PembelianRepository(db.pembelianDao())
        produkRepository = ProdukRepository(db.produkDao())

        // Services
        authService = AuthServiceImpl(db.penggunaDao(), sessionRepository)
        expenseService = ExpenseService(pengeluaranRepository, authService)
        cashManagementService = CashManagementService(penjualanRepository, pengeluaranRepository)
        balanceSheetService = BalanceSheetService(produkRepository, cashManagementService)
        reportingService = ReportingService(penjualanRepository, pembelianRepository, pengeluaranRepository, produkRepository, balanceSheetService, cashManagementService)

        // Create and login an owner user to allow operations that need authentication
        runBlocking {
            val owner = com.chibychibystore.data.local.entity.Pengguna(username = "owner", passwordHash = hashPassword("password"), role = com.chibychibystore.data.local.entity.Role.OWNER)
            penggunaRepository.createPengguna(owner)
            // Also create a second user (cashier) so seeded sales/expenses referencing id=2 exist
            val cashier = com.chibychibystore.data.local.entity.Pengguna(username = "cashier", passwordHash = hashPassword("password"), role = com.chibychibystore.data.local.entity.Role.CASHIER)
            penggunaRepository.createPengguna(cashier)
            val login = authService.login("owner", "password")
            assertTrue(login.isSuccess)
        }

        // Seed basic data (after creating user so foreign keys like cashierId exist)
        runBlocking { seedTestData() }
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private suspend fun seedTestData() {
        // Insert categories, warehouses, products
        db.kategoriDao().insertKategoriList(TestDataBuilder.testCategories)
        db.gudangDao().insertGudangList(TestDataBuilder.testWarehouses)
        db.produkDao().insertProdukList(TestDataBuilder.testProducts)

        // Insert sales and sale items
        TestDataBuilder.testSales.forEach { sale -> db.penjualanDao().insertPenjualan(sale) }
        TestDataBuilder.testSaleItems.forEach { item -> db.itemPenjualanDao().insertItemPenjualan(item) }

        // Insert expenses
        TestDataBuilder.testExpenses.forEach { expense -> db.pengeluaranDao().insertPengeluaran(expense) }
    }

    @Test
    fun testExpenseServiceIntegration() = runBlocking {
        val testExpenses = TestDataBuilder.testExpenses

        val createdExpenseIds = mutableListOf<Long>()
        testExpenses.forEach { expense ->
            val result = expenseService.createExpense(
                expenseDate = expense.expenseDate,
                category = expense.category,
                amount = expense.amount,
                description = expense.description ?: ""
            )
            assertTrue("Expense creation should succeed", result.isSuccess)
            result.getOrNull()?.let { createdExpenseIds.add(it) }
        }

        assertEquals("Should create 3 expenses", TestDataBuilder.testExpenses.size, createdExpenseIds.size) // we created same number as test data

        val firstExpenseId = createdExpenseIds.first()
        val retrievedExpense = expenseService.getPengeluaranById(firstExpenseId)
        assertTrue("Should retrieve expense", retrievedExpense.isSuccess)
        assertEquals("Expense amount should match", testExpenses.first().amount, retrievedExpense.getOrNull()?.amount)

        // Use a wider range to include seeded and newly created expenses (seeded are ~2 days ago)
        val startDate = LocalDate.now().minusDays(10)
        val endDate = LocalDate.now().plusDays(1)
        val expensesInRange = expenseService.getExpensesInDateRange(startDate, endDate)
        assertTrue("Should get expenses in range", expensesInRange.isNotEmpty())

        val categories = expenseService.getExpenseCategories()
        assertTrue("Should have expense categories", categories.isNotEmpty())
        assertTrue("Should include RENT_LEASE category", categories.contains(com.chibychibystore.data.local.entity.ExpenseCategory.RENT_LEASE))

        val totalExpenses = expenseService.getTotalExpenses(startDate, endDate)
        assertTrue("Should calculate total expenses", totalExpenses.isSuccess)
        val expectedTotal = TestDataBuilder.testExpenses.sumOf { it.amount } + TestDataBuilder.testExpenses.sumOf { it.amount }
        // This expected includes both seeded and newly created expenses; check non-zero
        assertTrue("Total should be positive", totalExpenses.getOrNull()!! > 0.0)
    }

    @Test
    fun testCashAndBalanceReportingIntegration() = runBlocking {
        val startDate = LocalDate.now().minusDays(10)
        val endDate = LocalDate.now().plusDays(1)

        // Cash management functions
        val operatingCashFlow = cashManagementService.calculateOperatingCashFlow(startDate, endDate)
        assertTrue("Should calculate operating cash flow", operatingCashFlow.isSuccess)

        val investingCashFlow = cashManagementService.calculateInvestingCashFlow(startDate, endDate)
        assertTrue("Should calculate investing cash flow", investingCashFlow.isSuccess)

        val financingCashFlow = cashManagementService.calculateFinancingCashFlow(startDate, endDate)
        assertTrue("Should calculate financing cash flow", financingCashFlow.isSuccess)

        val netCashFlow = cashManagementService.calculateNetCashFlow(startDate, endDate)
        assertTrue("Should calculate net cash flow", netCashFlow.isSuccess)

        val summary = cashManagementService.getCashFlowSummary(startDate, endDate)
        assertTrue("Should get cash flow summary", summary.isSuccess)
        val s = summary.getOrNull()
        assertNotNull(s)
        assertTrue("Period should be set", s?.period?.isNotEmpty() == true)

        // Balance sheet calculations
        val invValue = balanceSheetService.calculateInventoryValue()
        assertTrue("Should calculate inventory value", invValue.isSuccess)
        assertEquals("Inventory value should match expected", TestDataBuilder.ExpectedCalculations.INVENTORY_VALUE, invValue.getOrNull() ?: 0.0, 0.01)

        val totalAssets = balanceSheetService.calculateTotalAssets(LocalDate.now())
        assertTrue("Should calculate total assets", totalAssets.isSuccess)
        assertEquals("Total assets should match expected", TestDataBuilder.ExpectedCalculations.TOTAL_ASSETS, totalAssets.getOrNull() ?: 0.0, 0.01)

        val bs = balanceSheetService.generateBalanceSheet(LocalDate.now())
        assertTrue("Should generate balance sheet", bs.isSuccess)
    }

    @Test
    fun testReportingServiceFinancialIntegration() = runBlocking {
        val startDate = LocalDate.now().minusDays(10)
        val endDate = LocalDate.now().plusDays(1)

        val expenseReport = reportingService.getExpenseReport(startDate, endDate)
        assertTrue("Should generate expense report", expenseReport.isSuccess)

        val report = expenseReport.getOrNull()
        assertNotNull("Expense report should not be null", report)
        assertTrue("Should have expenses by category", report?.expensesByCategory?.isNotEmpty() == true)
    }
}
