package com.chibychibystore.service.impl

import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.*
import com.chibychibystore.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
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
    private val cashManagementService: CashManagementService,
    private val authService: AuthService,
    private val db: ChibyChibyDatabase
) : ReportingService {

    private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

    private fun getEndDateWithTime(date: LocalDate): Date {
        return Date.from(date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
    }

    override suspend fun getDailySalesReport(date: LocalDate): Result<LaporanPenjualanHarian> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan"))
        } else {
            val totalSales = penjualanRepository.getTotalCashReceipts(date, date).getOrNull() ?: 0.0
            val totalTransactions = penjualanRepository.getPenjualanCountByDateRange(date, date).getOrNull() ?: 0
            val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
            Result.success(LaporanPenjualanHarian(date, totalSales, totalTransactions, avg, emptyList()))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getDailySalesReport failed", e))
    }

    override suspend fun getMonthlySalesReport(year: Int, month: Int): Result<LaporanPenjualanBulanan> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan"))
        } else {
            val startDate = LocalDate.of(year, month, 1)
            val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            val totalSales = sales.filter { !it.isRefunded }.sumOf { maxOf(0.0, it.totalAmount - it.tax) }
            val totalTransactions = sales.count { !it.isRefunded }

            val dailyMap = sales.filter { !it.isRefunded }.groupBy {
                it.saleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            }.mapValues { (_, daySales) ->
                DataPenjualanHarian(
                    tanggal = daySales.first().saleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    totalPenjualan = daySales.sumOf { maxOf(0.0, it.totalAmount - it.tax) },
                    jumlahTransaksi = daySales.size
                )
            }.values.toList().sortedBy { it.tanggal }

            Result.success(LaporanPenjualanBulanan(year, month, totalSales, totalTransactions, dailyMap, emptyList()))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getMonthlySalesReport failed", e))
    }

    override suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<LaporanKeuangan> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan keuangan"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = getEndDateWithTime(endDate)

            val totalRevenue = penjualanRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0
            val totalExpenses = pengeluaranRepository.getTotalPengeluaranAmount(dStart, dEnd).getOrNull() ?: 0.0

            val purchases = pembelianRepository.getPurchasesInDateRange(startDate, endDate)
            val totalCost = purchases.sumOf { it.totalAmount }

            val grossProfit = totalRevenue - totalCost
            val netProfit = grossProfit - totalExpenses
            val profitMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
            Result.success(LaporanKeuangan(startDate, endDate, totalRevenue, totalCost, grossProfit, totalExpenses, netProfit, profitMargin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getFinancialReport failed", e))
    }

    override suspend fun getInventoryReport(): Result<LaporanStok> = try {
        if (!authService.hasPermission("VIEW_INVENTORY_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan inventory"))
        } else {
            val totalProducts = produkRepository.getProdukCount().getOrNull() ?: 0
            val totalValue = produkRepository.getTotalInventoryValue().getOrNull() ?: 0.0

            val lowStockCount = produkRepository.countLowStock().getOrNull() ?: 0
            val outOfStockCount = produkRepository.countOutOfStock().getOrNull() ?: 0

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
            val topProductsResult = itemPenjualanRepository.getTopSellingProduks(limit)
            val topProductsDto = topProductsResult.getOrNull() ?: emptyList()

            val productIds = topProductsDto.map { it.produkId }
            val productsMap = produkRepository.getProductsByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            val result = topProductsDto.map { dto ->
                DataPenjualanProduk(
                    produkId = dto.produkId,
                    namaProduk = productsMap[dto.produkId]?.name ?: "Unknown Product",
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
            val lowStockProduks = produkRepository.getLowStockProduk().first()
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

    override fun observeSalesMetrics(): kotlinx.coroutines.flow.Flow<MetrikPenjualan> = kotlinx.coroutines.flow.flow {
        var last = MetrikPenjualan(0.0, 0, 0.0, 0.0)
        while (true) {
            val next = try {
                val today = LocalDate.now()
                val startOfMonth = today.withDayOfMonth(1)

                val todaySales = penjualanRepository.getTotalRevenue(today, today).getOrNull() ?: 0.0
                val todayTx = penjualanRepository.getPenjualanCountNonRefunded(today, today).getOrNull() ?: 0
                val monthSales = penjualanRepository.getTotalRevenue(startOfMonth, today).getOrNull() ?: 0.0
                val avgTx = if (todayTx > 0) todaySales / todayTx else 0.0
                MetrikPenjualan(todaySales, todayTx, monthSales, avgTx)
            } catch (e: Exception) {
                // Re-throw CancellationException so coroutine cancellation
                // propagates correctly. Catching it here would briefly delay
                // cancellation (until the next emit/delay suspend point) and
                // mask cooperative cancellation semantics. All other transient
                // errors fall through to keep emitting last-known-good values
                // rather than terminating the flow and freezing the dashboard.
                if (e is kotlinx.coroutines.CancellationException) throw e
                last
            }
            last = next
            emit(next)
            kotlinx.coroutines.delay(30000) // Update every 30 seconds
        }
    }

    override suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<LaporanPenjualanKotor> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat data penjualan kotor"))
        } else {
            val totalSales = penjualanRepository.getTotalCashReceipts(startDate, endDate).getOrNull() ?: 0.0
            val totalTransactions = penjualanRepository.getPenjualanCountNonRefunded(startDate, endDate).getOrNull() ?: 0
            val avg = if (totalTransactions > 0) totalSales / totalTransactions else 0.0
            Result.success(LaporanPenjualanKotor(totalSales, totalTransactions, avg))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getGrossSales failed", e))
    }

    override suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<LaporanMarginLaba> = try {
        if (!authService.hasPermission("VIEW_FINANCIAL_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat margin keuntungan"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = getEndDateWithTime(endDate)

            val revenue = penjualanRepository.getTotalRevenue(startDate, endDate).getOrNull() ?: 0.0
            val purchases = pembelianRepository.getPurchasesInDateRange(startDate, endDate)
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
            val dEnd = getEndDateWithTime(endDate)

            val profitResRes = getProfitMargin(startDate, endDate)
            if (profitResRes is Result.Failure) throw profitResRes.exception
            val profitRes = (profitResRes as Result.Success).data

            val expenses = pengeluaranRepository.getPengeluaransByDateRange(dStart, dEnd).first()
            val totalExpenses = expenses.sumOf { it.amount }

            val netProfit = profitRes.labaKotor - totalExpenses
            val revenue = profitRes.totalPendapatan
            val netMargin = if (revenue > 0) (netProfit / revenue) * 100 else 0.0

            Result.success(LaporanLabaBersih(profitRes.labaKotor, totalExpenses, netProfit, netMargin))
        }
    } catch (e: Exception) {
        Result.failure(Exception("getNetProfit failed", e))
    }

    override suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<PenjualanProduk>> = try {
        if (!authService.hasPermission("VIEW_SALES_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan penjualan per product"))
        } else {
            val dStart = startDate.toDate()
            val dEnd = getEndDateWithTime(endDate)
            val statsResult = itemPenjualanRepository.getProdukPenjualansStats(dStart, dEnd)
            val salesStats = statsResult.getOrNull() ?: emptyList()

            val productIds = salesStats.map { it.produkId }
            val productsMap = produkRepository.getProductsByIds(productIds).getOrNull()?.associateBy { it.id } ?: emptyMap()

            val result = salesStats.map { stat ->
                val prod = productsMap[stat.produkId]
                val costPrice = prod?.costPrice ?: 0.0
                val totalCost = costPrice * stat.jumlahTerjual
                val profit = stat.totalPendapatan - totalCost

                PenjualanProduk(
                    produkId = stat.produkId,
                    namaProduk = prod?.name ?: "Unknown Produk",
                    jumlahTerjual = stat.jumlahTerjual,
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
            val dEnd = getEndDateWithTime(endDate)
            val statsResult = itemPenjualanRepository.getSalesByCategory(dStart, dEnd)
            val salesStats = statsResult.getOrNull() ?: emptyList()

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
            val allSales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
            val trend = mutableListOf<DataTren>()
            for (i in 0..days) {
                val day = startDate.plusDays(i.toLong())
                val sales = allSales.filter { it.saleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == day && !it.isRefunded }
                // Sum tax-exclusive net revenue per row (matching getTotalRevenue
                // semantics) instead of `totalAmount`. As of MIGRATION_11_12,
                // `totalAmount` is the post-tax/discount amount paid, so summing
                // it directly here would silently shift the trend chart upward by
                // the tax portion on every row — a misleading change for a
                // visualization that previously showed pre-tax sales subtotals.
                // The MAX(0, …) floor mirrors the floor applied to legacy rows by
                // the migration when `discount > subtotal + tax`, so an extreme
                // discount contributes 0 rather than a negative -tax value.
                val total = sales.sumOf { maxOf(0.0, it.totalAmount - it.tax) }
                val tx = sales.size
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
            val startOfMonth = date.withDayOfMonth(1)
            val dStart = startOfMonth.toDate()
            val dEnd = getEndDateWithTime(date)

            val expenses = pengeluaranRepository.getPengeluaransByDateRange(dStart, dEnd).first()
            val revenue = penjualanRepository.getTotalRevenue(startOfMonth, date).getOrNull() ?: 0.0
            val cogs = itemPenjualanRepository.calculateTotalCogs(dStart, dEnd).getOrNull() ?: 0.0
            
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
            val dEnd = getEndDateWithTime(endDate)
            val expenses = pengeluaranRepository.getPengeluaransByDateRange(dStart, dEnd).first()
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

    override suspend fun getStockMovementReport(startDate: LocalDate, endDate: LocalDate): Result<List<StockMovement>> = try {
        if (!authService.hasPermission("VIEW_INVENTORY_REPORTS")) {
            Result.failure(Exception("Tidak memiliki izin untuk melihat laporan pergerakan stok"))
        } else {
            val movements = mutableListOf<StockMovement>()
            val warehouses = db.gudangDao().getAllGudang().first().associateBy { it.id }
            val products = produkRepository.getAllProduk().first().associateBy { it.id }

            // 1. Sales
            val sales = penjualanRepository.getPenjualanWithItemsByRentangTanggal(startDate.toString(), endDate.toString()).first()
            sales.forEach { saleWithItems ->
                saleWithItems.items.forEach { item ->
                    movements.add(StockMovement(
                        date = saleWithItems.sale.saleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        productId = item.productId,
                        productName = products[item.productId]?.name ?: "Unknown Product",
                        type = if (saleWithItems.sale.isRefunded) "REFUND" else "SALE",
                        quantity = if (saleWithItems.sale.isRefunded) item.quantity else -item.quantity,
                        warehouseName = warehouses[saleWithItems.sale.warehouseId]?.name ?: "Gudang Utama",
                        referenceId = "S-${saleWithItems.sale.id}"
                    ))
                }
            }

            // 2. Purchases
            val purchases = db.pembelianDao().getPembelianByRentangTanggal(startDate.toDate(), getEndDateWithTime(endDate)).first()
            purchases.forEach { purchase ->
                val items = db.itemPembelianDao().getItemsByPembelianId(purchase.id).first()
                items.forEach { item ->
                    movements.add(StockMovement(
                        date = purchase.purchaseDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        productId = item.productId,
                        productName = products[item.productId]?.name ?: "Unknown Product",
                        type = "PURCHASE",
                        quantity = item.quantity,
                        warehouseName = warehouses[purchase.warehouseId]?.name ?: "Gudang Utama",
                        referenceId = "P-${purchase.id}"
                    ))
                }
            }

            // 3. Audits (Completed)
            val audits = db.inventoryAuditDao().getAllAuditsList().filter {
                val date = it.auditDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                (date.isAfter(startDate) || date.isEqual(startDate)) && (date.isBefore(endDate) || date.isEqual(endDate)) && it.status == com.chibychibystore.data.local.entity.AuditStatus.COMPLETED
            }
            // Load all audit items once and group by auditId, instead of
            // re-fetching the entire table for each audit (O(N*M) → O(N+M)).
            val allAuditItemsByAudit = db.inventoryAuditDao().getAllAuditItems().groupBy { it.auditId }
            audits.forEach { audit ->
                val items = allAuditItemsByAudit[audit.id] ?: emptyList()
                items.forEach { item ->
                    if (item.difference != 0) {
                        movements.add(StockMovement(
                            date = audit.auditDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            productId = item.productId,
                            productName = products[item.productId]?.name ?: "Unknown Product",
                            type = "ADJUSTMENT",
                            quantity = item.difference,
                            warehouseName = warehouses[audit.warehouseId]?.name ?: "Gudang Utama",
                            referenceId = "A-${audit.id}"
                        ))
                    }
                }
            }

            Result.success(movements.sortedByDescending { it.date })
        }
    } catch (e: Exception) {
        Result.failure(Exception("getStockMovementReport failed", e))
    }
}
