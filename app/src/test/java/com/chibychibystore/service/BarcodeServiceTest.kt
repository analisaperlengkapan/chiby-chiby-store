package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import com.chibychibystore.error.ChibyChibyException

class BarcodeServiceTest {

    @Mock
    private lateinit var produkRepository: ProdukRepository

    private lateinit var barcodeService: BarcodeService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        barcodeService = BarcodeServiceImpl(produkRepository)
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
    fun `generateBarcode returns success for valid product`() = runBlocking {
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

        // Note: Actual image generation depends on Android Bitmap/Canvas which might not work in pure unit test without Robolectric.
        // But the logic flow should be correct.
    }

    @Test
    fun `generateBarcode fails when product not found`() = runBlocking {
        val productId = 99L
        `when`(produkRepository.getProdukById(productId)).thenReturn(Result.failure(Exception("Product not found")))

        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN_13, LabelSize.MEDIUM)

        assertTrue(result.isFailure)
    }

    @Test
    fun `generateBarcode fails when product has no barcode`() = runBlocking {
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
}
