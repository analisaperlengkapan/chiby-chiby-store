package com.chibychibystore.repository

import com.chibychibystore.data.local.entity.Pemasok
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun getAllSuppliers(): Flow<List<Pemasok>>
    fun searchSuppliers(query: String): Flow<List<Pemasok>>
    suspend fun getSupplierById(id: Long): Result<Pemasok?>
    suspend fun createSupplier(supplier: Pemasok): Result<Long>
    suspend fun updateSupplier(supplier: Pemasok): Result<Unit>
    suspend fun deleteSupplier(id: Long): Result<Unit>
}
