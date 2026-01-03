package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Warehouse
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.repository.WarehouseRepository
import com.chibychibystore.repository.ProductRepository
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
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val authService: AuthService
) : WarehouseService {

    override suspend fun createWarehouse(warehouse: Warehouse): Result<Warehouse> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengelola gudang"))
            }
            // Validasi input handled by repository
            val idResult = warehouseRepository.createWarehouse(warehouse)
            if (idResult is Result.Error) return Result.failure(idResult.exception)

            // Assuming createWarehouse returns Result<Long>
            val id = (idResult as Result.Success).data

            val createdResult = warehouseRepository.getWarehouseById(id)
            if (createdResult is Result.Success && createdResult.data != null) {
                Result.success(createdResult.data)
            } else {
                Result.failure(Exception("Gagal mengambil data gudang setelah dibuat"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWarehouse(warehouse: Warehouse): Result<Warehouse> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengupdate gudang"))
            }
            val updateResult = warehouseRepository.updateWarehouse(warehouse)
            if (updateResult is Result.Error) return Result.failure(updateResult.exception)
            Result.success(warehouse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWarehouse(id: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk menghapus gudang"))
            }
            // Cek apakah gudang masih memiliki produk
            val productsInWarehouse = productRepository.getProductByWarehouse(id).first()
            if (productsInWarehouse.isNotEmpty()) {
                return Result.failure(Exception("Tidak dapat menghapus gudang yang masih memiliki produk"))
            }

            val deleteResult = warehouseRepository.deleteWarehouse(id)
            if (deleteResult is Result.Error) return Result.failure(deleteResult.exception)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWarehouse(id: Long): Result<Warehouse?> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat gudang"))
            }
            val result = warehouseRepository.getWarehouseById(id)
            if (result is Result.Success) {
                Result.success(result.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWarehouses(): Result<List<Warehouse>> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat daftar gudang"))
            }
            val warehouses = warehouseRepository.getAllWarehouses().first()
            Result.success(warehouses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignProductToWarehouse(productId: Long, warehouseId: Long): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mengelola lokasi produk"))
            }
            // Validasi bahwa gudang exists
            val warehouseResult = warehouseRepository.getWarehouseById(warehouseId)
            if (warehouseResult is Result.Error) {
                return Result.failure(Exception("Gudang tidak ditemukan"))
            }

            // Validasi bahwa produk exists
            val productResult = productRepository.getProductById(productId)
            if (productResult is Result.Error) {
                 return Result.failure(Exception("Produk tidak ditemukan"))
            }
            val product = (productResult as Result.Success).data

            // Update warehouseId produk
            val updatedProduct = product.copy(warehouseId = warehouseId)
            val updateResult = productRepository.updateProduct(updatedProduct)
            if (updateResult is Result.Error) return Result.failure(updateResult.exception)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun transferStock(
        productId: Long,
        fromWarehouseId: Long,
        toWarehouseId: Long,
        quantity: Int
    ): Result<Unit> {
        return try {
            if (!authService.hasPermission("MANAGE_WAREHOUSES")) {
                return Result.failure(Exception("Tidak memiliki izin untuk mentransfer stok"))
            }
            if (quantity <= 0) {
                return Result.failure(Exception("Jumlah transfer harus lebih dari 0"))
            }

            if (fromWarehouseId == toWarehouseId) {
                return Result.failure(Exception("Gudang asal dan tujuan tidak boleh sama"))
            }

            // Get product
            val productResult = productRepository.getProductById(productId)
             if (productResult is Result.Error) {
                 return Result.failure(Exception("Produk tidak ditemukan"))
            }
            val product = (productResult as Result.Success).data

            // Validate source warehouse
            if (product.warehouseId != fromWarehouseId) {
                return Result.failure(Exception("Produk tidak berada di gudang asal"))
            }

            // Validate stock
            if (product.stockQuantity < quantity) {
                return Result.failure(Exception("Stok gudang asal tidak mencukupi"))
            }

            if (quantity == product.stockQuantity) {
                // Move entire stock (change warehouse)
                val updatedProduct = product.copy(warehouseId = toWarehouseId)
                val updateResult = productRepository.updateProduct(updatedProduct)
                if (updateResult is Result.Error) return Result.failure(updateResult.exception)
            } else {
                // Partial transfer logic not fully supported due to unique barcode constraint
                 return Result.failure(Exception("Transfer stok sebagian belum didukung karena batasan barcode unik. Silakan transfer seluruh stok."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWarehouseStock(warehouseId: Long): Result<List<Product>> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat stok gudang"))
            }
            val products = productRepository.getProductByWarehouse(warehouseId).first()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllWarehouseStock(): Result<Map<Warehouse, List<Product>>> {
        return try {
            if (!authService.hasPermission("VIEW_INVENTORY")) {
                return Result.failure(Exception("Tidak memiliki izin untuk melihat semua stok gudang"))
            }
            val warehouses = warehouseRepository.getAllWarehouses().first()
            val result = mutableMapOf<Warehouse, List<Product>>()

            for (warehouse in warehouses) {
                val products = productRepository.getProductByWarehouse(warehouse.id).first()
                result[warehouse] = products
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeWarehouses(): Flow<List<Warehouse>> {
        return warehouseRepository.getAllWarehouses()
    }

    override fun observeWarehouseStock(warehouseId: Long): Flow<List<Product>> {
        return productRepository.getProductByWarehouse(warehouseId)
    }
}
