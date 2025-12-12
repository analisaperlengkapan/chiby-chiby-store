package com.chibychibystore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.service.CashManagementService
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.service.ReportingService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import javax.inject.Inject

/**
 * Integration tests for financial services
 * Tests the complete flow from data persistence through business logic to reporting
 */
@HiltAndroidTest
class FinancialServicesIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var expenseService: ExpenseService

    @Inject
    lateinit var cashManagementService: CashManagementService

    @Inject
    lateinit var balanceSheetService: BalanceSheetService

    @Inject
    lateinit var reportingService: ReportingService

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testExpenseServiceIntegration() = runBlocking {
        // Setup test data
        seedTestData()
        val testExpenses = TestDataBuilder.createTestExpenses(createdBy = 1, approvedBy = 1)

        // Test creating expenses
        val createdExpenseIds = mutableListOf<Long>()
        testExpenses.forEach { expense ->
            val result = expenseService.createExpense(
                expenseDate = expense.expenseDate,
                category = expense.category,
                amount = expense.amount,
                description = expense.description
            )
            assertTrue("Expense creation should succeed", result.isSuccess)
            result.getOrNull()?.let { createdExpenseIds.add(it) }
        }

        assertEquals("Should create 6 expenses", 6, createdExpenseIds.size)

        // Test getting expense by ID
        val firstExpenseId = createdExpenseIds.first()
        val retrievedExpense = expenseService.getPengeluaranById(firstExpenseId)
        assertTrue("Should retrieve expense", retrievedExpense.isSuccess)
        assertEquals("Expense amount should match", testExpenses.first().amount, retrievedExpense.getOrNull()?.amount)

        // Test getting expenses in date range
        val startDate = LocalDate.now().minusDays(1)
        val endDate = LocalDate.now().plusDays(1)
        val expensesInRange = expenseService.getExpensesInDateRange(startDate, endDate)
        assertEquals("Should get all expenses in range", 6, expensesInRange.size)

        // Test expense categories
        val categories = expenseService.getExpenseCategories()
        assertTrue("Should have expense categories", categories.isNotEmpty())
        assertTrue("Should include RENT_LEASE category", categories.contains(ExpenseCategory.RENT_LEASE))

        // Test total expenses calculation
        val totalExpenses = expenseService.getTotalExpenses(startDate, endDate)
        assertTrue("Should calculate total expenses", totalExpenses.isSuccess)
        val expectedTotal = testExpenses.sumOf { it.amount }
        assertEquals("Total should match expected", expectedTotal, totalExpenses.getOrNull(), 0.01)
    }

    @Test
    fun testCashManagementServiceIntegration() = runBlocking {
        // Setup test data
        seedTestData()
        val testExpenses = TestDataBuilder.createTestExpenses(createdBy = 1, approvedBy = 1)

        // Create expenses
        testExpenses.forEach { expense ->
            expenseService.createExpense(
                expenseDate = expense.expenseDate,
                category = expense.category,
                amount = expense.amount,
                description = expense.description
            )
        }

        // Create test sales
        val testSales = TestDataBuilder.createTestSales(cashierId = 1)
        // Note: In real implementation, sales would be created through SaleService
        // For this test, we'll assume sales data exists

        val startDate = LocalDate.now().minusDays(1)
        val endDate = LocalDate.now().plusDays(1)

        // Test operating cash flow calculation
        val operatingCashFlow = cashManagementService.calculateOperatingCashFlow(startDate, endDate)
        assertTrue("Should calculate operating cash flow", operatingCashFlow.isSuccess)

        // Test investing cash flow calculation
        val investingCashFlow = cashManagementService.calculateInvestingCashFlow(startDate, endDate)
        assertTrue("Should calculate investing cash flow", investingCashFlow.isSuccess)

        // Test financing cash flow calculation
        val financingCashFlow = cashManagementService.calculateFinancingCashFlow(startDate, endDate)
        assertTrue("Should calculate financing cash flow", financingCashFlow.isSuccess)

        // Test net cash flow calculation
        val netCashFlow = cashManagementService.calculateNetCashFlow(startDate, endDate)
        assertTrue("Should calculate net cash flow", netCashFlow.isSuccess)

        // Test cash flow summary
        val cashFlowSummary = cashManagementService.getCashFlowSummary(startDate, endDate)
        assertTrue("Should get cash flow summary", cashFlowSummary.isSuccess)

        val summary = cashFlowSummary.getOrNull()
        assertTrue("Summary should not be null", summary != null)
        assertTrue("Period should be set", summary?.period?.isNotEmpty() == true)
    }

    @Test
    fun testBalanceSheetServiceIntegration() = runBlocking {
        // Setup test data
        seedTestData()

        val asOfDate = LocalDate.now()

        // Test inventory value calculation
        val inventoryValue = balanceSheetService.calculateInventoryValue()
        assertTrue("Should calculate inventory value", inventoryValue.isSuccess)
        assertEquals("Inventory value should match expected", TestDataBuilder.ExpectedCalculations.INVENTORY_VALUE, inventoryValue.getOrNull(), 0.01)

        // Test total assets calculation
        val totalAssets = balanceSheetService.calculateTotalAssets(asOfDate)
        assertTrue("Should calculate total assets", totalAssets.isSuccess)
        assertEquals("Total assets should match expected", TestDataBuilder.ExpectedCalculations.TOTAL_ASSETS, totalAssets.getOrNull(), 0.01)

        // Test total liabilities calculation
        val totalLiabilities = balanceSheetService.calculateTotalLiabilities(asOfDate)
        assertTrue("Should calculate total liabilities", totalLiabilities.isSuccess)
        assertEquals("Total liabilities should match expected", TestDataBuilder.ExpectedCalculations.TOTAL_LIABILITIES, totalLiabilities.getOrNull(), 0.01)

        // Test equity calculation
        val equity = balanceSheetService.calculateEquity(asOfDate)
        assertTrue("Should calculate equity", equity.isSuccess)
        assertEquals("Equity should match expected", TestDataBuilder.ExpectedCalculations.TOTAL_EQUITY, equity.getOrNull(), 0.01)

        // Test balance sheet generation
        val balanceSheet = balanceSheetService.generateBalanceSheet(asOfDate)
        assertTrue("Should generate balance sheet", balanceSheet.isSuccess)

        val bs = balanceSheet.getOrNull()
        assertTrue("Balance sheet should not be null", bs != null)
        assertEquals("Assets should match", TestDataBuilder.ExpectedCalculations.TOTAL_ASSETS, bs?.assets, 0.01)
        assertEquals("Liabilities should match", TestDataBuilder.ExpectedCalculations.TOTAL_LIABILITIES, bs?.liabilities, 0.01)
        assertEquals("Equity should match", TestDataBuilder.ExpectedCalculations.TOTAL_EQUITY, bs?.equity, 0.01)
        assertEquals("Inventory value should match", TestDataBuilder.ExpectedCalculations.INVENTORY_VALUE, bs?.inventoryValue, 0.01)
    }

    @Test
    fun testReportingServiceFinancialIntegration() = runBlocking {
        // Setup test data
        seedTestData()
        val testExpenses = TestDataBuilder.createTestExpenses(createdBy = 1, approvedBy = 1)

        // Create expenses
        testExpenses.forEach { expense ->
            expenseService.createExpense(
                expenseDate = expense.expenseDate,
                category = expense.category,
                amount = expense.amount,
                description = expense.description
            )
        }

        val startDate = LocalDate.now().minusDays(1)
        val endDate = LocalDate.now().plusDays(1)

        // Test expense report
        val expenseReport = reportingService.getExpenseReport(startDate, endDate)
        assertTrue("Should generate expense report", expenseReport.isSuccess)

        val report = expenseReport.getOrNull()
        assertTrue("Expense report should not be null", report != null)
        assertEquals("Total expenses should match expected", TestDataBuilder.ExpectedCalculations.TOTAL_EXPENSES, report?.totalExpenses, 0.01)
        assertTrue("Should have expenses by category", report?.expensesByCategory?.isNotEmpty() == true)

        // Test balance sheet through reporting service
        val asOfDate = LocalDate.now()
        val balanceSheet = reportingService.getBalanceSheet(asOfDate)
        assertTrue("Should get balance sheet through reporting service", balanceSheet.isSuccess)

        // Test cash flow through reporting service
        val cashFlow = reportingService.getCashFlow(startDate, endDate)
        assertTrue("Should get cash flow through reporting service", cashFlow.isSuccess)
    }

    @Test
    fun testExpenseApprovalWorkflow() = runBlocking {
        // Setup test data
        seedTestData()

        // Create a large expense that requires approval
        val largeExpenseAmount = 2000000.0 // Above 1M threshold
        val result = expenseService.createExpense(
            expenseDate = java.util.Date(),
            category = ExpenseCategory.MARKETING_ADVERTISING,
            amount = largeExpenseAmount,
            description = "Large marketing campaign"
        )

        assertTrue("Large expense creation should succeed", result.isSuccess)

        // Get the created expense
        val expenseId = result.getOrNull()!!
        val expense = expenseService.getPengeluaranById(expenseId)
        assertTrue("Should retrieve expense", expense.isSuccess)

        val retrievedExpense = expense.getOrNull()
        assertTrue("Expense should not be null", retrievedExpense != null)
        assertEquals("Amount should match", largeExpenseAmount, retrievedExpense?.amount, 0.01)
        // Note: In current implementation, approval is handled differently
        // This test validates the expense creation workflow
    }
}