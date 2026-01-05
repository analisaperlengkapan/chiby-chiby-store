package com.chibychibystore.service.impl

import com.chibychibystore.data.model.Result
import com.chibychibystore.service.*
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
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
            val totalPenjualan = saleRepository.getTotalCashReceipts(date, date).getOrNull() ?: 0.0
            val totalTransactions = saleRepository.getPenjualanCountByDateRange(date, date).getOrNull() ?: 0
            val avg = if (totalTransactions > 0) totalPenjualan / totalTransactions else 0.0
            Result.success(LaporanPenjualanHarian(date, totalPenjualan, totalTransactions, avg, emptyList()))
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

            val totalRevenue = saleRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0
            val totalPengeluaran = expenseRepository.getTotalPengeluaranAmount(dStart, dEnd).getOrNull() ?: 0.0

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
            val productsMap = productRepository.getProdukByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            val result = topProductsDto.map { dto ->
                DataPenjualanProduk(
                    produkId = dto.produkId,
                    namaProduk = productsMap[dto.produkId]?.name ?: "Unknown Produk",
                    jumlahTerjual = dto.jumlahTerjual,
                    totalPendapatan = dto.totalPendapatan
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

            val avg = if (totalTransactions > 0) totalPenjualan / totalTransactions else 0.0
            Result.success(LaporanPenjualanKotor(totalPenjualan, totalTransactions, avg))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getGrossSales failed", e))
    }

    override suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<LaporanMarginLaba> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat margin keuntungan"))
        } else {
            val revenue = saleRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0
            val purchases = purchaseRepository.getPurchasesInDateRange(startDate, endDate)
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

            val statsResult = saleItemRepository.getProdukPenjualansStats(dStart, dEnd)
            val salesStats = statsResult.getOrNull() ?: emptyList()

            val productIds = salesStats.map { it.produkId }
            val productsMap = productRepository.getProdukByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

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
            val byKategori = mutableMapOf<Long, Triple<Int, Double, Double>>()
            val salesWithItems = saleRepository.getSalesWithItemsInDateRange(startDate, endDate)
            val salesFiltered = salesWithItems.filter { !it.penjualan.isRefunded }

            // Pre-fetch all products to avoid N+1 query
            val productIds = salesFiltered.flatMap { it.items }.map { it.productId }.distinct()
            val productsMap = productRepository.getProdukByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            salesFiltered.forEach { saleWithItems ->
                saleWithItems.items.forEach { item ->
                    val prod = productsMap[item.productId]
                    val catId = prod?.categoryId ?: -1L
                    val cost = (prod?.costPrice ?: 0.0) * item.quantity
                    val cur = byKategori[catId] ?: Triple(0, 0.0, 0.0)
                    byKategori[catId] = Triple(cur.first + item.quantity, cur.second + item.totalPrice, cur.third + cost)
                }
            }

            val result = byKategori.map { (catId, t) ->
                PenjualanKategori(
                    kategoriId = catId,
                    namaKategori = "Kategori $catId",
                    jumlahTerjual = t.first,
                    totalPendapatan = t.second,
                    totalBiaya = t.third,
                    laba = t.second - t.third
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
                val salesForDay = salesByDate[day] ?: emptyList()
                val total = salesForDay.sumOf { it.totalAmount }
                val tx = salesForDay.size
                trend.add(DataTren(tanggal = day, penjualan = total, transaksi = tx))
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
            val start = date.withDayOfMonth(1)
            val end = date

            val salesWithItems = saleRepository.getSalesWithItemsInDateRange(start, end)
            val salesFiltered = salesWithItems.filter { !it.penjualan.isRefunded }
            val revenue = salesFiltered.sumOf { it.penjualan.totalAmount }

            // Pre-fetch all products
            val productIds = salesFiltered.flatMap { it.items }.map { it.productId }.distinct()
            val productsMap = productRepository.getProdukByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            var cogs = 0.0
            salesFiltered.forEach { saleWithItems ->
                saleWithItems.items.forEach { item ->
                    val prod = productsMap[item.productId]
                    cogs += (prod?.costPrice ?: 0.0) * item.quantity
                }
            }
            val expenses = expenseRepository.getPengeluaransByDateRange(start.toDate(), end.toDate()).first()
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
