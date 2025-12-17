package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.flow.first
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

    override suspend fun getDailySalesReport(date: LocalDate): Result<DailySalesReport> {
        return try {
            val sales = penjualanRepository.getSalesInDateRange(date, date)
            val totalSales = sales.sumOf { it.totalAmount }
            val totalTransactions = sales.size
            val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
            val topProducts = emptyList<ProductSalesData>()
            Result.success(DailySalesReport(date, totalSales, totalTransactions, avg, topProducts))
        } catch (e: Exception) {
            Result.failure(Exception("getDailySalesReport failed", e))
        }
    }

    override suspend fun getMonthlySalesReport(year: Int, month: Int): Result<MonthlySalesReport> {
        return try {
            Result.success(MonthlySalesReport(year, month, 0.0, 0, emptyList(), emptyList()))
        } catch (e: Exception) {
            Result.failure(Exception("getMonthlySalesReport failed", e))
        }
    }

    override suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<FinancialReport> {
        return try {
            val totalRevenue = penjualanRepository.getTotalPenjualanByDateRange(startDate, endDate).getOrNull() ?: 0.0
            val cashFlow = cashManagementService.getCashFlowSummary(startDate, endDate).getOrNull()
            val totalExpenses = pengeluaranRepository.getTotalExpenseAmount(startDate, endDate).getOrNull() ?: 0.0
            val totalCost = 0.0
            val grossProfit = totalRevenue - totalCost
            val netProfit = grossProfit - totalExpenses
            val profitMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
            Result.success(FinancialReport(startDate, endDate, totalRevenue, totalCost, grossProfit, totalExpenses, netProfit, profitMargin))
        } catch (e: Exception) {
            Result.failure(Exception("getFinancialReport failed", e))
        }
    }

    override suspend fun getInventoryReport(): Result<InventoryReport> {
        return try {
            Result.success(InventoryReport(0, 0.0, 0, 0, emptyList()))
        } catch (e: Exception) {
            Result.failure(Exception("getInventoryReport failed", e))
        }
    }

    override suspend fun getTopSellingProducts(limit: Int): Result<List<ProductSalesData>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(Exception("getTopSellingProducts failed", e))
        }
    }

    override suspend fun getLowStockReport(): Result<List<LowStockProduct>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(Exception("getLowStockReport failed", e))
        }
    }

    override fun observeSalesMetrics(): kotlinx.coroutines.flow.Flow<SalesMetrics> {
        return kotlinx.coroutines.flow.flowOf(SalesMetrics(0.0, 0, 0.0, 0.0))
    }

    // ----- Additional helper methods expected by UI/tests -----
    override suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> {
        return try {
            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            val totalSales = sales.sumOf { it.totalAmount }
            val totalTransactions = sales.size
            val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
            val map = mapOf("totalSales" to totalSales, "totalTransactions" to totalTransactions, "averageTransaction" to avg)
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(Exception("getGrossSales failed", e))
        }
    }

    override suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> {
        return try {
            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            val revenue = sales.sumOf { it.totalAmount }

            // compute COGS by summing items' quantity * product.hargaBeli
            var costOfGoods = 0.0
            sales.forEach { sale ->
                val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
                items.forEach { item ->
                    val product = produkRepository.getProduk(item.productId)
                    val cost = (product?.costPrice ?: 0.0) * item.quantity
                    costOfGoods += cost
                }
            }

            val grossProfit = revenue - costOfGoods
            val margin = if (revenue > 0) (grossProfit / revenue) * 100 else 0.0
            val map = mapOf("revenue" to revenue, "costOfGoodsSold" to costOfGoods, "grossProfit" to grossProfit, "marginPercentage" to margin)
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(Exception("getProfitMargin failed", e))
        }
    }

    override suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> {
        return try {
            val profitRes = getProfitMargin(startDate, endDate).getOrNull()
            val grossProfit = (profitRes?.get("grossProfit") as? Double) ?: 0.0
            val expenses = pengeluaranRepository.getExpensesInDateRange(startDate, endDate)
            val totalExpenses = expenses.sumOf { it.amount }
            val netProfit = grossProfit - totalExpenses
            val map = mapOf("grossProfit" to grossProfit, "totalExpenses" to totalExpenses, "netProfit" to netProfit)
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(Exception("getNetProfit failed", e))
        }
    }

    override suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<Map<String, Any>>> {
        return try {
            val itemsByProduct = mutableMapOf<Long, Pair<Int, Double>>() // productId -> (qty, revenue)
            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            sales.forEach { sale ->
                val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
                items.forEach { item ->
                    val current = itemsByProduct[item.productId] ?: (0 to 0.0)
                    val newQty = current.first + item.quantity
                    val newRev = current.second + item.totalPrice
                    itemsByProduct[item.productId] = newQty to newRev
                }
            }

            val result = itemsByProduct.map { (productId, pair) ->
                val prod = produkRepository.getProduk(productId)
                mapOf("productId" to productId, "productName" to (prod?.name ?: "-"), "quantity" to pair.first, "revenue" to pair.second)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(Exception("getSalesByProduct failed", e))
        }
    }

    override suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<Map<String, Any>>> {
        return try {
            val byCategory = mutableMapOf<Long, Triple<Int, Double, Double>>() // categoryId -> (qty, revenue, cost)
            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            sales.forEach { sale ->
                val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
                items.forEach { item ->
                    val prod = produkRepository.getProduk(item.productId)
                    val catId = prod?.categoryId ?: -1L
                    val current = byCategory[catId] ?: Triple(0, 0.0, 0.0)
                    val newQty = current.first + item.quantity
                    val newRev = current.second + item.totalPrice
                    val newCost = current.third + (prod?.costPrice ?: 0.0) * item.quantity
                    byCategory[catId] = Triple(newQty, newRev, newCost)
                }
            }

            val result = byCategory.map { (catId, triple) ->
                val name = "Kategori $catId"
                mapOf("categoryId" to catId, "categoryName" to name, "quantitySold" to triple.first, "totalRevenue" to triple.second, "totalCost" to triple.third, "profit" to (triple.second - triple.third))
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(Exception("getSalesByCategory failed", e))
        }
    }

    override suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<Map<String, Any>>> {
        return try {
            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
            val trend = mutableListOf<Map<String, Any>>()
            for (i in 0..days) {
                val day = startDate.plusDays(i.toLong())
                val sales = penjualanRepository.getSalesInDateRange(day, day)
                val total = sales.sumOf { it.totalAmount }
                val tx = sales.size
                trend.add(mapOf("date" to day, "sales" to total, "transactions" to tx))
            }
            Result.success(trend)
        } catch (e: Exception) {
            Result.failure(Exception("getSalesTrend failed", e))
        }
    }

    override suspend fun getIncomeStatement(date: LocalDate): Result<Map<String, Any>> {
        return try {
            val start = date.withDayOfMonth(1)
            val end = date
            val sales = penjualanRepository.getSalesInDateRange(start, end)
            val revenue = sales.sumOf { it.totalAmount }
            var costOfGoods = 0.0
            sales.forEach { sale ->
                val items = itemPenjualanRepository.getItemsBySaleId(sale.id).first()
                items.forEach { item ->
                    val prod = produkRepository.getProduk(item.productId)
                    costOfGoods += (prod?.costPrice ?: 0.0) * item.quantity
                }
            }
            val grossProfit = revenue - costOfGoods
            val expenses = pengeluaranRepository.getExpensesInDateRange(start, end)
            val operatingExpenses = expenses.sumOf { it.amount }
            val netIncome = grossProfit - operatingExpenses
            val map = mapOf("revenue" to revenue, "costOfGoodsSold" to costOfGoods, "grossProfit" to grossProfit, "operatingExpenses" to operatingExpenses, "netIncome" to netIncome)
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(Exception("getIncomeStatement failed", e))
        }
    }

    override suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<CashFlow> {
        return try {
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
    }

    override suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<Map<String, Any>> {
        return try {
            val expenses = pengeluaranRepository.getExpensesInDateRange(startDate, endDate)
            val total = expenses.sumOf { it.amount }
            val byCategory = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
            val map = mapOf("totalExpenses" to total, "expensesByCategory" to byCategory)
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(Exception("getExpenseReport failed", e))
        }
    }

    override suspend fun getBalanceSheet(asOfDate: LocalDate): Result<Map<String, Any>> {
        return try {
            val bs = balanceSheetService.generateBalanceSheet(asOfDate).getOrNull()
            val map = mapOf("assets" to (bs?.assets ?: 0.0), "liabilities" to (bs?.liabilities ?: 0.0), "equity" to (bs?.equity ?: 0.0), "inventoryValue" to (bs?.inventoryValue ?: 0.0))
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(Exception("getBalanceSheet failed", e))
        }
    }
}
