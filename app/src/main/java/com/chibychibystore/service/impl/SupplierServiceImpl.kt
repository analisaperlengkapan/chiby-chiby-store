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
                contact = telepon,
                email = email
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
                contact = telepon,
                email = email
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
        }
    }
}
