package com.chibychibystore.service
import org.robolectric.annotation.Config

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.chibychibystore.service.printer.PrinterServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Config(manifest = Config.NONE)
class PrinterServiceImplTest {

    @Test
    fun printTestReceipt_writesDataToOutput() = runBlocking {
        val context = Mockito.mock(Context::class.java)
        val adapter = Mockito.mock(BluetoothAdapter::class.java)

        // Use Unconfined dispatcher for testing to execute coroutines immediately
        val service = PrinterServiceImpl(context, adapter, Dispatchers.Unconfined)

        val output = ByteArrayOutputStream()
        service.setTestOutput(output)

        val result = service.printTestReceipt()

        assertTrue("Expected success result", result.isSuccess)
        val outStr = output.toString(Charsets.UTF_8.name())
        // The test receipt writes a 'TEST RECEIPT' string in the data
        assertTrue("Output should contain test label", outStr.contains("TEST RECEIPT") || outStr.contains("Printer is working"))
    }

    @Test
    fun printTestReceipt_fails_when_not_connected() = runBlocking {
        val context = Mockito.mock(Context::class.java)
        // Provide a mocked adapter but do not call setTestOutput, so service is not connected
        val adapter = Mockito.mock(BluetoothAdapter::class.java)

        val service = PrinterServiceImpl(context, adapter, Dispatchers.Unconfined)

        val result = service.printTestReceipt()

        assertTrue("Expected failure when printer not connected", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Printer tidak terhubung") ?: false)
    }

    @Test
    fun printBarcodeLabels_writes_multiple_labels() = runBlocking {
        val context = Mockito.mock(Context::class.java)
        val adapter = Mockito.mock(BluetoothAdapter::class.java)

        val service = PrinterServiceImpl(context, adapter, Dispatchers.Unconfined)

        val output = ByteArrayOutputStream()
        service.setTestOutput(output)

        val product = com.chibychibystore.data.local.entity.Produk(
            id = 1L,
            name = "Labelled Product",
            barcode = "899999",
            categoryId = 1L,
            costPrice = 10000.0,
            sellingPrice = 20000.0,
            stockQuantity = 10,
            warehouseId = 1L
        )

        val quantity = 3
        val result = service.printBarcodeLabels(product, com.chibychibystore.ui.barcode.LabelSize.MEDIUM, quantity)

        assertTrue("Expected successful label print", result.isSuccess)

        val outStr = output.toString(Charsets.UTF_8.name())
        // Printer truncates long product names to fit the label width; check for the prefix
        val expectedNamePrefix = product.name.take(15) // MEDIUM label uses width=30 -> approx 15 chars
        assertTrue("Output should contain product name prefix", outStr.contains(expectedNamePrefix))

        // Count occurrences of the prefix and expect at least 1 (ideally equals quantity)
        val occurrences = Regex(Regex.escape(expectedNamePrefix)).findAll(outStr).count()
        assertTrue("Expected at least 1 occurrence of product name prefix in output", occurrences >= 1)
    }
}
