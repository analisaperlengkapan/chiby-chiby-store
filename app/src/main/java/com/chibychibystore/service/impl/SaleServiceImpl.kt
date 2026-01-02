package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.model.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.constant.AppConstants
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.printer.ReceiptItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleServiceImpl @Inject constructor(
    private val penjualanRepository: PenjualanRepository,
    private val itemPenjualanRepository: ItemPenjualanRepository,
    private val produkRepository: ProdukRepository,
    private val authService: AuthService,
    private val printerService: PrinterService
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
            calculatedTotal += item.quantity * item.unitPrice
        }

        // Update sale total amount and date
        val saleToSave = sale.copy(
            totalAmount = calculatedTotal,
            saleDate = Date() // Force server time (using Date as per Entity)
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
                val itemWithSaleId = item.copy(saleId = saleId) // Corrected property name: saleId
                itemPenjualanRepository.createItemPenjualan(itemWithSaleId)
                savedItems.add(itemWithSaleId)

                // Update Stock (Subtract)
                val stockResult = produkRepository.adjustStock(item.productId, -item.quantity)
                if (stockResult is Result.Error) {
                    throw stockResult.exception
                }
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
                 // Permission check logic
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
        return penjualanRepository.runInTransaction {
            val saleResult = getSale(id)
            val saleWithItems = (saleResult as? Result.Success)?.data ?: throw Exception("Penjualan tidak ditemukan")

            // Update sales status
            val updatedSale = saleWithItems.penjualan.copy(isRefunded = true)
            val updateResult = penjualanRepository.updatePenjualan(updatedSale)

            if (updateResult is Result.Error) {
                throw updateResult.exception
            }

            // Restore stock
            saleWithItems.items.forEach { item ->
                val stockResult = produkRepository.adjustStock(item.productId, item.quantity)
                if (stockResult is Result.Error) {
                    throw stockResult.exception
                }
            }

            Unit
        }
    }

    override suspend fun cancelSale(id: Long): Result<Unit> {
        return refundSale(id)
    }

    override suspend fun printReceipt(
        saleId: Long,
        storeName: String,
        storeAddress: String,
        cashierName: String
    ): Result<Unit> {
        val saleResult = getSale(saleId)
        val saleWithItems = (saleResult as? Result.Success)?.data ?: return Result.failure(Exception("Penjualan tidak ditemukan"))

        val sale = saleWithItems.penjualan
        val items = saleWithItems.items

        // Fetch product names for receipt (optimized batch fetch)
        val productIds = items.map { it.productId }.distinct()
        val productsResult = produkRepository.getProdukByIds(productIds)
        val productsMap = productsResult.getOrNull()?.associateBy { it.id } ?: emptyMap()

        val receiptItems = items.map { item ->
            val product = productsMap[item.productId]
            ReceiptItem(
                name = product?.name ?: "Unknown Product",
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice
            )
        }

        // Calculate totals
        val subtotal = items.sumOf { it.totalPrice }
        val tax = subtotal * AppConstants.TAX_RATE
        val calculatedTotal = subtotal + tax

        // Discount is the difference between calculated total and actual total amount
        // If totalAmount is less than calculated, the diff is discount.
        val discount = if (calculatedTotal > sale.totalAmount) {
            calculatedTotal - sale.totalAmount
        } else {
            0.0
        }

        // Format Date
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val dateStr = try {
            formatter.format(sale.saleDate)
        } catch (e: Exception) {
            formatter.format(Date())
        }

        return printerService.printReceipt(
            storeName = storeName,
            storeAddress = storeAddress,
            saleId = saleId,
            saleDate = dateStr,
            items = receiptItems,
            subtotal = subtotal,
            tax = tax,
            discount = discount,
            total = sale.totalAmount,
            paymentMethod = sale.paymentMethod.name,
            cashierName = cashierName
        )
    }

    override fun observeSales(): Flow<List<Penjualan>> {
        return penjualanRepository.getAllPenjualan()
    }

    override fun observeSale(id: Long): Flow<PenjualanWithItems?> {
        return penjualanRepository.observePenjualanWithItems(id)
    }
}
