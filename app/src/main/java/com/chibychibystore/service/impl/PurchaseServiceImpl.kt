package com.chibychibystore.service.impl

import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.StockAdjustment
import com.chibychibystore.repository.ItemPembelianRepository
import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.PurchaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

@Singleton
class PurchaseServiceImpl @Inject constructor(
    private val db: ChibyChibyDatabase,
    private val pembelianRepository: PembelianRepository,
    private val itemPembelianRepository: ItemPembelianRepository,
    private val stokGudangRepository: StokGudangRepository
) : PurchaseService {

    override suspend fun createPembelian(
        pembelian: Pembelian,
        items: List<ItemPembelian>
    ): Result<Pembelian> {
        return createPembelianWithWarehouse(pembelian, items, pembelian.warehouseId)
    }

    override suspend fun createPembelianWithWarehouse(
        pembelian: Pembelian,
        items: List<ItemPembelian>,
        warehouseId: Long
    ): Result<Pembelian> {
        if (items.isEmpty()) {
            return Result.failure(Exception("Item pembelian tidak boleh kosong"))
        }

        return try {
            db.withTransaction {
                // 1. Create Purchase Header
                val purchaseResult = pembelianRepository.createPembelian(pembelian.copy(warehouseId = warehouseId))
                if (purchaseResult is Result.Failure) throw purchaseResult.exception
                val createdPurchase = (purchaseResult as Result.Success).data

                // 2. Create Purchase Items with the new purchase ID
                val itemsWithId = items.map { it.copy(purchaseId = createdPurchase.id) }
                val itemsResult = itemPembelianRepository.createItemPembelianList(itemsWithId)
                if (itemsResult is Result.Failure) throw itemsResult.exception

                // 3. Update Stock (Increase stock on purchase)
                val adjustments = items.map {
                    StockAdjustment(it.productId, warehouseId, it.quantity)
                }
                val stockResult = stokGudangRepository.adjustStockBatch(adjustments)
                if (stockResult is Result.Failure) throw stockResult.exception

                Result.success(createdPurchase)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPembelianById(id: Long): Result<Pembelian?> {
        return try {
            val result = pembelianRepository.getPembelianById(id)
            if (result is Result.Success) {
                Result.success(result.data)
            } else {
                Result.failure((result as Result.Failure).exception)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPembelianInDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Pembelian>> {
        return try {
            val purchases = pembelianRepository.getPurchasesInDateRange(startDate, endDate)
            Result.success(purchases)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePembelian(id: Long): Result<Unit> {
        return try {
            db.withTransaction {
                val purchaseResult = getPembelianById(id)
                if (purchaseResult is Result.Failure) throw purchaseResult.exception
                val purchase = (purchaseResult as Result.Success).data ?: throw Exception("Pembelian tidak ditemukan")

                val items = itemPembelianRepository.getItemsByPurchaseId(id).first()
                val adjustments = items.map {
                    StockAdjustment(it.productId, purchase.warehouseId, -it.quantity)
                }
                val stockResult = stokGudangRepository.adjustStockBatch(adjustments)
                if (stockResult is Result.Failure) throw stockResult.exception

                val deleteItemsResult = itemPembelianRepository.deleteItemsByPurchaseId(id)
                if (deleteItemsResult is Result.Failure) throw deleteItemsResult.exception

                val deletePurchaseResult = pembelianRepository.deletePembelian(id)
                if (deletePurchaseResult is Result.Failure) throw deletePurchaseResult.exception
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllPurchases(): Flow<List<Pembelian>> {
        return pembelianRepository.getAllPurchases()
    }

    override suspend fun getItemsByPurchaseId(purchaseId: Long): Result<List<ItemPembelian>> {
        return try {
            val items = itemPembelianRepository.getItemsByPurchaseId(purchaseId).first()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
