package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.data.model.Result
import com.chibychibystore.data.model.Penjualan
import com.chibychibystore.data.model.ItemPenjualan
import com.chibychibystore.data.model.Produk
import com.chibychibystore.repository.PenjualanRepository
import com.chibychibystore.repository.ItemPenjualanRepository
import com.chibychibystore.repository.ProdukRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

class SaleServiceTest {

    @Mock
    private lateinit var penjualanRepository: PenjualanRepository

    @Mock
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository

    @Mock
    private lateinit var produkRepository: ProdukRepository

    private lateinit var saleService: SaleService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        saleService = SaleServiceImpl(
            penjualanRepository,
            itemPenjualanRepository,
            produkRepository
        )
    }

    @Test
    fun `createSale should create sale and update inventory correctly`() = runTest {
        // Given
        val saleItems = listOf(
            ItemPenjualan(penjualanId = 0, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0),
            ItemPenjualan(penjualanId = 0, produkId = 2, quantity = 1, unitPrice = 30000.0, totalPrice = 30000.0)
        )
        val sale = Penjualan(
            tanggalPenjualan = LocalDate.now(),
            totalAmount = 130000.0,
            metodePembayaran = "CASH",
            kasirId = 1,
            items = saleItems
        )

        val mockProduct1 = Produk(
            id = 1, nama = "Product A", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1
        )
        val mockProduct2 = Produk(
            id = 2, nama = "Product B", barcode = "1234567890124",
            kategoriId = 1, hargaBeli = 20000.0, hargaJual = 30000.0,
            stokQuantity = 5, gudangId = 1
        )

        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct1)
        `when`(produkRepository.getProduk(2)).thenReturn(mockProduct2)
        `when`(penjualanRepository.insertPenjualan(any())).thenReturn(1L)
        `when`(itemPenjualanRepository.insertItemPenjualan(any())).thenReturn(1L, 2L)

        // When
        val result = saleService.createSale(sale)

        // Then
        assertTrue(result is Result.Success)
        val createdSale = (result as Result.Success).data

        // Verify sale was created
        verify(penjualanRepository).insertPenjualan(any())

        // Verify sale items were created
        verify(itemPenjualanRepository, times(2)).insertItemPenjualan(any())

        // Verify inventory was updated
        verify(produkRepository).updateProduk(argThat { it.id == 1 && it.stokQuantity == 8 }) // 10 - 2
        verify(produkRepository).updateProduk(argThat { it.id == 2 && it.stokQuantity == 4 }) // 5 - 1
    }

    @Test
    fun `createSale should return error when insufficient stock`() = runTest {
        // Given
        val saleItems = listOf(
            ItemPenjualan(penjualanId = 0, produkId = 1, quantity = 15, unitPrice = 50000.0, totalPrice = 750000.0)
        )
        val sale = Penjualan(
            tanggalPenjualan = LocalDate.now(),
            totalAmount = 750000.0,
            metodePembayaran = "CASH",
            kasirId = 1,
            items = saleItems
        )

        val mockProduct = Produk(
            id = 1, nama = "Product A", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1 // Only 10 in stock
        )

        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct)

        // When
        val result = saleService.createSale(sale)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertTrue(error.exception.message?.contains("stok tidak mencukupi") == true)
    }

    @Test
    fun `createSale should return error when product not found`() = runTest {
        // Given
        val saleItems = listOf(
            ItemPenjualan(penjualanId = 0, produkId = 999, quantity = 1, unitPrice = 50000.0, totalPrice = 50000.0)
        )
        val sale = Penjualan(
            tanggalPenjualan = LocalDate.now(),
            totalAmount = 50000.0,
            metodePembayaran = "CASH",
            kasirId = 1,
            items = saleItems
        )

        `when`(produkRepository.getProduk(999)).thenReturn(null)

        // When
        val result = saleService.createSale(sale)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `getSale should return sale with items correctly`() = runTest {
        // Given
        val saleId = 1L
        val mockSale = Penjualan(
            id = saleId,
            tanggalPenjualan = LocalDate.of(2025, 1, 15),
            totalAmount = 130000.0,
            metodePembayaran = "CASH",
            kasirId = 1
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = saleId, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0),
            ItemPenjualan(id = 2, penjualanId = saleId, produkId = 2, quantity = 1, unitPrice = 30000.0, totalPrice = 30000.0)
        )

        `when`(penjualanRepository.getPenjualan(saleId)).thenReturn(mockSale)
        `when`(itemPenjualanRepository.getItemPenjualanByPenjualanId(saleId)).thenReturn(mockSaleItems)

        // When
        val result = saleService.getSale(saleId)

        // Then
        assertTrue(result is Result.Success)
        val saleWithItems = (result as Result.Success).data
        assertEquals(saleId, saleWithItems.id)
        assertEquals(130000.0, saleWithItems.totalAmount)
        assertEquals(2, saleWithItems.items.size)
        assertEquals(100000.0, saleWithItems.items[0].totalPrice)
        assertEquals(30000.0, saleWithItems.items[1].totalPrice)
    }

    @Test
    fun `getSale should return null for non-existent sale`() = runTest {
        // Given
        val saleId = 999L
        `when`(penjualanRepository.getPenjualan(saleId)).thenReturn(null)

        // When
        val result = saleService.getSale(saleId)

        // Then
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }

    @Test
    fun `getSales should return sales list correctly`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 31)
        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = LocalDate.of(2025, 1, 15), totalAmount = 50000.0, metodePembayaran = "CASH", kasirId = 1),
            Penjualan(id = 2, tanggalPenjualan = LocalDate.of(2025, 1, 20), totalAmount = 75000.0, metodePembayaran = "CARD", kasirId = 2)
        )

        `when`(penjualanRepository.getPenjualanByDateRange(startDate, endDate)).thenReturn(mockSales)

        // When
        val result = saleService.getSales(startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val sales = (result as Result.Success).data
        assertEquals(2, sales.size)
        assertEquals(50000.0, sales[0].totalAmount)
        assertEquals(75000.0, sales[1].totalAmount)
    }

    @Test
    fun `refundSale should restore inventory and mark sale as refunded`() = runTest {
        // Given
        val saleId = 1L
        val mockSale = Penjualan(
            id = saleId,
            tanggalPenjualan = LocalDate.of(2025, 1, 15),
            totalAmount = 130000.0,
            metodePembayaran = "CASH",
            kasirId = 1
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = saleId, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0),
            ItemPenjualan(id = 2, penjualanId = saleId, produkId = 2, quantity = 1, unitPrice = 30000.0, totalPrice = 30000.0)
        )
        val mockProduct1 = Produk(
            id = 1, nama = "Product A", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 8, gudangId = 1 // Current stock after sale
        )
        val mockProduct2 = Produk(
            id = 2, nama = "Product B", barcode = "1234567890124",
            kategoriId = 1, hargaBeli = 20000.0, hargaJual = 30000.0,
            stokQuantity = 4, gudangId = 1
        )

        `when`(penjualanRepository.getPenjualan(saleId)).thenReturn(mockSale)
        `when`(itemPenjualanRepository.getItemPenjualanByPenjualanId(saleId)).thenReturn(mockSaleItems)
        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct1)
        `when`(produkRepository.getProduk(2)).thenReturn(mockProduct2)

        // When
        val result = saleService.refundSale(saleId)

        // Then
        assertTrue(result is Result.Success)

        // Verify inventory was restored
        verify(produkRepository).updateProduk(argThat { it.id == 1 && it.stokQuantity == 10 }) // 8 + 2
        verify(produkRepository).updateProduk(argThat { it.id == 2 && it.stokQuantity == 5 }) // 4 + 1

        // Verify sale was marked as refunded (this would be implementation specific)
        verify(penjualanRepository).updatePenjualan(any())
    }

    @Test
    fun `refundSale should return error for non-existent sale`() = runTest {
        // Given
        val saleId = 999L
        `when`(penjualanRepository.getPenjualan(saleId)).thenReturn(null)

        // When
        val result = saleService.refundSale(saleId)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `cancelSale should restore inventory without refund processing`() = runTest {
        // Given
        val saleId = 1L
        val mockSale = Penjualan(
            id = saleId,
            tanggalPenjualan = LocalDate.of(2025, 1, 15),
            totalAmount = 130000.0,
            metodePembayaran = "CASH",
            kasirId = 1
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = saleId, produkId = 1, quantity = 2, unitPrice = 50000.0, totalPrice = 100000.0)
        )
        val mockProduct = Produk(
            id = 1, nama = "Product A", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 8, gudangId = 1
        )

        `when`(penjualanRepository.getPenjualan(saleId)).thenReturn(mockSale)
        `when`(itemPenjualanRepository.getItemPenjualanByPenjualanId(saleId)).thenReturn(mockSaleItems)
        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct)

        // When
        val result = saleService.cancelSale(saleId)

        // Then
        assertTrue(result is Result.Success)

        // Verify inventory was restored
        verify(produkRepository).updateProduk(argThat { it.id == 1 && it.stokQuantity == 10 }) // 8 + 2

        // Verify sale was deleted
        verify(penjualanRepository).deletePenjualan(saleId)
        verify(itemPenjualanRepository).deleteItemPenjualanByPenjualanId(saleId)
    }

    @Test
    fun `cancelSale should return error for non-existent sale`() = runTest {
        // Given
        val saleId = 999L
        `when`(penjualanRepository.getPenjualan(saleId)).thenReturn(null)

        // When
        val result = saleService.cancelSale(saleId)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertTrue(error.exception.message?.contains("tidak ditemukan") == true)
    }

    @Test
    fun `createSale should validate payment method correctly`() = runTest {
        // Given
        val saleItems = listOf(
            ItemPenjualan(penjualanId = 0, produkId = 1, quantity = 1, unitPrice = 50000.0, totalPrice = 50000.0)
        )
        val sale = Penjualan(
            tanggalPenjualan = LocalDate.now(),
            totalAmount = 50000.0,
            metodePembayaran = "INVALID", // Invalid payment method
            kasirId = 1,
            items = saleItems
        )

        val mockProduct = Produk(
            id = 1, nama = "Product A", barcode = "1234567890123",
            kategoriId = 1, hargaBeli = 30000.0, hargaJual = 50000.0,
            stokQuantity = 10, gudangId = 1
        )

        `when`(produkRepository.getProduk(1)).thenReturn(mockProduct)

        // When
        val result = saleService.createSale(sale)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertTrue(error.exception.message?.contains("metode pembayaran") == true)
    }

    @Test
    fun `createSale should handle empty sale items correctly`() = runTest {
        // Given
        val sale = Penjualan(
            tanggalPenjualan = LocalDate.now(),
            totalAmount = 0.0,
            metodePembayaran = "CASH",
            kasirId = 1,
            items = emptyList()
        )

        // When
        val result = saleService.createSale(sale)

        // Then
        assertTrue(result is Result.Failure)
        val error = result as Result.Failure
        assertTrue(error.exception.message?.contains("item penjualan") == true)
    }
}