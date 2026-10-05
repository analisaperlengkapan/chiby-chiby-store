package com.chibychibystore.service.impl

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.AuthService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Date

/**
 * Restores the warehouse coverage lost with the deleted `WarehouseServiceTest`.
 * The legacy suite targeted an older service that owned name-uniqueness and
 * capacity rules; those now live in [GudangRepository]. The behaviour
 * [WarehouseServiceImpl] still owns — permission gates, the transfer
 * pre-conditions, and the two-table stock move — is pinned here, with the stock
 * moves driven against a real in-memory database.
 */
@RunWith(RobolectricTestRunner::class)
class WarehouseServiceImplTest {

    private lateinit var warehouseRepository: GudangRepository
    private lateinit var productRepository: ProdukRepository
    private lateinit var stokGudangRepository: StokGudangRepository
    private lateinit var authService: AuthService
    private lateinit var service: WarehouseServiceImpl

    @Before
    fun setup() {
        warehouseRepository = mock()
        productRepository = mock()
        stokGudangRepository = mock()
        authService = mock()
        runBlocking { whenever(authService.hasPermission(any())).thenReturn(true) }
        service = WarehouseServiceImpl(
            warehouseRepository, productRepository, stokGudangRepository, authService, mock()
        )
    }

    private fun gudang(id: Long = 1L, name: String = "Gudang") =
        Gudang(id = id, name = name, location = "Lokasi", capacity = 100)

    private fun produk(id: Long = 1L, name: String = "Beras", warehouseId: Long = 1L, stock: Int = 10) =
        Produk(
            id = id,
            name = name,
            barcode = "111",
            categoryId = 1L,
            costPrice = 5_000.0,
            sellingPrice = 7_000.0,
            stockQuantity = stock,
            warehouseId = warehouseId,
            createdAt = Date()
        )

    @Test
    fun `createGudang is denied without MANAGE_WAREHOUSES`() = runTest {
        whenever(authService.hasPermission("MANAGE_WAREHOUSES")).thenReturn(false)

        val result = service.createGudang(gudang())

        assertTrue(result.isFailure)
        verify(warehouseRepository, never()).createGudang(any())
    }

    @Test
    fun `createGudang surfaces a repository failure`() = runTest {
        whenever(warehouseRepository.createGudang(any()))
            .thenReturn(Result.failure(Exception("Nama gudang sudah digunakan")))

        val result = service.createGudang(gudang())

        assertTrue(result.isFailure)
    }

    @Test
    fun `createGudang returns the freshly stored warehouse`() = runTest {
        val stored = gudang(id = 42L)
        whenever(warehouseRepository.createGudang(any())).thenReturn(Result.success(42L))
        whenever(warehouseRepository.getGudangById(42L)).thenReturn(Result.success(stored))

        val result = service.createGudang(stored)

        assertEquals(stored, result.getOrNull())
    }

    @Test
    fun `deleteGudang is denied without MANAGE_WAREHOUSES`() = runTest {
        whenever(authService.hasPermission("MANAGE_WAREHOUSES")).thenReturn(false)

        val result = service.deleteGudang(1L)

        assertTrue(result.isFailure)
        verify(warehouseRepository, never()).deleteGudang(any())
    }

    @Test
    fun `getGudangs is denied without VIEW_INVENTORY`() = runTest {
        whenever(authService.hasPermission("VIEW_INVENTORY")).thenReturn(false)

        val result = service.getGudangs()

        assertTrue(result.isFailure)
        verify(warehouseRepository, never()).getAllGudang()
    }

    @Test
    fun `getGudangs returns the repository flow contents`() = runTest {
        whenever(warehouseRepository.getAllGudang()).thenReturn(flowOf(listOf(gudang(), gudang(2L, "B"))))

        val result = service.getGudangs()

        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun `transferStok rejects a non-positive quantity`() = runTest {
        val result = service.transferStok(produkId = 1L, dariGudangId = 1L, keGudangId = 2L, jumlah = 0)

        assertTrue(result.isFailure)
    }

    @Test
    fun `transferStok rejects the same source and destination`() = runTest {
        val result = service.transferStok(produkId = 1L, dariGudangId = 1L, keGudangId = 1L, jumlah = 5)

        assertTrue(result.isFailure)
    }

    @Test
    fun `transferStok is denied without MANAGE_WAREHOUSES`() = runTest {
        whenever(authService.hasPermission("MANAGE_WAREHOUSES")).thenReturn(false)

        val result = service.transferStok(1L, 1L, 2L, 5)

        assertTrue(result.isFailure)
    }

    @Test
    fun `transferStok moves stock between warehouses`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        val categoryId = database.kategoriDao().insertKategori(Kategori(name = "Kat"))
        val source = database.gudangDao().insertGudang(Gudang(name = "Sumber"))
        val target = database.gudangDao().insertGudang(Gudang(name = "Tujuan"))
        val product = produk(warehouseId = source, stock = 10)
            .copy(id = 0, categoryId = categoryId)
        val productId = database.produkDao().insertProduk(product)
        database.stokGudangDao().insertOrUpdateStock(StokGudang(productId = productId, warehouseId = source, quantity = 10))

        val real = WarehouseServiceImpl(
            GudangRepository(database.gudangDao(), database.produkDao()),
            ProdukRepository(database.produkDao()),
            StokGudangRepository(database.stokGudangDao(), database.produkDao()),
            authService,
            database
        )

        val result = real.transferStok(productId, source, target, 4)

        assertTrue(result.isSuccess)
        assertEquals(6, database.stokGudangDao().getStock(productId, source)?.quantity)
        assertEquals(4, database.stokGudangDao().getStock(productId, target)?.quantity)
        database.close()
    }

    @Test
    fun `transferStok rejects insufficient source stock`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        val categoryId = database.kategoriDao().insertKategori(Kategori(name = "Kat"))
        val source = database.gudangDao().insertGudang(Gudang(name = "Sumber"))
        val target = database.gudangDao().insertGudang(Gudang(name = "Tujuan"))
        val productId = database.produkDao().insertProduk(
            produk(warehouseId = source, stock = 3).copy(id = 0, categoryId = categoryId)
        )
        database.stokGudangDao().insertOrUpdateStock(StokGudang(productId = productId, warehouseId = source, quantity = 3))

        val real = WarehouseServiceImpl(
            GudangRepository(database.gudangDao(), database.produkDao()),
            ProdukRepository(database.produkDao()),
            StokGudangRepository(database.stokGudangDao(), database.produkDao()),
            authService,
            database
        )

        val result = real.transferStok(productId, source, target, 4)

        assertTrue(result.isFailure)
        assertEquals(3, database.stokGudangDao().getStock(productId, source)?.quantity)
        database.close()
    }

    @Test
    fun `getSemuaStokGudang maps each warehouse to its products`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        val categoryId = database.kategoriDao().insertKategori(Kategori(name = "Kat"))
        val warehouse = database.gudangDao().insertGudang(Gudang(name = "Gudang"))
        val productId = database.produkDao().insertProduk(
            produk(warehouseId = warehouse, stock = 5).copy(id = 0, categoryId = categoryId)
        )
        database.stokGudangDao().insertOrUpdateStock(StokGudang(productId = productId, warehouseId = warehouse, quantity = 5))

        val real = WarehouseServiceImpl(
            GudangRepository(database.gudangDao(), database.produkDao()),
            ProdukRepository(database.produkDao()),
            StokGudangRepository(database.stokGudangDao(), database.produkDao()),
            authService,
            database
        )

        val result = real.getSemuaStokGudang()

        val map = result.getOrNull()!!
        assertEquals(1, map.size)
        assertEquals(1, map.values.first().size)
        database.close()
    }

    @Test
    fun `tugaskanProdukKeGudang keeps the product when the warehouse is missing`() = runTest {
        whenever(warehouseRepository.getGudangById(99L))
            .thenReturn(Result.failure(Exception("Gudang tidak ditemukan")))

        val result = service.tugaskanProdukKeGudang(produkId = 1L, gudangId = 99L)

        assertTrue(result.isFailure)
        verify(productRepository, never()).updateProduk(any())
    }

    @Test
    fun `tugaskanProdukKeGudang reassigns the product warehouse`() = runTest {
        whenever(warehouseRepository.getGudangById(2L)).thenReturn(Result.success(gudang(2L, "B")))
        whenever(productRepository.getProdukById(1L))
            .thenReturn(Result.success(produk(warehouseId = 1L)))
        whenever(productRepository.updateProduk(any())).thenReturn(Result.success(Unit))

        val result = service.tugaskanProdukKeGudang(produkId = 1L, gudangId = 2L)

        assertTrue(result.isSuccess)
        verify(productRepository).updateProduk(any())
    }
}
