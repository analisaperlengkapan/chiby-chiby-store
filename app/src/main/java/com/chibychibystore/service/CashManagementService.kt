package com.chibychibystore.service

import androidx.room.withTransaction
import com.chibychibystore.data.local.database.ChibyChibyDatabase
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
    private val db: ChibyChibyDatabase,
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
        return try {
            // Wrap the check-then-create in a single transaction so that two
            // concurrent calls for the same `kasirId` cannot both pass the
            // "no existing open shift" check and create duplicate open shifts.
            // Duplicate open shifts would split sales between them and corrupt
            // the financial reconciliation done at shift close.
            db.withTransaction {
                val existingResult = shiftRepository.getOpenShiftByKasir(kasirId)
                if (existingResult is Result.Failure) throw existingResult.exception
                val existingOpen = (existingResult as Result.Success).data
                if (existingOpen != null) {
                    throw ChibyChibyException.BusinessLogicError("Shift sebelumnya belum ditutup")
                }

                val shift = Shift(
                    kasirId = kasirId,
                    startingCash = startingCash,
                    status = ShiftStatus.OPEN
                )
                val createResult = shiftRepository.createShift(shift)
                if (createResult is Result.Failure) throw createResult.exception
                Result.success((createResult as Result.Success).data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeShift(shiftId: Long, actualCash: Double, notes: String?): Result<Unit> {
        return try {
            // Wrap the read-compute-write sequence in a single transaction so that a
            // concurrent sale being assigned to this shift between the totals query
            // and the final update cannot cause `totalSales` / `expectedCash` to
            // omit that sale.
            db.withTransaction {
                val shiftResult = shiftRepository.getShiftById(shiftId)
                if (shiftResult is Result.Failure) throw shiftResult.exception

                val shift = (shiftResult as Result.Success).data
                    ?: throw ChibyChibyException.DatabaseError("Shift tidak ditemukan")
                if (shift.status == ShiftStatus.CLOSED) {
                    throw ChibyChibyException.BusinessLogicError("Shift sudah ditutup")
                }

                // Compute totals from sales linked to this shift so the shift's financial
                // summary reflects reality. `totalSales` covers all non-refunded sales for
                // the shift, while `expectedCash` is what the cash drawer *should* contain
                // (starting cash + cash sales - cash expenses) for reconciliation against
                // `actualCash`. Propagate repository failures rather than silently defaulting
                // to 0.0, since this is a financial reconciliation and a hidden DB error
                // would produce a closed shift with bogus zeroed-out totals and a misleading
                // cash discrepancy against `actualCash`.
                val totalSalesResult = penjualanRepository.getTotalSalesByShift(shiftId)
                if (totalSalesResult is Result.Failure) throw totalSalesResult.exception
                val totalSales = (totalSalesResult as Result.Success).data

                val cashSalesResult = penjualanRepository.getTotalCashSalesByShift(shiftId)
                if (cashSalesResult is Result.Failure) throw cashSalesResult.exception
                val cashSales = (cashSalesResult as Result.Success).data

                // Approved expenses recorded between shift start and close. Only operating
                // expense categories drain the *physical cash drawer* during a shift —
                // categories like INVENTORY_PURCHASES (COGS), EQUIPMENT (investing),
                // LOAN_REPAYMENT/DIVIDEND (financing) are normally paid via bank transfer
                // and shouldn't reduce expectedCash. DEPRECIATION (non-cash) likewise
                // doesn't affect the drawer. Counting them here would deflate expectedCash
                // and produce phantom cash discrepancies at shift close.
                //
                // `Pengeluaran` has no shift FK, so we approximate by time window. To
                // avoid double-counting across two cashiers with overlapping shifts, we
                // additionally restrict the lookup to expenses *created by this shift's
                // cashier*. This still imperfect (a cashier could record an expense
                // outside their own shift window) but is closer to reality than the
                // unconstrained time-window-only query.
                val closeTime = Date()
                val expensesResult =
                    pengeluaranRepository.getApprovedExpensesByCategoryForCashier(
                        shift.kasirId,
                        shift.startTime,
                        closeTime
                    )
                if (expensesResult is Result.Failure) throw expensesResult.exception
                val expensesByCategory = (expensesResult as Result.Success).data
                val totalExpenses = expensesByCategory
                    .filterKeys { it in KategoriPengeluaran.OPERATING_EXPENSE_CATEGORIES }
                    .values.sum()

                val expectedCash = shift.startingCash + cashSales - totalExpenses

                val closedShift = shift.copy(
                    endTime = closeTime,
                    actualCash = actualCash,
                    expectedCash = expectedCash,
                    totalSales = totalSales,
                    totalExpenses = totalExpenses,
                    notes = notes,
                    status = ShiftStatus.CLOSED
                )
                val updateResult = shiftRepository.updateShift(closedShift)
                if (updateResult is Result.Failure) throw updateResult.exception
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
