package com.chibychibystore.service

import android.content.Context
import com.chibychibystore.data.Result
import com.chibychibystore.data.model.*
import com.chibychibystore.repository.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

class RestoreServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var penggunaRepository: PenggunaRepository

    @Mock
    private lateinit var kategoriRepository: KategoriRepository

    @Mock
    private lateinit var gudangRepository: GudangRepository

    @Mock
    private lateinit var produkRepository: ProdukRepository

    @Mock
    private lateinit var pemasokRepository: PemasokRepository

    @Mock
    private lateinit var penjualanRepository: PenjualanRepository

    @Mock
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    private lateinit var restoreService: RestoreService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        restoreService = RestoreServiceImpl(
            context,
            penggunaRepository,
            kategoriRepository,
            gudangRepository,
            produkRepository,
            pemasokRepository,
            penjualanRepository,
            itemPenjualanRepository,
            pengeluaranRepository
        )
    }

    @Test
    fun `restoreFromBackup should import all data successfully`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // Mock successful repository operations
        `when`(penggunaRepository.insertPengguna(any())).thenReturn(1L)
        `when`(kategoriRepository.insertKategori(any())).thenReturn(1L)
        `when`(gudangRepository.insertGudang(any())).thenReturn(1L)
        `when`(produkRepository.insertProduk(any())).thenReturn(1L)
        `when`(pemasokRepository.insertPemasok(any())).thenReturn(1L)
        `when`(penjualanRepository.insertPenjualan(any())).thenReturn(1L)
        `when`(itemPenjualanRepository.insertItemPenjualan(any())).thenReturn(1L)
        `when`(pengeluaranRepository.insertPengeluaran(any())).thenReturn(1L)

        // When
        val result = restoreService.restoreFromBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertTrue(success)
    }

    @Test
    fun `restoreFromBackup should return error for invalid backup file`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(false)

        // When
        val result = restoreService.restoreFromBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak valid") == true)
    }

    @Test
    fun `restoreFromBackup should return error when repository operation fails`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // Mock repository failure
        `when`(penggunaRepository.insertPengguna(any()))
            .thenThrow(RuntimeException("Database constraint violation"))

        // When
        val result = restoreService.restoreFromBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("Database constraint violation", error.exception.message)
    }

    @Test
    fun `getRestorePreview should return backup contents summary`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // When
        val result = restoreService.getRestorePreview(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val preview = (result as Result.Success).data
        assertNotNull(preview)

        // Verify preview contains expected data structure
        assertTrue(preview.containsKey("totalUsers"))
        assertTrue(preview.containsKey("totalCategories"))
        assertTrue(preview.containsKey("totalWarehouses"))
        assertTrue(preview.containsKey("totalProducts"))
        assertTrue(preview.containsKey("totalSuppliers"))
        assertTrue(preview.containsKey("totalSales"))
        assertTrue(preview.containsKey("totalSaleItems"))
        assertTrue(preview.containsKey("totalExpenses"))
    }

    @Test
    fun `getRestorePreview should return error for invalid backup file`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(false)

        // When
        val result = restoreService.getRestorePreview(mockBackupFile)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak valid") == true)
    }

    @Test
    fun `partialRestore should restore data within date range`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        val startDate = java.time.LocalDate.of(2025, 1, 1)
        val endDate = java.time.LocalDate.of(2025, 1, 31)

        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // Mock successful repository operations
        `when`(penggunaRepository.insertPengguna(any())).thenReturn(1L)
        `when`(kategoriRepository.insertKategori(any())).thenReturn(1L)
        `when`(gudangRepository.insertGudang(any())).thenReturn(1L)
        `when`(produkRepository.insertProduk(any())).thenReturn(1L)
        `when`(pemasokRepository.insertPemasok(any())).thenReturn(1L)
        `when`(penjualanRepository.insertPenjualan(any())).thenReturn(1L)
        `when`(itemPenjualanRepository.insertItemPenjualan(any())).thenReturn(1L)
        `when`(pengeluaranRepository.insertPengeluaran(any())).thenReturn(1L)

        // When
        val result = restoreService.partialRestore(mockBackupFile, startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertTrue(success)
    }

    @Test
    fun `partialRestore should filter data by date range correctly`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        val startDate = java.time.LocalDate.of(2025, 1, 15)
        val endDate = java.time.LocalDate.of(2025, 1, 31)

        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // Mock successful repository operations
        `when`(penggunaRepository.insertPengguna(any())).thenReturn(1L)
        `when`(kategoriRepository.insertKategori(any())).thenReturn(1L)
        `when`(gudangRepository.insertGudang(any())).thenReturn(1L)
        `when`(produkRepository.insertProduk(any())).thenReturn(1L)
        `when`(pemasokRepository.insertPemasok(any())).thenReturn(1L)
        `when`(penjualanRepository.insertPenjualan(any())).thenReturn(1L)
        `when`(itemPenjualanRepository.insertItemPenjualan(any())).thenReturn(1L)
        `when`(pengeluaranRepository.insertPengeluaran(any())).thenReturn(1L)

        // When
        val result = restoreService.partialRestore(mockBackupFile, startDate, endDate)

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertTrue(success)
    }

    @Test
    fun `partialRestore should return error for invalid date range`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        val startDate = java.time.LocalDate.of(2025, 1, 31) // End before start
        val endDate = java.time.LocalDate.of(2025, 1, 1)

        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // When
        val result = restoreService.partialRestore(mockBackupFile, startDate, endDate)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tanggal") == true)
    }

    @Test
    fun `restoreFromBackup should handle corrupted backup data gracefully`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // Mock repository to throw exception during restore
        `when`(produkRepository.insertProduk(any()))
            .thenThrow(RuntimeException("Invalid product data"))

        // When
        val result = restoreService.restoreFromBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("Invalid product data", error.exception.message)
    }

    @Test
    fun `getRestorePreview should handle empty backup file correctly`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)
        `when`(mockBackupFile.length()).thenReturn(0L)

        // When
        val result = restoreService.getRestorePreview(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val preview = (result as Result.Success).data

        // Verify all counts are zero for empty backup
        assertEquals(0, preview["totalUsers"])
        assertEquals(0, preview["totalCategories"])
        assertEquals(0, preview["totalWarehouses"])
        assertEquals(0, preview["totalProducts"])
        assertEquals(0, preview["totalSuppliers"])
        assertEquals(0, preview["totalSales"])
        assertEquals(0, preview["totalSaleItems"])
        assertEquals(0, preview["totalExpenses"])
    }

    @Test
    fun `restoreFromBackup should rollback on partial failure`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)

        // Mock successful operations first, then failure
        `when`(penggunaRepository.insertPengguna(any())).thenReturn(1L)
        `when`(kategoriRepository.insertKategori(any())).thenReturn(1L)
        `when`(produkRepository.insertProduk(any()))
            .thenThrow(RuntimeException("Constraint violation"))

        // When
        val result = restoreService.restoreFromBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("Constraint violation", error.exception.message)
    }
}