package com.chibychibystore.service

import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.SaleWithItems
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

/**
 * Sale Service untuk manajemen transaksi penjualan di Chiby Chiby Store
 *
 * Service ini menangani semua operasi Point of Sale (POS) termasuk:
 * - Pembuatan transaksi penjualan dengan validasi inventory
 * - Manajemen stok otomatis saat penjualan/refund
 * - Pencarian dan filtering transaksi penjualan
 * - Receipt printing untuk struk penjualan
 * - Real-time updates untuk UI reactive
 *
 * **Fitur Utama:**
 * - Transaction processing dengan inventory validation
 * - Automatic stock deduction saat penjualan
 * - Stock restoration saat refund/cancel
 * - Receipt printing dengan thermal printer
 * - Reactive data streams untuk real-time UI
 * - Comprehensive transaction history dan search
 *
 * **Business Rules:**
 * - Stock validation sebelum penjualan (tidak boleh minus)
 * - Automatic inventory updates untuk setiap transaksi
 * - Transaction integrity dengan rollback on failure
 * - Receipt generation dengan format standar retail
 *
 * **Integration Points:**
 * - [ProductService] untuk inventory validation dan stock updates
 * - [PrinterService] untuk receipt printing
 * - POS UI untuk transaction creation
 * - Reports untuk sales analytics
 *
 * **Payment Methods:**
 * - CASH: Pembayaran tunai
 * - CARD: Pembayaran kartu (future expansion ready)
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see Sale
 * @see SaleItem
 * @see SaleWithItems
 * @see ProductService
 * @see PrinterService
 */
interface SaleService {

    /**
     * Membuat transaksi penjualan baru dengan validasi inventory lengkap
     *
     * **Proses Transaksi:**
     * 1. Validasi data penjualan dan items
     * 2. Cek ketersediaan stok untuk setiap product
     * 3. Kurangi stok inventory secara otomatis
     * 4. Simpan transaksi dan items ke database
     * 5. Return data penjualan lengkap dengan items
     *
     * **Inventory Management:**
     * - Stock validation: Pastikan stok mencukupi sebelum transaksi
     * - Automatic deduction: Stok berkurang saat transaksi berhasil
     * - Atomic operation: Rollback jika ada kegagalan di tengah proses
     *
     * @param sale Data header penjualan (tanggal, total, metode pembayaran, kasir)
     * @param items List item penjualan dengan quantity dan harga per product
     * @return Result dengan [SaleWithItems] lengkap atau exception
     *
     * @throws IllegalArgumentException jika validasi data gagal
     * @throws Exception jika stok tidak mencukupi atau product tidak ditemukan
     *
     * @sample
     * ```kotlin
     * val sale = Sale(
     *     saleDate = "2025-12-12",
     *     totalAmount = 15000.0,
     *     paymentMethod = "CASH",
     *     cashierId = 1
     * )
     * val items = listOf(
     *     SaleItem(productId = "prod1", quantity = 2, unitPrice = 5000.0, totalPrice = 10000.0),
     *     SaleItem(productId = "prod2", quantity = 1, unitPrice = 5000.0, totalPrice = 5000.0)
     * )
     * val result = saleService.createSale(sale, items)
     * ```
     */
    suspend fun createSale(sale: Sale, items: List<SaleItem>): Result<SaleWithItems>

    /**
     * Get penjualan by ID
     */
    suspend fun getSale(id: Long): Result<SaleWithItems?>

    /**
     * Get semua penjualan dengan filter opsional
     */
    suspend fun getSales(
        startDate: String? = null,
        endDate: String? = null,
        cashierId: Long? = null,
        query: String? = null
    ): Result<List<Sale>>

    /**
     * Get recent sales with limit
     */
    suspend fun getRecentSales(limit: Int): Result<List<Sale>>

    /**
     * Search penjualan berdasarkan query
     */
    suspend fun searchSales(query: String): Result<List<Sale>>

    /**
     * Update penjualan
     */
    suspend fun updateSale(id: Long, sale: Sale): Result<Sale>

    /**
     * Hapus penjualan
     */
    suspend fun deleteSale(id: Long): Result<Unit>

    /**
     * Refund penjualan
     */
    suspend fun refundSale(id: Long): Result<Unit>

    /**
     * Cancel penjualan
     */
    suspend fun cancelSale(id: Long): Result<Unit>

    /**
     * Get total penjualan by date range
     */
    suspend fun getTotalSalesByDateRange(startDate: String, endDate: String): Result<Double>

    /**
     * Get sales count by date range
     */
    suspend fun getSalesCountByDateRange(startDate: String, endDate: String): Result<Int>

    /**
     * Observable untuk semua penjualan
     */
    fun observeSales(): Flow<List<Sale>>

    /**
     * Observable untuk penjualan dengan items
     */
    fun observeSalesWithItems(): Flow<List<SaleWithItems>>

    /**
     * Observable untuk penjualan by date range dengan optional query
     */
    fun observeSalesFiltered(startDate: String, endDate: String, query: String? = null): Flow<List<Sale>>

    /**
     * Print receipt untuk transaksi penjualan
     *
     * **Receipt Content:**
     * - Store information (nama, alamat)
     * - Transaction details (ID, tanggal, kasir)
     * - Item list dengan quantity dan harga
     * - Subtotal, tax, discount, total
     * - Payment method
     *
     * **Printing Integration:**
     * - Menggunakan thermal printer via Bluetooth
     * - Format receipt standar retail Indonesia
     * - Error handling untuk printer connectivity
     *
     * @param saleId ID penjualan yang akan di-print receipt-nya
     * @param storeName Nama toko (default: "Chiby Chiby Store")
     * @param storeAddress Alamat toko
     * @param cashierName Nama kasir
     * @return Result menunjukkan keberhasilan printing
     */
    suspend fun printReceipt(
        saleId: Long,
        storeName: String = "Chiby Chiby Store",
        storeAddress: String = "Jl. Example No. 123, Jakarta",
        cashierName: String = "Kasir"
    ): Result<Unit>

    /**
     * Observable untuk penjualan dengan items by date range
     */
    fun observeSalesWithItemsByDateRange(startDate: String, endDate: String): Flow<List<SaleWithItems>>
}
