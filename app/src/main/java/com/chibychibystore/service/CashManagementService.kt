package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface untuk manajemen arus kas retail dan shift kasir
 */
interface CashManagementService {

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

    suspend fun openShift(kasirId: Long, startingCash: Double): Result<Long>
    suspend fun closeShift(shiftId: Long, actualCash: Double, notes: String?): Result<Unit>
    fun observeAllShifts(): Flow<List<Shift>>
    suspend fun getOpenShift(kasirId: Long): Result<Shift?>
    suspend fun calculateOperatingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double>
    suspend fun calculateInvestingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double>
    suspend fun calculateFinancingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double>
    suspend fun calculateNetCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double>
    suspend fun getCashFlowSummary(startDate: LocalDate, endDate: LocalDate): Result<CashFlowSummary>
    suspend fun getCurrentCashPosition(): Result<Double>
}
