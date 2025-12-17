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
}

/**
 * Data classes untuk reporting
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
