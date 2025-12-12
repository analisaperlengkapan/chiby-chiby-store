package com.chibychibystore.service

import com.chibychibystore.data.Result
import com.chibychibystore.repository.ProdukRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service untuk menghitung neraca (balance sheet) retail
 * Assets = Liabilities + Equity
 */
@Singleton
class BalanceSheetService @Inject constructor(
    private val productRepository: ProdukRepository,
    private val cashManagementService: CashManagementService
) {

    /**
     * Hitung total assets
     * Assets = Cash + Inventory Value + Other assets
     */
    suspend fun calculateTotalAssets(asOfDate: LocalDate): Result<Double> {
        return try {
            // Get inventory value (cost basis)
            val inventoryValue = calculateInventoryValue()

            // Get cash position
            val cashBalance = cashManagementService.getCurrentCashPosition().getOrNull() ?: 0.0

            // For retail business, main assets are inventory and cash
            // Other assets (equipment, etc.) not tracked yet
            val totalAssets = cashBalance + inventoryValue

            Result.Success(totalAssets)
        } catch (e: Exception) {
            Result.Error("Gagal menghitung total assets: ${e.message}")
        }
    }

    /**
     * Hitung nilai inventory berdasarkan cost
     */
    suspend fun calculateInventoryValue(): Result<Double> {
        return try {
            // Get all products and calculate total inventory value
            val products = productRepository.getAllProduk()
            val inventoryValue = products.sumOf { it.costPrice * it.stockQuantity }

            Result.Success(inventoryValue)
        } catch (e: Exception) {
            Result.Error("Gagal menghitung nilai inventory: ${e.message}")
        }
    }

    /**
     * Hitung total liabilities
     * Liabilities = Accounts payable + Other liabilities
     * Untuk retail sederhana, ini terutama hutang supplier
     */
    suspend fun calculateTotalLiabilities(asOfDate: LocalDate): Result<Double> {
        return try {
            // For now, liabilities are not tracked
            // In real implementation, this would include:
            // - Accounts payable (supplier debts)
            // - Bank loans
            // - Other short/long term liabilities
            val totalLiabilities = 0.0

            Result.Success(totalLiabilities)
        } catch (e: Exception) {
            Result.Error("Gagal menghitung total liabilities: ${e.message}")
        }
    }

    /**
     * Hitung equity (owner's equity)
     * Equity = Assets - Liabilities = Retained earnings + Owner investments
     */
    suspend fun calculateEquity(asOfDate: LocalDate): Result<Double> {
        return try {
            val assets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0

            val equity = assets - liabilities
            Result.Success(equity)
        } catch (e: Exception) {
            Result.Error("Gagal menghitung equity: ${e.message}")
        }
    }

    /**
     * Generate balance sheet lengkap
     */
    suspend fun generateBalanceSheet(asOfDate: LocalDate): Result<ReportingService.BalanceSheet> {
        return try {
            val assets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0
            val equity = assets - liabilities
            val inventoryValue = calculateInventoryValue().getOrNull() ?: 0.0
            val cashBalance = cashManagementService.getCurrentCashPosition().getOrNull() ?: 0.0

            Result.Success(ReportingService.BalanceSheet(
                assets = assets,
                liabilities = liabilities,
                equity = equity,
                inventoryValue = inventoryValue,
                cashBalance = cashBalance,
                asOfDate = asOfDate
            ))
        } catch (e: Exception) {
            Result.Error("Gagal membuat balance sheet: ${e.message}")
        }
    }

    /**
     * Hitung debt-to-equity ratio
     * Formula: Total Liabilities / Total Equity
     */
    suspend fun calculateDebtToEquityRatio(asOfDate: LocalDate): Result<Double> {
        return try {
            val liabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0
            val equity = calculateEquity(asOfDate).getOrNull() ?: 0.0

            val ratio = if (equity != 0.0) liabilities / equity else 0.0
            Result.Success(ratio)
        } catch (e: Exception) {
            Result.Error("Gagal menghitung debt-to-equity ratio: ${e.message}")
        }
    }

    /**
     * Hitung current ratio (liquidity ratio)
     * Formula: Current Assets / Current Liabilities
     * Untuk retail: (Cash + Inventory) / Current Liabilities
     */
    suspend fun calculateCurrentRatio(asOfDate: LocalDate): Result<Double> {
        return try {
            val currentAssets = calculateTotalAssets(asOfDate).getOrNull() ?: 0.0
            val currentLiabilities = calculateTotalLiabilities(asOfDate).getOrNull() ?: 0.0

            val ratio = if (currentLiabilities != 0.0) currentAssets / currentLiabilities else 0.0
            Result.Success(ratio)
        } catch (e: Exception) {
            Result.Error("Gagal menghitung current ratio: ${e.message}")
        }
    }
}