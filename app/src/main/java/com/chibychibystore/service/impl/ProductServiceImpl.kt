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

            val updatedProdukResult = productRepository.getProdukById(produk.id)
            if (updatedProdukResult is Result.Failure) return Result.failure(updatedProdukResult.exception)
            val updatedProduk = (updatedProdukResult as Result.Success).data ?: return Result.failure(Exception("Produk tidak ditemukan setelah diupdate"))
            
            Result.success(updatedProduk)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduk(id: String): Result<Unit> {
        return try {
            if (!authService.hasPermission(Permissions.EDIT_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.EDIT_INVENTORY))
            }
            val idLong = id.toLongOrNull() ?: return Result.failure(ChibyChibyException.ValidationError("id", "ID Produk tidak valid"))
            productRepository.deleteProduk(idLong)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduk(id: String): Result<Produk?> {
        return try {
            if (!authService.hasPermission(Permissions.VIEW_INVENTORY)) {
                return Result.failure(ChibyChibyException.PermissionError(Permissions.VIEW_INVENTORY))
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
                !searchQuery.isNullOrBlank() -> productRepository.searchProduk(searchQuery)
                categoryId != null -> productRepository.getProdukByKategori(categoryId.toLong())
                warehouseId != null -> stokGudangRepository.getProductsByWarehouse(warehouseId.toLong())
                else -> productRepository.getAllProduk()
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
            Result.success(productRepository.searchProduk(query).first())
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

            // Update both tables
            productRepository.updateStock(idLong, newStock)
            stokGudangRepository.insertOrUpdateStock(
                StokGudang(
                    productId = idLong,
                    warehouseId = product.warehouseId,
                    quantity = newStock
                )
            )
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
            Result.success(productRepository.getLowStockProduk().first())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProduks(): Flow<List<Produk>> {
        return productRepository.getAllProduk()
    }

    override fun observeProduksByKategori(categoryId: String): Flow<List<Produk>> {
        return productRepository.getProdukByKategori(categoryId.toLong())
    }

    override fun observeProduksByGudang(warehouseId: String): Flow<List<Produk>> {
        return stokGudangRepository.getProductsByWarehouse(warehouseId.toLong())
    }

    override fun observeSearchProduks(query: String): Flow<List<Produk>> {
        return productRepository.searchProduk(query)
    }

    override fun observeLowStockProduks(): Flow<List<Produk>> {
        return productRepository.getLowStockProduk()
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
