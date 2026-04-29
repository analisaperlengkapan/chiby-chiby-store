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

                // Approved expenses recorded between shift start and close. We subtract
                // every category EXCEPT NON_CASH_CATEGORIES (DEPRECIATION), because any
                // approved expense recorded by this cashier during the shift could
                // plausibly have been paid out of the physical cash drawer.
                //
                // The previous implementation only subtracted OPERATING_EXPENSE_CATEGORIES,
                // excluding INVENTORY_PURCHASES (COGS), EQUIPMENT (investing) and
                // LOAN_REPAYMENT/DIVIDEND (financing) on the assumption they're always
                // paid via bank transfer. That assumption breaks for stores that pay
                // suppliers, equipment vendors or loan installments in cash from the
                // drawer: those cash outflows would not reduce expectedCash, creating a
                // phantom surplus at shift close that could mask theft or skim. A false
                // deficit (when these categories WERE paid by bank) is far easier to
                // surface and explain than a hidden surplus, so the conservative default
                // is to count them. Stores that need finer-grained tracking should add
                // an explicit `paidFromCashDrawer` flag to `Pengeluaran` in a follow-up.
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
                    .filterKeys { it !in KategoriPengeluaran.NON_CASH_CATEGORIES }
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

            // Use `getTotalRevenue` (non-refunded, tax-exclusive) rather than summing
            // `totalAmount` from `getSalesInDateRange`. Two reasons:
            //   1. `getSalesInDateRange` has NO `isRefunded = 0` filter, so refunded
            //      sales would inflate operating revenue even though the cash was
            //      returned to the customer.
            //   2. As of MIGRATION_11_12, `totalAmount` is post-tax (gross cash paid).
            //      Including tax in operating revenue double-counts it against the
            //      tax-exclusive expense categories below, producing an inflated
            //      operating cash flow. Tax collected is a pass-through liability,
            //      not operating revenue.
            // Delegating to the DAO also avoids loading every Penjualan row into memory
            // for what is just a SUM aggregation.
            val revenueResult = penjualanRepository.getTotalRevenue(startDate, endDate)
            if (revenueResult is Result.Failure) throw revenueResult.exception
            val salesRevenue = (revenueResult as Result.Success).data

            // Use the Result-returning variant so a transient DB error on the
            // expense side propagates instead of silently returning an empty map.
            // The non-Result variant (`getApprovedRingkasanPengeluaranPerKategori`)
            // catches exceptions and returns `emptyMap()`, which would treat all
            // expenses as zero and inflate operating cash flow to equal revenue.
            // That asymmetry was previously masked because revenue was also
            // computed from a non-throwing path; now that revenue throws on
            // failure (above), expenses must do the same to avoid producing a
            // misleading "successful" cash flow figure built on partial data.
            val expensesResult = pengeluaranRepository
                .getApprovedRingkasanPengeluaranPerKategoriResult(start, end)
            if (expensesResult is Result.Failure) throw expensesResult.exception
            val approvedExpenses = (expensesResult as Result.Success).data

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
