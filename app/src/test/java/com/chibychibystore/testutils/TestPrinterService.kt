package com.chibychibystore.testutils
import org.robolectric.annotation.Config

import com.chibychibystore.data.model.Result
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.printer.PrinterStatus
import com.chibychibystore.service.printer.ReceiptItem
import com.chibychibystore.ui.barcode.LabelSize

class TestPrinterService : PrinterService {
    override fun getPrinterStatus(): PrinterStatus = PrinterStatus.CONNECTED

    override suspend fun getAvailableDevices(): Result<List<android.bluetooth.BluetoothDevice>> = Result.success(emptyList())

    override suspend fun connectPrinter(device: android.bluetooth.BluetoothDevice): Result<Unit> = Result.success(Unit)

    override suspend fun disconnectPrinter(): Result<Unit> = Result.success(Unit)

    override suspend fun printReceipt(
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
    ): Result<Unit> = Result.success(Unit)

    override suspend fun printTestReceipt(): Result<Unit> = Result.success(Unit)

    override suspend fun printBarcodeLabels(
        product: com.chibychibystore.data.local.entity.Produk,
        labelSize: LabelSize,
        quantity: Int
    ): Result<Unit> = Result.success(Unit)

    override fun isConnected(): Boolean = true
}