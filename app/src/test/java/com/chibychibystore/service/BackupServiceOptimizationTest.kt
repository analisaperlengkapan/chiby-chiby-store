package com.chibychibystore.service

import android.content.Context
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.*
import com.chibychibystore.service.impl.BackupServiceImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class BackupServiceOptimizationTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Mock private lateinit var context: Context

    @Mock private lateinit var userRepository: PenggunaRepository
    @Mock private lateinit var categoryRepository: KategoriRepository
    @Mock private lateinit var warehouseRepository: GudangRepository
    @Mock private lateinit var productRepository: ProdukRepository
    @Mock private lateinit var supplierRepository: PemasokRepository
    @Mock private lateinit var saleRepository: PenjualanRepository
    @Mock private lateinit var itemPenjualanRepository: ItemPenjualanRepository
    @Mock private lateinit var purchaseRepository: PembelianRepository
    @Mock private lateinit var itemPembelianRepository: ItemPembelianRepository
    @Mock private lateinit var expenseRepository: PengeluaranRepository

    private lateinit var backupService: BackupServiceImpl
    private lateinit var backupDir: File
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        // MockitoAnnotations.openMocks(this) // handled by runner

        backupDir = tempFolder.newFolder("ChibyChibyBackup")
        cacheDir = tempFolder.newFolder("cache")
        `when`(context.cacheDir).thenReturn(cacheDir)

        // Mock Repositories to return empty flows by default
        `when`(userRepository.getAllUsers()).thenReturn(flowOf(emptyList()))
        `when`(categoryRepository.getAllKategori()).thenReturn(flowOf(emptyList()))
        `when`(warehouseRepository.getAllGudang()).thenReturn(flowOf(emptyList()))
        `when`(productRepository.getAllProduk()).thenReturn(flowOf(emptyList()))
        `when`(supplierRepository.getAllPemasok()).thenReturn(flowOf(emptyList()))
        `when`(saleRepository.getAllPenjualan()).thenReturn(flowOf(emptyList()))
        `when`(itemPenjualanRepository.getAllSaleItems()).thenReturn(flowOf(emptyList()))
        `when`(purchaseRepository.getAllPurchases()).thenReturn(flowOf(emptyList()))
        `when`(itemPembelianRepository.getAllPurchaseItems()).thenReturn(flowOf(emptyList()))
        `when`(expenseRepository.getAllPengeluarans()).thenReturn(flowOf(emptyList()))

        backupService = BackupServiceImpl(
            context,
            userRepository,
            categoryRepository,
            warehouseRepository,
            productRepository,
            supplierRepository,
            saleRepository,
            itemPenjualanRepository,
            purchaseRepository,
            itemPembelianRepository,
            expenseRepository
        )

        // Override backup directory
        backupService.setBackupDirectoryForTest(backupDir)
    }

    @Test
    fun `createBackup should create a valid backup file with streamed content`() = runTest {
        // Given
        val user = Pengguna(id = 1, username = "test", passwordHash = "hash", role = Role.OWNER)
        `when`(userRepository.getAllUsers()).thenReturn(flowOf(listOf(user)))

        // When
        val result = backupService.createBackup()

        // Then
        assertTrue("Backup failed: " + (result as? Result.Failure)?.exception?.message, result is Result.Success)
        val backupInfo = (result as Result.Success).data
        val file = File(backupInfo.filePath)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        // Verify Content
        val validation = backupService.validateBackup(file.absolutePath)
        assertTrue("Validation failed: " + (validation as? Result.Success)?.data?.errors, validation is Result.Success)
        val validationData = (validation as Result.Success).data
        assertTrue("Validation isValid is false", validationData.isValid)
        assertEquals(1, validationData.recordCounts?.get("pengguna"))
        assertEquals("1.0", validationData.version)
    }

    @Test
    fun `validateBackup should pass for valid compact backup`() = runTest {
         // Given
        val user = Pengguna(id = 1, username = "test", passwordHash = "hash", role = Role.OWNER)
        `when`(userRepository.getAllUsers()).thenReturn(flowOf(listOf(user)))
        val result = backupService.createBackup()
        val backupFile = File((result as Result.Success).data.filePath)

        // When
        val validation = backupService.validateBackup(backupFile.absolutePath)

        // Then
        assertTrue(validation is Result.Success)
        assertTrue((validation as Result.Success).data.isValid)
    }
}
