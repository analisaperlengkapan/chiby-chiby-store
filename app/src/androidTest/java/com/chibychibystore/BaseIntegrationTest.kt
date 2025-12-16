package com.chibychibystore

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import javax.inject.Inject

/**
 * Base class for integration tests with in-memory database
 */
abstract class BaseIntegrationTest {

    lateinit var database: ChibyChibyDatabase

    @Inject
    lateinit var penggunaRepository: PenggunaRepository

    @Inject
    lateinit var kategoriRepository: KategoriRepository

    @Inject
    lateinit var gudangRepository: GudangRepository

    @Inject
    lateinit var produkRepository: ProdukRepository

    @Inject
    lateinit var pemasokRepository: PemasokRepository

    @Inject
    lateinit var pengeluaranRepository: PengeluaranRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Seed basic test data for integration tests
     */
    protected suspend fun seedTestData() {
        // Seed users
        val owner = Pengguna(
            username = "owner",
            passwordHash = "hash",
            role = Role.OWNER
        )
        penggunaRepository.createPengguna(owner)

        // Seed categories
        val foodCategory = Kategori(id = 0, name = "Makanan", description = "Kategori makanan")
        kategoriRepository.createKategori(foodCategory)

        // Seed warehouses
        val warehouse = Gudang(id = 0, name = "Gudang Utama", location = "Jakarta")
        gudangRepository.createGudang(warehouse)

        // Seed suppliers
        val supplier = Pemasok(id = 0, name = "PT Supplier", contact = "08123456789", address = "Jakarta")
        pemasokRepository.createPemasok(supplier)

        // Seed products
        val product = Produk(
            id = 0,
            name = "Nasi Goreng",
            barcode = "123456789012",
            categoryId = 1,
            warehouseId = 1,
            costPrice = 15000.0,
            sellingPrice = 20000.0,
            stockQuantity = 100,
            minStock = 10
        )
        produkRepository.createProduk(product)
    }

    /**
     * Clear all test data
     */
    protected suspend fun clearTestData() {
        runBlocking {
            database.clearAllTables()
        }
    }
}