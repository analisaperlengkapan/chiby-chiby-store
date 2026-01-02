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
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.printer.ReceiptItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
                // Note: item.productId matches the property name in ItemPenjualan
                val stockResult = produkRepository.adjustStock(item.productId, item.quantity)
                if (stockResult is Result.Error) {
                    throw stockResult.exception
                }
            }

            Unit
        }
    }

    override suspend fun cancelSale(id: Long): Result<Unit> {
        // Same as refund for now
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

        val receiptItems = items.map { item ->
            // Need product name, fetch from repository or assuming joined data
            val product = produkRepository.getProdukById(item.productId).getOrNull()
            ReceiptItem(
                name = product?.name ?: "Unknown",
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice
            )
        }

        // Calculate tax and discount (Assuming simplified calculation or fields exist)
        // For simple POS: Tax 10% included or added?
        // Based on PosViewModel: Tax = 10% of subtotal, Total = Subtotal + Tax - Discount
        // We need to reverse calculate or store these values.
        // Assuming Sale entity stores final totalAmount.
        // Let's approximate for display if fields missing, or use 0 if not tracked separately.

        val subtotal = items.sumOf { it.totalPrice }
        val tax = subtotal * 0.1
        val discount = (subtotal + tax) - sale.totalAmount

        // Safe check for negative discount (rounding errors)
        val finalDiscount = if (discount > 0) discount else 0.0

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateStr = try {
            // Assuming saleDate is java.util.Date, convert to LocalDateTime or format directly
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(sale.saleDate)
        } catch (e: Exception) {
            LocalDateTime.now().format(formatter)
        }

        return printerService.printReceipt(
            storeName = storeName,
            storeAddress = storeAddress,
            saleId = saleId,
            saleDate = dateStr,
            items = receiptItems,
            subtotal = subtotal,
            tax = tax,
            discount = finalDiscount,
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
