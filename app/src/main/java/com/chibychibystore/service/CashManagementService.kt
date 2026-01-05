package com.chibychibystore.service

import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.ExpenseRepository
import com.chibychibystore.repository.SaleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service untuk manajemen arus kas retail
 * Mengelola cash flows dari operasi, investasi, dan financing
 */
@Singleton
class CashManagementService @Inject constructor(
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository
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
            // Get sales revenue (cash inflows from operations)
            val sales = saleRepository.getSalesInDateRange(startDate, endDate)
            val salesRevenue = sales.sumOf { it.totalAmount }

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

            val operatingCashFlow = salesRevenue - operatingExpenses - inventoryPurchases
            Result.success(operatingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung operating cash flow", e))
        }
    }

    /**
     * Hitung investing cash flow
     * Investing cash flow = - (Equipment purchases + Store improvements)
     * Untuk retail sederhana, ini terutama pembelian equipment dan improvements
     */
    suspend fun calculateInvestingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            // Equipment purchases and store improvements
            val startDateDate = java.util.Date.from(startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            val endDateDate = java.util.Date.from(endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant())

            val equipmentExpenses = expenseRepository.getExpensesByDateRange(startDateDate, endDateDate).first()
                .filter { it.category == ExpenseCategory.SUPPLIES_MAINTENANCE ||
                         it.category == ExpenseCategory.DEPRECIATION }
                .sumOf { it.amount }

            // Investing cash flow is negative (cash outflows)
            val investingCashFlow = -equipmentExpenses
            Result.success(investingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung investing cash flow", e))
        }
    }

    /**
     * Hitung financing cash flow
     * Financing cash flow = Owner investments - Owner withdrawals
     * Untuk retail sederhana, ini terutama modal owner
     */
    suspend fun calculateFinancingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            // For now, financing cash flow is 0 as we don't track owner investments/withdrawals
            // This can be extended later when we add owner equity tracking
            val financingCashFlow = 0.0
            Result.success(financingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung financing cash flow", e))
        }
    }

    /**
     * Hitung net cash flow
     * Net cash flow = Operating + Investing + Financing cash flows
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
     * Ini adalah placeholder - dalam implementasi nyata, ini akan track cash balance
     */
    suspend fun getCurrentCashPosition(): Result<Double> {
        return try {
            // Placeholder: in real implementation, this would track actual cash balance
            // For now, return 0 as we don't have cash tracking yet
            Result.success(0.0)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal mendapatkan posisi kas", e))
        }
    }
}
