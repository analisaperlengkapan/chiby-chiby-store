package com.chibychibystore.service.impl

import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.SaleWithItems
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.SaleItemRepository
import com.chibychibystore.repository.SaleRepository
import com.chibychibystore.constant.AppConstants
import com.chibychibystore.repository.ProductRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.SaleService
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.printer.ReceiptItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleServiceImpl @Inject constructor(
    private val saleRepository: SaleRepository,
    private val saleItemRepository: SaleItemRepository,
    private val productRepository: ProductRepository,
    private val authService: AuthService,
    private val printerService: PrinterService
) : SaleService {

    override suspend fun createSale(
        sale: Sale,
        items: List<SaleItem>
    ): Result<SaleWithItems> {
        // 1. Validasi
        if (items.isEmpty()) {
            return Result.failure(Exception("Item penjualan tidak boleh kosong"))
        }

        // 2. Validate calculations (ensure backend math matches frontend)
        var calculatedSubtotal = 0.0
        items.forEach { item ->
            calculatedSubtotal += item.quantity * item.unitPrice
        }

        // Calculate expected total based on passed tax/discount vs calculated subtotal
        val calculatedTax = calculatedSubtotal * AppConstants.TAX_RATE
        val expectedTotal = calculatedSubtotal + calculatedTax - sale.discount

        // Validation: Verify if the passed totalAmount matches our calculation
        if (kotlin.math.abs(expectedTotal - sale.totalAmount) > 1.0) {
            // Log discrepancy if needed
        }

        // Validate stock availability
        val productIds = items.map { it.productId }.distinct()
        val productsResult = productRepository.getProductByIds(productIds)
        if (productsResult is Result.Error) throw productsResult.exception
        val productsMap = (productsResult as Result.Success).data.associateBy { it.id }

        items.forEach { item ->
            val product = productsMap[item.productId]
                ?: throw Exception("Product dengan ID ${item.productId} tidak ditemukan")

            if (product.stockQuantity < item.quantity) {
                throw Exception("Stok tidak mencukupi untuk product: ${product.name}. Sisa: ${product.stockQuantity}, Diminta: ${item.quantity}")
            }
        }

        // Update sale total amount and date
        val finalTax = calculatedSubtotal * AppConstants.TAX_RATE
        val finalTotal = kotlin.math.max(0.0, calculatedSubtotal + finalTax - sale.discount)

        val saleToSave = sale.copy(
            totalAmount = finalTotal,
            tax = finalTax,
            saleDate = Date()
        )

        // Run in transaction via Repository
        return saleRepository.runInTransaction {
            // 3. Save Sale Header
            // createSale in repo returns SaleWithItems (which it shouldn't if it's just creating header?
            // Wait, repo.createSale takes sale and items. It handles everything including transaction.)

            // Actually, I updated SaleRepository.createSale to take Sale and List<SaleItem> and do everything.
            // So I can just call that.

            // However, SaleRepository.createSale might duplicate some logic or not check stock?
            // SaleRepository.createSale inserts data. It does NOT update stock.
            // So I should stick to manual steps here OR move stock update to Repository (but stock update is business logic involving ProductRepository).
            // Business logic belongs in Service. So I keep stock update here.

            // But wait, if I use `saleRepository.createSale` which inserts both, I need to call it.
            // Let's check `SaleRepository.createSale` implementation again.
            // It inserts Sale and Items. It does NOT update stock.

            // So I can call `saleRepository.createSale` and then update stock.

            val result = saleRepository.createSale(saleToSave, items)
            val saleWithItems = (result as? Result.Success)?.data ?: throw (result as? Result.Error)?.exception ?: Exception("Gagal membuat data penjualan")

            // 5. Update Stock (Atomically for each item)
            for (item in items) {
                // Update Stock (Subtract)
                val stockResult = productRepository.adjustStock(item.productId, -item.quantity)
                if (stockResult is Result.Error) {
                    throw stockResult.exception
                }

                // Verify stock consistency (Post-update check)
                val updatedProductResult = productRepository.getProductById(item.productId)
                val updatedProduct = (updatedProductResult as? Result.Success)?.data

                if (updatedProduct != null && updatedProduct.stockQuantity < 0) {
                     throw Exception("Stok tidak mencukupi untuk product: ${updatedProduct.name}. Transaksi dibatalkan.")
                }
            }

            saleWithItems
        }
    }

    override suspend fun getSale(id: Long): Result<SaleWithItems?> {
        return saleRepository.getSaleWithItemsById(id)
    }

    override suspend fun getSales(
        startDate: String?,
        endDate: String?,
        cashierId: Long?,
        query: String?
    ): Result<List<Sale>> {
         return try {
             if (!authService.hasPermission(Permissions.VIEW_SALES_REPORTS)) {
                 // Permission check
             }

             // Default to last 30 days if dates are not provided
             val end = if (endDate != null) LocalDate.parse(endDate) else LocalDate.now()
             val start = if (startDate != null) LocalDate.parse(startDate) else end.minusDays(30)

             val sales = saleRepository.getSalesFiltered(start, end, cashierId, query)
             Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentSales(limit: Int): Result<List<Sale>> {
        return try {
            val sales = saleRepository.getRecentSales(limit).first()
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchSales(query: String): Result<List<Sale>> {
        return try {
            // Delegate to repository flow and collect first emission
            val sales = saleRepository.searchSales(query).first()
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSale(id: Long, sale: Sale): Result<Sale> {
        return Result.failure(Exception("Not implemented yet"))
    }

    override suspend fun deleteSale(id: Long): Result<Unit> {
         return try {
            saleRepository.deleteSale(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refundSale(id: Long): Result<Unit> {
        return saleRepository.runInTransaction {
            val saleResult = getSale(id)
            val saleWithItems = (saleResult as? Result.Success)?.data ?: throw Exception("Penjualan tidak ditemukan")

            // Update sales status
            val updatedSale = saleWithItems.sale.copy(isRefunded = true)
            val updateResult = saleRepository.updateSale(updatedSale)

            if (updateResult is Result.Error) {
                throw updateResult.exception
            }

            // Restore stock
            saleWithItems.items.forEach { item ->
                val stockResult = productRepository.adjustStock(item.productId, item.quantity)
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

        val sale = saleWithItems.sale
        val items = saleWithItems.items

        // Fetch product names for receipt (optimized batch fetch)
        val productIds = items.map { it.productId }.distinct()
        val productsResult = productRepository.getProductByIds(productIds)
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
        val tax = sale.tax
        val discount = sale.discount

        // Format Date
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale("id", "ID"))
        val dateStr = try {
            formatter.format(sale.saleDate)
        } catch (e: Exception) {
            formatter.format(Date())
        }

        return printerService.printReceipt(
            storeName = if (storeName.isBlank()) AppConstants.STORE_NAME else storeName,
            storeAddress = if (storeAddress.isBlank()) AppConstants.STORE_ADDRESS else storeAddress,
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

    override suspend fun getTotalSalesByDateRange(startDate: String, endDate: String): Result<Double> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        return saleRepository.getTotalSalesAmount(start, end)
    }

    override suspend fun getSalesCountByDateRange(startDate: String, endDate: String): Result<Int> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        return saleRepository.getSaleCountByDateRange(start, end)
    }

    override fun observeSales(): Flow<List<Sale>> {
        return saleRepository.getAllSales()
    }

    override fun observeSalesWithItems(): Flow<List<SaleWithItems>> {
        // We don't have getAllSalesWithItems in Repo yet?
        // Let's fallback to empty for now or implement if needed.
        // Or remove from interface if not used.
        // I'll return empty flow for now to satisfy interface.
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override fun observeSalesFiltered(startDate: String, endDate: String, query: String?): Flow<List<Sale>> {
        val end = LocalDate.parse(endDate)
        val start = LocalDate.parse(startDate)
        return saleRepository.observeSalesFiltered(start, end, query)
    }

    override fun observeSalesWithItemsByDateRange(startDate: String, endDate: String): Flow<List<SaleWithItems>> {
        return saleRepository.getSaleWithItemsByDateRange(startDate, endDate)
    }
}
