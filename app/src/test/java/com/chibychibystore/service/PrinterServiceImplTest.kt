package com.chibychibystore.service

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.chibychibystore.service.printer.PrinterServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayOutputStream

class PrinterServiceImplTest {

    @Test
    fun printTestReceipt_writesDataToOutput() = runBlocking {
        val context = Mockito.mock(Context::class.java)
        val adapter = Mockito.mock(BluetoothAdapter::class.java)

        val service = PrinterServiceImpl(context, adapter)

        val output = ByteArrayOutputStream()
        service.setTestOutput(output)

        val result = service.printTestReceipt()

        assertTrue("Expected success result", result.isSuccess)
        val outStr = output.toString(Charsets.UTF_8.name())
        // The test receipt writes a 'TEST RECEIPT' string in the data
        assertTrue("Output should contain test label", outStr.contains("TEST RECEIPT") || outStr.contains("Printer is working"))
    }
}
