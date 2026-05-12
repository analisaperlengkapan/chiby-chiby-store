package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.ItemPembelianRepository
import com.chibychibystore.repository.PembelianRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.impl.PurchaseServiceImpl
import com.chibychibystore.testutils.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseServiceTest : BaseTest() {

    @Mock
    private lateinit var db: ChibyChibyDatabase
    @Mock
    private lateinit var pembelianRepository: PembelianRepository
    @Mock
    private lateinit var itemPembelianRepository: ItemPembelianRepository
    @Mock
    private lateinit var stokGudangRepository: StokGudangRepository

    private lateinit var purchaseService: PurchaseService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        purchaseService = PurchaseServiceImpl(
            db,
            pembelianRepository,
            itemPembelianRepository,
            stokGudangRepository
        )
    }

    @Test
    fun `createPembelian should return error when items are empty`() = runTest {
        val pembelian = Pembelian(supplierId = 1L, invoiceNumber = "INV-001", totalAmount = 0.0)
        val result = purchaseService.createPembelian(pembelian, emptyList())
        assertTrue(result is Result.Failure)
    }
}
