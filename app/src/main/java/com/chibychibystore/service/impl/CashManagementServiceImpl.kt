package com.chibychibystore.service.impl

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
import com.chibychibystore.service.CashManagementService
import com.chibychibystore.util.Permissions
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashManagementServiceImpl @Inject constructor(
    private val db: ChibyChibyDatabase,
    private val penjualanRepository: PenjualanRepository,
    private val pengeluaranRepository: PengeluaranRepository,
    private val shiftRepository: ShiftRepository,
    private val authService: com.chibychibystore.service.AuthService
) : CashManagementService {

    override suspend fun openShift(kasirId: Long, startingCash: Double): Result<Long> {
        return try {
            if (startingCash < 0) {
                return Result.failure(ChibyChibyException.ValidationError("startingCash", "Jumlah kas awal tidak boleh negatif"))
            }

            val currentUser = authService.getCurrentUser()
                ?: return Result.failure(ChibyChibyException.BusinessLogicError("Pengguna belum login"))
            if (currentUser.id != kasirId && !authService.hasPermission(Permissions.MANAGE_SHIFTS)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.MANAGE_SHIFTS))
            }

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

    override suspend fun closeShift(shiftId: Long, actualCash: Double, notes: String?): Result<Unit> {
        return try {
            if (actualCash < 0) {
                return Result.failure(ChibyChibyException.ValidationError("actualCash", "Jumlah kas aktual tidak boleh negatif"))
            }

            val currentUser = authService.getCurrentUser()
                ?: return Result.failure(ChibyChibyException.BusinessLogicError("Pengguna belum login"))

            db.withTransaction {
                val shiftResult = shiftRepository.getShiftById(shiftId)
                if (shiftResult is Result.Failure) throw shiftResult.exception

                val shift = (shiftResult as Result.Success).data
                    ?: throw ChibyChibyException.DatabaseError("Shift tidak ditemukan")
                if (shift.status == ShiftStatus.CLOSED) {
                    throw ChibyChibyException.BusinessLogicError("Shift sudah ditutup")
                }

                if (shift.kasirId != currentUser.id && !authService.hasPermission(Permissions.MANAGE_SHIFTS)) {
                    throw ChibyChibyException.PermissionError(Permissions.MANAGE_SHIFTS)
                }

                val totalSalesResult = penjualanRepository.getTotalSalesByShift(shiftId)
                if (totalSalesResult is Result.Failure) throw totalSalesResult.exception
                val totalSales = (totalSalesResult as Result.Success).data

                val cashSalesResult = penjualanRepository.getTotalCashSalesByShift(shiftId)
                if (cashSalesResult is Result.Failure) throw cashSalesResult.exception
                val cashSales = (cashSalesResult as Result.Success).data

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

    override fun observeAllShifts(): Flow<List<Shift>> = shiftRepository.getAllShifts()

    override suspend fun getOpenShift(kasirId: Long): Result<Shift?> = shiftRepository.getOpenShiftByKasir(kasirId)

    override suspend fun calculateOperatingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            val revenueResult = penjualanRepository.getTotalRevenue(startDate, endDate)
            if (revenueResult is Result.Failure) throw revenueResult.exception
            val salesRevenue = (revenueResult as Result.Success).data

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

    override suspend fun calculateInvestingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val expensesResult = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategoriResult(start, end)
            if (expensesResult is Result.Failure) throw expensesResult.exception
            val approvedExpenses = (expensesResult as Result.Success).data
            val equipmentExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.INVESTING_CATEGORIES }
                .values.sum()
            Result.success(-equipmentExpenses)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung investing cash flow", e))
        }
    }

    override suspend fun calculateFinancingCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val end = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val expensesResult = pengeluaranRepository.getApprovedRingkasanPengeluaranPerKategoriResult(start, end)
            if (expensesResult is Result.Failure) throw expensesResult.exception
            val approvedExpenses = (expensesResult as Result.Success).data
            val financingExpenses = approvedExpenses
                .filterKeys { it in KategoriPengeluaran.FINANCING_CATEGORIES }
                .values.sum()
            Result.success(-financingExpenses)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung financing cash flow", e))
        }
    }

    override suspend fun calculateNetCashFlow(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val operatingResult = calculateOperatingCashFlow(startDate, endDate)
            if (operatingResult is Result.Failure) return operatingResult

            val investingResult = calculateInvestingCashFlow(startDate, endDate)
            if (investingResult is Result.Failure) return investingResult

            val financingResult = calculateFinancingCashFlow(startDate, endDate)
            if (financingResult is Result.Failure) return financingResult

            Result.success(
                (operatingResult as Result.Success).data +
                (investingResult as Result.Success).data +
                (financingResult as Result.Success).data
            )
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung net cash flow", e))
        }
    }

    override suspend fun getCashFlowSummary(startDate: LocalDate, endDate: LocalDate): Result<CashManagementService.CashFlowSummary> {
        return try {
            val operatingResult = calculateOperatingCashFlow(startDate, endDate)
            if (operatingResult is Result.Failure) return operatingResult

            val investingResult = calculateInvestingCashFlow(startDate, endDate)
            if (investingResult is Result.Failure) return investingResult

            val financingResult = calculateFinancingCashFlow(startDate, endDate)
            if (financingResult is Result.Failure) return financingResult

            val operatingCF = (operatingResult as Result.Success).data
            val investingCF = (investingResult as Result.Success).data
            val financingCF = (financingResult as Result.Success).data
            val netCF = operatingCF + investingCF + financingCF
            Result.success(CashManagementService.CashFlowSummary(operatingCF, investingCF, financingCF, netCF, "$startDate - $endDate"))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal membuat cash flow summary", e))
        }
    }

    override suspend fun getCurrentCashPosition(): Result<Double> {
        return try {
            val currentUser = authService.getCurrentUser()
                ?: return Result.failure(ChibyChibyException.BusinessLogicError("Pengguna belum login"))

            val openShiftResult = shiftRepository.getOpenShiftByKasir(currentUser.id)
            if (openShiftResult is Result.Failure) throw openShiftResult.exception

            val openShift = (openShiftResult as Result.Success).data
                ?: return Result.success(0.0) // No open shift, cash position is 0 or base it on something else?

            val cashSalesResult = penjualanRepository.getTotalCashSalesByShift(openShift.id)
            if (cashSalesResult is Result.Failure) throw cashSalesResult.exception
            val cashSales = (cashSalesResult as Result.Success).data

            val expensesResult = pengeluaranRepository.getApprovedExpensesByCategoryForCashier(
                openShift.kasirId,
                openShift.startTime,
                Date()
            )
            if (expensesResult is Result.Failure) throw expensesResult.exception
            val expensesByCategory = (expensesResult as Result.Success).data
            val totalCashExpenses = expensesByCategory
                .filterKeys { it !in KategoriPengeluaran.NON_CASH_CATEGORIES }
                .values.sum()

            val currentCash = openShift.startingCash + cashSales - totalCashExpenses
            Result.success(currentCash)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung posisi kas saat ini", e))
        }
    }
}
