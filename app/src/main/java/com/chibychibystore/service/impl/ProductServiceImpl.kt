package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.ProductService
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductServiceImpl @Inject constructor(
    private val productRepository: ProdukRepository,
    private val stokGudangRepository: StokGudangRepository,
    private val authService: AuthService
) : ProductService {

    override suspend fun createProduk(produk: Produk): Result<Produk> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.EDIT_INVENTORY))
            }
            validateProduk(produk)

            val createResult = productRepository.createProduk(produk)
            if (createResult is Result.Failure) return Result.failure(createResult.exception)
            val createdProdukId = (createResult as Result.Success).data

<<<<<<< HEAD
            // Initialize stock for the product's warehouse
            stokGudangRepository.insertOrUpdateStock(
                StokGudang(
                    productId = createdProdukId,
                    warehouseId = produk.warehouseId,
                    quantity = produk.stockQuantity
                )
            )

            val createdProdukResult = productRepository.getProdukById(createdProdukId)
            if (createdProdukResult is Result.Failure) return Result.failure(createdProdukResult.exception)
            val createdProduk = (createdProdukResult as Result.Success).data ?: return Result.failure(Exception("Produk tidak ditemukan setelah dibuat"))
            
            Result.success(createdProduk)
=======
            try {
                val createResult = productRepository.createProduct(product)
                val createdProductId = (createResult as? Result.Success)?.data ?: return Result.failure((createResult as? Result.Failure)?.exception ?: Exception("Gagal membuat product"))

                val createdProductResult = productRepository.getProductById(createdProductId)
                val createdProduct = (createdProductResult as? Result.Success)?.data ?: return Result.failure((createdProductResult as? Result.Failure)?.exception ?: Exception("Gagal mengambil product yang dibuat"))
                Result.success(createdProduct)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Result.failure(Exception("Barcode sudah digunakan (Constraint Error)"))
            } catch (e: Exception) {
                 // Check message for constraint violation if SQLiteConstraintException isn't caught directly (wrapper issues)
                if (e.message?.contains("constraint", ignoreCase = true) == true) {
                    Result.failure(Exception("Barcode sudah digunakan"))
                } else {
                    throw e
                }
            }
>>>>>>> feat/ui-overhaul
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduk(produk: Produk): Result<Produk> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.EDIT_INVENTORY))
            }
            validateProduk(produk)

            val updateResult = productRepository.updateProduk(produk)
            if (updateResult is Result.Failure) return Result.failure(updateResult.exception)

<<<<<<< HEAD
            val updatedProdukResult = productRepository.getProdukById(produk.id)
            if (updatedProdukResult is Result.Failure) return Result.failure(updatedProdukResult.exception)
            val updatedProduk = (updatedProdukResult as Result.Success).data ?: return Result.failure(Exception("Produk tidak ditemukan setelah diupdate"))
            
            Result.success(updatedProduk)
=======
            val updateResult = productRepository.updateProduct(product)
            if (updateResult is Result.Failure) return Result.failure(updateResult.exception)

            val updatedProductResult = productRepository.getProductById(product.id)
            val updatedProduct = (updatedProductResult as? Result.Success)?.data ?: return Result.failure((updatedProductResult as? Result.Failure)?.exception ?: Exception("Gagal mengambil product yang diupdate"))
            Result.success(updatedProduct)
>>>>>>> feat/ui-overhaul
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduk(id: String): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.EDIT_INVENTORY))
            }
<<<<<<< HEAD
            val idLong = id.toLongOrNull() ?: return Result.failure(ChibyChibyException.ValidationError("id", "ID Produk tidak valid"))
            productRepository.deleteProduk(idLong)
=======
            val idLong = id.toLongOrNull() ?: return Result.failure(Exception("ID Product tidak valid: $id"))
            val deleteResult = productRepository.deleteProduct(idLong)
            if (deleteResult is Result.Failure) return Result.failure(deleteResult.exception)
            Result.success(Unit)
>>>>>>> feat/ui-overhaul
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduk(id: String): Result<Produk?> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
<<<<<<< HEAD
                return Result.failure(ChibyChibyException.PermissionError(Permissions.VIEW_INVENTORY))
=======
                return Result.failure(Exception("Tidak memiliki izin untuk melihat inventory"))
            }
            val idLong = id.toLongOrNull() ?: return Result.failure(Exception("ID Product tidak valid: $id"))
            val result = productRepository.getProductById(idLong)
            when (result) {
                is Result.Success -> Result.success(result.data)
                is Result.Failure -> Result.success(null) // Return null if not found, or propagate error? Interface says Result<Product?>
>>>>>>> feat/ui-overhaul
            }
            val idLong = id.toLongOrNull() ?: return Result.failure(ChibyChibyException.ValidationError("id", "ID Produk tidak valid"))
            productRepository.getProdukById(idLong)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduks(
        categoryId: String?,
        warehouseId: String?,
        searchQuery: String?
    ): Result<List<Produk>> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.VIEW_INVENTORY))
            }
            val flow = when {
<<<<<<< HEAD
                !searchQuery.isNullOrBlank() -> productRepository.searchProduk(searchQuery)
                categoryId != null -> productRepository.getProdukByKategori(categoryId.toLong())
                warehouseId != null -> stokGudangRepository.getProductsByWarehouse(warehouseId.toLong())
                else -> productRepository.getAllProduk()
=======
                !searchQuery.isNullOrBlank() -> productRepository.searchProducts(searchQuery)
                categoryId != null -> productRepository.getProductByCategory(categoryId.toLong())
                warehouseId != null -> productRepository.getProductByWarehouse(warehouseId.toLong())
                else -> productRepository.getAllProducts()
>>>>>>> feat/ui-overhaul
            }
            Result.success(flow.first())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProduks(query: String): Result<List<Produk>> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.VIEW_INVENTORY))
            }
<<<<<<< HEAD
            Result.success(productRepository.searchProduk(query).first())
=======
            val products = productRepository.searchProducts(query).first()
            Result.success(products)
>>>>>>> feat/ui-overhaul
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStock(productId: String, newStock: Int): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.EDIT_INVENTORY))
            }
            val idLong = productId.toLongOrNull() ?: return Result.failure(ChibyChibyException.ValidationError("id", "ID Produk tidak valid"))
            
            val productResult = productRepository.getProdukById(idLong)
            if (productResult is Result.Failure) return Result.failure(productResult.exception)
            val product = (productResult as Result.Success).data ?: return Result.failure(Exception("Produk tidak ditemukan"))

<<<<<<< HEAD
            // Update both tables
            productRepository.updateStock(idLong, newStock)
            stokGudangRepository.insertOrUpdateStock(
                StokGudang(
                    productId = idLong,
                    warehouseId = product.warehouseId,
                    quantity = newStock
                )
            )
=======
            val idLong = productId.toLongOrNull() ?: return Result.failure(Exception("ID Product tidak valid: $productId"))

            if (newStock < 0) {
                return Result.failure(Exception("Stok tidak boleh negatif"))
            }

            // Ensure product exists
            val productResult = productRepository.getProductById(idLong)
            if (productResult is Result.Failure) {
                return Result.failure(Exception("Product tidak ditemukan"))
            }

            // Perform atomic update (Set Absolute Stock)
            val stockUpdateResult = productRepository.setStock(idLong, newStock)
            if (stockUpdateResult is Result.Failure) return Result.failure(stockUpdateResult.exception)
>>>>>>> feat/ui-overhaul
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLowStockProduks(): Result<List<Produk>> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.VIEW_INVENTORY))
            }
<<<<<<< HEAD
            Result.success(productRepository.getLowStockProduk().first())
=======
            val products = productRepository.getLowStockProducts().first()
            Result.success(products)
>>>>>>> feat/ui-overhaul
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

<<<<<<< HEAD
    override fun observeProduks(): Flow<List<Produk>> {
        return productRepository.getAllProduk()
=======
    override fun observeProducts(): Flow<List<Product>> {
        return productRepository.getAllProducts()
>>>>>>> feat/ui-overhaul
    }

    override fun observeProduksByKategori(categoryId: String): Flow<List<Produk>> {
        return productRepository.getProdukByKategori(categoryId.toLong())
    }

    override fun observeProduksByGudang(warehouseId: String): Flow<List<Produk>> {
        return stokGudangRepository.getProductsByWarehouse(warehouseId.toLong())
    }

<<<<<<< HEAD
    override fun observeSearchProduks(query: String): Flow<List<Produk>> {
        return productRepository.searchProduk(query)
    }

    override fun observeLowStockProduks(): Flow<List<Produk>> {
        return productRepository.getLowStockProduk()
=======
    override fun observeSearchProducts(query: String): Flow<List<Product>> {
        return productRepository.searchProducts(query)
    }

    override fun observeLowStockProducts(): Flow<List<Product>> {
        return productRepository.getLowStockProducts()
>>>>>>> feat/ui-overhaul
    }

    override suspend fun getProductByBarcode(barcode: String): Result<Produk?> {
        return try {
            val result = productRepository.getProdukByBarcode(barcode)
            if (result.isSuccess) {
                Result.success(result.getOrNull())
            } else {
                // Barcode not found is not an error, return null
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateProduk(produk: Produk) {
        if (produk.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Nama produk tidak boleh kosong")
        }
    }
}
