package com.chibychibystore.service

import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.local.entity.ShiftStatus
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service untuk manajemen arus kas retail dan shift kasir
 */
@Singleton
class CashManagementService @Inject constructor(
    private val penjualanRepository: PenjualanRepository,
    private val pengeluaranRepository: PengeluaranRepository,
    private val shiftRepository: ShiftRepository
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
     * Manajemen Shift
     */

    suspend fun openShift(kasirId: Long, startingCash: Double): Result<Long> {
        val existingResult = shiftRepository.getOpenShiftByKasir(kasirId)
        if (existingResult is Result.Failure) return Result.failure(existingResult.exception)
        val existingOpen = (existingResult as Result.Success).data
        if (existingOpen != null) {
            return Result.failure(ChibyChibyException.BusinessLogicError("Shift sebelumnya belum ditutup"))
        }

        val shift = Shift(
            kasirId = kasirId,
            startingCash = startingCash,
            status = ShiftStatus.OPEN
        )
        return shiftRepository.createShift(shift)
    }

    suspend fun closeShift(shiftId: Long, actualCash: Double, notes: String?): Result<Unit> {
        val shiftResult = shiftRepository.getShiftById(shiftId)
        if (shiftResult is Result.Failure) return Result.failure((shiftResult as Result.Failure).exception)

        val shift = (shiftResult as Result.Success).data ?: return Result.failure(ChibyChibyException.DatabaseError("Shift tidak ditemukan"))
        if (shift.status == ShiftStatus.CLOSED) return Result.failure(ChibyChibyException.BusinessLogicError("Shift sudah ditutup"))

        // Compute totals from sales linked to this shift so the shift's financial
        // summary reflects reality. `totalSales` covers all non-refunded sales for
        // the shift, while `expectedCash` is what the cash drawer *should* contain
        // (starting cash + cash sales) for reconciliation against `actualCash`.
        // Propagate repository failures rather than silently defaulting to 0.0,
        // since this is a financial reconciliation and a hidden DB error would
        // produce a closed shift with bogus zeroed-out totals and a misleading
        // cash discrepancy against `actualCash`.
        val totalSalesResult = penjualanRepository.getTotalSalesByShift(shiftId)
        if (totalSalesResult is Result.Failure) return Result.failure(totalSalesResult.exception)
        val totalSales = (totalSalesResult as Result.Success).data

        val cashSalesResult = penjualanRepository.getTotalCashSalesByShift(shiftId)
        if (cashSalesResult is Result.Failure) return Result.failure(cashSalesResult.exception)
        val cashSales = (cashSalesResult as Result.Success).data

        val expectedCash = shift.startingCash + cashSales

        val closedShift = shift.copy(
            endTime = Date(),
            actualCash = actualCash,
            expectedCash = expectedCash,
            totalSales = totalSales,
            notes = notes,
            status = ShiftStatus.CLOSED
        )
        return shiftRepository.updateShift(closedShift)
    }

    fun observeAllShifts(): Flow<List<Shift>> = shiftRepository.getAllShifts()

    suspend fun getOpenShift(kasirId: Long): Result<Shift?> = shiftRepository.getOpenShiftByKasir(kasirId)

    /**
     * Hitung operating cash flow
     */
    suspend fun calculateOperatingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            val sales = penjualanRepository.getSalesInDateRange(startDate, endDate)
            val salesRevenue = sales.sumOf { it.totalAmount }

            val approvedExpenses = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategori(start, end)

            val operatingExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.OPERATING_EXPENSE_CATEGORIES }
                .values.sum()

            val inventoryPurchases = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.COGS_CATEGORIES }
                .values.sum()

            val operatingCashFlow = salesRevenue - operatingExpenses - inventoryPurchases
            Result.success(operatingCashFlow)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung operating cash flow", e))
        }
    }

    suspend fun calculateInvestingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val approvedExpenses = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategori(start, end)
            val equipmentExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.INVESTING_CATEGORIES }
                .values.sum()
            Result.success(-equipmentExpenses)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung investing cash flow", e))
        }
    }

    suspend fun calculateFinancingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val approvedExpenses = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategori(start, end)
            val financingExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.FINANCING_CATEGORIES }
                .values.sum()
            Result.success(-financingExpenses)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung financing cash flow", e))
        }
    }

    suspend fun calculateNetCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val operatingCF = calculateOperatingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val investingCF = calculateInvestingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val financingCF = calculateFinancingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            Result.success(operatingCF + investingCF + financingCF)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung net cash flow", e))
        }
    }

    suspend fun getCashFlowSummary(startDate: LocalDate, endDate: LocalDate): Result<CashFlowSummary> {
        return try {
            val operatingCF = calculateOperatingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val investingCF = calculateInvestingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val financingCF = calculateFinancingCashFlow(startDate, endDate).getOrNull() ?: 0.0
            val netCF = operatingCF + investingCF + financingCF
            Result.success(CashFlowSummary(operatingCF, investingCF, financingCF, netCF, "$startDate - $endDate"))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal membuat cash flow summary", e))
        }
    }

    suspend fun getCurrentCashPosition(): Result<Double> {
        return Result.success(0.0)
    }
}
