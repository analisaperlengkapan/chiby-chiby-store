package com.chibychibystore.service

import com.chibychibystore.data.Result
import com.chibychibystore.data.model.Produk
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

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
    fun `generateBarcode should return success for valid EAN13 barcode`() = runTest {
        // Given
        val productId = 1L
        val format = BarcodeFormat.EAN13
        val size = LabelSize.MEDIUM
        val mockProduct = Produk(
            id = productId,
            nama = "Test Product",
            barcode = "8991234567890",
            kategoriId = 1,
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stokQuantity = 10,
            gudangId = 1
        )

        `when`(produkRepository.getProduk(productId)).thenReturn(mockProduct)

        // When
        val result = barcodeService.generateBarcode(productId, format, size)

        // Then
        assertTrue(result is Result.Success)
        val barcodeData = (result as Result.Success).data
        assertEquals("8991234567890", barcodeData.barcode)
        assertEquals(format, barcodeData.format)
        assertEquals(size, barcodeData.size)
    }

    @Test
    fun `generateBarcode should return error for non-existent product`() = runTest {
        // Given
        val productId = 999L
        `when`(produkRepository.getProduk(productId)).thenReturn(null)

        // When
        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN13, LabelSize.SMALL)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `generateBarcode should return error for product without barcode`() = runTest {
        // Given
        val productId = 1L
        val mockProduct = Produk(
            id = productId,
            nama = "Test Product",
            barcode = null,
            kategoriId = 1,
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stokQuantity = 10,
            gudangId = 1
        )

        `when`(produkRepository.getProduk(productId)).thenReturn(mockProduct)

        // When
        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN13, LabelSize.SMALL)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak memiliki barcode") == true)
    }

    @Test
    fun `validateBarcode should return true for valid EAN13 with correct check digit`() = runTest {
        // Given
        val validBarcode = "1234567890128" // Valid EAN13 with correct check digit

        // When
        val result = barcodeService.validateBarcode(validBarcode)

        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
    }

    @Test
    fun `validateBarcode should return false for invalid EAN13 check digit`() = runTest {
        // Given
        val invalidBarcode = "1234567890123" // Invalid check digit

        // When
        val result = barcodeService.validateBarcode(invalidBarcode)

        // Then
        assertTrue(result is Result.Success)
        assertFalse((result as Result.Success).data)
    }

    @Test
    fun `validateBarcode should return error for invalid length`() = runTest {
        // Given
        val invalidBarcode = "12345" // Too short

        // When
        val result = barcodeService.validateBarcode(invalidBarcode)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("panjang") == true)
    }

    @Test
    fun `calculateCheckDigit should return correct check digit for EAN13`() {
        // Given
        val barcodeWithoutCheck = "123456789012"

        // When
        val result = barcodeService.calculateCheckDigit(barcodeWithoutCheck)

        // Then
        assertTrue(result is Result.Success)
        val checkDigit = (result as Result.Success).data
        assertEquals(8, checkDigit) // Expected check digit for this sequence
    }

    @Test
    fun `calculateCheckDigit should return error for invalid input length`() {
        // Given
        val invalidInput = "12345"

        // When
        val result = barcodeService.calculateCheckDigit(invalidInput)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("panjang") == true)
    }

    @Test
    fun `getSupportedFormats should return all barcode formats`() {
        // When
        val result = barcodeService.getSupportedFormats()

        // Then
        assertTrue(result is Result.Success)
        val formats = (result as Result.Success).data
        assertTrue(formats.contains(BarcodeFormat.EAN13))
        assertTrue(formats.contains(BarcodeFormat.CODE128))
        assertTrue(formats.contains(BarcodeFormat.QR_CODE))
        assertTrue(formats.contains(BarcodeFormat.DATA_MATRIX))
    }

    @Test
    fun `generateBarcode should handle GS1 Indonesia prefix correctly`() = runTest {
        // Given
        val productId = 1L
        val mockProduct = Produk(
            id = productId,
            nama = "Indonesian Product",
            barcode = "8999876543210", // GS1 Indonesia prefix
            kategoriId = 1,
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stokQuantity = 10,
            gudangId = 1
        )

        `when`(produkRepository.getProduk(productId)).thenReturn(mockProduct)

        // When
        val result = barcodeService.generateBarcode(productId, BarcodeFormat.EAN13, LabelSize.LARGE)

        // Then
        assertTrue(result is Result.Success)
        val barcodeData = (result as Result.Success).data
        assertEquals("8999876543210", barcodeData.barcode)
        assertEquals(LabelSize.LARGE, barcodeData.size)
    }
}