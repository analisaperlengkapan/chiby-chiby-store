package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.FinancialReport
import com.chibychibystore.service.GrossSalesReport
import com.chibychibystore.service.InventoryReport
import com.chibychibystore.service.ReportingService
import com.chibychibystore.service.DailySalesReport
import com.chibychibystore.service.MonthlySalesReport
import com.chibychibystore.service.ProductSalesData
import com.chibychibystore.service.LowStockProduct
import com.chibychibystore.service.ProfitMarginReport
import com.chibychibystore.service.NetProfitReport
import com.chibychibystore.service.ProductSales
import com.chibychibystore.service.CategorySales
import com.chibychibystore.service.TrendData
import com.chibychibystore.service.IncomeStatement
import com.chibychibystore.service.CashFlow
import com.chibychibystore.service.ExpenseReport
import com.chibychibystore.service.BalanceSheet
import com.chibychibystore.service.SalesMetrics
import com.chibychibystore.service.CashManagementService
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.repository.SaleItemRepository
import com.chibychibystore.repository.SaleRepository
import com.chibychibystore.repository.PurchaseRepository
import com.chibychibystore.repository.ExpenseRepository
import com.chibychibystore.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportingServiceImpl @Inject constructor(
    private val saleRepository: SaleRepository,
    private val saleItemRepository: SaleItemRepository,
    private val productRepository: ProductRepository,
    private val expenseRepository: ExpenseRepository,
    private val purchaseRepository: PurchaseRepository,
    private val balanceSheetService: BalanceSheetService,
    private val cashManagementService: CashManagementService,
    private val authService: AuthService
) : ReportingService {

    private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

    // Basic reports
    override suspend fun getDailySalesReport(date: LocalDate): Result<DailySalesReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan"))
        } else {
            val dDate = date.toDate()
            // Optimized to use DB aggregation for totalAmount (Cash Receipts)
            val totalSales = saleRepository.getTotalCashReceipts(date, date).getOrNull() ?: 0.0
            val totalTransactions = saleRepository.getSaleCountByDateRange(date, date).getOrNull() ?: 0
            val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
            Result.success(DailySalesReport(date, totalSales, totalTransactions, avg, emptyList()))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getDailySalesReport failed", e))
    }

    override suspend fun getMonthlySalesReport(year: Int, month: Int): Result<MonthlySalesReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan"))
        } else {
            Result.success(MonthlySalesReport(year, month, 0.0, 0, emptyList(), emptyList()))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getMonthlySalesReport failed", e))
    }

    override suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<FinancialReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan keuangan"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // Use Revenue (Net Sales) for financial reporting
            val totalRevenue = saleRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0
            val totalExpenses = expenseRepository.getTotalExpense(dStart, dEnd).getOrNull() ?: 0.0

            // Cost of Goods Sold
            val purchases = purchaseRepository.getPurchasesByDateRange(dStart, dEnd).first()
            val totalCost = purchases.sumOf { it.totalAmount }

            val grossProfit = totalRevenue - totalCost
            val netProfit = grossProfit - totalExpenses
            val profitMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
            Result.success(FinancialReport(startDate, endDate, totalRevenue, totalCost, grossProfit, totalExpenses, netProfit, profitMargin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getFinancialReport failed", e))
    }

    override suspend fun getInventoryReport(): Result<InventoryReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan inventory"))
        } else {
            val totalProducts = productRepository.getProductCount().getOrNull() ?: 0
            val totalValue = productRepository.getTotalInventoryValue().getOrNull() ?: 0.0

            val lowStockCount = productRepository.countLowStock().getOrNull() ?: 0
            val outOfStockCount = productRepository.countOutOfStock().getOrNull() ?: 0

            Result.success(InventoryReport(
                totalProducts = totalProducts,
                totalValue = totalValue,
                lowStockCount = lowStockCount,
                outOfStockCount = outOfStockCount,
                categoryBreakdown = emptyList() // Needs complex aggregation
            ))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getInventoryReport failed", e))
    }

    override suspend fun getTopSellingProducts(limit: Int): Result<List<ProductSalesData>> = try {
        if (!authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat data product terlaris"))
        } else {
            val topProductsDto = saleItemRepository.getTopSellingProducts(limit).getOrNull() ?: emptyList()

            // Hydrate with product names (N+1 problem if naive, but batch fetch is better)
            val productIds = topProductsDto.map { it.productId }
            val productsMap = productRepository.getProductByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            val result = topProductsDto.map { dto ->
                ProductSalesData(
                    productId = dto.productId,
                    productName = productsMap[dto.productId]?.name ?: "Unknown Product",
                    quantitySold = dto.quantitySold.toInt(),
                    totalRevenue = dto.totalRevenue
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getTopSellingProducts failed", e))
    }

    override suspend fun getLowStockReport(): Result<List<LowStockProduct>> = try {
        if (!authService.hasPermission(Permissions.VIEW_INVENTORY_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan stok rendah"))
        } else {
            val lowStockProducts = productRepository.getLowStockProducts().first()
            val result = lowStockProducts.map {
                LowStockProduct(
                    productId = it.id,
                    productName = it.name,
                    currentStock = it.stockQuantity,
                    minStock = it.minStock,
                    reorderQuantity = it.minStock * 2 // Simple rule
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getLowStockReport failed", e))
    }

    override fun observeSalesMetrics() = flowOf(SalesMetrics(0.0, 0, 0.0, 0.0))

    // --- Helper methods used by UI/PDF/tests ---
    override suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<GrossSalesReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat data penjualan kotor"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // Optimized using DB aggregation
            val totalSales = saleRepository.getTotalCashReceipts(startDate, endDate).getOrNull() ?: 0.0

            // Optimized count using DB query
            val totalTransactions = saleRepository.getSaleCountNonRefunded(startDate, endDate).getOrNull() ?: 0

            val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
            Result.success(GrossSalesReport(totalSales, totalTransactions, avg))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getGrossSales failed", e))
    }

    override suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<ProfitMarginReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat margin keuntungan"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // For profit calculations, use Revenue (Net Sales excluding tax)
            val revenue = saleRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0

            // Cost of goods sold is derived from purchases in the period (pembelian)
            val purchases = purchaseRepository.getPurchasesByDateRange(dStart, dEnd).first()
            val costOfGoods = purchases.sumOf { it.totalAmount }

            val grossProfit = revenue - costOfGoods
            val margin = if (revenue > 0) (grossProfit / revenue) * 100 else 0.0
            Result.success(ProfitMarginReport(revenue, costOfGoods, grossProfit, margin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getProfitMargin failed", e))
    }

    override suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<NetProfitReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laba bersih"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // Calculate Profit Margin first
            val profitRes = getProfitMargin(startDate, endDate).getOrNull()
            val grossProfit = profitRes?.grossProfit ?: 0.0

            // Calculate Expenses
            val expenses = expenseRepository.getExpensesByDateRange(dStart, dEnd).first()
            val totalExpenses = expenses.sumOf { it.amount }

            val netProfit = grossProfit - totalExpenses

            val revenue = profitRes?.totalRevenue ?: 0.0
            val netMargin = if (revenue > 0) (netProfit / revenue) * 100 else 0.0

            Result.success(NetProfitReport(grossProfit, totalExpenses, netProfit, netMargin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getNetProfit failed", e))
    }

    override suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<ProductSales>> = try {
        if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan per product"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // Optimized using single SQL query aggregation
            val statsResult = saleItemRepository.getProductSalesStats(startDate, endDate)
            val salesStats = statsResult.getOrNull() ?: emptyList()

            // Bulk fetch product details to avoid N+1
            val productIds = salesStats.map { it.productId }
            val productsMap = productRepository.getProductByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            val result = salesStats.map { stat ->
                val prod = productsMap[stat.productId]
                val costPrice = prod?.costPrice ?: 0.0
                val totalCost = costPrice * stat.quantitySold
                val profit = stat.totalRevenue - totalCost

                ProductSales(
                    productId = stat.productId,
                    productName = prod?.name ?: "Unknown Product",
                    quantitySold = stat.quantitySold.toInt(),
                    totalRevenue = stat.totalRevenue,
                    totalCost = totalCost,
                    profit = profit
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getSalesByProduct failed", e))
    }

    override suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<CategorySales>> = try {
        if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan per kategori"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            val byCategory = mutableMapOf<Long, Triple<Int, Double, Double>>()
            val sales = saleRepository.getSalesInDateRange(startDate, endDate)
            // Exclude refunded sales
            val salesFiltered = sales.filter { !it.isRefunded }

            salesFiltered.forEach { sale ->
                val items = saleItemRepository.getItemsBySaleId(sale.id).first()
                items.forEach { item ->
                    val prod = productRepository.getProduct(item.productId)
                    val catId = prod?.categoryId ?: -1L
                    val cost = (prod?.costPrice ?: 0.0) * item.quantity
                    val cur = byCategory[catId] ?: Triple(0, 0.0, 0.0)
                    byCategory[catId] = Triple(cur.first + item.quantity, cur.second + item.totalPrice, cur.third + cost)
                }
            }

            val result = byCategory.map { (catId, t) ->
                CategorySales(
                    categoryId = catId,
                    categoryName = "Kategori $catId", // In real app, fetch category name
                    quantitySold = t.first,
                    totalRevenue = t.second,
                    totalCost = t.third,
                    profit = t.second - t.third
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getSalesByCategory failed", e))
    }

    override suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<TrendData>> = try {
        if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat tren penjualan"))
        } else {
            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
            val trend = mutableListOf<TrendData>()
            for (i in 0..days) {
                val day = startDate.plusDays(i.toLong())
                val sales = saleRepository.getSalesInDateRange(day, day)
                // Exclude refunded sales
                val salesFiltered = sales.filter { !it.isRefunded }
                val total = salesFiltered.sumOf { it.totalAmount }
                val tx = salesFiltered.size
                // Use default if no sales, but allow 0 sales for trend continuity if needed
                trend.add(TrendData(date = day, sales = total, transactions = tx))
            }
            Result.success(trend)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getSalesTrend failed", e))
    }

    override suspend fun getIncomeStatement(date: LocalDate): Result<IncomeStatement> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan laba rugi"))
        } else {
            val startOfMonth = date.withDayOfMonth(1)
            val sales = saleRepository.getSalesInDateRange(startOfMonth, date)
            // Exclude refunded sales
            val salesFiltered = sales.filter { !it.isRefunded }
            val revenue = salesFiltered.sumOf { it.totalAmount }
            var cogs = 0.0
            salesFiltered.forEach { sale ->
                val items = saleItemRepository.getItemsBySaleId(sale.id).first()
                items.forEach { item ->
                    val prod = productRepository.getProduct(item.productId)
                    cogs += (prod?.costPrice ?: 0.0) * item.quantity
                }
            }
            val expenses = expenseRepository.getExpensesByDateRange(startOfMonth.toDate(), date.toDate()).first()
            val operatingExpenses = expenses.sumOf { it.amount }
            val grossProfit = revenue - cogs
            val net = grossProfit - operatingExpenses

            Result.success(IncomeStatement(revenue, cogs, grossProfit, operatingExpenses, net))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getIncomeStatement failed", e))
    }

    override suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<CashFlow> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan arus kas"))
        } else {
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
        }
    } catch (e: Exception) {
        Result.failure(Exception("getCashFlow failed", e))
    }

    override suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<ExpenseReport> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan pengeluaran"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()
            val expenses = expenseRepository.getExpensesByDateRange(dStart, dEnd).first()
            val total = expenses.sumOf { it.amount }
            val byCategory = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
            Result.success(ExpenseReport(total, byCategory.mapKeys { it.key as Any }))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getExpenseReport failed", e))
    }

    override suspend fun getBalanceSheet(asOfDate: LocalDate): Result<BalanceSheet> = try {
        if (!authService.hasPermission(Permissions.VIEW_FINANCIAL_REPORTS)) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan neraca"))
        } else {
            val bs = balanceSheetService.generateBalanceSheet(asOfDate).getOrNull()
            Result.success(BalanceSheet(
                assets = bs?.assets ?: 0.0,
                liabilities = bs?.liabilities ?: 0.0,
                equity = bs?.equity ?: 0.0,
                inventoryValue = bs?.inventoryValue ?: 0.0
            ))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getBalanceSheet failed", e))
    }
}
