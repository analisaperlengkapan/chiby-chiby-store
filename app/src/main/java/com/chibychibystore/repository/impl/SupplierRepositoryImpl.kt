package com.chibychibystore.repository.impl

import com.chibychibystore.data.local.dao.PemasokDao
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val pemasokDao: PemasokDao
) : SupplierRepository {

    override fun getAllSuppliers(): Flow<List<Pemasok>> = pemasokDao.getAllPemasok()

    override fun searchSuppliers(query: String): Flow<List<Pemasok>> = pemasokDao.searchPemasok(query)

    override suspend fun getSupplierById(id: Long): Result<Pemasok?> = try {
        Result.success(pemasokDao.getPemasokById(id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createSupplier(supplier: Pemasok): Result<Long> = try {
        Result.success(pemasokDao.insertPemasok(supplier))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateSupplier(supplier: Pemasok): Result<Unit> = try {
        pemasokDao.updatePemasok(supplier)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteSupplier(id: Long): Result<Unit> = try {
        pemasokDao.deletePemasokById(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
