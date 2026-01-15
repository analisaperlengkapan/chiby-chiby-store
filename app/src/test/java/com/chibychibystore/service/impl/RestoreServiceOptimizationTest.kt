package com.chibychibystore.service.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class RestoreServiceOptimizationTest {

    private lateinit var db: ChibyChibyDatabase
    @Mock private lateinit var context: Context
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())

        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).allowMainThreadQueries().build()

        restoreService = RestoreServiceImpl(
            context,
            db,
            userRepository,
            categoryRepository,
            warehouseRepository,
            productRepository,
            supplierRepository,
            saleRepository,
            saleItemRepository,
            purchaseRepository,
            purchaseItemRepository,
            expenseRepository
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `restorePengeluarans should insert expenses in batch (optimized)`() = runTest {
        // Arrange
        val expenses = List(10) { index ->
            Pengeluaran(
                id = index.toLong(),
                expenseDate = Date(),
                category = KategoriPengeluaran.UTILITIES,
                amount = 1000.0,
                createdBy = 1L
            )
        }

        `when`(expenseRepository.createPengeluarans(anyList())).thenReturn(Result.success(10))

        // Act
        // Use reflection to call private method
        val method = RestoreServiceImpl::class.java.getDeclaredMethod("restorePengeluarans", List::class.java)
        method.isAccessible = true
        val count = method.invoke(restoreService, expenses) as Int

        // Assert
        assertEquals(10, count)
        verify(expenseRepository, times(1)).createPengeluarans(anyList())
        verify(expenseRepository, never()).createPengeluaran(any())
    }
}
