package com.chibychibystore.service

import android.content.Context
import android.os.Environment
import com.chibychibystore.data.model.Result
import com.chibychibystore.di.IoDispatcher
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** PDF Export Service Menyediakan fungsi export laporan ke format PDF */
@Singleton
class PdfExportService
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val reportingService: ReportingService,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Export Gross Sales Report to PDF */
    suspend fun exportGrossSalesReport(startDate: LocalDate, endDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getGrossSales(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Penjualan_Kotor_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createGrossSalesPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Profit Margin Report to PDF */
    suspend fun exportProfitMarginReport(startDate: LocalDate, endDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getProfitMargin(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Margin_Keuntungan_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createProfitMarginPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Net Profit Report to PDF */
    suspend fun exportNetProfitReport(startDate: LocalDate, endDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getNetProfit(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Keuntungan_Bersih_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createNetProfitPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Sales by Product Report to PDF */
    suspend fun exportSalesByProductReport(
            startDate: LocalDate,
            endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getSalesByProduct(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Penjualan_Product_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createSalesByProductPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Sales by Category Report to PDF */
    suspend fun exportSalesByCategoryReport(
            startDate: LocalDate,
            endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getSalesByCategory(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Penjualan_Kategori_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createSalesByCategoryPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Sales Trend Report to PDF */
    suspend fun exportSalesTrendReport(startDate: LocalDate, endDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getSalesTrend(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Trend_Penjualan_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createSalesTrendPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Income Statement to PDF */
    suspend fun exportIncomeStatement(date: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getIncomeStatement(date)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Laba_Rugi_${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createIncomeStatementPdf(filePath, reportData.data, date)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Cash Flow Report to PDF */
    suspend fun exportCashFlowReport(startDate: LocalDate, endDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getCashFlow(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Arus_Kas_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createCashFlowPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Expense Report to PDF */
    suspend fun exportExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getExpenseReport(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Laporan_Pengeluaran_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createExpenseReportPdf(filePath, reportData.data, startDate, endDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /** Export Balance Sheet to PDF */
    suspend fun exportBalanceSheet(asOfDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getBalanceSheet(asOfDate)
            when (reportData) {
                is Result.Success -> {
                    withContext(ioDispatcher) {
                        val fileName =
                                "Neraca_${asOfDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                        val filePath = createPdfFile(fileName)

                        createBalanceSheetPdf(filePath, reportData.data, asOfDate)
                        Result.success(filePath)
                    }
                }
                is Result.Failure ->
                        Result.Failure(
                                Exception(
                                        "Gagal mendapatkan data laporan: ${reportData.exception.message}"
                                )
                        )
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    private fun createPdfFile(fileName: String): String {
        val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        return file.absolutePath
    }

    private fun createGrossSalesPdf(
            filePath: String,
            data: LaporanPenjualanKotor,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            // Header
            addHeader(document, "Laporan Penjualan Kotor", startDate, endDate)

            // Summary metrics
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            summaryTable.setWidth(UnitValue.createPercentValue(100f))

            summaryTable.addCell(createCell("Total Penjualan", true))
            summaryTable.addCell(createCell("Rp ${"%,.0f".format(data.totalPenjualan)}"))

            summaryTable.addCell(createCell("Jumlah Transaksi", true))
            summaryTable.addCell(createCell("${data.totalTransaksi}"))

            summaryTable.addCell(createCell("Rata-rata per Transaksi", true))
            summaryTable.addCell(createCell("Rp ${"%,.0f".format(data.rataRataTransaksi)}"))

            document.add(summaryTable as com.itextpdf.layout.element.IBlockElement)

            // Footer
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createProfitMarginPdf(
            filePath: String,
            data: LaporanMarginLaba,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Margin Keuntungan", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createCell("Total Pendapatan", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.totalPendapatan)}"))

            table.addCell(createCell("Total Biaya", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.totalBiaya)}"))

            table.addCell(createCell("Laba Kotor", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.labaKotor)}"))

            table.addCell(createCell("Margin Keuntungan", true))
            table.addCell(createCell("${"%.1f".format(data.marginLaba)}%"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createNetProfitPdf(
            filePath: String,
            data: LaporanLabaBersih,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Keuntungan Bersih", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createCell("Laba Kotor", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.labaKotor)}"))

            table.addCell(createCell("Total Pengeluaran", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.totalPengeluaran)}"))

            table.addCell(createCell("Laba Bersih", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.labaBersih)}"))

            table.addCell(createCell("Margin Keuntungan", true))
            table.addCell(createCell("${"%.1f".format(data.marginLaba)}%"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createSalesByProductPdf(
            filePath: String,
            data: List<PenjualanProduk>,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Penjualan per Product", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 15f, 20f, 20f, 20f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            // Header row
            table.addCell(createHeaderCell("Product"))
            table.addCell(createHeaderCell("Terjual"))
            table.addCell(createHeaderCell("Pendapatan"))
            table.addCell(createHeaderCell("Biaya"))
            table.addCell(createHeaderCell("Laba"))

            // Data rows
            data.take(20).forEach { product ->
                table.addCell(createDataCell(product.namaProduk))
                table.addCell(createDataCell("${product.jumlahTerjual}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(product.totalPendapatan)}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(product.totalBiaya)}"))
                table.addCell(createDataCell("Rp ${"%.0f".format(product.laba)}"))
            }

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createSalesByCategoryPdf(
            filePath: String,
            data: List<PenjualanKategori>,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Penjualan per Kategori", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 15f, 20f, 20f, 20f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createHeaderCell("Kategori"))
            table.addCell(createHeaderCell("Terjual"))
            table.addCell(createHeaderCell("Pendapatan"))
            table.addCell(createHeaderCell("Biaya"))
            table.addCell(createHeaderCell("Laba"))

            data.forEach { category ->
                table.addCell(createDataCell(category.namaKategori))
                table.addCell(createDataCell("${category.jumlahTerjual}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(category.totalPendapatan)}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(category.totalBiaya)}"))
                table.addCell(createDataCell("Rp ${"%.0f".format(category.laba)}"))
            }

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createSalesTrendPdf(
            filePath: String,
            data: List<DataTren>,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Trend Penjualan", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createHeaderCell("Tanggal"))
            table.addCell(createHeaderCell("Penjualan"))
            table.addCell(createHeaderCell("Transaksi"))

            data.forEach { trend ->
                table.addCell(createDataCell(trend.tanggal.format(dateFormatter)))
                table.addCell(createDataCell("Rp ${"%,.0f".format(trend.penjualan)}"))
                table.addCell(createDataCell("${trend.transaksi}"))
            }

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createIncomeStatementPdf(filePath: String, data: LaporanLabaRugi, date: LocalDate) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Laba Rugi", null, null, date)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createCell("Pendapatan", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.pendapatan)}"))

            table.addCell(createCell("Harga Pokok Penjualan", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.hargaPokokPenjualan)}"))

            table.addCell(createCell("Laba Kotor", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.labaKotor)}"))

            table.addCell(createCell("Beban Operasional", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.bebanOperasional)}"))

            table.addCell(createCell("Laba Bersih", true))
            table.addCell(createCell("Rp ${"%.0f".format(data.labaBersih)}"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createCashFlowPdf(
            filePath: String,
            data: ArusKas,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Arus Kas", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createCell("Arus Kas Operasional", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.arusKasOperasional)}"))

            table.addCell(createCell("Arus Kas Investasi", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.arusKasInvestasi)}"))

            table.addCell(createCell("Arus Kas Pendanaan", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.arusKasPendanaan)}"))

            table.addCell(createCell("Arus Kas Bersih", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.arusKasBersih)}"))

            table.addCell(createCell("Saldo Akhir", true))
            table.addCell(createCell("Rp ${"%.0f".format(data.saldoAkhir)}"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createExpenseReportPdf(
            filePath: String,
            data: LaporanPengeluaran,
            startDate: LocalDate,
            endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Pengeluaran", startDate, endDate)

            // Total expenses
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            summaryTable.setWidth(UnitValue.createPercentValue(100f))

            summaryTable.addCell(createHeaderCell("Total Pengeluaran"))
            summaryTable.addCell(createDataCell("Rp ${"%.0f".format(data.totalPengeluaran)}"))

            document.add(summaryTable as com.itextpdf.layout.element.IBlockElement)
            document.add(Paragraph("\n"))

            // Expenses by category
            val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            categoryTable.setWidth(UnitValue.createPercentValue(100f))

            categoryTable.addCell(createHeaderCell("Kategori"))
            categoryTable.addCell(createHeaderCell("Jumlah"))

            data.pengeluaranPerKategori.forEach { entry: Map.Entry<Any, Double> ->
                val category = entry.key
                val amount = entry.value
                val name = if (category is Enum<*>) category.name else category.toString()
                categoryTable.addCell(createDataCell(name))
                categoryTable.addCell(createDataCell("Rp ${"%.0f".format(amount)}"))
            }

            document.add(categoryTable as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun createBalanceSheetPdf(filePath: String, data: NeracaSaldo, asOfDate: LocalDate) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Neraca", null, null, asOfDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            table.addCell(createCell("Total Aset", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.aset)}"))

            table.addCell(createCell("Total Liabilitas", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.liabilitas)}"))

            table.addCell(createCell("Ekuitas", true))
            table.addCell(createCell("Rp ${"%,.0f".format(data.ekuitas)}"))

            table.addCell(createCell("Nilai Inventaris", true))
            table.addCell(createCell("Rp ${"%.0f".format(data.nilaiPersediaan)}"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)
        } finally {
            document.close()
        }
    }

    private fun addHeader(
            document: Document,
            title: String,
            startDate: LocalDate? = null,
            endDate: LocalDate? = null,
            asOfDate: LocalDate? = null
    ) {
        val titleParagraph =
                Paragraph("Chiby Chiby Store")
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(18f)
                        .setTextAlignment(TextAlignment.CENTER)

        val subtitleParagraph =
                Paragraph(title)
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(14f)
                        .setTextAlignment(TextAlignment.CENTER)

        document.add(titleParagraph)
        document.add(subtitleParagraph)

        val dateText =
                when {
                    startDate != null && endDate != null ->
                            "Periode: ${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"
                    asOfDate != null -> "Per tanggal: ${asOfDate.format(dateFormatter)}"
                    else -> ""
                }

        if (dateText.isNotEmpty()) {
            val dateParagraph =
                    Paragraph(dateText)
                            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                            .setFontSize(10f)
                            .setTextAlignment(TextAlignment.CENTER)
            document.add(dateParagraph)
        }

        document.add(Paragraph("\n"))
    }

    private fun addFooter(document: Document) {
        val footerText =
                "Dibuat pada: ${java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}"
        val footerParagraph =
                Paragraph(footerText)
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                        .setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)

        document.add(Paragraph("\n"))
        document.add(footerParagraph)
    }

    private fun createHeaderCell(text: String): Cell {
        return createCell(text, true)
    }

    private fun createDataCell(text: String): Cell {
        return createCell(text, false)
    }

    private fun createCell(text: String, isHeader: Boolean = false): Cell {
        val cell =
                Cell().add(Paragraph(text))
                        .setFont(
                                PdfFontFactory.createFont(
                                        if (isHeader) StandardFonts.HELVETICA_BOLD
                                        else StandardFonts.HELVETICA
                                )
                        )
                        .setFontSize(if (isHeader) 10f else 9f)
                        .setTextAlignment(
                                if (isHeader) TextAlignment.CENTER else TextAlignment.LEFT
                        )

        if (isHeader) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
        }

        return cell
    }
}
