package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service untuk menghitung neraca (balance sheet) retail
 * Assets = Liabilities + Equity
 */
@Singleton
class BalanceSheetService @Inject constructor(
    private val produkRepository: ProdukRepository,
    private val pengeluaranRepository: PengeluaranRepository,
    private val cashManagementService: CashManagementService
) {

    data class BalanceSheetData(
        val assets: Double,
        val liabilities: Double,
        val equity: Double,
        val inventoryValue: Double,
        val cashBalance: Double,
        val asOfDate: LocalDate
    )

    /**
     * Hitung total assets
     */
    suspend fun calculateTotalAssets(asOfDate: LocalDate): Result<Double> {
        return try {
            val inventoryValueResult = calculateInventoryValue()
            val inventoryValue = inventoryValueResult.getOrNull() ?: 0.0

            val cashBalance = cashManagementService.getCurrentCashPosition().getOrNull() ?: 0.0

            val totalAssets = cashBalance + inventoryValue

            Result.success(totalAssets)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung total assets", e))
        }
    }

    /**
     * Hitung nilai inventory berdasarkan cost
     */
    suspend fun calculateInventoryValue(): Result<Double> {
        return try {
<<<<<<< HEAD
            val products = produkRepository.getAllProduk().first()
=======
            // Get all products and calculate total inventory value
            val products = productRepository.getAllProducts().first()
>>>>>>> feat/ui-overhaul
            val inventoryValue = products.sumOf { product -> product.costPrice * product.stockQuantity }

            Result.success(inventoryValue)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung nilai inventory", e))
        }
    }

    /**
     * Hitung total liabilities
     */
    suspend fun calculateTotalLiabilities(asOfDate: LocalDate): Result<Double> {
        return try {
            val date = Date.from(asOfDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

            // Liabilities = Unapproved expenses (Accounts Payable)
            val totalLiabilities = pengeluaranRepository.getUnapprovedPengeluaranTotalBeforeDate(date)

            Result.success(totalLiabilities)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung total liabilities", e))
        }
    }

    /**
     * Hitung equity (owner's equity)
     */
    suspend fun calculateEquity(asOfDate: LocalDate): Result<Double> {
        return try {
            val assets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0

            val equity = assets - liabilities
            Result.success(equity)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung equity", e))
        }
    }

    /**
     * Generate balance sheet lengkap
     */
    suspend fun generateBalanceSheet(asOfDate: LocalDate): Result<BalanceSheetData> {
        return try {
            val assets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0
            val equity = assets - liabilities
            val inventoryValue = calculateInventoryValue().getOrNull() ?: 0.0
            val cashBalance = cashManagementService.getCurrentCashPosition().getOrNull() ?: 0.0

            Result.success(BalanceSheetData(
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

    /**
     * Hitung debt-to-equity ratio
     */
    suspend fun calculateDebtToEquityRatio(asOfDate: LocalDate): Result<Double> {
        return try {
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0
            val equity = calculateEquity(asOfDate).getOrNull() ?: 0.0

            val ratio = if (equity != 0.0) liabilities / equity else 0.0
            Result.success(ratio)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("Gagal menghitung debt-to-equity ratio", e))
        }
    }

    /**
     * Hitung current ratio (liquidity ratio)
     */
    suspend fun calculateCurrentRatio(asOfDate: LocalDate): Result<Double> {
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