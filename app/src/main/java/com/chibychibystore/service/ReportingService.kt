package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface untuk Reporting Service
 * 
 * Service ini menangani semua operasi pelaporan bisnis:
 * - Laporan penjualan harian/bulanan
 * - Laporan keuangan
 * - Laporan inventory
 * - Analisis performa
 */
interface ReportingService {
    
    /**
     * Mendapatkan laporan penjualan harian
     */
    suspend fun getDailySalesReport(date: LocalDate): Result<DailySalesReport>
    
    /**
     * Mendapatkan laporan penjualan bulanan
     */
    suspend fun getMonthlySalesReport(year: Int, month: Int): Result<MonthlySalesReport>
    
    /**
     * Mendapatkan laporan keuangan
     */
    suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<FinancialReport>
    
    /**
     * Mendapatkan laporan inventory
     */
    suspend fun getInventoryReport(): Result<InventoryReport>
    
    /**
     * Mendapatkan top selling products
     */
    suspend fun getTopSellingProducts(limit: Int = 10): Result<List<ProductSalesData>>
    
    /**
     * Mendapatkan low stock products
     */
    suspend fun getLowStockReport(): Result<List<LowStockProduct>>
    
    /**
     * Observable untuk real-time sales metrics
     */
    fun observeSalesMetrics(): Flow<SalesMetrics>

    // Additional helper reporting methods used by UI and PDF export
    suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<GrossSalesReport>
    suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<ProfitMarginReport>
    suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<NetProfitReport>
    suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<ProductSales>>
    suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<CategorySales>>
    suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<TrendData>>
    suspend fun getIncomeStatement(date: LocalDate): Result<IncomeStatement>
    suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<CashFlow>
    suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<ExpenseReport>
    suspend fun getBalanceSheet(asOfDate: LocalDate): Result<BalanceSheet>
}

// DTOs for Reporting

data class GrossSalesReport(
    val totalSales: Double,
    val totalTransactions: Int,
    val averageTransaction: Double
)

data class ProfitMarginReport(
    val totalRevenue: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val profitMargin: Double
)

data class NetProfitReport(
    val grossProfit: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val profitMargin: Double
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
    val netIncome: Double
)

data class ExpenseReport(
    val totalExpenses: Double,
    val expensesByCategory: Map<Any, Double> // Using Any for category enum or string
)

data class BalanceSheet(
    val assets: Double,
    val liabilities: Double,
    val equity: Double,
    val inventoryValue: Double
)

// Simple DTO used by reporting for cash flow export
data class CashFlow(
    val operatingCashFlow: Double,
    val investingCashFlow: Double,
    val financingCashFlow: Double,
    val netCashFlow: Double,
    val beginningCash: Double = 0.0,
    val endingCash: Double = 0.0,
    val period: String? = null
)

/**
 * Data classes untuk basic reporting
 */
data class DailySalesReport(
    val date: LocalDate,
    val totalSales: Double,
    val totalTransactions: Int,
    val averageTransaction: Double,
    val topProducts: List<ProductSalesData>
)

data class MonthlySalesReport(
    val year: Int,
    val month: Int,
    val totalSales: Double,
    val totalTransactions: Int,
    val dailyBreakdown: List<DailySalesData>,
    val topProducts: List<ProductSalesData>
)

data class FinancialReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRevenue: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val profitMargin: Double
)

data class InventoryReport(
    val totalProducts: Int,
    val totalValue: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val categoryBreakdown: List<CategoryInventoryData>
)

data class ProductSalesData(
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double
)

data class LowStockProduct(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val minStock: Int,
    val reorderQuantity: Int
)

data class DailySalesData(
    val date: LocalDate,
    val totalSales: Double,
    val transactionCount: Int
)

data class CategoryInventoryData(
    val categoryId: Long,
    val categoryName: String,
    val productCount: Int,
    val totalValue: Double
)

data class SalesMetrics(
    val todaySales: Double,
    val todayTransactions: Int,
    val monthToDateSales: Double,
    val averageTransactionValue: Double
)
