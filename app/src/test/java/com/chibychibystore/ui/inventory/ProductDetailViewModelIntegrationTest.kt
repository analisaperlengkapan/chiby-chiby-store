package com.chibychibystore.ui.inventory
import org.robolectric.annotation.Config

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.KategoriRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.service.ProductServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*
import com.chibychibystore.testutils.BaseTest

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var productService: ProductServiceImpl

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val kategoriRepo = KategoriRepository(db.kategoriDao())
        val gudangRepo = GudangRepository(db.gudangDao())
        val produkRepo = ProdukRepository(db.produkDao())

        productService = ProductServiceImpl(produkRepo)

        // Create supporting master data
        runTest {
            kategoriRepo.createKategori(Kategori(name = "Default"))
            gudangRepo.createGudang(Gudang(name = "Main", location = "Jakarta", capacity = 100))
        }
    }

    @After
    fun teardown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun create_update_updateStock_delete_product_flow() = runTest(testDispatcher) {
        val kategoriId = db.kategoriDao().getAllKategori().first().first().id
        val gudangId = db.gudangDao().getAllGudang().first().first().id

        // Create ViewModel in 'create' mode (no productId)
        val vmCreate = ProductDetailViewModel(productService, SavedStateHandle())

        val newProduct = Produk(
            id = 0,
            name = "NewProd",
            barcode = "8990001",
            categoryId = kategoriId,
            costPrice = 10000.0,
            sellingPrice = 15000.0,
            stockQuantity = 5,
            warehouseId = gudangId,
            createdAt = Date(),
            updatedAt = Date()
        )

        vmCreate.saveProduct(newProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        // After save, product should have been created and success message set
        val created = vmCreate.uiState.value.product
        assertNotNull(created)
        assertTrue(created!!.id > 0)
        assertEquals("Produk berhasil dibuat", vmCreate.uiState.value.successMessage)

        // Now test update flow: instantiate VM with productId
        val idStr = created.id.toString()
        val vmEdit = ProductDetailViewModel(productService, SavedStateHandle(mapOf("productId" to idStr)))
        testDispatcher.scheduler.advanceUntilIdle()

        // Should load product into state
        val loaded = vmEdit.uiState.value.product
        assertNotNull(loaded)
        assertEquals(created.id, loaded!!.id)

        // Update product name and save
        val updatedProduct = loaded.copy(name = "UpdatedName")
        vmEdit.updateLocalProduct(updatedProduct)
        vmEdit.saveProduct(updatedProduct)
        testDispatcher.scheduler.advanceUntilIdle()

        val afterUpdate = vmEdit.uiState.value.product
        assertEquals("UpdatedName", afterUpdate?.name)
        assertEquals("Produk berhasil diperbarui", vmEdit.uiState.value.successMessage)

        // Update stock
        vmEdit.adjustStock(10)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Stok berhasil diperbarui", vmEdit.uiState.value.successMessage)

        // Delete product
        vmEdit.deleteProduct()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Produk berhasil dihapus", vmEdit.uiState.value.successMessage)
    }
}
