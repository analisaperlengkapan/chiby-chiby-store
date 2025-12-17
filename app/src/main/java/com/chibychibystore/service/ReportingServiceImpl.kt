package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportingServiceImpl @Inject constructor(
    private val penjualanRepository: PenjualanRepository,
    private val itemPenjualanRepository: ItemPenjualanRepository,
    private val produkRepository: ProdukRepository,
    private val pengeluaranRepository: PengeluaranRepository,
    private val pembelianRepository: PembelianRepository,
    private val balanceSheetService: BalanceSheetService,
    private val cashManagementService: CashManagementService
) : ReportingService {

    // Basic reports
    override suspend fun getDailySalesReport(date: LocalDate): Result<DailySalesReport> = try {
        val sales = penjualanRepository.getSalesInDateRange(date, date)
        val totalSales = sales.sumOf { it.totalAmount }
        val totalTransactions = sales.size
        val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
        Result.success(DailySalesReport(date, totalSales, totalTransactions, avg, emptyList()))
    } catch (e: Exception) {
        Result.failure(Exception("getDailySalesReport failed", e))
    }

    override suspend fun getMonthlySalesReport(year: Int, month: Int): Result<MonthlySalesReport> = try {
        Result.success(MonthlySalesReport(year, month, 0.0, 0, emptyList(), emptyList()))
    } catch (e: Exception) {
        Result.failure(Exception("getMonthlySalesReport failed", e))
    }

    override suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<FinancialReport> = try {
        val totalRevenue = penjualanRepository.getTotalPenjualanByDateRange(startDate, endDate).getOrNull() ?: 0.0
        val totalExpenses = pengeluaranRepository.getTotalExpenseAmount(startDate, endDate).getOrNull() ?: 0.0
        val totalCost = 0.0
        val grossProfit = totalRevenue - totalCost
        val netProfit = grossProfit - totalExpenses
        val profitMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
        Result.success(FinancialReport(startDate, endDate, totalRevenue, totalCost, grossProfit, totalExpenses, netProfit, profitMargin))
    } catch (e: Exception) {
        Result.failure(Exception("getFinancialReport failed", e))
    }

    override suspend fun getInventoryReport(): Result<InventoryReport> = try {
        Result.success(InventoryReport(0, 0.0, 0, 0, emptyList()))
    } catch (e: Exception) {
        Result.failure(Exception("getInventoryReport failed", e))
    }

    override suspend fun getTopSellingProducts(limit: Int): Result<List<ProductSalesData>> = try {
        Result.success(emptyList())
    } catch (e: Exception) {
        Result.failure(Exception("getTopSellingProducts failed", e))
    }

    override suspend fun getLowStockReport(): Result<List<LowStockProduct>> = try {
        Result.success(emptyList())
    } catch (e: Exception) {
        Result.failure(Exception("getLowStockReport failed", e))
    }

    override fun observeSalesMetrics() = flowOf(SalesMetrics(0.0, 0, 0.0, 0.0))

    // --- Helper methods used by UI/PDF/tests ---
    override suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> = try {
        val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
        // Exclude refunded sales from gross calculations
        val salesFiltered = sales.filter { !it.isRefunded }
        val totalSales = salesFiltered.sumOf { it.totalAmount }
        val totalTransactions = salesFiltered.size
        val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
        Result.success(mapOf("totalSales" to totalSales, "totalTransactions" to totalTransactions, "averageTransaction" to avg))
    } catch (e: Exception) {
        Result.failure(Exception("getGrossSales failed", e))
    }

    override suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> = try {
        // For profit calculations, revenue is the total sales in the period (including refunded sales)
        val revenue = penjualanRepository.getTotalPenjualanByDateRange(startDate, endDate).getOrNull() ?: 0.0

        // Cost of goods sold is derived from purchases in the period (pembelian)
        val purchases = pembelianRepository.getPurchasesInDateRange(startDate, endDate)
        val costOfGoods = purchases.sumOf { it.totalAmount }

        val grossProfit = revenue - costOfGoods
        val margin = if (revenue > 0) (grossProfit / revenue) * 100 else 0.0
        Result.success(mapOf("revenue" to revenue, "costOfGoodsSold" to costOfGoods, "grossProfit" to grossProfit, "marginPercentage" to margin))
    } catch (e: Exception) {
        Result.failure(Exception("getProfitMargin failed", e))
    }

    override suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> = try {
        val profitRes = getProfitMargin(startDate, endDate).getOrNull()
        val grossProfit = (profitRes?.get("grossProfit") as? Double) ?: 0.0
        val expenses = pengeluaranRepository.getExpensesInDateRange(startDate, endDate)
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses
        Result.success(mapOf("grossProfit" to grossProfit, "totalExpenses" to totalExpenses, "netProfit" to netProfit))
    } catch (e: Exception) {
        Result.failure(Exception("getNetProfit failed", e))
    }

    override suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<Map<String, Any>>> = try {
        val itemsByProduct = mutableMapOf<Long, Pair<Int, Double>>()
        val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
        // Exclude refunded sales
        val salesFiltered = sales.filter { !it.isRefunded }
        salesFiltered.forEach { sale ->
            val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
            items.forEach { item ->
                val current = itemsByProduct[item.productId] ?: (0 to 0.0)
                itemsByProduct[item.productId] = (current.first + item.quantity) to (current.second + item.totalPrice)
            }
        }
        val result = itemsByProduct.map { (pid, pair) ->
            val prod = produkRepository.getProduk(pid)
            mapOf("productId" to pid, "productName" to (prod?.name ?: "-"), "quantity" to pair.first, "revenue" to pair.second)
        }
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(Exception("getSalesByProduct failed", e))
    }

    override suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<Map<String, Any>>> = try {
        val byCategory = mutableMapOf<Long, Triple<Int, Double, Double>>()
        val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
        // Exclude refunded sales
        val salesFiltered = sales.filter { !it.isRefunded }
        salesFiltered.forEach { sale ->
            val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
            items.forEach { item ->
                val prod = produkRepository.getProduk(item.productId)
                val catId = prod?.categoryId ?: -1L
                val cur = byCategory[catId] ?: Triple(0, 0.0, 0.0)
                byCategory[catId] = Triple(cur.first + item.quantity, cur.second + item.totalPrice, cur.third + (prod?.costPrice ?: 0.0) * item.quantity)
            }
        }
        val result = byCategory.map { (catId, t) -> mapOf("categoryId" to catId, "categoryName" to "Kategori $catId", "quantitySold" to t.first, "totalRevenue" to t.second, "totalCost" to t.third, "profit" to (t.second - t.third)) }
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(Exception("getSalesByCategory failed", e))
    }

    override suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<Map<String, Any>>> = try {
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
        val trend = mutableListOf<Map<String, Any>>()
        for (i in 0..days) {
            val day = startDate.plusDays(i.toLong())
            val sales = penjualanRepository.getSalesInDateRange(day, day)
            // Exclude refunded sales
            val salesFiltered = sales.filter { !it.isRefunded }
            val total = salesFiltered.sumOf { it.totalAmount }
            val tx = salesFiltered.size
            // Only include days that have transactions
            if (tx > 0) {
                trend.add(mapOf("date" to day, "sales" to total, "transactions" to tx))
            }
        }
        Result.success(trend)
    } catch (e: Exception) {
        Result.failure(Exception("getSalesTrend failed", e))
    }

    override suspend fun getIncomeStatement(date: LocalDate): Result<Map<String, Any>> = try {
        val start = date.withDayOfMonth(1)
        val end = date
        val sales = penjualanRepository.getSalesInDateRange(start, end)
        // Exclude refunded sales
        val salesFiltered = sales.filter { !it.isRefunded }
        val revenue = salesFiltered.sumOf { it.totalAmount }
        var cogs = 0.0
        salesFiltered.forEach { sale ->
            val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
            items.forEach { item ->
                val prod = produkRepository.getProduk(item.productId)
                cogs += (prod?.costPrice ?: 0.0) * item.quantity
            }
        }
        val expenses = pengeluaranRepository.getExpensesInDateRange(start, end)
        val operatingExpenses = expenses.sumOf { it.amount }
        val net = (revenue - cogs) - operatingExpenses
        val grossProfit = revenue - cogs
        Result.success(mapOf("revenue" to revenue, "costOfGoodsSold" to cogs, "grossProfit" to grossProfit, "operatingExpenses" to operatingExpenses, "netIncome" to net))
    } catch (e: Exception) {
        Result.failure(Exception("getIncomeStatement failed", e))
    }

    override suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<CashFlow> = try {
        val summary = cashManagementService.getCashFlowSummary(startDate, endDate).getOrNull()
        val cf = CashFlow(
            operatingCashFlow = summary?.operatingCashFlow ?: 0.0,
            investingCashFlow = summary?.investingCashFlow ?: 0.0,
            financingCashFlow = summary?.financingCashFlow ?: 0.0,
            netCashFlow = summary?.netCashFlow ?: 0.0,
            beginningCash = 0.0,
            endingCash = summary?.netCashFlow ?: 0.0,
            period = "$startDate - $endDate"
        )
        Result.success(cf)
    } catch (e: Exception) {
        Result.failure(Exception("getCashFlow failed", e))
    }

    override suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> = try {
        val expenses = pengeluaranRepository.getExpensesInDateRange(startDate, endDate)
        val total = expenses.sumOf { it.amount }
        val byCategory = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        Result.success(mapOf("totalExpenses" to total, "expensesByCategory" to byCategory))
    } catch (e: Exception) {
        Result.failure(Exception("getExpenseReport failed", e))
    }

    override suspend fun getBalanceSheet(asOfDate: LocalDate): Result<Map<String, Any>> = try {
        val bs = balanceSheetService.generateBalanceSheet(asOfDate).getOrNull()
        Result.success(mapOf("assets" to (bs?.assets ?: 0.0), "liabilities" to (bs?.liabilities ?: 0.0), "equity" to (bs?.equity ?: 0.0), "inventoryValue" to (bs?.inventoryValue ?: 0.0)))
    } catch (e: Exception) {
        Result.failure(Exception("getBalanceSheet failed", e))
    }
}
