package com.chibychibystore.service

import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.repository.GudangRepository
import com.chibychibystore.repository.ProdukRepository
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.impl.WarehouseServiceImpl
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class WarehouseServiceTest {

    @Mock private lateinit var warehouseRepository: GudangRepository
    @Mock private lateinit var productRepository: ProdukRepository
    @Mock private lateinit var stokGudangRepository: StokGudangRepository
    @Mock private lateinit var authService: AuthService
    @Mock private lateinit var db: ChibyChibyDatabase

    private lateinit var warehouseService: WarehouseService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        warehouseService = WarehouseServiceImpl(
            warehouseRepository,
            productRepository,
            stokGudangRepository,
            authService,
            db
        )
    }

    @Test
    fun `getGudang should return data from repository`() = runTest {
        val id = 1L
        `when`(warehouseRepository.getGudangById(id)).thenReturn(Result.failure(Exception("Not found")))

        val result = warehouseService.getGudang(id)

        assertTrue(result.isFailure)
    }
}
