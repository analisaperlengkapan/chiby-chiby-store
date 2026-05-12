package com.chibychibystore.service

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.chibychibystore.service.printer.PrinterServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import kotlin.system.measureTimeMillis

class PrinterServiceBenchmarkTest {

    @Test
    fun benchmarkPrinting() = runBlocking {
        val context = Mockito.mock(Context::class.java)
        val adapter = Mockito.mock(BluetoothAdapter::class.java)
        val service = PrinterServiceImpl(context, adapter)

        // Mock connection
        service.setTestOutput(ByteArrayOutputStream())

        // Number of concurrent jobs.
        // Dispatchers.IO usually has 64 threads max.
        // We need enough jobs to exhaust the pool to see the blocking effect.
        val jobCount = 100

        val time = measureTimeMillis {
            val jobs = List(jobCount) {
                async(Dispatchers.Default) { // Launch from outside, service uses IO internally
                    service.printTestReceipt()
                }
            }
            jobs.awaitAll()
        }

        println("Benchmark Result: Processed $jobCount jobs in $time ms")
    }
}
