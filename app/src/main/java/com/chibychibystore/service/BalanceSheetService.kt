package com.chibychibystore.service

import com.chibychibystore.data.model.Result
import java.time.LocalDate

/**
 * Interface untuk menghitung neraca (balance sheet) retail
 */
interface BalanceSheetService {

    data class BalanceSheetData(
        val assets: Double,
        val liabilities: Double,
        val equity: Double,
        val inventoryValue: Double,
        val cashBalance: Double,
        val asOfDate: LocalDate
    )

    suspend fun calculateTotalAssets(asOfDate: LocalDate): Result<Double>
    suspend fun calculateInventoryValue(): Result<Double>
    suspend fun calculateTotalLiabilities(asOfDate: LocalDate): Result<Double>
    suspend fun calculateEquity(asOfDate: LocalDate): Result<Double>
    suspend fun generateBalanceSheet(asOfDate: LocalDate): Result<BalanceSheetData>
    suspend fun calculateDebtToEquityRatio(asOfDate: LocalDate): Result<Double>
    suspend fun calculateCurrentRatio(asOfDate: LocalDate): Result<Double>
}
