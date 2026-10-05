package com.chibychibystore.service.impl

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.constant.Permissions
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.AuthService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Date

/**
 * Restores the service-level product coverage lost with the deleted
 * `ProductServiceTest`. The legacy suite targeted a different API (barcode
 * uniqueness enforced in the service); the rules now live in
 * [ProdukRepository], so these cases pin the behaviour [ProductServiceImpl]
 * still owns: permission gates, validation, and the two-table stock write.
 */
@RunWith(RobolectricTestRunner::class)
class ProductServiceImplTest {

    private lateinit var productRepository: ProdukRepository
    private lateinit var stokGudangRepository: StokGudangRepository
    private lateinit var authService: AuthService
    private lateinit var service: ProductServiceImpl

    @Before
    fun setup() {
        productRepository = mock()
        stokGudangRepository = mock()
        authService = mock()
        runBlocking { whenever(authService.hasPermission(any())).thenReturn(true) }
        service = ProductServiceImpl(productRepository, stokGudangRepository, authService)
    }

    private fun produk(
        name: String = "Produk",
        stock: Int = 10,
        warehouseId: Long = 1L
    ) = Produk(
        id = 1L,
        name = name,
        barcode = "111",
        categoryId = 1L,
        costPrice = 5.0,
        sellingPrice = 10.0,
        stockQuantity = stock,
        warehouseId = warehouseId,
        createdAt = Date()
    )

    @Test
    fun `createProduk rejects a blank name`() = runTest {
        val result = service.createProduk(produk(name = "  "))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ChibyChibyException.ValidationError)
        verify(productRepository, never()).createProduk(any())
    }

    @Test
    fun `createProduk is denied without EDIT_INVENTORY`() = runTest {
        whenever(authService.hasPermission(Permissions.EDIT_INVENTORY)).thenReturn(false)

        val result = service.createProduk(produk())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ChibyChibyException.PermissionError)
        verify(productRepository, never()).createProduk(any())
    }

    @Test
    fun `createProduk surfaces a repository barcode conflict and writes no stock`() = runTest {
        val p = produk()
        whenever(productRepository.createProduk(p))
            .thenReturn(Result.failure(ChibyChibyException.ValidationError("barcode", "Barcode sudah digunakan")))

        val result = service.createProduk(p)

        assertTrue(result.isFailure)
        verify(stokGudangRepository, never()).insertOrUpdateStock(any())
    }

    @Test
    fun `updateStock syncs both the product row and the warehouse stock`() = runTest {
        val p = produk(stock = 100, warehouseId = 3L)
        whenever(productRepository.getProdukById(1L)).thenReturn(Result.success(p))
        whenever(productRepository.updateStock(1L, 40)).thenReturn(Result.success(Unit))
        whenever(stokGudangRepository.insertOrUpdateStock(any())).thenReturn(Result.success(Unit))

        val result = service.updateStock("1", 40)

        assertTrue(result.isSuccess)
        verify(productRepository).updateStock(1L, 40)
        val captor = argumentCaptor<StokGudang>()
        verify(stokGudangRepository).insertOrUpdateStock(captor.capture())
        assertEquals(1L, captor.firstValue.productId)
        assertEquals(3L, captor.firstValue.warehouseId)
        assertEquals(40, captor.firstValue.quantity)
    }

    @Test
    fun `updateStock rejects a non-numeric id`() = runTest {
        val result = service.updateStock("abc", 10)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ChibyChibyException.ValidationError)
        verify(productRepository, never()).updateStock(any(), any())
    }

    @Test
    fun `getProductsByIds returns the repository result`() = runTest {
        whenever(productRepository.getProductsByIds(listOf(1L, 2L)))
            .thenReturn(Result.success(listOf(produk())))

        val result = service.getProductsByIds(listOf(1L, 2L))

        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getProductsByIds is denied without VIEW_INVENTORY`() = runTest {
        whenever(authService.hasPermission(Permissions.VIEW_INVENTORY)).thenReturn(false)

        val result = service.getProductsByIds(listOf(1L))

        assertTrue(result.isFailure)
        verify(productRepository, never()).getProductsByIds(any())
    }

    @Test
    fun `createProduk initialises the warehouse stock against a real database`() = runBlocking {
        // The per-warehouse row and the product's total quantity must agree after
        // a create; this drives the real StokGudangRepository over an in-memory DB.
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        val kategoriId = database.kategoriDao().insertKategori(Kategori(name = "Kat"))
        val gudangId = database.gudangDao().insertGudang(Gudang(name = "Gudang"))

        val realService = ProductServiceImpl(
            ProdukRepository(database.produkDao()),
            StokGudangRepository(database.stokGudangDao(), database.produkDao()),
            authService
        )

        val created = realService.createProduk(
            Produk(
                name = "Beras",
                barcode = "999",
                categoryId = kategoriId,
                costPrice = 10_000.0,
                sellingPrice = 12_000.0,
                stockQuantity = 25,
                warehouseId = gudangId
            )
        )

        assertTrue(created.isSuccess)
        val productId = created.getOrNull()!!.id
        assertEquals(25, database.stokGudangDao().getStock(productId, gudangId)?.quantity)
        assertEquals(25, database.produkDao().getProdukById(productId)?.stockQuantity)

        database.close()
    }

    @Test
    fun `create update and delete round-trip against a real database`() = runBlocking {
        // Restores the create/update/delete flow the deleted
        // `ProductDetailViewModelIntegrationTest` exercised through the ViewModel.
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        val kategoriId = database.kategoriDao().insertKategori(Kategori(name = "Kat"))
        val gudangId = database.gudangDao().insertGudang(Gudang(name = "Gudang"))

        val realService = ProductServiceImpl(
            ProdukRepository(database.produkDao()),
            StokGudangRepository(database.stokGudangDao(), database.produkDao()),
            authService
        )

        val created = realService.createProduk(
            Produk(
                name = "NewProd",
                barcode = "8990001",
                categoryId = kategoriId,
                costPrice = 10_000.0,
                sellingPrice = 15_000.0,
                stockQuantity = 5,
                warehouseId = gudangId
            )
        ).getOrNull()!!
        assertEquals(5, created.stockQuantity)

        val updated = realService.updateProduk(created.copy(name = "UpdatedName", sellingPrice = 16_000.0)).getOrNull()!!
        assertEquals("UpdatedName", updated.name)
        assertEquals("UpdatedName", database.produkDao().getProdukById(created.id)?.name)

        assertTrue(realService.updateStock(created.id.toString(), 12).isSuccess)
        assertEquals(12, database.produkDao().getProdukById(created.id)?.stockQuantity)
        assertEquals(12, database.stokGudangDao().getStock(created.id, gudangId)?.quantity)

        assertTrue(realService.deleteProduk(created.id.toString()).isSuccess)
        assertTrue(database.produkDao().getProdukById(created.id) == null)

        database.close()
    }
}
