package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PengeluaranRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseServiceImpl @Inject constructor(
    private val pengeluaranRepository: PengeluaranRepository
) : ExpenseService {

    override suspend fun createExpense(expense: Pengeluaran): Result<Pengeluaran> {
        val insertResult = pengeluaranRepository.insertPengeluaran(expense)
        return when (insertResult) {
            is com.chibychibystore.data.model.Result.Success -> {
                val id = insertResult.data
                val fetched = pengeluaranRepository.getPengeluaranById(id)
                when (fetched) {
                    is com.chibychibystore.data.model.Result.Success -> com.chibychibystore.data.model.Result.success(fetched.data)
                    is com.chibychibystore.data.model.Result.Failure -> com.chibychibystore.data.model.Result.failure(fetched.exception)
                }
            }
            is com.chibychibystore.data.model.Result.Failure -> com.chibychibystore.data.model.Result.failure(insertResult.exception)
        }
    }

    override suspend fun updateExpense(expense: Pengeluaran): Result<Pengeluaran> {
        return when (val res = pengeluaranRepository.updatePengeluaran(expense)) {
            is Result.Success -> Result.success(expense)
            is Result.Failure -> Result.failure(res.exception)
        }
    }

    override suspend fun deleteExpense(id: Long): Result<Unit> {
        return pengeluaranRepository.deletePengeluaran(id)
    }

    override suspend fun getExpense(id: Long): Result<Pengeluaran?> {
        return pengeluaranRepository.getPengeluaranById(id)
    }

    override suspend fun getExpenses(
        startDate: LocalDate?,
        endDate: LocalDate?,
        category: String?
    ): Result<List<Pengeluaran>> {
        // Implementation of getExpenses with category filtering
        return try {
            val start = startDate ?: LocalDate.now().minusDays(30)
            val end = endDate ?: LocalDate.now()
            val expenses = pengeluaranRepository.getExpensesInDateRange(start, end)
            // Filter by category
            val filtered = category?.let { cat ->
                expenses.filter { it.category.name == cat }
            } ?: expenses
            Result.success(filtered) // Return filtered results
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getExpenses", e))
        }
    }

    override suspend fun getTotalExpenses(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val total = pengeluaranRepository.getTotalExpenseAmount(startDate, endDate).getOrNull() ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalExpenses", e))
        }
    }

    override suspend fun approveExpense(id: Long, approverId: Long): Result<Unit> {
        return pengeluaranRepository.approvePengeluaran(id, approverId)
    }

    override fun observeExpenses(): Flow<List<Pengeluaran>> = pengeluaranRepository.getAllPengeluaran()

    override fun observeExpensesByCategory(category: String): Flow<List<Pengeluaran>> {
        return try {
            pengeluaranRepository.getPengeluaranByCategory(ExpenseCategory.valueOf(category))
        } catch (e: Exception) {
            // In case of invalid category string, return empty flow
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
}
