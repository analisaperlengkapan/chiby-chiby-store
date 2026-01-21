package com.chibychibystore.service

import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
<<<<<<< HEAD
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.PenjualanRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
=======
import com.chibychibystore.repository.ExpenseRepository
import com.chibychibystore.repository.SaleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
>>>>>>> feat/ui-overhaul
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service untuk manajemen arus kas retail
 * Mengelola cash flows dari operasi, investasi, dan financing
 */
@Singleton
class CashManagementService @Inject constructor(
<<<<<<< HEAD
    private val penjualanRepository: PenjualanRepository,
    private val pengeluaranRepository: PengeluaranRepository
=======
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository
>>>>>>> feat/ui-overhaul
) {

    /**
     * Data class untuk cash flow summary
     */
    data class CashFlowSummary(
        val operatingCashFlow: Double,
        val investingCashFlow: Double,
        val financingCashFlow: Double,
        val netCashFlow: Double,
        val period: String
    )

    /**
     * Hitung operating cash flow
     * Operating cash flow = Sales revenue - Operating expenses - Inventory purchases
     */
    suspend fun calculateOperatingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            // Get sales revenue (cash inflows from operations)
            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            val salesRevenue = sales.sumOf { it.totalAmount }

<<<<<<< HEAD
            // Get approved expenses grouped by category to avoid in-memory filtering
            val approvedExpenses = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategori(start, end)

            // Calculate operating expenses
            val operatingExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.OPERATING_EXPENSE_CATEGORIES }
                .values.sum()

            // Calculate inventory purchases (COGS)
            val inventoryPurchases = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.COGS_CATEGORIES }
                .values.sum()
=======
            // Get operating expenses (cash outflows)
            val startDateDate = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val endDateDate = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())

            val expenses = expenseRepository.getExpensesByDateRange(startDateDate, endDateDate).first()
            
            val operatingExpenses = expenses
                .filter { it.category in ExpenseCategory.OPERATING_EXPENSE_CATEGORIES }
                .sumOf { it.amount }

            // Get inventory purchases (COGS - cash outflows for inventory)
            val inventoryPurchases = expenses
                .filter { it.category in ExpenseCategory.COGS_CATEGORIES }
                .sumOf { it.amount }
>>>>>>> feat/ui-overhaul

            val operatingCashFlow = salesRevenue - operatingExpenses - inventoryPurchases
            Result.success(operatingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung operating cash flow", e))
        }
    }

    /**
     * Hitung investing cash flow
     */
    suspend fun calculateInvestingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
<<<<<<< HEAD
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            // Get approved expenses grouped by category to avoid in-memory filtering
            val approvedExpenses = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategori(start, end)

            // Equipment purchases and store improvements (Investing activities)
            val equipmentExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.INVESTING_CATEGORIES }
                .values.sum()
=======
            // Equipment purchases and store improvements
            val startDateDate = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val endDateDate = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())

            val equipmentExpenses = expenseRepository.getExpensesByDateRange(startDateDate, endDateDate).first()
                .filter { it.category == ExpenseCategory.SUPPLIES_MAINTENANCE ||
                         it.category == ExpenseCategory.DEPRECIATION }
                .sumOf { it.amount }
>>>>>>> feat/ui-overhaul

            val investingCashFlow = -equipmentExpenses
            Result.success(investingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung investing cash flow", e))
        }
    }

    /**
     * Hitung financing cash flow
     */
    suspend fun calculateFinancingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            // Get approved expenses grouped by category to avoid in-memory filtering
            val approvedExpenses = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategori(start, end)

            // Financing expenses (loan repayments, dividends)
            val financingExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.FINANCING_CATEGORIES }
                .values.sum()

            // Cash outflows are negative
            val financingCashFlow = -financingExpenses
            Result.success(financingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung financing cash flow", e))
        }
    }

    /**
     * Hitung net cash flow
     */
    suspend fun calculateNetCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val operatingCF = calculateOperatingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val investingCF = calculateInvestingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val financingCF = calculateFinancingCashFlow(startDate, endDate).getOrNull() ?: 0.0

            val netCashFlow = operatingCF + investingCF + financingCF
            Result.success(netCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung net cash flow", e))
        }
    }

    /**
     * Get cash flow summary untuk periode tertentu
     */
    suspend fun getCashFlowSummary(startDate: LocalDate, endDate: LocalDate): Result<CashFlowSummary> {
        return try {
            val operatingCF = calculateOperatingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val investingCF = calculateInvestingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val financingCF = calculateFinancingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val netCF = operatingCF + investingCF + financingCF

            val period = "${startDate.toString()} - ${endDate.toString()}"

            Result.success(CashFlowSummary(
                operatingCashFlow = operatingCF,
                investingCashFlow = investingCF,
                financingCashFlow = financingCF,
                netCashFlow = netCF,
                period = period
            ))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal membuat cash flow summary", e))
        }
    }

    /**
     * Get current cash position
     */
    suspend fun getCurrentCashPosition(): Result<Double> {
        return try {
            Result.success(0.0)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal mendapatkan posisi kas", e))
        }
    }
}
