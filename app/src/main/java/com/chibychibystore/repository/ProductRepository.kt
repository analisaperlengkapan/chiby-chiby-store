package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ProductDao
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): Result<Product> {
        return try {
            val product = productDao.getProductById(id)
            if (product != null) {
                Result.success(product)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Product with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProductById", e))
        }
    }

    suspend fun getProductByIds(ids: List<Long>): Result<List<Product>> {
        return try {
            val productList = productDao.getProductByIds(ids)
            Result.success(productList)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProductByIds", e))
        }
    }

    suspend fun getProduct(id: Long): Product? {
        return try {
            val res = getProductById(id)
            res.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductByBarcode(barcode: String): Result<Product> {
        return try {
            val product = productDao.getProductByBarcode(barcode)
            if (product != null) {
                Result.success(product)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Product with barcode $barcode not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProductByBarcode", e))
        }
    }

    fun getProductByCategory(categoryId: Long): Flow<List<Product>> = productDao.getProductByCategory(categoryId)

    fun getProductByWarehouse(warehouseId: Long): Flow<List<Product>> = productDao.getProductByWarehouse(warehouseId)

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    fun getLowStockProducts(): Flow<List<Product>> = productDao.getLowStockProducts()

    fun getOutOfStockProducts(): Flow<List<Product>> = productDao.getOutOfStockProducts()

    suspend fun createProduct(product: Product): Result<Long> {
        return try {
            val id = productDao.insertProduct(product)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createProduct", e))
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            productDao.updateProduct(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateProduct", e))
        }
    }

    suspend fun adjustStock(id: Long, quantity: Int): Result<Unit> {
        return try {
            productDao.adjustStock(id, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("adjustStock", e))
        }
    }

    suspend fun setStock(id: Long, quantity: Int): Result<Unit> {
        return try {
            productDao.setStock(id, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("setStock", e))
        }
    }

    suspend fun updateStock(id: Long, quantity: Int): Result<Unit> {
        return adjustStock(id, quantity)
    }

    suspend fun deleteProduct(id: Long): Result<Unit> {
        return try {
            val product = productDao.getProductById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Product not found"))

            productDao.deleteProductById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteProduct", e))
        }
    }

    suspend fun getProductCount(): Result<Int> {
        return try {
            val count = productDao.getProductCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getProductCount", e))
        }
    }

    suspend fun getTotalStock(): Result<Int> {
        return try {
            val total = productDao.getTotalStock() ?: 0
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalStock", e))
        }
    }

    suspend fun countLowStock(): Result<Int> {
        return try {
            val count = productDao.countLowStock()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countLowStock", e))
        }
    }

    suspend fun countOutOfStock(): Result<Int> {
        return try {
            val count = productDao.countOutOfStock()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("countOutOfStock", e))
        }
    }

    suspend fun getTotalInventoryValue(): Result<Double> {
        return try {
            val value = productDao.getTotalInventoryValue() ?: 0.0
            Result.success(value)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getTotalInventoryValue", e))
        }
    }
}
