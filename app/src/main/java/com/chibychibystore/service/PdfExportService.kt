package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import java.time.LocalDate

/** PDF Export Service Menyediakan fungsi export laporan ke format PDF */
interface PdfExportService {
    /** Export Gross Sales Report to PDF */
    suspend fun exportGrossSalesReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Profit Margin Report to PDF */
    suspend fun exportProfitMarginReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Net Profit Report to PDF */
    suspend fun exportNetProfitReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Sales by Product Report to PDF */
    suspend fun exportSalesByProductReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Sales by Category Report to PDF */
    suspend fun exportSalesByCategoryReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Sales Trend Report to PDF */
    suspend fun exportSalesTrendReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Income Statement to PDF */
    suspend fun exportIncomeStatement(date: LocalDate): Result<String>

    /** Export Cash Flow Report to PDF */
    suspend fun exportCashFlowReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Expense Report to PDF */
    suspend fun exportExpenseReport(startDate: LocalDate, endDate: LocalDate): Result<String>

    /** Export Balance Sheet to PDF */
    suspend fun exportBalanceSheet(asOfDate: LocalDate): Result<String>

    /** Export Periodic Summary Report to PDF */
    suspend fun exportPeriodicSummaryReport(startDate: LocalDate, endDate: LocalDate): Result<String>
}
