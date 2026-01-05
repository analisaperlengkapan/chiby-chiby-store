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
    suspend fun getDailySalesReport(date: LocalDate): Result<LaporanPenjualanHarian>
    
    /**
     * Mendapatkan laporan penjualan bulanan
     */
    suspend fun getMonthlySalesReport(year: Int, month: Int): Result<LaporanPenjualanBulanan>
    
    /**
     * Mendapatkan laporan keuangan
     */
    suspend fun getFinancialReport(startDate: LocalDate, endDate: LocalDate): Result<LaporanKeuangan>
    
    /**
     * Mendapatkan laporan inventory
     */
    suspend fun getInventoryReport(): Result<LaporanStok>
    
    /**
     * Mendapatkan top selling products
     */
    suspend fun getTopSellingProducts(limit: Int = 10): Result<List<DataPenjualanProduk>>
    
    /**
     * Mendapatkan low stock products
     */
    suspend fun getLowStockReport(): Result<List<ProdukStokRendah>>
    
    /**
     * Observable untuk real-time sales metrics
     */
    fun observeSalesMetrics(): Flow<MetrikPenjualan>

    // Additional helper reporting methods used by UI and PDF export
    suspend fun getGrossSales(startDate: LocalDate, endDate: LocalDate): Result<LaporanPenjualanKotor>
    suspend fun getProfitMargin(startDate: LocalDate, endDate: LocalDate): Result<LaporanMarginLaba>
    suspend fun getNetProfit(startDate: LocalDate, endDate: LocalDate): Result<LaporanLabaBersih>
    suspend fun getSalesByProduct(startDate: LocalDate, endDate: LocalDate): Result<List<PenjualanProduk>>
    suspend fun getSalesByCategory(startDate: LocalDate, endDate: LocalDate): Result<List<PenjualanKategori>>
    suspend fun getSalesTrend(startDate: LocalDate, endDate: LocalDate): Result<List<DataTren>>
    suspend fun getIncomeStatement(date: LocalDate): Result<LaporanLabaRugi>
    suspend fun getCashFlow(startDate: LocalDate, endDate: LocalDate): Result<ArusKas>
    suspend fun getExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<LaporanPengeluaran>
    suspend fun getBalanceSheet(asOfDate: LocalDate): Result<NeracaSaldo>
}

// DTOs for Reporting

data class LaporanPenjualanKotor(
    val totalPenjualan: Double,
    val totalTransaksi: Int,
    val rataRataTransaksi: Double
)

data class LaporanMarginLaba(
    val totalPendapatan: Double,
    val totalBiaya: Double,
    val labaKotor: Double,
    val marginLaba: Double
)

data class LaporanLabaBersih(
    val labaKotor: Double,
    val totalPengeluaran: Double,
    val labaBersih: Double,
    val marginLaba: Double
)

data class PenjualanProduk(
    val produkId: Long,
    val namaProduk: String,
    val jumlahTerjual: Int,
    val totalPendapatan: Double,
    val totalBiaya: Double,
    val laba: Double
)

data class PenjualanKategori(
    val kategoriId: Long,
    val namaKategori: String,
    val jumlahTerjual: Int,
    val totalPendapatan: Double,
    val totalBiaya: Double,
    val laba: Double
)

/**
 * Data class representing sales trend for a specific date.
 */
data class DataTren(
    val tanggal: LocalDate,
    val penjualan: Double,
    val transaksi: Int
)

data class LaporanLabaRugi(
    val pendapatan: Double,
    val hargaPokokPenjualan: Double, // COGS
    val labaKotor: Double,
    val bebanOperasional: Double,
    val labaBersih: Double
)

data class LaporanPengeluaran(
    val totalPengeluaran: Double,
    val pengeluaranPerKategori: Map<Any, Double>
)

data class NeracaSaldo(
    val aset: Double,
    val liabilitas: Double,
    val ekuitas: Double,
    val nilaiPersediaan: Double
)

data class ArusKas(
    val arusKasOperasional: Double,
    val arusKasInvestasi: Double,
    val arusKasPendanaan: Double,
    val arusKasBersih: Double,
    val saldoAwal: Double = 0.0,
    val saldoAkhir: Double = 0.0,
    val periode: String? = null
)

/**
 * Data classes untuk basic reporting
 */
data class LaporanPenjualanHarian(
    val tanggal: LocalDate,
    val totalPenjualan: Double,
    val totalTransaksi: Int,
    val rataRataTransaksi: Double,
    val produkTerlaris: List<DataPenjualanProduk>
)

data class LaporanPenjualanBulanan(
    val tahun: Int,
    val bulan: Int,
    val totalPenjualan: Double,
    val totalTransaksi: Int,
    val rincianHarian: List<DataPenjualanHarian>,
    val produkTerlaris: List<DataPenjualanProduk>
)

data class LaporanKeuangan(
    val tanggalMulai: LocalDate,
    val tanggalSelesai: LocalDate,
    val totalPendapatan: Double,
    val totalBiaya: Double,
    val labaKotor: Double,
    val totalPengeluaran: Double,
    val labaBersih: Double,
    val marginLaba: Double
)

data class LaporanStok(
    val totalProduk: Int,
    val totalNilai: Double,
    val stokRendahCount: Int,
    val stokHabisCount: Int,
    val rincianKategori: List<DataStokKategori>
)

data class DataPenjualanProduk(
    val produkId: Long,
    val namaProduk: String,
    val jumlahTerjual: Int,
    val totalPendapatan: Double
)

data class ProdukStokRendah(
    val produkId: Long,
    val namaProduk: String,
    val stokSaatIni: Int,
    val stokMinimal: Int,
    val jumlahPesanKembali: Int
)

data class DataPenjualanHarian(
    val tanggal: LocalDate,
    val totalPenjualan: Double,
    val jumlahTransaksi: Int
)

data class DataStokKategori(
    val kategoriId: Long,
    val namaKategori: String,
    val jumlahProduk: Int,
    val totalNilai: Double
)

data class MetrikPenjualan(
    val penjualanHariIni: Double,
    val transaksiHariIni: Int,
    val penjualanBulanIni: Double,
    val rataRataNilaiTransaksi: Double
)
