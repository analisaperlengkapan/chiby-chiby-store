package com.chibychibystore.service.printer

import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.PenjualanWithItems
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class untuk formatting receipt data
 */
object ReceiptFormatter {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /**
     * Format sale data untuk receipt printing
     */
    fun formatSaleForReceipt(
        saleWithItems: PenjualanWithItems,
        storeName: String = "Chiby Chiby Store",
        storeAddress: String = "Jl. Example No. 123, Jakarta",
        cashierName: String = "Kasir"
    ): ReceiptData {
        val items = saleWithItems.items.map { item ->
            ReceiptItem(
                name = item.productName ?: "Produk #${item.productId}",
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice
            )
        }

        val subtotal = items.sumOf { it.totalPrice }
        val tax = subtotal * 0.1 // 10% tax
        val discount = 0.0 // TODO: implement discount logic
        val total = subtotal + tax - discount

        return ReceiptData(
            storeName = storeName,
            storeAddress = storeAddress,
            saleId = saleWithItems.penjualan.id,
            saleDate = dateFormat.format(saleWithItems.penjualan.saleDate),
            items = items,
            subtotal = subtotal,
            tax = tax,
            discount = discount,
            total = total,
            paymentMethod = saleWithItems.penjualan.paymentMethod.name,
            cashierName = cashierName
        )
    }

    /**
     * Data class untuk receipt formatting
     */
    data class ReceiptData(
        val storeName: String,
        val storeAddress: String,
        val saleId: Long,
        val saleDate: String,
        val items: List<ReceiptItem>,
        val subtotal: Double,
        val tax: Double,
        val discount: Double,
        val total: Double,
        val paymentMethod: String,
        val cashierName: String
    )
}