package com.chibychibystore.service

import android.content.Context
import android.os.Environment
import com.chibychibystore.data.model.Result
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.font.constants.StandardFonts
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF Export Service
 * Menyediakan fungsi export laporan ke format PDF
 */
@Singleton
class PdfExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportingService: ReportingService
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Export Gross Sales Report to PDF
     */
    suspend fun exportGrossSalesReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getGrossSales(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Penjualan_Kotor_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createGrossSalesPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Profit Margin Report to PDF
     */
    suspend fun exportProfitMarginReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getProfitMargin(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Margin_Keuntungan_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createProfitMarginPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Net Profit Report to PDF
     */
    suspend fun exportNetProfitReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getNetProfit(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Keuntungan_Bersih_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createNetProfitPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Sales by Product Report to PDF
     */
    suspend fun exportSalesByProductReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getSalesByProduct(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Penjualan_Produk_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createSalesByProductPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Sales by Category Report to PDF
     */
    suspend fun exportSalesByCategoryReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getSalesByCategory(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Penjualan_Kategori_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createSalesByCategoryPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Sales Trend Report to PDF
     */
    suspend fun exportSalesTrendReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getSalesTrend(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Trend_Penjualan_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createSalesTrendPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Income Statement to PDF
     */
    // PDF export for income statement - single date overload (month end)
    suspend fun exportIncomeStatement(date: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getIncomeStatement(date)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Laba_Rugi_${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createIncomeStatementPdf(filePath, reportData.data, date)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Cash Flow Report to PDF
     */
    suspend fun exportCashFlowReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getCashFlow(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Arus_Kas_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createCashFlowPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Expense Report to PDF
     */
    suspend fun exportExpenseReport(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<String> {
        return try {
            val reportData = reportingService.getExpenseReport(startDate, endDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Laporan_Pengeluaran_${startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_${endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createExpenseReportPdf(filePath, reportData.data, startDate, endDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    /**
     * Export Balance Sheet to PDF
     */
    suspend fun exportBalanceSheet(asOfDate: LocalDate): Result<String> {
        return try {
            val reportData = reportingService.getBalanceSheet(asOfDate)
            when (reportData) {
                is Result.Success -> {
                    val fileName = "Neraca_${asOfDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.pdf"
                    val filePath = createPdfFile(fileName)

                    createBalanceSheetPdf(filePath, reportData.data, asOfDate)
                    Result.success(filePath)
                }
                is Result.Failure -> Result.Failure(Exception("Gagal mendapatkan data laporan: ${reportData.exception.message}"))
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Gagal export PDF: ${e.message}"))
        }
    }

    private fun createPdfFile(fileName: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        return file.absolutePath
    }

    private fun createGrossSalesPdf(
        filePath: String,
        data: Map<String, Any>,
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

            val totalSales = (data["totalSales"] as? Number)?.toDouble() ?: 0.0
            val totalTransactions = (data["totalTransactions"] as? Number)?.toInt() ?: 0
            val averageTransaction = (data["averageTransaction"] as? Number)?.toDouble() ?: 0.0

            summaryTable.addCell(createHeaderCell("Total Penjualan"))
            summaryTable.addCell(createDataCell("Rp ${"%,.0f".format(totalSales)}"))

            summaryTable.addCell(createHeaderCell("Jumlah Transaksi"))
            summaryTable.addCell(createDataCell("${totalTransactions}"))

            summaryTable.addCell(createHeaderCell("Rata-rata per Transaksi"))
            summaryTable.addCell(createDataCell("Rp ${"%,.0f".format(averageTransaction)}"))

            document.add(summaryTable as com.itextpdf.layout.element.IBlockElement)

            // Footer
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createProfitMarginPdf(
        filePath: String,
        data: Map<String, Any>,
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

            val totalRevenue = (data["revenue"] as? Number)?.toDouble() ?: 0.0
            val totalCost = (data["costOfGoodsSold"] as? Number)?.toDouble() ?: 0.0
            val grossProfit = (data["grossProfit"] as? Number)?.toDouble() ?: 0.0
            val profitMargin = (data["marginPercentage"] as? Number)?.toDouble() ?: 0.0

            table.addCell(createHeaderCell("Total Pendapatan"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(totalRevenue)}"))

            table.addCell(createHeaderCell("Total Biaya"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(totalCost)}"))

            table.addCell(createHeaderCell("Laba Kotor"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(grossProfit)}"))

            table.addCell(createHeaderCell("Margin Keuntungan"))
            table.addCell(createDataCell("${"%.1f".format(profitMargin)}%"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createNetProfitPdf(
        filePath: String,
        data: Map<String, Any>,
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

            val grossProfit = (data["grossProfit"] as? Number)?.toDouble() ?: 0.0
            val totalExpenses = (data["totalExpenses"] as? Number)?.toDouble() ?: 0.0
            val netProfit = (data["netProfit"] as? Number)?.toDouble() ?: 0.0
            val profitMargin = (data["profitMargin"] as? Number)?.toDouble() ?: 0.0

            table.addCell(createHeaderCell("Laba Kotor"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(grossProfit)}"))

            table.addCell(createHeaderCell("Total Pengeluaran"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(totalExpenses)}"))

            table.addCell(createHeaderCell("Laba Bersih"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(netProfit)}"))

            table.addCell(createHeaderCell("Margin Keuntungan"))
            table.addCell(createDataCell("${"%.1f".format(profitMargin)}%"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createSalesByProductPdf(
        filePath: String,
        data: List<Map<String, Any>>,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Penjualan per Produk", startDate, endDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(25f, 15f, 20f, 20f, 20f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            // Header row
            table.addCell(createHeaderCell("Produk"))
            table.addCell(createHeaderCell("Terjual"))
            table.addCell(createHeaderCell("Pendapatan"))
            table.addCell(createHeaderCell("Biaya"))
            table.addCell(createHeaderCell("Laba"))

            // Data rows
            data.take(20).forEach { product ->
                val name = product["productName"] as? String ?: "-"
                val qty = (product["quantity"] as? Number)?.toInt() ?: 0
                val revenue = (product["revenue"] as? Number)?.toDouble() ?: 0.0
                val cost = (product["totalCost"] as? Number)?.toDouble() ?: 0.0
                val profit = (product["profit"] as? Number)?.toDouble() ?: (revenue - cost)

                table.addCell(createDataCell(name))
                table.addCell(createDataCell("${qty}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(revenue)}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(cost)}"))
                table.addCell(createDataCell("Rp ${"%.0f".format(profit)}"))
            }

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createSalesByCategoryPdf(
        filePath: String,
        data: List<Map<String, Any>>,
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
                val name = category["categoryName"] as? String ?: "-"
                val qty = (category["quantitySold"] as? Number)?.toInt() ?: 0
                val revenue = (category["totalRevenue"] as? Number)?.toDouble() ?: 0.0
                val cost = (category["totalCost"] as? Number)?.toDouble() ?: 0.0
                val profit = (category["profit"] as? Number)?.toDouble() ?: (revenue - cost)

                table.addCell(createDataCell(name))
                table.addCell(createDataCell("${qty}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(revenue)}"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(cost)}"))
                table.addCell(createDataCell("Rp ${"%.0f".format(profit)}"))
            }

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createSalesTrendPdf(
        filePath: String,
        data: List<Map<String, Any>>,
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
                val date = trend["date"] as? java.time.LocalDate
                val sales = (trend["sales"] as? Number)?.toDouble() ?: 0.0
                val transactions = (trend["transactions"] as? Number)?.toInt() ?: 0

                table.addCell(createDataCell(date?.format(dateFormatter) ?: "-"))
                table.addCell(createDataCell("Rp ${"%,.0f".format(sales)}"))
                table.addCell(createDataCell("${transactions}"))
            }

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createIncomeStatementPdf(
        filePath: String,
        data: Map<String, Any>,
        date: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Laporan Laba Rugi", null, null, date)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            val revenue = (data["revenue"] as? Number)?.toDouble() ?: 0.0
            val costOfGoods = (data["costOfGoodsSold"] as? Number)?.toDouble() ?: 0.0
            val grossProfit = (data["grossProfit"] as? Number)?.toDouble() ?: 0.0
            val operatingExpenses = (data["operatingExpenses"] as? Number)?.toDouble() ?: 0.0
            val netIncome = (data["netIncome"] as? Number)?.toDouble() ?: 0.0

            table.addCell(createHeaderCell("Pendapatan"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(revenue)}"))

            table.addCell(createHeaderCell("Harga Pokok Penjualan"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(costOfGoods)}"))

            table.addCell(createHeaderCell("Laba Kotor"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(grossProfit)}"))

            table.addCell(createHeaderCell("Beban Operasional"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(operatingExpenses)}"))

            table.addCell(createHeaderCell("Laba Bersih"))
            table.addCell(createDataCell("Rp ${"%.0f".format(netIncome)}"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createCashFlowPdf(
        filePath: String,
        data: CashFlow,
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

            table.addCell(createHeaderCell("Arus Kas Operasional"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(data.operatingCashFlow)}"))

            table.addCell(createHeaderCell("Arus Kas Investasi"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(data.investingCashFlow)}"))

            table.addCell(createHeaderCell("Arus Kas Pendanaan"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(data.financingCashFlow)}"))

            table.addCell(createHeaderCell("Arus Kas Bersih"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(data.netCashFlow)}"))

            table.addCell(createHeaderCell("Saldo Akhir"))
            table.addCell(createDataCell("Rp ${"%.0f".format(data.endingCash)}"))

            document.add(table as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createExpenseReportPdf(
        filePath: String,
        data: Map<String, Any>,
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

            val totalExpenses = (data["totalExpenses"] as? Number)?.toDouble() ?: 0.0
            summaryTable.addCell(createHeaderCell("Total Pengeluaran"))
            summaryTable.addCell(createDataCell("Rp ${"%.0f".format(totalExpenses)}"))

            document.add(summaryTable as com.itextpdf.layout.element.IBlockElement)
            document.add(Paragraph("\n"))

            // Expenses by category
            val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            categoryTable.setWidth(UnitValue.createPercentValue(100f))

            categoryTable.addCell(createHeaderCell("Kategori"))
            categoryTable.addCell(createHeaderCell("Jumlah"))

            val byCategory = data["expensesByCategory"] as? Map<*, *> ?: emptyMap<Any, Any>()
            byCategory.forEach { (category, amount) ->
                val name = (category as? String) ?: category.toString()
                val amt = (amount as? Number)?.toDouble() ?: 0.0
                categoryTable.addCell(createDataCell(name))
                categoryTable.addCell(createDataCell("Rp ${"%.0f".format(amt)}"))
            }

            document.add(categoryTable as com.itextpdf.layout.element.IBlockElement)
            addFooter(document)

        } finally {
            document.close()
        }
    }

    private fun createBalanceSheetPdf(
        filePath: String,
        data: Map<String, Any>,
        asOfDate: LocalDate
    ) {
        val writer = PdfWriter(filePath)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            addHeader(document, "Neraca", null, null, asOfDate)

            val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            val assets = (data["assets"] as? Number)?.toDouble() ?: 0.0
            val liabilities = (data["liabilities"] as? Number)?.toDouble() ?: 0.0
            val equity = (data["equity"] as? Number)?.toDouble() ?: 0.0
            val inventoryValue = (data["inventoryValue"] as? Number)?.toDouble() ?: 0.0

            table.addCell(createHeaderCell("Total Aset"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(assets)}"))

            table.addCell(createHeaderCell("Total Liabilitas"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(liabilities)}"))

            table.addCell(createHeaderCell("Ekuitas"))
            table.addCell(createDataCell("Rp ${"%,.0f".format(equity)}"))

            table.addCell(createHeaderCell("Nilai Inventaris"))
            table.addCell(createDataCell("Rp ${"%.0f".format(inventoryValue)}"))

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
        val titleParagraph = Paragraph("Chiby Chiby Store")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER)

        val subtitleParagraph = Paragraph(title)
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(14f)
            .setTextAlignment(TextAlignment.CENTER)

        document.add(titleParagraph)
        document.add(subtitleParagraph)

        val dateText = when {
            startDate != null && endDate != null ->
                "Periode: ${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"
            asOfDate != null ->
                "Per tanggal: ${asOfDate.format(dateFormatter)}"
            else -> ""
        }

        if (dateText.isNotEmpty()) {
            val dateParagraph = Paragraph(dateText)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(dateParagraph)
        }

        document.add(Paragraph("\n"))
    }

    private fun addFooter(document: Document) {
        val footerText = "Dibuat pada: ${java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}"
        val footerParagraph = Paragraph(footerText)
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)

        document.add(Paragraph("\n"))
        document.add(footerParagraph)
    }

    private fun createHeaderCell(text: String): Cell {
        return Cell().add(Paragraph(text))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
    }

    private fun createDataCell(text: String): Cell {
        return Cell().add(Paragraph(text))
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
            .setFontSize(9f)
            .setTextAlignment(TextAlignment.LEFT)
    }
}