package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.SupplierDao
import com.chibychibystore.data.local.entity.Supplier
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepository @Inject constructor(
    private val supplierDao: SupplierDao
) {

    fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()

    suspend fun getSupplierById(id: Long): Result<Supplier> {
        return try {
            val supplier = supplierDao.getSupplierById(id)
            if (supplier != null) {
                Result.success(supplier)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Supplier with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getSupplierById", e))
        }
    }

    fun searchSuppliers(query: String): Flow<List<Supplier>> = supplierDao.searchSuppliers(query)

    suspend fun createSupplier(supplier: Supplier): Result<Long> {
        return try {
            validateSupplierData(supplier)
            if (supplier.id == 0L) {
                 val id = supplierDao.insertSupplier(supplier)
                 Result.success(id)
            } else {
                supplierDao.updateSupplier(supplier)
                Result.success(supplier.id)
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createSupplier", e))
        }
    }

    // Alias for updating to handle both cases if needed, but keeping separate is cleaner.
    // For simplicity in refactor, createSupplier handles both as 'save' logic or we can add update.
    suspend fun updateSupplier(supplier: Supplier): Result<Unit> {
        return try {
            validateSupplierData(supplier)
            supplierDao.updateSupplier(supplier)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateSupplier", e))
        }
    }

    suspend fun deleteSupplier(id: Long): Result<Unit> {
        return try {
            supplierDao.getSupplierById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Supplier not found"))

            supplierDao.deleteSupplierById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteSupplier", e))
        }
    }

    suspend fun getSupplierCount(): Result<Int> {
        return try {
            val count = supplierDao.getSupplierCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getSupplierCount", e))
        }
    }

    private fun validateSupplierData(supplier: Supplier) {
        if (supplier.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Supplier name cannot be empty")
        }
        if (supplier.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Supplier name must be at least 2 characters")
        }
    }
}
