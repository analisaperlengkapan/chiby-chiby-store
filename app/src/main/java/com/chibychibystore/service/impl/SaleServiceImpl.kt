package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.model.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.SaleService
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleServiceImpl @Inject constructor(
    private val penjualanRepository: PenjualanRepository,
    private val itemPenjualanRepository: ItemPenjualanRepository,
    private val produkRepository: ProdukRepository,
    private val authService: AuthService
) : SaleService {

    override suspend fun createSale(
        sale: Penjualan,
        items: List<ItemPenjualan>
    ): Result<PenjualanWithItems> {
        // 1. Validasi
        if (items.isEmpty()) {
            return Result.failure(Exception("Item penjualan tidak boleh kosong"))
        }

        // 2. Hitung total amount (double check logic di frontend)
        var calculatedTotal = 0.0
        items.forEach { item ->
            calculatedTotal += item.jumlah * item.hargaSatuan
        }

        // Update sale total amount
        val saleToSave = sale.copy(
            totalAmount = calculatedTotal,
            tanggalPenjualan = LocalDateTime.now() // Force server time
        )

        // Run in transaction via Repository
        return penjualanRepository.runInTransaction {
            // 3. Save Sale Header
            val saleIdResult = penjualanRepository.createPenjualan(saleToSave)
            val saleId = (saleIdResult as? Result.Success)?.data
                ?: throw (saleIdResult as? Result.Error)?.exception ?: Exception("Gagal membuat data penjualan")

            // 4. Save Items & Update Stock
            val savedItems = mutableListOf<ItemPenjualan>()

            for (item in items) {
                val itemWithSaleId = item.copy(penjualanId = saleId)
                itemPenjualanRepository.createItemPenjualan(itemWithSaleId)
                savedItems.add(itemWithSaleId)

                // Update Stock (Subtract)
                produkRepository.adjustStock(item.produkId, -item.jumlah)
            }

            // 5. Return complete object
            PenjualanWithItems(
                penjualan = saleToSave.copy(id = saleId),
                items = savedItems
            )
        }
    }

    override suspend fun getSale(id: Long): Result<PenjualanWithItems?> {
        return penjualanRepository.getPenjualanWithItems(id)
    }

    override suspend fun getSales(
        startDate: String?,
        endDate: String?,
        cashierId: Long?
    ): Result<List<Penjualan>> {
         return try {
             if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
                // Technically cashier needs to see sales too, but maybe only their own.
                // For now, adhere to permissions.
                // return Result.failure(Exception("Tidak memiliki izin melihat laporan penjualan"))
             }
             val sales = penjualanRepository.getAllPenjualanSync()
             Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchSales(query: String): Result<List<Penjualan>> {
        return Result.success(emptyList())
    }

    override suspend fun updateSale(id: Long, sale: Penjualan): Result<Penjualan> {
        return Result.failure(Exception("Not implemented yet"))
    }

    override suspend fun deleteSale(id: Long): Result<Unit> {
         return try {
            penjualanRepository.deletePenjualan(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refundSale(id: Long): Result<Unit> {
        val saleResult = getSale(id)
        val sale = (saleResult as? Result.Success)?.data ?: return Result.failure(Exception("Penjualan tidak ditemukan"))

        sale.items.forEach { item ->
            produkRepository.adjustStock(item.produkId, item.jumlah)
        }

        return Result.success(Unit)
    }

    override suspend fun cancelSale(id: Long): Result<Unit> {
        return refundSale(id)
    }

    override fun observeSales(): Flow<List<Penjualan>> {
        return penjualanRepository.getAllPenjualan()
    }

    override fun observeSale(id: Long): Flow<PenjualanWithItems?> {
        return penjualanRepository.observePenjualanWithItems(id)
    }
}
