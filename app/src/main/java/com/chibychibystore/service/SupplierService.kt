package com.chibychibystore.service

<<<<<<< HEAD
import com.chibychibystore.data.local.entity.Pemasok
=======
import com.chibychibystore.data.local.entity.Supplier
>>>>>>> feat/ui-overhaul
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

interface SupplierService {
    fun ambilSemuaPemasok(): Flow<List<Pemasok>>
    fun cariPemasok(query: String): Flow<List<Pemasok>>
    suspend fun ambilPemasokBerdasarkanId(id: Long): Result<Pemasok>
    suspend fun buatPemasok(nama: String, alamat: String?, telepon: String?, email: String?): Result<Long>
    suspend fun perbaruiPemasok(id: Long, nama: String, alamat: String?, telepon: String?, email: String?): Result<Unit>
    suspend fun hapusPemasok(id: Long): Result<Unit>
}
