package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import androidx.room.withTransaction
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.WarehouseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation WarehouseService menggunakan repository
 */
@Singleton
class WarehouseServiceImpl @Inject constructor(
    private val warehouseRepository: GudangRepository,
    private val productRepository: ProdukRepository,
    private val stokGudangRepository: StokGudangRepository,
    private val authService: AuthService,
    private val db: ChibyChibyDatabase
) : WarehouseService {

    override suspend fun createGudang(gudang: Gudang): Result<Gudang> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengelola gudang"))
            }
            
            val idResult = warehouseRepository.createGudang(gudang)
            if (idResult is Result.Failure) return Result.failure(idResult.exception)

            val id = (idResult as Result.Success).data

            val createdResult = warehouseRepository.getGudangById(id)
            if (createdResult is Result.Success && createdResult.data != null) {
                Result.success(createdResult.data)
            } else {
                Result.failure(Exception("Gagal mengambil data gudang setelah dibuat"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGudang(gudang: Gudang): Result<Gudang> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengupdate gudang"))
            }
            val updateResult = warehouseRepository.updateGudang(gudang)
            if (updateResult is Result.Failure) return Result.failure(updateResult.exception)
            Result.success(gudang)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGudang(id: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk menghapus gudang"))
            }
            
            // Logic validation is handled in Repository

            val deleteResult = warehouseRepository.deleteGudang(id)
            if (deleteResult is Result.Failure) return Result.failure(deleteResult.exception)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGudang(id: Long): Result<Gudang?> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat gudang"))
            }
            val result = warehouseRepository.getGudangById(id)
            if (result is Result.Success) {
                Result.success(result.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGudangs(): Result<List<Gudang>> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat daftar gudang"))
            }
            val warehouses = warehouseRepository.getAllGudang().first()
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun tugaskanProdukKeGudang(produkId: Long, gudangId: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengelola lokasi product"))
            }
            
            val warehouseResult = warehouseRepository.getGudangById(gudangId)
            if (warehouseResult is Result.Failure) {
                return Result.failure(Exception("Gudang tidak ditemukan"))
            }

            val productResult = productRepository.getProdukById(produkId)
            if (productResult is Result.Failure) {
                 return Result.failure(Exception("Produk tidak ditemukan"))
            }
            val product = (productResult as Result.Success).data ?: return Result.failure(Exception("Produk tidak ditemukan"))

            val updatedProduk = product.copy(warehouseId = gudangId)
            val updateResult = productRepository.updateProduk(updatedProduk)
            if (updateResult is Result.Failure) return Result.failure(updateResult.exception)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun transferStok(
        produkId: Long,
        dariGudangId: Long,
        keGudangId: Long,
        jumlah: Int
    ): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mentransfer stok"))
            }
            if (jumlah <= 0) {
                return Result.failure(Exception("Jumlah transfer harus lebih dari 0"))
            }

            if (dariGudangId == keGudangId) {
                return Result.failure(Exception("Gudang asal dan tujuan tidak boleh sama"))
            }

            db.withTransaction {
                val productResult = productRepository.getProdukById(produkId)
                if (productResult is Result.Failure) {
                     throw Exception("Produk tidak ditemukan")
                }
                val product = (productResult as Result.Success).data ?: throw Exception("Produk tidak ditemukan")

                // Get source stock
                val sourceStockResult = stokGudangRepository.getStock(produkId, dariGudangId)
                val sourceStock = if (sourceStockResult.isSuccess) sourceStockResult.getOrNull() else null

                // If no stock record, check legacy stock in Product if dariGudangId matches product.warehouseId
                var currentSourceQty = sourceStock?.quantity ?: 0
                if (sourceStock == null && product.warehouseId == dariGudangId) {
                    // Initialize stock record from legacy product data
                    currentSourceQty = product.stockQuantity
                    stokGudangRepository.insertOrUpdateStock(StokGudang(productId = produkId, warehouseId = dariGudangId, quantity = currentSourceQty))
                }

                if (currentSourceQty < jumlah) {
                    throw Exception("Stok gudang asal tidak mencukupi")
                }

                // Update source stock
                val newSourceQty = currentSourceQty - jumlah
                stokGudangRepository.insertOrUpdateStock(StokGudang(productId = produkId, warehouseId = dariGudangId, quantity = newSourceQty))

                // Update target stock
                val targetStockResult = stokGudangRepository.getStock(produkId, keGudangId)
                val targetStock = if (targetStockResult.isSuccess) targetStockResult.getOrNull() else null
                val currentTargetQty = targetStock?.quantity ?: 0
                val newTargetQty = currentTargetQty + jumlah

                stokGudangRepository.insertOrUpdateStock(StokGudang(productId = produkId, warehouseId = keGudangId, quantity = newTargetQty))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStokGudang(gudangId: Long): Result<List<Produk>> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat stok gudang"))
            }
            val products = stokGudangRepository.getProductsByWarehouse(gudangId).first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSemuaStokGudang(): Result<Map<Gudang, List<Produk>>> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat semua stok gudang"))
            }
            val warehouses = warehouseRepository.getAllGudang().first()
            val result = mutableMapOf<Gudang, List<Produk>>()

            for (warehouse in warehouses) {
                val products = stokGudangRepository.getProductsByWarehouse(warehouse.id).first()
                result[warehouse] = products
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeGudangs(): Flow<List<Gudang>> {
        return warehouseRepository.getAllGudang()
    }

    override fun observeStokGudang(gudangId: Long): Flow<List<Produk>> {
        return stokGudangRepository.getProductsByWarehouse(gudangId)
    }
}
