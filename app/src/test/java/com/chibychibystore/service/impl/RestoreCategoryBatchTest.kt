package com.chibychibystore.service.impl

import android.content.Context
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class RestoreCategoryBatchTest {

    @Mock private lateinit var context: Context
    @Mock private lateinit var database: ChibyChibyDatabase
    @Mock private lateinit var penggunaRepository: PenggunaRepository
    @Mock private lateinit var kategoriRepository: KategoriRepository
    @Mock private lateinit var gudangRepository: GudangRepository
    @Mock private lateinit var produkRepository: ProdukRepository
    @Mock private lateinit var pemasokRepository: PemasokRepository
    @Mock private lateinit var penjualanRepository: PenjualanRepository
    @Mock private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    @Mock private lateinit var purchaseRepository: PembelianRepository
    @Mock private lateinit var purchaseItemRepository: ItemPembelianRepository
    @Mock private lateinit var expenseRepository: PengeluaranRepository
    @Mock private lateinit var shiftRepository: ShiftRepository
    @Mock private lateinit var pelangganRepository: PelangganRepository
    @Mock private lateinit var stokGudangRepository: StokGudangRepository
    @Mock private lateinit var inventoryAuditRepository: InventoryAuditRepository

    private lateinit var restoreService: RestoreServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        restoreService = RestoreServiceImpl(
            context,
            database,
            Dispatchers.Unconfined,
            penggunaRepository,
            kategoriRepository,
            gudangRepository,
            produkRepository,
            pemasokRepository,
            penjualanRepository,
            itemPenjualanRepository,
            purchaseRepository,
            purchaseItemRepository,
            expenseRepository,
            shiftRepository,
            pelangganRepository,
            stokGudangRepository,
            inventoryAuditRepository
        )
    }

    @Test
    fun `restoreCategories should use batch insertion`() = runTest {
        val categories = listOf(
            Kategori(id = 1, name = "Cat 1"),
            Kategori(id = 2, name = "Cat 2")
        )

        // Mock repository response
        `when`(kategoriRepository.createKategoriList(anyList())).thenReturn(Result.success(2))

        // Call the internal method directly
        restoreService.restoreCategories(categories)

        // Verify that createKategoriList was called once with the correct list
        verify(kategoriRepository, times(1)).createKategoriList(categories)
        // Verify that createKategori (single) was NOT called
        verify(kategoriRepository, never()).createKategori(any())
    }
}
