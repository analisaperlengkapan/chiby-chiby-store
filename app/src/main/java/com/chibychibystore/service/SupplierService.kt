package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Supplier
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

interface SupplierService {
    fun getAllSuppliers(): Flow<List<Supplier>>
    fun searchSuppliers(query: String): Flow<List<Supplier>>
    suspend fun getSupplierById(id: Long): Result<Supplier>
    suspend fun createSupplier(name: String, address: String?, phone: String?, email: String?): Result<Long>
    suspend fun updateSupplier(id: Long, name: String, address: String?, phone: String?, email: String?): Result<Unit>
    suspend fun deleteSupplier(id: Long): Result<Unit>
}
