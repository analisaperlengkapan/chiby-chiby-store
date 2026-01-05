package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface untuk Pengeluaran Service
 */
interface ExpenseService {
    suspend fun createPengeluaran(pengeluaran: Pengeluaran): Result<Pengeluaran>
    suspend fun updatePengeluaran(pengeluaran: Pengeluaran): Result<Pengeluaran>
    suspend fun deletePengeluaran(id: Long): Result<Unit>
    suspend fun getPengeluaran(id: Long): Result<Pengeluaran?>
    suspend fun getPengeluarans(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        kategori: String? = null
    ): Result<List<Pengeluaran>>
    suspend fun getTotalPengeluarans(startDate: LocalDate, endDate: LocalDate): Result<Double>
    suspend fun approvePengeluaran(id: Long, approverId: Long): Result<Unit>
    fun observePengeluarans(): Flow<List<Pengeluaran>>
    fun observePengeluaransPerKategori(kategori: String): Flow<List<Pengeluaran>>
}
