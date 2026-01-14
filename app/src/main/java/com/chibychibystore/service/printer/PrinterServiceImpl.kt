package com.chibychibystore.service.printer

import android.bluetooth.BluetoothAdapter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.ByteArrayOutputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import java.io.IOException
import java.io.OutputStream
import java.util.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import androidx.annotation.VisibleForTesting

/**
 * ESC/POS Command Constants
 */
object EscPosCommands {
    // Initialize printer
    val INIT = byteArrayOf(0x1B, 0x40)

    // Cut paper
    val CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

    // Line feed
    val LF = byteArrayOf(0x0A)

    // Carriage return
    val CR = byteArrayOf(0x0D)

    // Bold on
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)

    // Bold off
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)

    // Center alignment
    val CENTER = byteArrayOf(0x1B, 0x61, 0x01)

    // Left alignment
    val LEFT = byteArrayOf(0x1B, 0x61, 0x00)

    // Right alignment
    val RIGHT = byteArrayOf(0x1B, 0x61, 0x02)

    // Font size normal
    val FONT_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)

    // Font size double height
    val FONT_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)

    // Font size double width
    val FONT_DOUBLE_WIDTH = byteArrayOf(0x1B, 0x21, 0x20)

    // Font size double height and width
    val FONT_DOUBLE = byteArrayOf(0x1B, 0x21, 0x30)
}

/**
 * Implementasi PrinterService untuk thermal printer Bluetooth
 */
@Singleton
class PrinterServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    // Accept optional adapter (for testing); resolve via BluetoothManager at runtime to avoid deprecated API
    private var bluetoothAdapter: BluetoothAdapter? = null,
    @IoDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : PrinterService {

    init {
        if (bluetoothAdapter == null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val manager = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
                    bluetoothAdapter = manager?.adapter
                } else {
                    // Older API levels should use the legacy adapter method
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                }
            } catch (e: Exception) {
                // fallback to legacy API if any unexpected error occurs
                bluetoothAdapter = legacyBluetoothAdapter()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyBluetoothAdapter(): BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    @VisibleForTesting
    internal fun setTestOutput(output: OutputStream) {
        this.outputStream = output
        this.currentStatus = PrinterStatus.CONNECTED
    }

    private val TAG = "PrinterService"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var currentStatus: PrinterStatus = PrinterStatus.DISCONNECTED

    override fun getPrinterStatus(): PrinterStatus = currentStatus

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun getAvailableDevices(): Result<List<BluetoothDevice>> = withContext(dispatcher) {
        try {
            val adapter = bluetoothAdapter
                ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di device ini"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(Exception("Tidak memiliki izin Bluetooth (BLUETOOTH_CONNECT). Mohon izinkan di pengaturan."))
            }

            if (!adapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth tidak aktif"))
            }

            val pairedDevices = adapter.bondedDevices
            val printerDevices = pairedDevices.filter { device ->
                // Filter devices that might be printers (you can customize this logic)
                device.name?.contains("printer", ignoreCase = true) == true ||
                device.name?.contains("thermal", ignoreCase = true) == true ||
                device.name?.contains("receipt", ignoreCase = true) == true
            }

            Result.success(printerDevices.toList())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available devices", e)
            Result.failure(Exception("Gagal mendapatkan daftar device: ${e.message}"))
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectPrinter(device: BluetoothDevice): Result<Unit> = withContext(dispatcher) {
        try {
            currentStatus = PrinterStatus.CONNECTING
            Log.d(TAG, "Connecting to printer: ${device.name}")

            // Close existing connection
            disconnectPrinter()

            // Create socket connection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(Exception("Tidak memiliki izin Bluetooth (BLUETOOTH_CONNECT). Mohon izinkan di pengaturan."))
            }

            // Ensure cancellation of discovery before connecting
            try {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not cancel discovery: missing permission", e)
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling discovery", e)
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()

            // Get output stream
            outputStream = bluetoothSocket?.outputStream

            currentStatus = PrinterStatus.CONNECTED
            Log.d(TAG, "Successfully connected to printer")

            Result.success(Unit)
        } catch (e: IOException) {
            currentStatus = PrinterStatus.ERROR
            Log.e(TAG, "Failed to connect to printer", e)
            Result.failure(Exception("Gagal terhubung ke printer: ${e.message}"))
        } catch (e: Exception) {
            currentStatus = PrinterStatus.ERROR
            Log.e(TAG, "Unexpected error connecting to printer", e)
            Result.failure(Exception("Error tidak terduga: ${e.message}"))
        }
    }

    override suspend fun disconnectPrinter(): Result<Unit> = withContext(dispatcher) {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
            currentStatus = PrinterStatus.DISCONNECTED
            Log.d(TAG, "Printer disconnected")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting printer", e)
            Result.failure(Exception("Gagal memutus koneksi printer: ${e.message}"))
        }
    }

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
    ): Result<Unit> = withContext(dispatcher) {
        try {
            if (!isConnected()) {
                return@withContext Result.failure(Exception("Printer tidak terhubung"))
            }

            currentStatus = PrinterStatus.PRINTING

            val receiptData = buildReceiptData(
                storeName, storeAddress, saleId, saleDate,
                items, subtotal, tax, discount, total, paymentMethod, cashierName
            )

            outputStream?.write(receiptData)
            outputStream?.flush()

            // Small delay to ensure printing is complete
            delay(500)

            currentStatus = PrinterStatus.CONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            currentStatus = PrinterStatus.ERROR
            Log.e(TAG, "Error printing receipt", e)
            Result.failure(Exception("Gagal mencetak receipt: ${e.message}"))
        }
    }

    override suspend fun printTestReceipt(): Result<Unit> = withContext(dispatcher) {
        try {
            if (!isConnected()) {
                return@withContext Result.failure(Exception("Printer tidak terhubung"))
            }

            currentStatus = PrinterStatus.PRINTING

            val testData = buildTestReceiptData()
            outputStream?.write(testData)
            outputStream?.flush()

            delay(500)

            currentStatus = PrinterStatus.CONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            currentStatus = PrinterStatus.ERROR
            Log.e(TAG, "Error printing test receipt", e)
            Result.failure(Exception("Gagal mencetak test receipt: ${e.message}"))
        }
    }

    override suspend fun printBarcodeLabels(
        product: com.chibychibystore.data.local.entity.Produk,
        labelSize: com.chibychibystore.ui.barcode.LabelSize,
        quantity: Int
    ): Result<Unit> = withContext(dispatcher) {
        try {
            if (!isConnected()) {
                return@withContext Result.failure(Exception("Printer tidak terhubung"))
            }

            currentStatus = PrinterStatus.PRINTING

            val labelData = buildBarcodeLabelData(product, labelSize)
            repeat(quantity) {
                outputStream?.write(labelData)
                outputStream?.flush()
                // Small delay between labels
                delay(200)
            }

            currentStatus = PrinterStatus.CONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            currentStatus = PrinterStatus.ERROR
            Log.e(TAG, "Error printing barcode labels", e)
            Result.failure(Exception("Gagal mencetak label barcode: ${e.message}"))
        }
    }

    override fun isConnected(): Boolean {
        // Consider the socket connection OR a test-injected output stream as connected when status is CONNECTED
        return (bluetoothSocket?.isConnected == true && currentStatus == PrinterStatus.CONNECTED)
                || (outputStream != null && currentStatus == PrinterStatus.CONNECTED)
    }

    private fun buildReceiptData(
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
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // Initialize printer
        stream.write(EscPosCommands.INIT)

        // Store header - centered and bold
        stream.write(EscPosCommands.CENTER)
        stream.write(EscPosCommands.BOLD_ON)
        stream.write(EscPosCommands.FONT_DOUBLE)
        stream.write("$storeName\n".toByteArray())
        stream.write(EscPosCommands.BOLD_OFF)
        stream.write(EscPosCommands.FONT_NORMAL)
        stream.write("$storeAddress\n".toByteArray())
        stream.write(EscPosCommands.LF)

        // Sale info
        stream.write(EscPosCommands.LEFT)
        stream.write("No. Penjualan: #$saleId\n".toByteArray())
        stream.write("Tanggal: $saleDate\n".toByteArray())
        stream.write("Kasir: $cashierName\n".toByteArray())
        stream.write(EscPosCommands.LF)

        // Separator line
        stream.write("================================\n".toByteArray())

        // Items header
        stream.write(EscPosCommands.BOLD_ON)
        stream.write("Item                    Qty  Total\n".toByteArray())
        stream.write(EscPosCommands.BOLD_OFF)
        stream.write("--------------------------------\n".toByteArray())

        // Items
        items.forEach { item ->
            val itemLine = formatItemLine(item.name, item.quantity, item.totalPrice)
            stream.write("$itemLine\n".toByteArray())
        }

        // Separator
        stream.write("================================\n".toByteArray())

        // Totals
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("id").setRegion("ID").build())

        stream.write("Subtotal: ${currencyFormat.format(subtotal)}\n".toByteArray())
        if (tax > 0) {
            stream.write("Pajak: ${currencyFormat.format(tax)}\n".toByteArray())
        }
        if (discount > 0) {
            stream.write("Diskon: ${currencyFormat.format(discount)}\n".toByteArray())
        }
        stream.write(EscPosCommands.BOLD_ON)
        stream.write("TOTAL: ${currencyFormat.format(total)}\n".toByteArray())
        stream.write(EscPosCommands.BOLD_OFF)

        stream.write(EscPosCommands.LF)
        stream.write("Metode Pembayaran: $paymentMethod\n".toByteArray())
        stream.write(EscPosCommands.LF)

        // Footer
        stream.write(EscPosCommands.CENTER)
        stream.write("Terima Kasih Atas Kunjungannya\n".toByteArray())
        stream.write("Barang yang sudah dibeli tidak dapat\n".toByteArray())
        stream.write("ditukar/dikembalikan\n".toByteArray())
        stream.write(EscPosCommands.LF)
        stream.write(EscPosCommands.LF)

        // Cut paper
        stream.write(EscPosCommands.CUT)

        return stream.toByteArray()
    }

    private fun formatItemLine(name: String, quantity: Int, totalPrice: Double): String {
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("id").setRegion("ID").build())
        val maxNameLength = 20
        val truncatedName = if (name.length > maxNameLength) {
            name.substring(0, maxNameLength - 3) + "..."
        } else {
            name
        }

        val qtyStr = quantity.toString()
        val totalStr = currencyFormat.format(totalPrice)

        // Format: Name (20 chars) | Qty (3 chars) | Total (10 chars)
        return String.format("%-20s %3s %10s", truncatedName, qtyStr, totalStr)
    }

    private fun buildTestReceiptData(): ByteArray {
        val stream = ByteArrayOutputStream()

        stream.write(EscPosCommands.INIT)
        stream.write(EscPosCommands.CENTER)
        stream.write(EscPosCommands.BOLD_ON)
        stream.write(EscPosCommands.FONT_DOUBLE)
        stream.write("TEST RECEIPT\n".toByteArray())
        stream.write(EscPosCommands.BOLD_OFF)
        stream.write(EscPosCommands.FONT_NORMAL)
        stream.write(EscPosCommands.LF)
        stream.write("Printer is working correctly!\n".toByteArray())
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(java.util.Date())
        stream.write("Date: $dateStr\n".toByteArray())
        stream.write(EscPosCommands.LF)
        stream.write(EscPosCommands.LF)
        stream.write(EscPosCommands.CUT)

        return stream.toByteArray()
    }

    private fun buildBarcodeLabelData(
        product: com.chibychibystore.data.local.entity.Produk,
        labelSize: com.chibychibystore.ui.barcode.LabelSize
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // Initialize printer
        stream.write(EscPosCommands.INIT)

        // Set label dimensions based on size
        val (width, height) = when (labelSize) {
            com.chibychibystore.ui.barcode.LabelSize.SMALL -> Pair(20, 10) // 2x1 cm
            com.chibychibystore.ui.barcode.LabelSize.MEDIUM -> Pair(30, 20) // 3x2 cm
            com.chibychibystore.ui.barcode.LabelSize.LARGE -> Pair(30, 20) // 3x2 cm
            com.chibychibystore.ui.barcode.LabelSize.EXTRA_LARGE -> Pair(50, 30) // 5x3 cm
        }

        // Center alignment
        stream.write(EscPosCommands.CENTER)

        // Product name (truncate if too long)
        val productName = product.name.take(width / 2) // Rough character limit
        stream.write(EscPosCommands.BOLD_ON)
        stream.write("$productName\n".toByteArray())
        stream.write(EscPosCommands.BOLD_OFF)

        // Price
        val priceText = "Rp ${product.sellingPrice.toInt()}"
        stream.write("$priceText\n".toByteArray())

        // Generate and print barcode
        try {
            val barcodeBitmap = generateBarcodeBitmap(product.barcode ?: product.id.toString(), width * 8, height * 8)
            val barcodeData = bitmapToEscPos(barcodeBitmap)
            stream.write(barcodeData)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating barcode", e)
            // Fallback: print barcode as text
            stream.write("Barcode: ${product.barcode ?: product.id}\n".toByteArray())
        }

        // Barcode number below
        stream.write("${product.barcode ?: product.id}\n".toByteArray())

        // Feed and cut
        stream.write(EscPosCommands.LF)
        stream.write(EscPosCommands.LF)
        stream.write(EscPosCommands.CUT)

        return stream.toByteArray()
    }

    private fun generateBarcodeBitmap(content: String, width: Int, height: Int): android.graphics.Bitmap {
        val writer = com.google.zxing.MultiFormatWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.CODE_128, width, height)
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        return bitmap
    }

    private fun bitmapToEscPos(bitmap: android.graphics.Bitmap): ByteArray {
        val builder = ByteArrayOutputStream()

        // ESC/POS bitmap printing command
        builder.write(byteArrayOf(0x1B, 0x40)) // Initialize
        builder.write(byteArrayOf(0x1B, 0x2A, 0x00)) // Select bit image mode

        val width = bitmap.width
        val height = bitmap.height

        // Convert bitmap to ESC/POS format
        for (y in 0 until height step 8) {
            for (x in 0 until width) {
                var byte = 0
                for (bit in 0 until 8) {
                    if (y + bit < height && bitmap.getPixel(x, y + bit) == android.graphics.Color.BLACK) {
                        byte = byte or (1 shl (7 - bit))
                    }
                }
                builder.write(byte)
            }
        }

        return builder.toByteArray()
    }
}
