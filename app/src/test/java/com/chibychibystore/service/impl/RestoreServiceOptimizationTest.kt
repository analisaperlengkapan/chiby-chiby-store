package com.chibychibystore.service.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.*
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RestoreServiceOptimizationTest {

    @Mock private lateinit var database: ChibyChibyDatabase
    @Mock private lateinit var userRepository: PenggunaRepository
    @Mock private lateinit var categoryRepository: KategoriRepository
    @Mock private lateinit var warehouseRepository: GudangRepository
    @Mock private lateinit var productRepository: ProdukRepository
    @Mock private lateinit var supplierRepository: PemasokRepository
    @Mock private lateinit var saleRepository: PenjualanRepository
    @Mock private lateinit var saleItemRepository: ItemPenjualanRepository
    @Mock private lateinit var purchaseRepository: PembelianRepository
    @Mock private lateinit var purchaseItemRepository: ItemPembelianRepository
    @Mock private lateinit var expenseRepository: PengeluaranRepository

    private lateinit var restoreService: RestoreServiceImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        restoreService = RestoreServiceImpl(
            context, database, userRepository, categoryRepository, warehouseRepository,
            productRepository, supplierRepository, saleRepository, saleItemRepository,
            purchaseRepository, purchaseItemRepository, expenseRepository
        )
    }

    @Test
    fun `restoreProduks should use batch insertion first`() = runTest {
        // Arrange
        val products = List(5) {
            Produk(
                id = it.toLong() + 1,
                name = "Product $it",
                categoryId = 1,
                costPrice = 100.0,
                sellingPrice = 150.0,
                warehouseId = 1,
                minStock = 5,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        // Mock createProdukList to return success
        `when`(productRepository.createProdukList(any())).thenReturn(Result.success(5))

        // Act
        val count = restoreService.restoreProduks(products)

        // Assert
        assert(count == 5)
        verify(productRepository).createProdukList(any())
        // Ensure individual insert is NOT called
        verify(productRepository, never()).createProduk(any())
    }

    @Test
    fun `restoreProduks should fallback to loop if batch fails`() = runTest {
        // Arrange
        val products = List(2) {
            Produk(
                id = it.toLong() + 1,
                name = "Product $it",
                categoryId = 1,
                costPrice = 100.0,
                sellingPrice = 150.0,
                warehouseId = 1,
                minStock = 5,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        // Mock createProdukList to return failure
        `when`(productRepository.createProdukList(any())).thenReturn(Result.failure(Exception("Batch failed")))
        // Mock individual insert to succeed
        `when`(productRepository.createProduk(any())).thenReturn(Result.success(1L))

        // Act
        val count = restoreService.restoreProduks(products)

        // Assert
        assert(count == 2)
        verify(productRepository).createProdukList(any())
        verify(productRepository, times(2)).createProduk(any())
    }
}
