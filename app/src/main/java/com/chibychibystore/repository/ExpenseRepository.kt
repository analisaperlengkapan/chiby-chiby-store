package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ExpenseDao
import com.chibychibystore.data.local.entity.Expense
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun getExpenseById(id: Long): Result<Expense> {
        return try {
            val expense = expenseDao.getExpenseById(id)
            if (expense != null) {
                Result.success(expense)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Expense with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getExpenseById", e))
        }
    }

    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    suspend fun createExpense(expense: Expense): Result<Long> {
        return try {
            val id = expenseDao.insertExpense(expense)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createExpense", e))
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.getExpenseById(expense.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Expense not found"))

            expenseDao.updateExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateExpense", e))
        }
    }

    suspend fun deleteExpense(id: Long): Result<Unit> {
        return try {
            expenseDao.getExpenseById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Expense not found"))

            expenseDao.deleteExpenseById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteExpense", e))
        }
    }

    suspend fun getTotalExpense(startDate: Date, endDate: Date): Result<Double> {
        return try {
            val total = expenseDao.getTotalExpense(startDate, endDate) ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalExpense", e))
        }
    }
}
