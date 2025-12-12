package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.repository.ProdukRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

class ProductServiceTest {

    @Mock
    private lateinit var produkRepository: ProdukRepository

    private lateinit var productService: ProductService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        productService = ProductServiceImpl(produkRepository)
    }

    @Test
    fun `createProduct should return success when product is valid and barcode is unique`() = runTest {
        // Given
        val kategori = Kategori("1", "Food", "Makanan")
        val product = Produk(
            id = "1",
            nama = "Test Product",
            barcode = "123456789",
            kategoriId = "1",
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stok = 10,
            gudangId = "1",
            minStok = 5
        )

        `when`(produkRepository.getProductByBarcode("123456789")).thenReturn(null)
        `when`(produkRepository.createProduct(product)).thenReturn(product)

        // When
        val result = productService.createProduct(product)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(product, result.getOrNull())
        verify(produkRepository).createProduct(product)
    }

    @Test
    fun `createProduct should return failure when barcode already exists`() = runTest {
        // Given
        val existingProduct = Produk(
            id = "existing",
            nama = "Existing Product",
            barcode = "123456789",
            kategoriId = "1",
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stok = 5,
            gudangId = "1"
        )
        val newProduct = Produk(
            id = "new",
            nama = "New Product",
            barcode = "123456789", // Same barcode
            kategoriId = "1",
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stok = 10,
            gudangId = "1"
        )

        `when`(produkRepository.getProductByBarcode("123456789")).thenReturn(existingProduct)

        // When
        val result = productService.createProduct(newProduct)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Barcode sudah digunakan", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).createProduct(any())
    }

    @Test
    fun `createProduct should return failure when selling price is less than cost price`() = runTest {
        // Given
        val product = Produk(
            id = "1",
            nama = "Test Product",
            barcode = "123456789",
            kategoriId = "1",
            hargaBeli = 15000.0,
            hargaJual = 10000.0, // Less than cost price
            stok = 10,
            gudangId = "1"
        )

        // When
        val result = productService.createProduct(product)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Harga jual harus lebih besar dari harga beli", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).createProduct(any())
    }

    @Test
    fun `updateProduct should return success when product is valid and barcode is unique`() = runTest {
        // Given
        val product = Produk(
            id = "1",
            nama = "Updated Product",
            barcode = "123456789",
            kategoriId = "1",
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stok = 10,
            gudangId = "1"
        )

        `when`(produkRepository.getProductByBarcode("123456789")).thenReturn(null)
        `when`(produkRepository.updateProduct(product)).thenReturn(product)

        // When
        val result = productService.updateProduct(product)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(product, result.getOrNull())
        verify(produkRepository).updateProduct(product)
    }

    @Test
    fun `updateStock should return success when quantity is valid`() = runTest {
        // Given
        val productId = "1"
        val currentStock = 10
        val additionalStock = 5
        val expectedNewStock = 15

        val product = Produk(
            id = productId,
            nama = "Test Product",
            barcode = "123456789",
            kategoriId = "1",
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stok = currentStock,
            gudangId = "1"
        )

        val updatedProduct = product.copy(stok = expectedNewStock)

        `when`(produkRepository.getProduct(productId)).thenReturn(product)
        `when`(produkRepository.updateProduct(updatedProduct)).thenReturn(updatedProduct)

        // When
        val result = productService.updateStock(productId, additionalStock)

        // Then
        assertTrue(result.isSuccess)
        verify(produkRepository).updateProduct(updatedProduct)
    }

    @Test
    fun `updateStock should return failure when resulting stock would be negative`() = runTest {
        // Given
        val productId = "1"
        val currentStock = 5
        val reduction = -10 // Would result in negative stock

        val product = Produk(
            id = productId,
            nama = "Test Product",
            barcode = "123456789",
            kategoriId = "1",
            hargaBeli = 10000.0,
            hargaJual = 15000.0,
            stok = currentStock,
            gudangId = "1"
        )

        `when`(produkRepository.getProduct(productId)).thenReturn(product)

        // When
        val result = productService.updateStock(productId, reduction)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Stok tidak boleh negatif", result.exceptionOrNull()?.message)
        verify(produkRepository, never()).updateProduct(any())
    }

    @Test
    fun `getProducts should return all products when no filter provided`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1"),
            Produk("2", "Product 2", "222", "1", 20000.0, 25000.0, 5, "1")
        )

        `when`(produkRepository.getProducts()).thenReturn(products)

        // When
        val result = productService.getProducts()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(products, result.getOrNull())
        verify(produkRepository).getProducts()
    }

    @Test
    fun `searchProducts should return filtered products when query provided`() = runTest {
        // Given
        val query = "test"
        val products = listOf(
            Produk("1", "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")
        )

        `when`(produkRepository.searchProducts(query)).thenReturn(products)

        // When
        val result = productService.searchProducts(query)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(products, result.getOrNull())
        verify(produkRepository).searchProducts(query)
    }

    @Test
    fun `getLowStockProducts should return products below minimum stock`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Low Stock Product", "111", "1", 10000.0, 15000.0, 2, "1", minStok = 5),
            Produk("2", "Normal Stock Product", "222", "1", 20000.0, 25000.0, 10, "1", minStok = 5)
        )

        `when`(produkRepository.getProducts()).thenReturn(products)

        // When
        val result = productService.getLowStockProducts()

        // Then
        assertTrue(result.isSuccess)
        val lowStockProducts = result.getOrNull()
        assertEquals(1, lowStockProducts?.size)
        assertEquals("1", lowStockProducts?.first()?.id)
    }

    @Test
    fun `observeProducts should return flow from repository`() = runTest {
        // Given
        val products = listOf(
            Produk("1", "Product 1", "111", "1", 10000.0, 15000.0, 10, "1")
        )
        val flow = flowOf(products)

        `when`(produkRepository.observeProducts()).thenReturn(flow)

        // When
        val result = productService.observeProducts().first()

        // Then
        assertEquals(products, result)
        verify(produkRepository).observeProducts()
    }
}