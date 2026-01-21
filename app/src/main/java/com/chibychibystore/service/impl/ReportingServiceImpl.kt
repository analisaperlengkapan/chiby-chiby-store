package com.chibychibystore.service.impl

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
    private val saleRepository: PenjualanRepository,
    private val saleItemRepository: ItemPenjualanRepository,
    private val productRepository: ProdukRepository,
    private val expenseRepository: PengeluaranRepository,
    private val purchaseRepository: PembelianRepository,
    private val balanceSheetService: BalanceSheetService,
    private val cashManagementService: CashManagementService,
    private val authService: AuthService
) : ReportingService {

    private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

    override suspend fun getDailySalesReport(date: LocalDate): Result<LaporanPenjualanHarian> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
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

    override suspend fun getMonthlySalesReport(year: Int, month: Int): Result<LaporanPenjualanBulanan> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan"))
        } else {
            Result.success(LaporanPenjualanBulanan(year, month, 0.0, 0, emptyList(), emptyList()))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getMonthlySalesReport failed", e))
    }

    override suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<LaporanKeuangan> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan keuangan"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // Use Revenue (Net Sales) for financial reporting
            val totalRevenue = saleRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0
            val totalExpenses = expenseRepository.getTotalExpense(dStart, dEnd).getOrNull() ?: 0.0

            val purchases = purchaseRepository.getPurchasesInDateRange(startDate, endDate)
            val totalCost = purchases.sumOf { it.totalAmount }

            val grossProfit = totalRevenue - totalCost
            val netProfit = grossProfit - totalPengeluaran
            val profitMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
            Result.success(LaporanKeuangan(startDate, endDate, totalRevenue, totalCost, grossProfit, totalPengeluaran, netProfit, profitMargin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getFinancialReport failed", e))
    }

    override suspend fun getInventoryReport(): Result<LaporanStok> = try {
        if (!authService.hasPermission("VIEW_INVENTORY_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan inventory"))
        } else {
            val totalProducts = productRepository.getProdukCount().getOrNull() ?: 0
            val totalValue = productRepository.getTotalInventoryValue().getOrNull() ?: 0.0

            val lowStockCount = productRepository.countLowStock().getOrNull() ?: 0
            val outOfStockCount = productRepository.countOutOfStock().getOrNull() ?: 0

            Result.success(LaporanStok(
                totalProduk = totalProducts,
                totalNilai = totalValue,
                stokRendahCount = lowStockCount,
                stokHabisCount = outOfStockCount,
                rincianKategori = emptyList()
            ))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getInventoryReport failed", e))
    }

    override suspend fun getTopSellingProducts(limit: Int): Result<List<DataPenjualanProduk>> = try {
        if (!authService.hasPermission("VIEW_INVENTORY_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat data product terlaris"))
        } else {
            val topProductsDto = saleItemRepository.getTopSellingProduks(limit).getOrNull() ?: emptyList()

            val productIds = topProductsDto.map { it.produkId }
            val productsMap: Map<Long, com.chibychibystore.data.local.entity.Produk> = productRepository.getProductsByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

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

    override suspend fun getLowStockReport(): Result<List<ProdukStokRendah>> = try {
        if (!authService.hasPermission("VIEW_INVENTORY_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan stok rendah"))
        } else {
            val lowStockProduks = productRepository.getLowStockProduk().first()
            val result = lowStockProduks.map {
                ProdukStokRendah(
                    produkId = it.id,
                    namaProduk = it.name,
                    stokSaatIni = it.stockQuantity,
                    stokMinimal = it.minStock,
                    jumlahPesanKembali = it.minStock * 2
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getLowStockReport failed", e))
    }

    override fun observeSalesMetrics() = flowOf(MetrikPenjualan(0.0, 0, 0.0, 0.0))

    override suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<LaporanPenjualanKotor> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat data penjualan kotor"))
        } else {
            val totalPenjualan = saleRepository.getTotalCashReceipts(startDate, endDate).getOrNull() ?: 0.0
            val totalTransactions = saleRepository.getPenjualanCountNonRefunded(startDate, endDate).getOrNull() ?: 0

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

    override suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<LaporanMarginLaba> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
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
            Result.success(LaporanMarginLaba(revenue, costOfGoods, grossProfit, margin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getProfitMargin failed", e))
    }

    override suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<LaporanLabaBersih> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laba bersih"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            val profitResRes = getProfitMargin(startDate, endDate)
            if (profitResRes is Result.Failure) throw profitResRes.exception
            val profitRes = (profitResRes as Result.Success).data

            val expenses = expenseRepository.getPengeluaransByDateRange(dStart, dEnd).first()
            val totalPengeluaran = expenses.sumOf { it.amount }

            val netProfit = profitRes.labaKotor - totalPengeluaran
            val revenue = profitRes.totalPendapatan
            val netMargin = if (revenue > 0) (netProfit / revenue) * 100 else 0.0

            Result.success(LaporanLabaBersih(profitRes.labaKotor, totalPengeluaran, netProfit, netMargin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getNetProfit failed", e))
    }

    override suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<PenjualanProduk>> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan per product"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            // Optimized using single SQL query aggregation
            val statsResult = saleItemRepository.getProductSalesStats(startDate, endDate)
            val salesStats = statsResult.getOrNull() ?: emptyList()

            val productIds = salesStats.map { it.produkId }
            val productsMap: Map<Long, com.chibychibystore.data.local.entity.Produk> = productRepository.getProductsByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            val result = salesStats.map { stat ->
                val prod = productsMap[stat.produkId]
                val costPrice = prod?.costPrice ?: 0.0
                val totalCost = costPrice * stat.jumlahTerjual
                val profit = stat.totalPendapatan - totalCost

                PenjualanProduk(
                    produkId = stat.produkId,
                    namaProduk = prod?.name ?: "Unknown Produk",
                    jumlahTerjual = stat.jumlahTerjual.toInt(),
                    totalPendapatan = stat.totalPendapatan,
                    totalBiaya = totalCost,
                    laba = profit
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getSalesByProduct failed", e))
    }

    override suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<PenjualanKategori>> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan per kategori"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()

            val byCategory = mutableMapOf<Long, Triple<Int, Double, Double>>()
            val sales = saleRepository.getSalesInDateRange(startDate, endDate)
            // Exclude refunded sales
            val salesFiltered = sales.filter { !it.isRefunded }

            val result = salesStats.map { stat ->
                PenjualanKategori(
                    kategoriId = stat.kategoriId,
                    namaKategori = stat.namaKategori ?: "Kategori ${stat.kategoriId}",
                    jumlahTerjual = stat.jumlahTerjual.toInt(),
                    totalPendapatan = stat.totalPendapatan,
                    totalBiaya = stat.totalBiaya,
                    laba = stat.totalPendapatan - stat.totalBiaya
                )
            }
            Result.success(result)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getSalesByCategory failed", e))
    }

    override suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<DataTren>> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat tren penjualan"))
        } else {
            val allSales = saleRepository.getSalesInDateRange(startDate, endDate)
            val salesByDate = allSales
                .filter { !it.isRefunded }
                .groupBy {
                    it.saleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }

            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
            val trend = mutableListOf<DataTren>()
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

    override suspend fun getIncomeStatement(date: LocalDate): Result<LaporanLabaRugi> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan laba rugi"))
        } else {
            val startOfMonth = date.withDayOfMonth(1)
            
            val expenses = expenseRepository.getExpensesByDateRange(startOfMonth.toDate(), date.toDate()).first()
            val totalRevenue = saleRepository.getTotalRevenue(startOfMonth, date).getOrNull() ?: 0.0

            // Calculate COGS using optimized query (perf optimization)
            val dStart = startOfMonth.toDate()
            val dEnd = Date.from(date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val cogs = saleItemRepository.calculateTotalCogs(dStart, dEnd).getOrNull() ?: 0.0
            
            // Using logic from feat:
            // val sales = saleRepository.getSalesInDateRange(startOfMonth, date)
            // val salesFiltered = sales.filter { !it.isRefunded }
            // val revenue = salesFiltered.sumOf { it.totalAmount }
            // But perf uses getTotalRevenue (which is Total Cash? No, Revenue = Net Sales).
            // perf used: val revenue = saleRepository.getTotalCashReceipts(start, end).getOrNull() ?: 0.0
            // feat used: val revenue = salesFiltered.sumOf { it.totalAmount }
            // I should use feat's definition of revenue for Income Statement?
            // perf used getTotalCashReceipts. feat used sumOf totalAmount.
            // If I stick to perf optimization for COGS, I should probably also assume I want optimized Revenue?
            // But feat is UI Overhaul, typically defining business logic.
            // I'll stick to feat's revenue calc logic if possible, OR if I use perf's optimized COGS, maybe I should use optimized Revenue too?
            // feat has "Optimized to use DB aggregation for totalAmount" elsewhere.
            // I'll use `saleRepository.getTotalRevenue` which feat introduced/uses in other methods.
            // Wait, feat loop calculates revenue manually.
            // I'll use `getTotalRevenue` for consistency if available, or allow manual loop?
            // `getIncomeStatement` line 403: `val revenue = salesFiltered.sumOf { it.totalAmount }`
            // Replacing loop with `getTotalRevenue`:
            val revenue = saleRepository.getTotalRevenue(startOfMonth, date).getOrNull() ?: 0.0

            val operatingExpenses = expenses.sumOf { it.amount }
            val grossProfit = revenue - cogs
            val net = grossProfit - operatingExpenses

            Result.success(LaporanLabaRugi(revenue, cogs, grossProfit, operatingExpenses, net))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getIncomeStatement failed", e))
    }

    override suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<ArusKas> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan arus kas"))
        } else {
            val summary = cashManagementService.getCashFlowSummary(startDate, endDate).getOrNull()
            val cf = ArusKas(
                arusKasOperasional = summary?.operatingCashFlow ?: 0.0,
                arusKasInvestasi = summary?.investingCashFlow ?: 0.0,
                arusKasPendanaan = summary?.financingCashFlow ?: 0.0,
                arusKasBersih = summary?.netCashFlow ?: 0.0,
                saldoAwal = 0.0,
                saldoAkhir = summary?.netCashFlow ?: 0.0,
                periode = "$startDate - $endDate"
            )
            Result.success(cf)
        }
    } catch (e: Exception) {
        Result.failure(Exception("getCashFlow failed", e))
    }

    override suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<LaporanPengeluaran> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan pengeluaran"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = endDate.toDate()
            val expenses = expenseRepository.getPengeluaransByDateRange(dStart, dEnd).first()
            val total = expenses.sumOf { it.amount }
            val byKategori = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
            Result.success(LaporanPengeluaran(total, byKategori.mapKeys { it.key as Any }))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getExpenseReport failed", e))
    }

    override suspend fun getBalanceSheet(asOfDate: LocalDate): Result<NeracaSaldo> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan neraca"))
        } else {
            val bs = balanceSheetService.generateBalanceSheet(asOfDate).getOrNull()
            Result.success(NeracaSaldo(
                aset = bs?.assets ?: 0.0,
                liabilitas = bs?.liabilities ?: 0.0,
                ekuitas = bs?.equity ?: 0.0,
                nilaiPersediaan = bs?.inventoryValue ?: 0.0
            ))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getBalanceSheet failed", e))
    }
}
