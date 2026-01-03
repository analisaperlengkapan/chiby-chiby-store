package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Expense
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseServiceImpl @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ExpenseService {

    override suspend fun createExpense(expense: Expense): Result<Expense> {
        val insertResult = expenseRepository.createExpense(expense)
        return when (insertResult) {
            is Result.Success -> {
                val id = insertResult.data
                val fetched = expenseRepository.getExpenseById(id)
                when (fetched) {
                    is Result.Success -> Result.success(fetched.data)
                    is Result.Failure -> Result.failure(fetched.exception)
                }
            }
            is Result.Failure -> Result.failure(insertResult.exception)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        return when (val res = expenseRepository.updateExpense(expense)) {
            is Result.Success -> Result.success(expense)
            is Result.Failure -> Result.failure(res.exception)
        }
    }

    override suspend fun deleteExpense(id: Long): Result<Unit> {
        return expenseRepository.deleteExpense(id)
    }

    override suspend fun getExpense(id: Long): Result<Expense?> {
        val result = expenseRepository.getExpenseById(id)
        return if (result.isSuccess) Result.success(result.getOrNull()) else Result.failure(result.exceptionOrNull()!!)
    }

    override suspend fun getExpenses(
        startDate: LocalDate?,
        endDate: LocalDate?,
        category: String?
    ): Result<List<Expense>> {
        return try {
            val start = startDate ?: LocalDate.now().minusDays(30)
            val end = endDate ?: LocalDate.now()

            // Convert LocalDate to Date for Room
            val startDateObj = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val endDateObj = Date.from(end.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toInstant()) // End of day

            val expenses = if (category != null) {
                try {
                    val catEnum = ExpenseCategory.valueOf(category)
                    expenseRepository.getExpensesByDateRangeAndCategory(startDateObj, endDateObj, catEnum)
                } catch (e: Exception) {
                    // Invalid category, fallback to all (or empty?) - assuming ignoring invalid category
                     expenseRepository.getExpensesByDateRangeList(startDateObj, endDateObj)
                }
            } else {
                expenseRepository.getExpensesByDateRangeList(startDateObj, endDateObj)
            }

            Result.success(expenses)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getExpenses", e))
        }
    }

    override suspend fun getTotalExpenses(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
             // Convert LocalDate to Date
            val startDateObj = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val endDateObj = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toInstant())

            val total = expenseRepository.getTotalExpenseAmount(startDateObj, endDateObj).getOrNull() ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalExpenses", e))
        }
    }

    override suspend fun approveExpense(id: Long, approverId: Long): Result<Unit> {
        return expenseRepository.approveExpense(id, approverId)
    }

    override fun observeExpenses(): Flow<List<Expense>> = expenseRepository.getAllExpenses()

    override fun observeExpensesByCategory(category: String): Flow<List<Expense>> {
        return try {
            expenseRepository.getExpensesByCategory(ExpenseCategory.valueOf(category))
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
}
