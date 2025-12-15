package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.error.ChibyChibyException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension function to convert Date to LocalDate
 */
fun Date.toLocalDate(): LocalDate {
    return this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

/**
 * Data classes for report results
 */
data class GrossSalesReport(
    val totalSales: Double,
    val totalTransactions: Int,
    val averageTransaction: Double,
    val period: String
)

data class ProfitMarginReport(
    val totalRevenue: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val profitMargin: Double,
    val period: String
)

data class NetProfitReport(
    val grossProfit: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val profitMargin: Double,
    val period: String
)

data class ProductSales(
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalCost: Double,
    val profit: Double
)

data class CategorySales(
    val categoryId: Long,
    val categoryName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalCost: Double,
    val profit: Double
)

data class TrendData(
    val date: LocalDate,
    val sales: Double,
    val transactions: Int
)

data class IncomeStatement(
    val revenue: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val operatingExpenses: Double,
    val netIncome: Double,
    val period: String
)

data class CashFlow(
    val operatingCashFlow: Double,
    val investingCashFlow: Double,
    val financingCashFlow: Double,
    val netCashFlow: Double,
    val beginningCash: Double,
    val endingCash: Double,
    val period: String
)

data class ExpenseReport(
    val totalExpenses: Double,
    val expensesByCategory: Map<ExpenseCategory, Double>,
    val period: String
)

data class BalanceSheet(
    val assets: Double,
    val liabilities: Double,
    val equity: Double,
    val inventoryValue: Double,
    val cashBalance: Double,
    val asOfDate: LocalDate
)

data class TaxReport(
    val taxableIncome: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val period: String
)

/**
 * Reporting Service
 * Menyediakan berbagai laporan bisnis untuk analisis
 */
@Singleton
class ReportingService @Inject constructor(
    private val saleRepository: com.chibychibystore.repository.PenjualanRepository,
    private val purchaseRepository: com.chibychibystore.repository.PembelianRepository,
    private val expenseRepository: com.chibychibystore.repository.PengeluaranRepository,
    private val productRepository: com.chibychibystore.repository.ProdukRepository,
    private val balanceSheetService: BalanceSheetService,
    private val cashManagementService: CashManagementService
) {

    suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<GrossSalesReport> {
        return try {
            // Get all sales in date range
            val sales = saleRepository.getSalesInDateRange(startDate, endDate)

            val totalSales = sales.sumOf { it.totalAmount }
            val totalTransactions = sales.size
            val averageTransaction = if (totalTransactions > 0) totalSales / totalTransactions else 0.0

            val period = "${startDate.toString()} - ${endDate.toString()}"

            Result.success(GrossSalesReport(
                totalSales = totalSales,
                totalTransactions = totalTransactions,
                averageTransaction = averageTransaction,
                period = period
            ))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.BusinessLogicError("Gagal menghitung penjualan kotor: ${e.message}"))
        }
    }

    suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<ProfitMarginReport> {
        return try {
            // Get sales and purchases in date range
            val sales = saleRepository.getSalesInDateRange(startDate, endDate)
            val purchases = purchaseRepository.getPurchasesInDateRange(startDate, endDate)

            val totalRevenue = sales.sumOf { it.totalAmount }
            val totalCost = purchases.sumOf { it.totalAmount }
            val grossProfit = totalRevenue - totalCost
            val profitMargin = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100 else 0.0

            val period = "${startDate.toString()} - ${endDate.toString()}"

            Result.success(ProfitMarginReport(
                totalRevenue = totalRevenue,
                totalCost = totalCost,
                grossProfit = grossProfit,
                profitMargin = profitMargin,
                period = period
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghitung margin keuntungan: ${e.message}")))
        }
    }

    suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<NetProfitReport> {
        return try {
            // Get profit margin and expenses
            val profitMarginResult = getProfitMargin(startDate, endDate)
            val expenses = expenseRepository.getExpensesInDateRange(startDate, endDate)

            if (profitMarginResult is Result.Success) {
                val profitMargin = profitMarginResult.data
                val totalExpenses = expenses.sumOf { it.amount }
                val netProfit = profitMargin.grossProfit - totalExpenses
                val profitMarginPercent = if (profitMargin.totalRevenue > 0) (netProfit / profitMargin.totalRevenue) * 100 else 0.0

                Result.success(NetProfitReport(
                    grossProfit = profitMargin.grossProfit,
                    totalExpenses = totalExpenses,
                    netProfit = netProfit,
                    profitMargin = profitMarginPercent,
                    period = profitMargin.period
                ))
            } else {
                Result.failure(Exception("Gagal menghitung keuntungan bersih")))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menghitung keuntungan bersih: ${e.message}")))
        }
    }

    suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<ProductSales>> {
        return try {
            // This would require joining sale items with products
            // For now, return empty list - will be implemented with proper DAO queries
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mendapatkan penjualan per produk: ${e.message}")))
        }
    }

    suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<CategorySales>> {
        return try {
            // This would require joining through products to categories
            // For now, return empty list - will be implemented with proper DAO queries
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mendapatkan penjualan per kategori: ${e.message}")))
        }
    }

    suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<TrendData>> {
        return try {
            // Group sales by date
            val sales = saleRepository.getSalesInDateRange(startDate, endDate)
            val salesByDate = sales.groupBy { it.saleDate.toLocalDate() }

            val trendData = salesByDate.map { (date, daySales) ->
                TrendData(
                    date = date,
                    sales = daySales.sumOf { it.totalAmount },
                    transactions = daySales.size
                )
            }.sortedBy { it.date }

            Result.success(trendData)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mendapatkan trend penjualan: ${e.message}")))
        }
    }

    suspend fun getIncomeStatement(startDate: LocalDate, endDate: LocalDate): Result<IncomeStatement> {
        return try {
            val profitResult = getNetProfit(startDate, endDate)

            if (profitResult is Result.Success) {
                val netProfit = profitResult.data
                val expenses = expenseRepository.getExpensesInDateRange(startDate, endDate)
                val operatingExpenses = expenses.sumOf { it.amount }

                Result.success(IncomeStatement(
                    revenue = netProfit.grossProfit + netProfit.totalExpenses, // Reverse calculation
                    costOfGoodsSold = netProfit.grossProfit - netProfit.netProfit, // This is approximate
                    grossProfit = netProfit.grossProfit,
                    operatingExpenses = operatingExpenses,
                    netIncome = netProfit.netProfit,
                    period = netProfit.period
                ))
            } else {
                Result.failure(Exception("Gagal membuat laporan laba rugi")))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membuat laporan laba rugi: ${e.message}")))
        }
    }

    suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<CashFlow> {
        return try {
            val cashFlowSummary = cashManagementService.getCashFlowSummary(startDate, endDate)
                .getOrNull()

            if (cashFlowSummary != null) {
                // For now, beginning and ending cash are placeholders
                // In real implementation, these would be tracked from cash register
                val beginningCash = 0.0
                val endingCash = beginningCash + cashFlowSummary.netCashFlow

                Result.success(CashFlow(
                    operatingCashFlow = cashFlowSummary.operatingCashFlow,
                    investingCashFlow = cashFlowSummary.investingCashFlow,
                    financingCashFlow = cashFlowSummary.financingCashFlow,
                    netCashFlow = cashFlowSummary.netCashFlow,
                    beginningCash = beginningCash,
                    endingCash = endingCash,
                    period = cashFlowSummary.period
                ))
            } else {
                Result.failure(Exception("Gagal mendapatkan cash flow summary")))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membuat laporan arus kas: ${e.message}")))
        }
    }

    suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<ExpenseReport> {
        return try {
            val expenses = expenseRepository.getExpensesInDateRange(startDate, endDate)
            val totalExpenses = expenses.sumOf { it.amount }
            val expensesByCategory = expenses.groupBy { it.category }.mapValues { it.value.sumOf { expense -> expense.amount } }

            val period = "${startDate.toString()} - ${endDate.toString()}"

            Result.success(ExpenseReport(
                totalExpenses = totalExpenses,
                expensesByCategory = expensesByCategory,
                period = period
            ))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membuat laporan pengeluaran: ${e.message}")))
        }
    }

    suspend fun getBalanceSheet(asOfDate: LocalDate): Result<BalanceSheet> {
        return balanceSheetService.generateBalanceSheet(asOfDate)
    }

    suspend fun getTaxReport(startDate: LocalDate, endDate: LocalDate): Result<TaxReport> {
        return try {
            val netProfitResult = getNetProfit(startDate, endDate)

            if (netProfitResult is Result.Success) {
                val netProfit = netProfitResult.data
                val taxRate = 0.11 // 11% corporate tax rate in Indonesia
                val taxAmount = netProfit.netProfit * taxRate

                Result.success(TaxReport(
                    taxableIncome = netProfit.netProfit,
                    taxRate = taxRate,
                    taxAmount = taxAmount,
                    period = netProfit.period
                ))
            } else {
                Result.failure(Exception("Gagal membuat laporan pajak")))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal membuat laporan pajak: ${e.message}")))
        }
    }
}