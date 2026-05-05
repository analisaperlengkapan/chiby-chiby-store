package com.chibychibystore.service.impl

import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.service.CashManagementService
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceSheetServiceImpl @Inject constructor(
    private val produkRepository: ProdukRepository,
    private val pengeluaranRepository: PengeluaranRepository,
    private val cashManagementService: javax.inject.Provider<CashManagementService>
) : BalanceSheetService {

    override suspend fun calculateTotalAssets(asOfDate: LocalDate): Result<Double> {
        return try {
            val inventoryValueResult = calculateInventoryValue()
            if (inventoryValueResult is Result.Failure) return inventoryValueResult

            val cashResult = cashManagementService.get().getCurrentCashPosition()
            if (cashResult is Result.Failure) return cashResult

            val totalAssets = (cashResult as Result.Success).data + (inventoryValueResult as Result.Success).data
            Result.success(totalAssets)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung total assets", e))
        }
    }

    override suspend fun calculateInventoryValue(): Result<Double> {
        return try {
            produkRepository.getTotalInventoryValue()
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung nilai inventory", e))
        }
    }

    override suspend fun calculateTotalLiabilities(asOfDate: LocalDate): Result<Double> {
        return try {
            val date = Date.from(asOfDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            // Liabilities = Unapproved expenses (Accounts Payable)
            val totalLiabilities = pengeluaranRepository.getUnapprovedPengeluaranTotalBeforeDate(date)

            Result.success(totalLiabilities)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung total liabilities", e))
        }
    }

    override suspend fun calculateEquity(asOfDate: LocalDate): Result<Double> {
        return try {
            val assetsResult = calculateTotalAssets(asOfDate)
            if (assetsResult is Result.Failure) return assetsResult

            val liabilitiesResult = calculateTotalLiabilities(asOfDate)
            if (liabilitiesResult is Result.Failure) return liabilitiesResult

            val equity = (assetsResult as Result.Success).data - (liabilitiesResult as Result.Success).data
            Result.success(equity)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung equity", e))
        }
    }

    override suspend fun generateBalanceSheet(asOfDate: LocalDate): Result<BalanceSheetService.BalanceSheetData> {
        return try {
            val assetsResult = calculateTotalAssets(asOfDate)
            if (assetsResult is Result.Failure) return assetsResult

            val liabilitiesResult = calculateTotalLiabilities(asOfDate)
            if (liabilitiesResult is Result.Failure) return liabilitiesResult

            val inventoryResult = calculateInventoryValue()
            if (inventoryResult is Result.Failure) return inventoryResult

            val cashResult = cashManagementService.get().getCurrentCashPosition()
            if (cashResult is Result.Failure) return cashResult

            val assets = (assetsResult as Result.Success).data
            val liabilities = (liabilitiesResult as Result.Success).data
            val equity = assets - liabilities

            Result.success(BalanceSheetService.BalanceSheetData(
                assets = assets,
                liabilities = liabilities,
                equity = equity,
                inventoryValue = (inventoryResult as Result.Success).data,
                cashBalance = (cashResult as Result.Success).data,
                asOfDate = asOfDate
            ))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal membuat balance sheet", e))
        }
    }

    override suspend fun calculateDebtToEquityRatio(asOfDate: LocalDate): Result<Double> {
        return try {
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0
            val equity = calculateEquity(asOfDate).getOrNull() ?: 0.0

            val ratio = if (equity != 0.0) liabilities / equity else 0.0
            Result.success(ratio)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung debt-to-equity ratio", e))
        }
    }

    override suspend fun calculateCurrentRatio(asOfDate: LocalDate): Result<Double> {
        return try {
            val currentAssets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val currentLiabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0

            val ratio = if (currentLiabilities != 0.0) currentAssets / currentLiabilities else 0.0
            Result.success(ratio)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung current ratio", e))
        }
    }
}
