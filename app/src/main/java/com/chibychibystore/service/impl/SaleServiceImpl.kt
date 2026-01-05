package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
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
import java.time.LocalDate
import java.util.Date
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleServiceImpl @Inject constructor(
    private val penjualanRepository: PenjualanRepository,
    private val itemPenjualanRepository: ItemPenjualanRepository,
    private val productRepository: ProdukRepository,
    private val authService: AuthService,
    private val printerService: PrinterService
) : SaleService {

    override suspend fun createPenjualan(
        sale: Penjualan,
        items: List<ItemPenjualan>
    ): Result<PenjualanWithItems> {
        if (items.isEmpty()) {
            return Result.failure(Exception("Item penjualan tidak boleh kosong"))
        }

        var calculatedSubtotal = 0.0
        items.forEach { item ->
            calculatedSubtotal += item.quantity * item.unitPrice
        }

        val finalTax = calculatedSubtotal * AppConstants.TAX_RATE
        val finalTotal = kotlin.math.max(0.0, calculatedSubtotal + finalTax - sale.discount)

        val saleToSave = sale.copy(
            totalAmount = finalTotal,
            tax = finalTax,
            saleDate = Date()
        )

        return try {
            val result = penjualanRepository.createPenjualan(saleToSave, items)
            if (result is Result.Success) {
                for (item in items) {
                    productRepository.adjustStock(item.productId, -item.quantity)
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPenjualanById(id: Long): Result<PenjualanWithItems?> {
        return penjualanRepository.getPenjualanWithItemsById(id)
    }

    override suspend fun getPenjualanByRentangTanggal(
        startDate: String?,
        endDate: String?,
        cashierId: Long?,
        query: String?
    ): Result<List<Penjualan>> {
         return try {
             val end = if (endDate != null) LocalDate.parse(endDate) else LocalDate.now()
             val start = if (startDate != null) LocalDate.parse(startDate) else end.minusDays(30)
             val sales = penjualanRepository.getSalesInDateRange(start, end)
             Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentPenjualan(limit: Int): Result<List<Penjualan>> {
        return try {
            val sales = penjualanRepository.getRecentPenjualan(limit).first()
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchPenjualan(query: String): Result<List<Penjualan>> {
        return try {
            val sales = penjualanRepository.searchPenjualan(query).first()
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePenjualan(id: Long, sale: Penjualan): Result<Penjualan> {
        return try {
             penjualanRepository.updatePenjualan(sale)
             Result.success(sale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePenjualan(id: Long): Result<Unit> {
         return try {
            penjualanRepository.deletePenjualan(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refundPenjualan(id: Long): Result<Unit> {
        return try {
            val saleResult = getPenjualanById(id)
            if (saleResult is Result.Failure) return Result.failure(saleResult.exception)
            val saleWithItems = (saleResult as Result.Success).data ?: return Result.failure(Exception("Penjualan tidak ditemukan"))

            val updatedPenjualan = saleWithItems.sale.copy(isRefunded = true)
            penjualanRepository.updatePenjualan(updatedPenjualan)

            saleWithItems.items.forEach { item ->
                productRepository.adjustStock(item.productId, item.quantity)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelPenjualan(id: Long): Result<Unit> {
        return refundPenjualan(id)
    }

    override suspend fun getTotalPenjualanByRentangTanggal(startDate: String, endDate: String): Result<Double> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        return penjualanRepository.getTotalPenjualanAmount(start, end)
    }

    override suspend fun getPenjualanCountByRentangTanggal(startDate: String, endDate: String): Result<Int> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        return penjualanRepository.getPenjualanCountByDateRange(start, end)
    }

    override fun observePenjualan(): Flow<List<Penjualan>> {
        return penjualanRepository.getAllPenjualan()
    }

    override fun observePenjualanWithItems(): Flow<List<PenjualanWithItems>> {
        return penjualanRepository.getAllPenjualanWithItems()
    }

    override fun observePenjualanFiltered(startDate: String, endDate: String, query: String?): Flow<List<Penjualan>> {
        return penjualanRepository.getPenjualanByRentangTanggal(startDate, endDate)
    }

    override suspend fun cetakStruk(
        saleId: Long,
        namaToko: String,
        alamatToko: String,
        namaKasir: String
    ): Result<Unit> {
        val saleResult = getPenjualanById(saleId)
        if (saleResult is Result.Failure) return Result.failure(saleResult.exception)
        val saleWithItems = (saleResult as Result.Success).data ?: return Result.failure(Exception("Penjualan tidak ditemukan"))

        val sale = saleWithItems.sale
        val items = saleWithItems.items

        val receiptItems = items.map { item ->
            ReceiptItem(
                name = "Produk #${item.productId}", 
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice
            )
        }

        val subtotal = items.sumOf { it.totalPrice }
        val tax = sale.tax
        val discount = sale.discount

        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("id", "ID"))
        val dateStr = formatter.format(sale.saleDate)

        return printerService.printReceipt(
            storeName = if (namaToko.isBlank()) AppConstants.STORE_NAME else namaToko,
            storeAddress = if (alamatToko.isBlank()) AppConstants.STORE_ADDRESS else alamatToko,
            saleId = saleId,
            saleDate = dateStr,
            items = receiptItems,
            subtotal = subtotal,
            tax = tax,
            discount = discount,
            total = sale.totalAmount,
            paymentMethod = sale.paymentMethod.name,
            cashierName = namaKasir
        )
    }

    override fun observePenjualanWithItemsByRentangTanggal(startDate: String, endDate: String): Flow<List<PenjualanWithItems>> {
        return penjualanRepository.getPenjualanWithItemsByRentangTanggal(startDate, endDate)
    }
}
