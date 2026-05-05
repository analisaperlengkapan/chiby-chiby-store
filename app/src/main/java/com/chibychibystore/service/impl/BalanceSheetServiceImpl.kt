package com.chibychibystore.service.impl

import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.BalanceSheetService
import com.chibychibystore.service.CashManagementService
import kotlinx.coroutines.flow.first
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
            val inventoryValue = inventoryValueResult.getOrNull() ?: 0.0

            val cashBalance = cashManagementService.get().getCurrentCashPosition().getOrNull() ?: 0.0

            val totalAssets = cashBalance + inventoryValue

            Result.success(totalAssets)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung total assets", e))
        }
    }

    override suspend fun calculateInventoryValue(): Result<Double> {
        return try {
            val productsFlow = produkRepository.getAllProduk()
            val products = productsFlow.first()
            val inventoryValue = products.sumOf { product -> product.costPrice * product.stockQuantity }

            Result.success(inventoryValue)
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
            val assets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0

            val equity = assets - liabilities
            Result.success(equity)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung equity", e))
        }
    }

    override suspend fun generateBalanceSheet(asOfDate: LocalDate): Result<BalanceSheetService.BalanceSheetData> {
        return try {
            val assets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0
            val equity = assets - liabilities
            val inventoryValue = calculateInventoryValue().getOrNull() ?: 0.0
            val cashBalance = cashManagementService.get().getCurrentCashPosition().getOrNull() ?: 0.0

            Result.success(BalanceSheetService.BalanceSheetData(
                assets = assets,
                liabilities = liabilities,
                equity = equity,
                inventoryValue = inventoryValue,
                cashBalance = cashBalance,
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
