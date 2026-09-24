package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.StockAdjustment
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.constant.AppConstants
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.util.Permissions
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.ShiftRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.PromoService
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.printer.ReceiptItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Date
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

@Singleton
class SaleServiceImpl @Inject constructor(
    private val db: ChibyChibyDatabase,
    private val penjualanRepository: PenjualanRepository,
    private val itemPenjualanRepository: ItemPenjualanRepository,
    private val productRepository: ProdukRepository,
    private val stokGudangRepository: StokGudangRepository,
    private val shiftRepository: ShiftRepository,
    private val authService: AuthService,
    private val printerService: PrinterService,
    private val promoService: PromoService
) : SaleService {

    override suspend fun createPenjualan(
        sale: Penjualan,
        items: List<ItemPenjualan>
    ): Result<PenjualanWithItems> {
        val pelangganId = sale.pelangganId
        if (!authService.hasPermission(Permissions.CREATE_SALES)) {
            return Result.failure(ChibyChibyException.PermissionError(Permissions.CREATE_SALES))
        }
        if (items.isEmpty()) {
            return Result.failure(Exception("Item penjualan tidak boleh kosong"))
        }

        return try {
            db.withTransaction {
                var calculatedSubtotal = 0.0
                items.forEach { item ->
                    calculatedSubtotal += item.quantity * item.unitPrice
                }

                val finalTax = calculatedSubtotal * AppConstants.TAX_RATE
                val promoDiscount = promoService.calculateDiscount(calculatedSubtotal)
                // Cap redeemed points so the point discount can never exceed the
                // remaining balance after promo discount. Without this cap, a sale
                // could effectively credit the customer (negative total floored at
                // zero) while still consuming all redeemed points.
                val maxRedeemableByAmount = kotlin.math.max(
                    0.0,
                    calculatedSubtotal + finalTax - promoDiscount
                ) / AppConstants.POINT_REDEMPTION_VALUE
                // Also cap by the customer's actual current point balance from the
                // database. The pointsRedeemed value comes from a (potentially stale)
                // UI cache; without this DB-side check, a customer could redeem more
                // points than they actually own, resulting in a financial loss.
                // If there's no customer attached to the sale, no points can be
                // redeemed — cap at 0. Defaulting to Int.MAX_VALUE here would let
                // a sale with pelangganId == null still apply a point discount
                // sourced from a stale UI state, granting a discount with no
                // corresponding point balance to deduct from (financial loss).
                val customerCurrentPoints = if (pelangganId == null) {
                    0
                } else if (sale.pointsRedeemed > 0) {
                    db.pelangganDao().getPelangganById(pelangganId)?.point ?: 0
                } else {
                    Int.MAX_VALUE
                }
                val effectivePointsRedeemed = kotlin.math.min(
                    kotlin.math.min(sale.pointsRedeemed, maxRedeemableByAmount.toInt()),
                    customerCurrentPoints
                )
                val pointDiscount = effectivePointsRedeemed * AppConstants.POINT_REDEMPTION_VALUE
                val finalDiscount = promoDiscount + pointDiscount
                val finalTotal = kotlin.math.max(0.0, calculatedSubtotal + finalTax - finalDiscount)

                val saleToSave = sale.copy(
                    totalAmount = finalTotal,
                    tax = finalTax,
                    discount = finalDiscount,
                    pointsRedeemed = effectivePointsRedeemed,
                    saleDate = Date()
                )

                val productIds = items.map { it.productId }.distinct()
                val productsResult = productRepository.getProductsByIds(productIds)

                if (productsResult is Result.Success) {
                     val products = productsResult.data ?: emptyList()
                     val productMap = products.associateBy { it.id }

                     val requiredQuantities = items.groupBy { it.productId }
                         .mapValues { (_, group) -> group.sumOf { it.quantity } }

                     val warehouseId = sale.warehouseId
                     val stockResult = stokGudangRepository.getStocks(requiredQuantities.keys.toList(), warehouseId)
                     val stockMap = if (stockResult is Result.Success) {
                         stockResult.data.associateBy { it.productId }
                     } else {
                         throw (stockResult as Result.Failure).exception
                     }

                     for ((productId, requiredQty) in requiredQuantities) {
                         val product = productMap[productId]
                             ?: throw IllegalStateException("Produk dengan ID $productId tidak ditemukan")

                         val currentStock = stockMap[productId]?.quantity ?: 0

                         if (currentStock < requiredQty) {
                             throw IllegalStateException("Stok tidak mencukupi untuk ${product.name} di Gudang $warehouseId. Tersedia: $currentStock, Dibutuhkan: $requiredQty")
                         }
                     }

                    var pointsEarned = 0
                // Point awarding logic: award points only on the amount the
                // customer actually paid (i.e. after both promo and point
                // discounts), so customers don't earn points on money they
                // didn't spend.
                    if (pelangganId != null) {
                        pointsEarned = (finalTotal / AppConstants.POINT_AWARD_THRESHOLD).toInt()
                    }

                    val saleToSaveWithPoints = saleToSave.copy(
                        pointsEarned = pointsEarned
                    )

                    // Hydrate items with current cost price before persistence
                    val hydratedItems = items.map { item ->
                        val prod = productMap[item.productId]
                        item.copy(costPrice = prod?.costPrice ?: 0.0)
                    }

                    // Create Sale (Persistence)
                    val result = penjualanRepository.createPenjualan(saleToSaveWithPoints, hydratedItems)

                    if (result is Result.Success) {
                        // Update Customer points if applicable
                        if (pelangganId != null) {
                            db.pelangganDao().getPelangganById(pelangganId)?.let { p ->
                                val netPointsChange = pointsEarned - saleToSaveWithPoints.pointsRedeemed
                                db.pelangganDao().updatePelanggan(
                                    p.copy(
                                        point = kotlin.math.max(0, p.point + netPointsChange),
                                        updatedAt = Date()
                                    )
                                )
                            }
                        }

                        // Batch adjust stocks
                        val adjustments = hydratedItems.map {
                            StockAdjustment(it.productId, sale.warehouseId, -it.quantity)
                        }
                        val stockAdjResult = stokGudangRepository.adjustStockBatch(adjustments)
                        if (stockAdjResult is Result.Failure) {
                            throw stockAdjResult.exception
                        }
                    } else if (result is Result.Failure) {
                        throw result.exception
                    }

                    result
                } else if (productsResult is Result.Failure) {
                    throw productsResult.exception
                } else {
                    Result.failure(Exception("Failed to fetch products for validation"))
                }
            }
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
            db.withTransaction {
                val saleResult = getPenjualanById(id)
                if (saleResult is Result.Failure) throw saleResult.exception
                val saleWithItems = (saleResult as Result.Success).data
                    ?: throw Exception("Penjualan tidak ditemukan")

                if (saleWithItems.sale.isRefunded) {
                    throw Exception("Penjualan sudah di-refund")
                }

                val updatedPenjualan = saleWithItems.sale.copy(isRefunded = true)
                val updateResult = penjualanRepository.updatePenjualan(updatedPenjualan)
                if (updateResult is Result.Failure) throw updateResult.exception

                val pelangganId = saleWithItems.sale.pelangganId
                if (pelangganId != null) {
                    val pointsEarned = saleWithItems.sale.pointsEarned
                    val pointsRedeemed = saleWithItems.sale.pointsRedeemed
                    db.pelangganDao().getPelangganById(pelangganId)?.let { p ->
                        val newPoint = kotlin.math.max(0, p.point - pointsEarned + pointsRedeemed)
                        db.pelangganDao().updatePelanggan(
                            p.copy(point = newPoint, updatedAt = Date())
                        )
                    }
                }

                val warehouseId = saleWithItems.sale.warehouseId
                val adjustments = mutableListOf<StockAdjustment>()
                val fallbackItems = mutableListOf<ItemPenjualan>()

                saleWithItems.items.forEach { item ->
                    val productResult = productRepository.getProdukById(item.productId)
                    if (productResult is Result.Success && productResult.data != null) {
                        adjustments.add(StockAdjustment(item.productId, warehouseId, item.quantity))
                    } else {
                        fallbackItems.add(item)
                    }
                }

                if (adjustments.isNotEmpty()) {
                    val stockResult = stokGudangRepository.adjustStockBatch(adjustments)
                    if (stockResult is Result.Failure) throw stockResult.exception
                }

                fallbackItems.forEach { item ->
                    productRepository.adjustStock(item.productId, item.quantity)
                }

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelPenjualan(id: Long): Result<Unit> {
        return try {
            db.withTransaction {
                val saleResult = getPenjualanById(id)
                if (saleResult is Result.Failure) throw saleResult.exception
                val saleWithItems = (saleResult as Result.Success).data
                    ?: throw Exception("Penjualan tidak ditemukan")

                // Restore per-warehouse stock, then delete the sale and its items.
                val warehouseId = saleWithItems.sale.warehouseId
                val adjustments = saleWithItems.items.map {
                    StockAdjustment(it.productId, warehouseId, it.quantity)
                }
                if (adjustments.isNotEmpty()) {
                    val stockResult = stokGudangRepository.adjustStockBatch(adjustments)
                    if (stockResult is Result.Failure) throw stockResult.exception
                }

                penjualanRepository.deletePenjualan(id)
                itemPenjualanRepository.deleteItemPenjualanByPenjualanId(id)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

        val productIds = items.map { it.productId }.distinct()
        val productsResult = productRepository.getProductsByIds(productIds)
        val productMap = if (productsResult is Result.Success) {
            productsResult.data?.associateBy { it.id } ?: emptyMap()
        } else {
            emptyMap()
        }

        val receiptItems = items.map { item ->
            val productName = productMap[item.productId]?.name ?: "Produk #${item.productId}"
            ReceiptItem(
                name = productName,
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

    override suspend fun getOpenShift(kasirId: Long): Result<com.chibychibystore.data.local.entity.Shift?> {
        return shiftRepository.getOpenShiftByKasir(kasirId)
    }
}
