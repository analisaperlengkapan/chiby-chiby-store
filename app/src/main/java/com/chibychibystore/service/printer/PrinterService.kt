package com.chibychibystore.service.printer

import android.bluetooth.BluetoothDevice
import com.chibychibystore.data.model.Result
import javax.inject.Singleton

/**
 * Status printer
 */
enum class PrinterStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    PRINTING,
    ERROR
}

/**
 * Interface untuk Printer Service
 */
interface PrinterService {

    /**
     * Get status printer saat ini
     */
    fun getPrinterStatus(): PrinterStatus

    /**
     * Get daftar device Bluetooth yang tersedia
     */
    suspend fun getAvailableDevices(): Result<List<BluetoothDevice>>

    /**
     * Connect ke printer Bluetooth
     */
    suspend fun connectPrinter(device: BluetoothDevice): Result<Unit>

    /**
     * Disconnect dari printer
     */
    suspend fun disconnectPrinter(): Result<Unit>

    /**
     * Print receipt dengan data yang diberikan
     */
    suspend fun printReceipt(
        storeName: String,
        storeAddress: String,
        saleId: Long,
        saleDate: String,
        items: List<ReceiptItem>,
        subtotal: Double,
        tax: Double,
        discount: Double,
        total: Double,
        paymentMethod: String,
        cashierName: String
    ): Result<Unit>

    /**
     * Print test receipt
     */
    suspend fun printTestReceipt(): Result<Unit>

    /**
     * Print barcode labels untuk produk
     */
    suspend fun printBarcodeLabels(
        product: com.chibychibystore.data.local.entity.Produk,
        labelSize: com.chibychibystore.ui.barcode.LabelSize,
        quantity: Int
    ): Result<Unit>

    /**
     * Check apakah printer terhubung
     */
    fun isConnected(): Boolean
}

/**
 * Data class untuk item receipt
 */
data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)