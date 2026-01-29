package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PengeluaranRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseServiceImpl @Inject constructor(
    private val expenseRepository: PengeluaranRepository
) : ExpenseService {

    override suspend fun createPengeluaran(pengeluaran: Pengeluaran): Result<Pengeluaran> {
        val insertResult = expenseRepository.createPengeluaran(pengeluaran)
        return when (insertResult) {
            is Result.Success -> {
                val id = insertResult.data
                val fetched = expenseRepository.getPengeluaranById(id)
                when (fetched) {
                    is Result.Success -> Result.success(fetched.data ?: pengeluaran.copy(id = id))
                    is Result.Failure -> Result.failure(fetched.exception)
                }
            }
            is Result.Failure -> Result.failure(insertResult.exception)
        }
    }

    override suspend fun updatePengeluaran(pengeluaran: Pengeluaran): Result<Pengeluaran> {
        return when (val res = expenseRepository.updatePengeluaran(pengeluaran)) {
            is Result.Success -> Result.success(pengeluaran)
            is Result.Failure -> Result.failure(res.exception)
        }
    }

    override suspend fun deletePengeluaran(id: Long): Result<Unit> {
        return expenseRepository.deletePengeluaran(id)
    }

    override suspend fun getPengeluaran(id: Long): Result<Pengeluaran?> {
        return expenseRepository.getPengeluaranById(id)
    }

    override suspend fun getPengeluarans(
        startDate: LocalDate?,
        endDate: LocalDate?,
        kategori: String?
    ): Result<List<Pengeluaran>> {
        return try {
            val start = startDate ?: LocalDate.now().minusDays(30)
            val end = endDate ?: LocalDate.now()

            val startDateObj = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val endDateObj = Date.from(end.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toInstant())

            val expenses = if (kategori != null) {
                try {
                    val catEnum = KategoriPengeluaran.valueOf(kategori)
                    expenseRepository.getPengeluaransByDateRangeAndKategori(startDateObj, endDateObj, catEnum)
                } catch (e: Exception) {
                     expenseRepository.getPengeluaransByDateRangeList(startDateObj, endDateObj)
                }
            } else {
                expenseRepository.getPengeluaransByDateRangeList(startDateObj, endDateObj)
            }

            Result.success(expenses)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getPengeluarans", e))
        }
    }

    override suspend fun getTotalPengeluarans(startDate: LocalDate, endDate: LocalDate): Result<Double> {
        return try {
            val startDateObj = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val endDateObj = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toInstant())

            val total = expenseRepository.getTotalPengeluaranAmount(startDateObj, endDateObj).getOrNull() ?: 0.0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalPengeluarans", e))
        }
    }

    override suspend fun approvePengeluaran(id: Long, approverId: Long): Result<Unit> {
        return expenseRepository.approvePengeluaran(id, approverId)
    }

    override fun observePengeluarans(): Flow<List<Pengeluaran>> = expenseRepository.getAllPengeluarans()

    override fun observePengeluaransPerKategori(kategori: String): Flow<List<Pengeluaran>> {
        return try {
            expenseRepository.getPengeluaransByKategori(KategoriPengeluaran.valueOf(kategori))
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
}
