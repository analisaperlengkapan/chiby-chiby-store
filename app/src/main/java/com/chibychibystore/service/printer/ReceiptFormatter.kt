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

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("id").setRegion("ID").build())
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
                name = "Product #${item.productId}",
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice
            )
        }

        val subtotal = items.sumOf { it.totalPrice }
        val tax = subtotal * 0.1 // 10% tax

        // Simple discount rule: 5% discount for subtotal >= 100,000 (Rp)
        // This is a business-rule placeholder; replace with promo engine later
        val discount = if (subtotal >= 100_000.0) (subtotal * 0.05) else 0.0
        val total = subtotal + tax - discount

        return ReceiptData(
            storeName = storeName,
            storeAddress = storeAddress,
            saleId = saleWithItems.sale.id,
            saleDate = dateFormat.format(saleWithItems.sale.saleDate),
            items = items,
            subtotal = subtotal,
            tax = tax,
            discount = discount,
            total = total,
            paymentMethod = saleWithItems.sale.paymentMethod.name,
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