package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.Supplier
import com.chibychibystore.repository.SupplierRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.SupplierService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierServiceImpl @Inject constructor(
    private val supplierRepository: SupplierRepository,
    private val authService: AuthService
) : SupplierService {

    override fun getAllSuppliers(): Flow<List<Supplier>> {
        return supplierRepository.getAllSuppliers()
    }

    override fun searchSuppliers(query: String): Flow<List<Supplier>> {
        return supplierRepository.searchSuppliers(query)
    }

    override suspend fun getSupplierById(id: Long): Result<Supplier> {
        return supplierRepository.getSupplierById(id)
    }

    override suspend fun createSupplier(name: String, address: String?, phone: String?, email: String?): Result<Long> {
        if (!authService.hasPermission(Permissions.MANAGE_INVENTORY)) {
            return Result.failure(Exception("Tidak memiliki izin untuk menambah pemasok"))
        }
        if (name.isBlank()) {
            return Result.failure(Exception("Nama pemasok tidak boleh kosong"))
        }

        val supplier = Supplier(
            name = name,
            address = address,
            contact = phone // Mapping phone to contact
        )

        return supplierRepository.createSupplier(supplier)
    }

    override suspend fun updateSupplier(id: Long, name: String, address: String?, phone: String?, email: String?): Result<Unit> {
        if (!authService.hasPermission(Permissions.MANAGE_INVENTORY)) {
            return Result.failure(Exception("Tidak memiliki izin untuk mengubah pemasok"))
        }
        if (name.isBlank()) {
            return Result.failure(Exception("Nama pemasok tidak boleh kosong"))
        }

        return supplierRepository.getSupplierById(id).mapCatching { existing ->
            val supplier = existing.copy(
                name = name,
                address = address,
                contact = phone
            )
            supplierRepository.updateSupplier(supplier).getOrThrow()
        }
    }

    override suspend fun deleteSupplier(id: Long): Result<Unit> {
        if (!authService.hasPermission(Permissions.MANAGE_INVENTORY)) {
            return Result.failure(Exception("Tidak memiliki izin untuk menghapus pemasok"))
        }
        return supplierRepository.deleteSupplier(id)
    }
}
