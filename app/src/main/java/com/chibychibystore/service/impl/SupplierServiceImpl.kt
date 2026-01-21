package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.repository.PemasokRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.SupplierService
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierServiceImpl @Inject constructor(
    private val supplierRepository: PemasokRepository,
    private val authService: AuthService
) : SupplierService {

    override fun ambilSemuaPemasok(): Flow<List<Pemasok>> {
        return supplierRepository.getAllPemasok()
    }

    override fun cariPemasok(query: String): Flow<List<Pemasok>> {
        return supplierRepository.searchPemasok(query)
    }

    override suspend fun ambilPemasokBerdasarkanId(id: Long): Result<Pemasok> {
        return supplierRepository.getPemasokById(id)
    }

<<<<<<< HEAD
    override suspend fun buatPemasok(nama: String, alamat: String?, telepon: String?, email: String?): Result<Long> {
        return try {
            if (!authService.hasPermission("MANAGE_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk menambah pemasok"))
            }
            if (nama.isBlank()) {
                return Result.failure(Exception("Nama pemasok tidak boleh kosong"))
            }

            val supplier = Pemasok(
                name = nama,
                address = alamat,
                contact = telepon
            )

            supplierRepository.createPemasok(supplier)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun perbaruiPemasok(id: Long, nama: String, alamat: String?, telepon: String?, email: String?): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengubah pemasok"))
            }
            if (nama.isBlank()) {
                return Result.failure(Exception("Nama pemasok tidak boleh kosong"))
            }

            val existingResult = supplierRepository.getPemasokById(id)
            if (existingResult is Result.Failure) return Result.failure(existingResult.exception)
            val existing = (existingResult as Result.Success).data ?: return Result.failure(Exception("Pemasok tidak ditemukan"))

            val supplier = existing.copy(
                name = nama,
                address = alamat,
                contact = telepon
            )
            val updateResult = supplierRepository.updatePemasok(supplier)
            if (updateResult is Result.Failure) return Result.failure(updateResult.exception)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hapusPemasok(id: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk menghapus pemasok"))
            }
            supplierRepository.deletePemasok(id)
        } catch (e: Exception) {
            Result.failure(e)
=======
    override suspend fun createSupplier(name: String, address: String?, phone: String?, email: String?): Result<Long> {
        if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
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
        if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
            return Result.failure(Exception("Tidak memiliki izin untuk mengubah pemasok"))
        }
        if (name.isBlank()) {
            return Result.failure(Exception("Nama pemasok tidak boleh kosong"))
        }

        val res = supplierRepository.getSupplierById(id)
        return if (res.isSuccess) {
            val existing = res.getOrNull()!!
            val supplier = existing.copy(
                name = name,
                address = address,
                contact = phone
            )
            supplierRepository.updateSupplier(supplier)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Gagal mendapatkan data pemasok"))
        }
    }

    override suspend fun deleteSupplier(id: Long): Result<Unit> {
        if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
            return Result.failure(Exception("Tidak memiliki izin untuk menghapus pemasok"))
>>>>>>> feat/ui-overhaul
        }
    }
}
