package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pemasok
import kotlinx.coroutines.flow.Flow

interface SupplierService {
    fun getAllSuppliers(): Flow<List<Pemasok>>
    fun searchSuppliers(query: String): Flow<List<Pemasok>>
    suspend fun getSupplierById(id: Long): Result<Pemasok>
    suspend fun createSupplier(name: String, address: String?, phone: String?, email: String?): Result<Long>
    suspend fun updateSupplier(id: Long, name: String, address: String?, phone: String?, email: String?): Result<Unit>
    suspend fun deleteSupplier(id: Long): Result<Unit>
}
