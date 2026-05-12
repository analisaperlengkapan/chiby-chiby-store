package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.impl.BarcodeServiceImpl
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BarcodeServiceTest {

    @Mock
    private lateinit var produkRepository: ProdukRepository

    private lateinit var barcodeService: BarcodeService

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        barcodeService = BarcodeServiceImpl(produkRepository, testDispatcher)
    }

    @Test
    fun `validateCheckDigit returns true for valid EAN-13`() {
        // Valid EAN-13: 4006381333931
        val validEan13 = "4006381333931"
        assertTrue(barcodeService.validateCheckDigit(validEan13))
    }

    @Test
    fun `calculateCheckDigit calculates correctly`() {
        // 400638133393 -> Check Digit is 1
        val code = "400638133393"
        val checkDigit = barcodeService.calculateCheckDigit(code)
        assertEquals(1, checkDigit)
    }

    @Test
    fun `generateBarcode returns success for valid product`() = runTest(testDispatcher) {
        val productId = 1L
        val product = Produk(
            id = productId,
            name = "Test Product",
            barcode = "1234567890123",
            categoryId = 1,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            warehouseId = 1
        )

        `when`(produkRepository.getProdukById(productId)).thenReturn(Result.success(product))

        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN_13, LabelSize.MEDIUM)

        assertTrue("Expected success but got failure: ${result.exceptionOrNull()}", result.isSuccess)
        val barcodeData = result.getOrNull()
        assertNotNull("Barcode data should not be null", barcodeData)
        assertNotNull("Image data should not be null", barcodeData?.imageData)
        assertTrue("Image data should not be empty", barcodeData?.imageData?.isNotEmpty() == true)
        assertEquals(BarcodeFormat.EAN_13, barcodeData?.format)
        assertEquals(LabelSize.MEDIUM, barcodeData?.size)
    }

    @Test
    fun `generateBarcode fails when product not found`() = runTest(testDispatcher) {
        val productId = 99L
        `when`(produkRepository.getProdukById(productId)).thenReturn(Result.failure(Exception("Product not found")))

        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN_13, LabelSize.MEDIUM)

        assertTrue(result.isFailure)
    }

    @Test
    fun `generateBarcode fails when product has no barcode`() = runTest(testDispatcher) {
        val productId = 2L
        val product = Produk(
            id = productId,
            name = "No Barcode Product",
            barcode = null,
            categoryId = 1,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            warehouseId = 1
        )

        `when`(produkRepository.getProdukById(productId)).thenReturn(Result.success(product))

        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN_13, LabelSize.MEDIUM)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ChibyChibyException.ValidationError)
    }

    @Test
    fun `generateNewBarcodeValue generates valid format and handles collisions`() = runTest(testDispatcher) {
        // Setup mock for collision check
        // First call matches any string and returns Success (simulating collision)
        // Second call matches any string and returns Failure (simulating not found/available)
        `when`(produkRepository.getProdukByBarcode(anyString()))
            .thenReturn(Result.success(Produk(
                id = 999L,
                name = "Existing Product",
                barcode = "2230101123456",
                categoryId = 1,
                costPrice = 0.0,
                sellingPrice = 0.0,
                warehouseId = 1
            )))
            .thenReturn(Result.failure(Exception("Not found")))

        val barcode = barcodeService.generateNewBarcodeValue()

        // Validate format
        assertNotNull(barcode)
        assertEquals(13, barcode.length)
        assertTrue("Barcode should start with prefix '2'", barcode.startsWith("2"))

        // Date part
        val datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
        assertEquals("Barcode should contain current date", datePart, barcode.substring(1, 7))

        // Digits
        assertTrue("Barcode should contain only digits", barcode.all { it.isDigit() })

        // Check digit validity
        assertTrue("Check digit should be valid", barcodeService.validateCheckDigit(barcode))
    }
}
